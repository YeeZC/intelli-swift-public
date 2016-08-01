package com.fr.bi.cal.analyze.cal.result;import com.fr.bi.cal.analyze.cal.utils.CubeReadingUtils;import com.fr.bi.field.target.key.sum.AvgKey;import com.fr.bi.stable.constant.BIBaseConstant;import com.fr.bi.stable.constant.BIReportConstant;import com.fr.bi.stable.gvi.GroupValueIndex;import com.fr.bi.stable.report.key.TargetGettingKey;import com.fr.bi.stable.report.result.*;import com.fr.general.ComparatorUtils;import com.fr.general.NameObject;import java.util.*;/** * Created by Hiram on 2015/1/27. */public class MergerNode implements IMergerNode {    List<LightNode> children = new ArrayList<LightNode>();    Map<Object, Object> summaryValue = null;    GroupValueIndex[] groupValueIndexArray;    private DimensionCalculator ck;    private LightNode sibling;    private LightNode parent;    private String showValue;    private Comparator comparator = BIBaseConstant.COMPARATOR.COMPARABLE.ASC;    private Object data;    private Map<TargetGettingKey, GroupValueIndex> targetIndexValueMap = null;    private Map<TargetGettingKey, GroupValueIndex> gviMap = new HashMap<TargetGettingKey, GroupValueIndex>(1);    private int topNCacheKey = -9998;    private double topNCacheValue = 0;    private Map<TargetGettingKey, Double> avgValueMap = null;    public MergerNode() {    }    public MergerNode(Object data) {        this.data = data;    }    public static MergerNode copyNode(LightNode node) {        MergerNode copy = new MergerNode();        NodeUtils.copyLightField(copy, node);        if (node instanceof MergerNode) {            MergerNode mergerNode = (MergerNode) node;            if (mergerNode.targetIndexValueMap != null) {                copy.setTargetIndexValueMap(mergerNode.targetIndexValueMap);            }            if (mergerNode.gviMap != null) {                copy.setGroupValueIndexMap(mergerNode.gviMap);            }        } else {            copy.setTargetIndexValueMap(node.getTargetIndexValueMap());            copy.setGroupValueIndexMap(node.getGroupValueIndexMap());        }        return copy;    }    @Override    public int getChildLength() {        if (children == null) {            return 0;        }        return children.size();    }    @Override    public LightNode getChild(int i) {        return children.get(i);    }    @Override    public LightNode getFirstChild() {        if (getChildLength() > 0) {            return children.get(0);        }        return null;    }    @Override    public LightNode getSibling() {        return sibling;    }    @Override    public void setSibling(LightNode sibling) {        this.sibling = sibling;    }    @Override    public LightNode getParent() {        return parent;    }    @Override    public void setParent(LightNode parent) {        this.parent = parent;    }    @Override    public LightNode getChild(Object key) {        for (int i = 0; i < getChildLength(); i++) {            LightNode child = getChild(i);            if (ComparatorUtils.equals(key, getChild(i).getData())) {                return child;            }        }        return null;    }    @Override    public void addChild(LightNode child) {        if (children == null) {            children = new ArrayList<LightNode>();        }        children.add(child);    }    private void initShowValue(DimensionCalculator key, Object data) {        setShowValue(data == null ? null : data.toString());    }    private boolean isTrue() {        return true;    }    @Override    public String getShowValue() {        if (showValue == null) {            initShowValue(ck, data);        }        return showValue;    }    @Override    public void setShowValue(String showValue) {        this.showValue = showValue;    }    @Override    public Comparator getComparator() {        return comparator;    }    @Override    public void setComparator(Comparator comparator) {        this.comparator = comparator;    }    @Override    public Object getData() {        return data;    }    @Override    public void setData(Object data) {        this.data = data;    }    @Override    public void setSummaryValue(Object key, Object value) {        if (summaryValue == null) {            summaryValue = new HashMap<Object, Object>(1);        }        if (value != null) {            value = ((Number) value).doubleValue();        }        summaryValue.put(key, value);    }    @Override    public Number getSummaryValue(Object key) {        if (summaryValue == null) {            return null;        }        /**         * 汇总方式求平均的时候是和计算指标一起最后算的,导致过滤的时候平均值取不出来,这里暂时特殊处理下         */        if(key instanceof TargetGettingKey){            TargetGettingKey targetKey = (TargetGettingKey) key;            if (targetKey.getTargetKey() instanceof AvgKey) {                String targetName = targetKey.getTargetName();                AvgKey avgKey = (AvgKey) targetKey.getTargetKey();                TargetGettingKey sumGettingKey = new TargetGettingKey(avgKey.getSumKey(), targetName);                TargetGettingKey countGettingKey = new TargetGettingKey(avgKey.getCountKey(), targetName);                Number sumValue = this.getSummaryValue(sumGettingKey);                Number countValue = this.getSummaryValue(countGettingKey);                double avgValue = 0;                if (sumValue != null && countValue != null) {                    avgValue = sumValue.doubleValue() / countValue.doubleValue();                }                return avgValue;            }        }        return (Number) summaryValue.get(key);    }    @Override    public GroupValueIndex[] getGroupValueIndexArray() {        return groupValueIndexArray;    }    @Override    public void setGroupValueIndexArray(GroupValueIndex[] groupValueIndexArray) {        this.groupValueIndexArray = groupValueIndexArray;    }    @Override    public Map getSummaryValueMap() {        return summaryValue;    }    @Override    public void setSummaryValueMap(Map summaryValue) {        this.summaryValue = summaryValue;    }    @Override    public Comparable getChildTOPNValueLine(int N) {        int count = this.getChildLength();        if (N < 1 || count == 0) {            return null;        }        return (Comparable) getChild(Math.min(N, count) - 1).getData();    }    @Override    public Comparable getChildBottomNValueLine(int N) {        return getChildTOPNValueLine(this.getChildLength() + 1 - N);    }    @Override    public LightNode getLastChild() {        if (getChildLength() == 0) {            return null;        }        return getChild(getChildLength() - 1);    }    @Override    public Map<TargetGettingKey, GroupValueIndex> getTargetIndexValueMap() {        if (targetIndexValueMap == null) {            return new HashMap<TargetGettingKey, GroupValueIndex>(1);        }        return targetIndexValueMap;    }    @Override    public void setTargetIndexValueMap(Map<TargetGettingKey, GroupValueIndex> targetIndexValueMap) {        this.targetIndexValueMap = targetIndexValueMap;    }    @Override    public Map<TargetGettingKey, GroupValueIndex> getGroupValueIndexMap() {        if (gviMap == null) {            return new HashMap<TargetGettingKey, GroupValueIndex>(1);        }        return this.gviMap;    }    @Override    public void setGroupValueIndexMap(Map<TargetGettingKey, GroupValueIndex> gviMap) {        this.gviMap = gviMap;    }    @Override    public MergerNode createSortedNode(NameObject targetSort,                                       Map<String, TargetGettingKey> targetsMap) {        ISortInfoList sortInfoList = new SortInfoList();        sortInfoList.setPrioritySortInfo(new SortInfo(targetSort.getName(), (Integer) targetSort.getObject()));        return createTargetSortedNode(targetSort, targetsMap, sortInfoList, 0);    }    @Override    public MergerNode createSortedNode(NameObject targetSort,                                       Map<String, TargetCalculator> targetsMap, ISortInfoList sortInfoList, int currentDeep) {        Map<String, TargetGettingKey> keys = new HashMap<String, TargetGettingKey>();        Iterator<Map.Entry<String, TargetCalculator>> it = targetsMap.entrySet().iterator();        while (it.hasNext()) {            Map.Entry<String, TargetCalculator> entry = it.next();            keys.put(entry.getKey(), entry.getValue().createTargetGettingKey());        }        return createTargetSortedNode(targetSort, keys, sortInfoList, 0);    }    @Override    public double getChildTOPNValueLine(TargetGettingKey key, int N) {        if (N < 1) {            return Double.POSITIVE_INFINITY;        }        if (N == topNCacheKey) {            return topNCacheValue;        }        double nLine = NodeUtils.getTopN(this, key, N);        topNCacheKey = N;        topNCacheValue = nLine;        return nLine;    }    public double getChildAVGValue(TargetGettingKey key) {        if (avgValueMap == null) {            avgValueMap = new HashMap();        }        if (summaryValue == null) {            summaryValue = new HashMap();        }        Double d = avgValueMap.get(key);        if (d == null) {            d = NodeUtils.getAVGValue(this, key);            avgValueMap.put(key, d);        }        return d;    }    @Override    public void setCk(DimensionCalculator ck) {        this.ck = ck;    }    private MergerNode createTargetSortedNode(NameObject targetSort,                                              Map<String, TargetGettingKey> targetsMap, ISortInfoList sortInfoList, int currentDeep) {        MergerNode newnode = copyNode(this);        MergerNode tempNode = null;        String sort_target = sortInfoList.getTargetName(currentDeep);        List<LightNode> childNodes = getNodeList();        final TargetGettingKey target_key = sort_target != null ? targetsMap.get(sort_target) : null;        final int sortType = sortInfoList.getSortType(currentDeep);        if (target_key != null && sortInfoList.shouldSort(currentDeep)) {            Collections.sort(childNodes, createComparetor(target_key, sortType));        }        for (int i = 0; i < childNodes.size(); i++) {            MergerNode temp_node = (MergerNode) childNodes.get(i);            MergerNode child = temp_node.createTargetSortedNode(targetSort, targetsMap, sortInfoList, currentDeep + 1);            //清除兄弟关系            temp_node.setSibling(null);            if (tempNode != null) {                CubeReadingUtils.setSibing(tempNode, child);            }            newnode.addChild(child);            tempNode = child;        }        return newnode;    }    private Comparator<LightNode> createComparetor(final TargetGettingKey target_key, final int sortType) {        return new Comparator<LightNode>() {            @Override            public int compare(LightNode o1, LightNode o2) {                Number v1 = o1.getSummaryValue(target_key);                Number v2 = o2.getSummaryValue(target_key);                if (target_key.getTargetKey() instanceof AvgKey) {                    v1 = getAVGValue(target_key, o1);                    v2 = getAVGValue(target_key, o2);                }                if (v1 == v2) {                    return 0;                }                if (v1 == null) {                    return 1;                }                if (v2 == null) {                    return -1;                }                if (v1.doubleValue() == v2.doubleValue()) {                    return 0;                }                boolean v = v1.doubleValue() < v2.doubleValue();                return (sortType == BIReportConstant.SORT.ASC || sortType == BIReportConstant.SORT.NUMBER_ASC) == v ? -1 : 1;            }        };    }    private double getAVGValue(TargetGettingKey targetGettingKey, Object node) {        String targetName = targetGettingKey.getTargetName();        AvgKey avgKey = (AvgKey) targetGettingKey.getTargetKey();        TargetGettingKey sumGettingKey = new TargetGettingKey(avgKey.getSumKey(), targetName);        TargetGettingKey countGettingKey = new TargetGettingKey(avgKey.getCountKey(), targetName);        Number sumValue = ((LightNode) node).getSummaryValue(sumGettingKey);        Number countValue = ((LightNode) node).getSummaryValue(countGettingKey);        double avgValue = 0;        if (sumValue != null && countValue != null) {            avgValue = sumValue.doubleValue() / countValue.doubleValue();        }        return avgValue;    }    private List<LightNode> getNodeList() {        if (children == null) {            return new ArrayList<LightNode>();        }        return children;    }}