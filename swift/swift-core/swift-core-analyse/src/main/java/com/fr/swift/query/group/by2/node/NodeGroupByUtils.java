package com.fr.swift.query.group.by2.node;

import com.fr.swift.log.SwiftLoggers;
import com.fr.swift.query.aggregator.AggregatorValue;
import com.fr.swift.query.group.info.GroupByInfo;
import com.fr.swift.query.group.info.MetricInfo;
import com.fr.swift.result.GroupNode;
import com.fr.swift.result.NodeMergeQRS;
import com.fr.swift.result.NodeMergeQRSImpl;
import com.fr.swift.result.SwiftNodeUtils;
import com.fr.swift.structure.iterator.RowTraversal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Lyon on 2018/4/27.
 */
public class NodeGroupByUtils {

    /**
     * 聚合单块segment数据，得到node结果集和压缩的字典值
     *
     * @param groupByInfo 维度相关信息
     * @param metricInfo  指标相关信息
     * @return
     */
    public static Iterator<NodeMergeQRS<GroupNode>> groupBy(GroupByInfo groupByInfo, MetricInfo metricInfo) {
        if (groupByInfo.getDimensions().isEmpty()) {
            // 只有指标的情况
            GroupNode root = new GroupNode(-1, null);
            aggregateRoot(root, groupByInfo.getDetailFilter().createFilterIndex(), metricInfo);
            SwiftLoggers.getLogger().debug("Node Group by result {}", SwiftNodeUtils.node2RowIterator(root).next().toString());
            List<NodeMergeQRS<GroupNode>> list = new ArrayList<NodeMergeQRS<GroupNode>>();
            list.add(new NodeMergeQRSImpl<GroupNode>(groupByInfo.getFetchSize(), root, new ArrayList<Map<Integer, Object>>()));
            return list.iterator();
        }
        return new NodePageIterator(groupByInfo.getFetchSize(), groupByInfo, metricInfo);
    }

    private static void aggregateRoot(GroupNode root, RowTraversal traversal, MetricInfo metricInfo) {
        AggregatorValue[] values = RowMapper.aggregateRow(traversal, metricInfo.getTargetLength(),
                metricInfo.getMetrics(), metricInfo.getAggregators());
        root.setAggregatorValue(values);
    }
}
