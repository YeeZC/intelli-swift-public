package com.fr.swift.adaptor.log;

import com.fr.decision.log.LogSearchConstants;
import com.fr.decision.log.LogSearchProvider;
import com.fr.decision.log.MetricBean;
import com.fr.log.message.AbstractMessage;
import com.fr.stable.StringUtils;
import com.fr.stable.query.condition.QueryCondition;
import com.fr.stable.query.data.DataList;
import com.fr.swift.db.Table;
import com.fr.swift.db.impl.SwiftDatabase;
import com.fr.swift.log.SwiftLogger;
import com.fr.swift.log.SwiftLoggers;
import com.fr.swift.query.aggregator.AggregatorFactory;
import com.fr.swift.query.aggregator.AggregatorType;
import com.fr.swift.query.filter.info.FilterInfo;
import com.fr.swift.query.info.bean.query.QueryInfoBeanFactory;
import com.fr.swift.query.info.element.dimension.Dimension;
import com.fr.swift.query.info.element.metric.GroupMetric;
import com.fr.swift.query.info.element.metric.Metric;
import com.fr.swift.query.info.group.GroupQueryInfo;
import com.fr.swift.query.info.group.GroupQueryInfoImpl;
import com.fr.swift.query.info.group.post.PostQueryInfo;
import com.fr.swift.query.query.QueryBean;
import com.fr.swift.query.query.QueryRunnerProvider;
import com.fr.swift.segment.column.ColumnKey;
import com.fr.swift.source.Row;
import com.fr.swift.source.SourceKey;
import com.fr.swift.source.SwiftResultSet;
import com.fr.swift.structure.iterator.IteratorUtils;
import com.fr.swift.structure.iterator.MapperIterator;
import com.fr.swift.util.Crasher;
import com.fr.swift.util.function.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Lyon on 2018/6/21.
 */
public class SwiftLogSearchProvider implements LogSearchProvider {

    private static final SwiftLogger LOGGER = SwiftLoggers.getLogger(SwiftLogSearchProvider.class);

    private static LogSearchProvider instance = new SwiftLogSearchProvider();

    public static LogSearchProvider getInstance() {
        return instance;
    }

    private SwiftLogSearchProvider() {
    }

    @Override
    public String getMarkString() {
        return LogSearchConstants.PROVIDER_MARK;
    }

    @Override
    public int count(Class<? extends AbstractMessage> logClass, QueryCondition condition) throws Exception {
        return countQuery(logClass, condition, "");
    }

    @Override
    public int countByColumn(Class<? extends AbstractMessage> logClass, QueryCondition condition, String columnName) throws Exception {
        // TODO: 2018/6/21 这个和count表没什么区别
        return countQuery(logClass, condition, "");
    }

    @Override
    public int distinctByColumn(Class<? extends AbstractMessage> logClass, QueryCondition condition, String columnName) throws Exception {
        return countQuery(logClass, condition, columnName);
    }

    @Override
    public List<Object> getValueByColumn(Class<? extends AbstractMessage> logClass, QueryCondition condition, String columnName) throws Exception {
        List<String> fieldNames = new ArrayList<String>();
        fieldNames.add(columnName);
        List<Row> rows = LogQueryUtils.detailQuery(logClass, condition, fieldNames).getList();
        return IteratorUtils.iterator2List(new MapperIterator<Row, Object>(rows.iterator(), new Function<Row, Object>() {
            @Override
            public Object apply(Row p) {
                return p.getValue(0);
            }
        }));
    }

    @Override
    public List<Object> getDistinctValueByColumn(Class<? extends AbstractMessage> logClass, QueryCondition condition, String columnName) throws Exception {
        List<String> fieldNames = new ArrayList<String>();
        fieldNames.add(columnName);
        List<Row> rows = LogQueryUtils.groupQuery(logClass, condition, fieldNames, new ArrayList<MetricBean>());
        return IteratorUtils.iterator2List(new MapperIterator<Row, Object>(rows.iterator(), new Function<Row, Object>() {
            @Override
            public Object apply(Row p) {
                return p.getValue(0);
            }
        }));
    }

    @Override
    public DataList<Map<String, Object>> groupByColumn(Class<? extends AbstractMessage> logClass, QueryCondition condition, List<MetricBean> metrics, String columnName) throws Exception {
        List<String> fieldNames = new ArrayList<String>();
        fieldNames.add(columnName);
        return groupByColumns(logClass, condition, metrics, fieldNames);
    }

    @Override
    public DataList<Map<String, Object>> groupByColumns(Class<? extends AbstractMessage> logClass, QueryCondition condition, List<MetricBean> metrics, List<String> fieldNames) throws Exception {
        final List<Row> rows = LogQueryUtils.groupQuery(logClass, condition, fieldNames, metrics);
        final List<String> columnNames = new ArrayList<String>(fieldNames);
        for (MetricBean bean : metrics) {
            columnNames.add(bean.getName());
        }
        List<Map<String, Object>> maps = IteratorUtils.iterator2List(new MapperIterator<Row, Map<String, Object>>(rows.iterator(), new Function<Row, Map<String, Object>>() {
            @Override
            public Map<String, Object> apply(Row p) {
                Map<String, Object> map = new HashMap<String, Object>();
                for (int i = 0; i < p.getSize(); i++) {
                    map.put(columnNames.get(i), p.getValue(i));
                }
                return map;
            }
        }));
        DataList<Map<String, Object>> dataList = new DataList<Map<String, Object>>();
        // TODO: 2018/6/21 这个totalCount不会是总行数吧？
        dataList.setTotalCount(maps.size());
        dataList.setList(maps);
        return dataList;
    }

    private static int countQuery(Class<? extends AbstractMessage> logClass, QueryCondition condition, String columnName) throws Exception {
        FilterInfo filterInfo = QueryConditionAdaptor.restriction2FilterInfo(condition.getRestriction());
        Table table = SwiftDatabase.getInstance().getTable(new SourceKey(SwiftMetaAdaptor.getTableName(logClass)));
        SourceKey sourceKey = table.getSourceKey();
        List<Metric> metrics = new ArrayList<Metric>();
        if (StringUtils.isEmpty(columnName)) {
            metrics.add(new GroupMetric(0, sourceKey, new ColumnKey(""), null, AggregatorFactory.createAggregator(AggregatorType.COUNT)));
        } else {
            metrics.add(new GroupMetric(0, sourceKey, new ColumnKey(columnName), null, AggregatorFactory.createAggregator(AggregatorType.DISTINCT)));
        }
        GroupQueryInfo queryInfo = new GroupQueryInfoImpl("", sourceKey, filterInfo, new ArrayList<Dimension>(),
                metrics, new ArrayList<PostQueryInfo>(0));
        QueryBean queryBean = QueryInfoBeanFactory.create(queryInfo);
        SwiftResultSet resultSet = QueryRunnerProvider.getInstance().executeQuery(queryBean);
        Row row = null;
        if (resultSet.next()) {
            row = resultSet.getRowData();
        }
        if (row != null && row.getSize() == 1) {
            return ((Number) row.getValue(0)).intValue();
        }
        return Crasher.crash(new RuntimeException("failed to count/distinctCount table " + logClass.getName()));
    }
}
