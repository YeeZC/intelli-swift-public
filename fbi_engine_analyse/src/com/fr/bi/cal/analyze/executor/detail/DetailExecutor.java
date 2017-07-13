package com.fr.bi.cal.analyze.executor.detail;

import com.finebi.cube.api.ICubeDataLoader;
import com.finebi.cube.api.ICubeTableService;
import com.finebi.cube.api.ICubeValueEntryGetter;
import com.finebi.cube.common.log.BILoggerFactory;
import com.finebi.cube.conf.field.BusinessField;
import com.finebi.cube.conf.field.BusinessFieldHelper;
import com.finebi.cube.conf.table.BusinessTable;
import com.fr.bi.base.FinalInt;
import com.fr.bi.cal.analyze.executor.BIEngineExecutor;
import com.fr.bi.cal.analyze.executor.GVIRunner;
import com.fr.bi.cal.analyze.executor.TableRowTraversal;
import com.fr.bi.cal.analyze.executor.detail.execute.DetailAllGVIRunner;
import com.fr.bi.cal.analyze.executor.detail.execute.DetailPartGVIRunner;
import com.fr.bi.cal.analyze.executor.iterator.TableCellIterator;
import com.fr.bi.cal.analyze.executor.paging.Paging;
import com.fr.bi.cal.analyze.executor.table.AbstractTableWidgetExecutor;
import com.fr.bi.cal.analyze.executor.utils.GolbalFilterUtils;
import com.fr.bi.cal.analyze.report.report.widget.AbstractBIWidget;
import com.fr.bi.cal.analyze.report.report.widget.BIDetailWidget;
import com.fr.bi.cal.analyze.report.report.widget.TableWidget;
import com.fr.bi.cal.analyze.session.BISession;
import com.fr.bi.cal.report.engine.CBCell;
import com.fr.bi.conf.report.BIWidget;
import com.fr.bi.conf.report.widget.field.target.detailtarget.BIDetailTarget;
import com.fr.bi.field.target.target.BISummaryTarget;
import com.fr.bi.stable.constant.CellConstant;
import com.fr.bi.stable.data.BIFieldID;
import com.fr.bi.stable.data.db.BIRowValue;
import com.fr.bi.stable.gvi.GVIUtils;
import com.fr.bi.stable.gvi.GroupValueIndex;
import com.fr.bi.stable.utils.BICollectionUtils;
import com.fr.bi.stable.utils.BITravalUtils;
import com.fr.general.DateUtils;
import com.fr.json.JSONArray;
import com.fr.json.JSONObject;
import com.fr.stable.ExportConstants;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO 分页机制待优化
 * 不需要每次都计算 索引然后再取位置取值
 *
 * @author Daniel
 */
public class DetailExecutor extends AbstractDetailExecutor {

    private final static int MEMORY_LIMIT = 3000;

    private final static int EXCEL_ROW_MODE_VALUE = ExportConstants.MAX_ROWS_2007 - 1;

    public DetailExecutor(BIDetailWidget widget,
                          //前台传过来的从1开始;
                          Paging paging,
                          BISession session) {

        super(widget, paging, session);

    }

    public TableCellIterator createCellIterator4Excel() throws Exception {

        final GroupValueIndex gvi = createDetailViewGvi();
        int count = gvi.getRowsCountWithData();
        paging.setTotalSize(count);
        //返回前台的时候再去掉不使用的字段
        final Set<Integer> usedDimensionIndexes = getUsedDimensionIndexes();
        final TableCellIterator iter = new TableCellIterator(usedDimensionIndexes.size(), count + 1);
        new Thread() {

            public void run() {

                try {
                    final FinalInt start = new FinalInt();
                    List<CBCell> cells = createCellTitle(CellConstant.CBCELL.TARGETTITLE_Y, usedDimensionIndexes);
                    Iterator<CBCell> it = cells.iterator();
                    while (it.hasNext()) {
                        iter.getIteratorByPage(start.value).addCell(it.next());
                    }
                    TableRowTraversal action = new TableRowTraversal() {

                        @Override
                        public boolean actionPerformed(BIRowValue row) {

                            int currentRow = (int) row.getRow() + 1;
                            int newRow = currentRow & EXCEL_ROW_MODE_VALUE;
                            if (newRow == 0) {
                                iter.getIteratorByPage(start.value).finish();
                                start.value++;
                            }
                            //row + 1 ? 不然覆盖掉了列名
                            fillOneLine(iter.getIteratorByPage(start.value), newRow, row.getValues(), currentRow, usedDimensionIndexes);
                            return false;
                        }
                    };
                    travel(action, gvi);
                } catch (Exception e) {
                    BILoggerFactory.getLogger().error(e.getMessage(), e);
                } finally {
                    iter.finish();
                }
            }
        }.start();
        return iter;
    }

    @Override
    public Rectangle getSouthEastRectangle() {

        return null;
    }

