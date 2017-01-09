package com.fr.bi.cal.analyze.cal.sssecret;


import com.finebi.cube.api.ICubeDataLoader;
import com.finebi.cube.api.ICubeTableService;
import com.finebi.cube.api.ICubeValueEntryGetter;
import com.finebi.cube.conf.table.BusinessTable;
import com.finebi.cube.relation.BITableSourceRelation;
import com.fr.bi.cal.analyze.cal.Executor.Executor;
import com.fr.bi.cal.analyze.cal.Executor.ExecutorPartner;
import com.fr.bi.cal.analyze.cal.Executor.ILazyExecutorOperation;
import com.fr.bi.cal.analyze.cal.index.loader.TargetAndKey;
import com.fr.bi.cal.analyze.exception.TerminateExecutorException;
import com.fr.bi.stable.constant.BIBaseConstant;
import com.fr.bi.stable.constant.BIReportConstant;
import com.fr.bi.stable.engine.cal.DimensionIteratorCreator;
import com.fr.bi.stable.gvi.GVIUtils;
import com.fr.bi.stable.gvi.GroupValueIndex;
import com.fr.bi.stable.io.sortlist.ArrayLookupHelper;
import com.fr.bi.stable.report.result.DimensionCalculator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * TODO 需要改成可以半路计算，提升即时计算性能
 * 即可以用之前已经计算好的结果继续计算
 *
 * @author Daniel
 *         分页机制，使用另外一个线程来判断计算当前已经计算了多少结果了 并取数
 */
public class SingleDimensionGroup extends ExecutorPartner implements ILazyExecutorOperation<Entry, MetricMergeResult>, ISingleDimensionGroup {


    private static int demoGroupLimit = BIBaseConstant.PART_DATA_GROUP_LIMIT;

    private BusinessTable[] metricTables;
    private List<TargetAndKey>[] summaryLists;
    private ICubeTableService[] tis;
    private DimensionCalculator[] columns;
    private ICubeValueEntryGetter[] getters;
    private GroupValueIndex[] gvis;
    private Object[] data;
    private boolean useRealData = true;
    private ICubeDataLoader loader;
    private List<MetricMergeResult> metricMergeResultList = new ArrayList<MetricMergeResult>();


    /**
     * Group计算的构造函数
     *
     */
    protected SingleDimensionGroup(BusinessTable[] metricTables, List<TargetAndKey>[] summaryLists, ICubeTableService[] tis, DimensionCalculator[] columns, ICubeValueEntryGetter[] getters,  Object[] data, GroupValueIndex[] gvis, ICubeDataLoader loader, boolean useRealData) {
        this.metricTables = metricTables;
        this.summaryLists = summaryLists;
        this.tis = tis;
        this.columns = columns;
        this.getters = getters;
        this.data = data;
        this.loader = loader;
        this.gvis = gvis;
        this.useRealData = useRealData;
        turnOnExecutor();
    }

    public static SingleDimensionGroup createDimensionGroup(BusinessTable[] metricTables, List<TargetAndKey>[] summaryLists, ICubeTableService[] tis, DimensionCalculator[] columns, ICubeValueEntryGetter[] getters, final Object[] data, GroupValueIndex[] gvis, ICubeDataLoader loader, boolean useRealData) {
        return new SingleDimensionGroup(metricTables, summaryLists, tis, columns, getters, data, gvis, loader, useRealData);
    }


    public void turnOnExecutor() {
        this.lazyExecutor = new Executor();
        Iterator iterator = getIterator();
        this.lazyExecutor.initial(this, iterator);
    }

    protected Iterator getIterator() {
        Iterator[] iterators = new Iterator[metricTables.length];
        for (int i = 0; i < iterators.length; i++){
            Iterator it = getIterator(i);
            if (!columns[i].getDirectToDimensionRelationList().isEmpty()) {
               it = new DirectToDimensionRelationIterator(it, columns[i], loader);
            }
            iterators[i] = it;
        }
        return new MergeIterator(iterators, columns[0].getComparator());
    }

    protected Iterator getIterator(int index) {
        if (gvis[index] == null || GVIUtils.isAllEmptyRoaringGroupValueIndex(gvis[index])){
            return MergeIterator.EMPTY;
        }
        boolean urd = useRealData;
        int groupLimit = demoGroupLimit;
        if (!useRealData){
            long rowCount = tis[index].getRowCount();
            if (rowCount < BIBaseConstant.PART_DATA_COUNT_LIMIT) {
                urd = true;
            } else {
                long groupCount = loader.getTableIndex(columns[index].getField().getTableBelongTo().getTableSource()).loadGroup(columns[index].createKey(), new ArrayList<BITableSourceRelation>()).nonPrecisionSize();
                groupLimit = (int) (groupCount * BIBaseConstant.PART_DATA_COUNT_LIMIT / rowCount);
                groupLimit = Math.min(Math.max(BIBaseConstant.PART_DATA_GROUP_LIMIT, groupLimit), BIBaseConstant.PART_DATA_GROUP_MAX_LIMIT);
            }
        }
        if (!urd || hasSpecialGroup(columns[index])) {
            return columns[index].createValueMapIterator(metricTables[index], loader, urd, groupLimit);
        }
        return getIterByAllCal(index);
    }