    @Override
    public JSONObject getCubeNode() throws Exception {

        long start = System.currentTimeMillis();
        GroupValueIndex gvi = createDetailViewGvi();
        paging.setTotalSize(gvi.getRowsCountWithData());
        final JSONArray ja = new JSONArray();
        JSONObject jo = new JSONObject();
        jo.put("value", ja);
        //返回前台的时候再去掉不使用的字段
        final BIDetailTarget[] dimensions = widget.getViewDimensions();
        final Set<Integer> usedDimensionIndexes = getUsedDimensionIndexes();
        TableRowTraversal action = new TableRowTraversal() {

            @Override
            public boolean actionPerformed(BIRowValue row) {

                Boolean x = checkPage(row);
                if (x != null) {
                    return x;
                }
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < row.getValues().length; i++) {
                    if (usedDimensionIndexes.contains(i)) {
                        jsonArray.put(BICollectionUtils.cubeValueToWebDisplay(dimensions[i].createShowValue(row.getValues()[i])));
                    }
                }
                ja.put(jsonArray);
                return false;
            }
        };
        travel(action, gvi);
        BILoggerFactory.getLogger().info(DateUtils.timeCostFrom(start) + ": cal time");
        return jo;
    }

    private Set<Integer> getUsedDimensionIndexes() {

        final BIDetailTarget[] dimensions = widget.getViewDimensions();
        String[] array = widget.getView();
        final Set<Integer> usedDimensionIndexes = new HashSet<Integer>();
        for (int i = 0; i < array.length; i++) {
            BIDetailTarget dimension = BITravalUtils.getTargetByName(array[i], dimensions);
            if (dimension.isUsed()) {
                usedDimensionIndexes.add(i);
            }
        }
        return usedDimensionIndexes;
    }

    protected GroupValueIndex getLinkFilter(GroupValueIndex gvi) {

        try {
            if (widget.getLinkWidget() != null && widget.getLinkWidget() instanceof TableWidget) {
                // 判断两个表格的基础表是否相同
                BusinessTable widgetTargetTable = widget.getTargetDimension();
                TableWidget linkWidget = widget.getLinkWidget();
                Map<String, JSONArray> clicked = widget.getClicked();
                BISummaryTarget summaryTarget = null;
                String[] ids = clicked.keySet().toArray(new String[]{});
                for (String linkTarget : ids) {
                    try {
                        summaryTarget = linkWidget.getBITargetByID(linkTarget);
                        break;
                    } catch (Exception e) {
                        BILoggerFactory.getLogger(TableWidget.class).warn("Target id " + linkTarget + " is absent in linked widget " + linkWidget.getWidgetName());
                    }
                }
                if (summaryTarget != null) {
                    BusinessTable linkTargetTable = summaryTarget.createTableKey();
                    // 基础表相同的时候才有联动的意义
                    if (widgetTargetTable.equals(linkTargetTable)) {
                        // 其联动组件的父联动gvi
                        GroupValueIndex pLinkGvi = linkWidget.createLinkedFilterGVI(widgetTargetTable, session);
                        // 其联动组件的点击过滤gvi
                        GroupValueIndex linkGvi = linkWidget.getLinkFilter(linkWidget, widgetTargetTable, clicked, session);
                        gvi = GVIUtils.AND(gvi, GVIUtils.AND(pLinkGvi, linkGvi));
                    }
                }
            }
        } catch (Exception e) {

        }
        return gvi;
    }

    public List<List> getData() {

        if (target == null) {
            return new ArrayList<List>();
        }
        GroupValueIndex gvi = createDetailViewGvi();
        paging.setTotalSize(gvi.getRowsCountWithData());
        final List<List> data = new ArrayList<List>();
        TableRowTraversal action = new TableRowTraversal() {

            @Override
            public boolean actionPerformed(BIRowValue row) {

                Boolean x = checkPage(row);
                if (x != null) {
                    return x;
                }
                List list = new ArrayList();
                for (int i = 0; i < row.getValues().length; i++) {
                    if (viewDimension[i].isUsed()) {
                        list.add(row.getValues()[i]);
                    }
                }
                data.add(list);
                return false;
            }
        };
        travel(action, gvi);
        return data;
    }

    private Boolean checkPage(BIRowValue row) {

        if (paging.getStartRow() > row.getRow()) {
            return false;
        }
        if (paging.getEndRow() <= row.getRow()) {
            return true;
        }
        return null;
    }

    private void travel(TableRowTraversal action, GroupValueIndex gvi) {

        if (gvi.getRowsCountWithData() < MEMORY_LIMIT) {
            GVIRunner runner = new DetailAllGVIRunner(gvi, widget, getLoader(), userId);
            runner.Traversal(action);
        } else {
            GVIRunner runner = new DetailPartGVIRunner(gvi, session, widget, paging, getLoader());
            runner.Traversal(action);
        }
    }

    @Override
    public JSONObject createJSONObject() throws Exception {

        return getCubeNode();
    }

    protected GroupValueIndex getJumpLinkFilter(GroupValueIndex g) {

        BIDetailWidget bw = (BIDetailWidget) widget;
        // 如果是跳转打开的才需要进行设置
        if (bw.getGlobalFilterWidget() != null) {
            // 如果已经设置了源字段和目标字段
            if (bw.getGlobalFilterWidget().settingSourceAndTargetField()) {
                g = GVIUtils.AND(g, GolbalFilterUtils.getSettingSourceAndTargetJumpFilter(widget, userId, session, target, bw.getGlobalFilterWidget().getBaseTable()));
            } else {
                g = GVIUtils.AND(g, GolbalFilterUtils.getNotSettingSourceAndTargetJumpFilter(session, target, widget, false));
            }
        }
        return g;
    }


}