    private boolean hasSpecialGroup(DimensionCalculator column) {
        int groupType = column.getGroup().getType();
        if (isCustomGroup(groupType)) {
            return true;
        }
        if (column.getSortType() == BIReportConstant.SORT.CUSTOM) {
            return true;
        }
        return false;
    }

    private boolean isCustomGroup(int groupType) {
        return groupType == BIReportConstant.GROUP.CUSTOM_GROUP
                || groupType == BIReportConstant.GROUP.CUSTOM_NUMBER_GROUP
                || groupType == BIReportConstant.GROUP.AUTO_GROUP;
    }


    private Iterator<Map.Entry<Object, GroupValueIndex>> getIterByAllCal(int index) {
        boolean asc = !(columns[index].getSortType() == BIReportConstant.SORT.DESC || columns[index].getSortType() == BIReportConstant.SORT.NUMBER_DESC);
        return DimensionIteratorCreator.createValueMapIterator(getters[index], gvis[index], asc);
    }

    @Override
    public int getChildIndexByValue(Object value) {
        return ArrayLookupHelper.lookup(new MetricMergeResult[]{new MetricMergeResult(value, null)}, new ArrayLookupHelper.Lookup<MetricMergeResult>() {
            @Override
            public int minIndex() {
                return 0;
            }

            @Override
            public int maxIndex() {
                return metricMergeResultList.size() - 1;
            }

            @Override
            public MetricMergeResult lookupByIndex(int index) {
                return metricMergeResultList.get(index);
            }

            @Override
            public int compare(MetricMergeResult t1, MetricMergeResult t2) {
                return columns[0].getComparator().compare(t1.getData(), t2.getData());
            }
        })[0];
    }

    @Override
    public NoneDimensionGroup getChildDimensionGroup(int row) {
        MetricMergeResult metricMergeResult = getMetricMergeResultByWait(row);
        if (isNull(metricMergeResult)) {
            return NoneDimensionGroup.NULL;
        }
        return NoneDimensionGroup.createDimensionGroup(metricTables, summaryLists, tis, metricMergeResult.getGvis(), loader);
    }

    private boolean isNull(MetricMergeResult node) {
        return node == MetricMergeResult.NULL;
    }

    @Override
    public Object getChildData(int row) {
        MetricMergeResult metricMergeResult = getMetricMergeResultByWait(row);
        return metricMergeResult.getData();
    }

    @Override
    public String getChildShowName(int row) {
        MetricMergeResult metricMergeResult = getMetricMergeResultByWait(row);
        return metricMergeResult.getShowValue();
    }

    protected MetricMergeResult getMetricMergeResultByWait(int row) {
        waitExecutor(row);
        if (row < metricMergeResultList.size()) {
            return metricMergeResultList.get(row);
        } else {
            if (row == 0) {
                MetricMergeResult result = createEmptyResult();
                addMetricMergeResult(result);
                return result;
            } else {
                return MetricMergeResult.NULL;
            }
        }
    }

    @Override
    public MetricMergeResult mainTaskConditions(Entry entry) {
        GroupValueIndex[] values = (GroupValueIndex[]) entry.getValue();
        GroupValueIndex[] result = new GroupValueIndex[gvis.length];
        for (int i = 0; i < gvis.length; i++){
            if (gvis[i] == null || values[i] == null){
                result[i] = null;
            } else {
                result[i] = gvis[i].AND(values[i]);
            }
        }
        return new MetricMergeResult(entry.getKey(), result);
    }

    @Override
    public boolean preJumpCurrentOne(Entry entry) throws TerminateExecutorException {
        return false;
    }

    @Override
    public boolean jumpCurrentOne(MetricMergeResult para) throws TerminateExecutorException {
        for (GroupValueIndex gvi : para.getGvis()){
            if (!indexIsAllEmpty(gvi)){
                return false;
            }
        }
        return true;
    }

    private boolean indexIsAllEmpty(GroupValueIndex gvi) {
        return gvi == null || gvi.isAllEmpty();
    }

    @Override
    public void mainTask(Entry obj, MetricMergeResult metricMergeResult) throws TerminateExecutorException {
        addMetricMergeResult(metricMergeResult);
    }

    private void addMetricMergeResult(MetricMergeResult metricMergeResult){
        metricMergeResultList.add(metricMergeResult);
    }

    @Override
    public void executorTerminated() {
        MetricMergeResult metricMergeResult = createEmptyResult();
        addMetricMergeResult(metricMergeResult);
    }

    private MetricMergeResult createEmptyResult() {
        MetricMergeResult metricMergeResult = new MetricMergeResult(columns[0].createEmptyValue(), gvis);
        return metricMergeResult;
    }

    /**
     * 释放资源，之前需要释放的，现在暂时没有什么需要释放的
     */
    @Override
    public void release() {
    }

    @Override
    public Object[] getData() {
        return data;
    }

}