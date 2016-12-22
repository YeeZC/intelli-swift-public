package com.finebi.cube.gen.oper;

import com.finebi.cube.common.log.BILoggerFactory;
import com.finebi.cube.conf.BICubeConfigureCenter;
import com.finebi.cube.conf.pack.data.IBusinessPackageGetterService;
import com.finebi.cube.conf.table.BIBusinessTable;
import com.finebi.cube.exception.BICubeColumnAbsentException;
import com.finebi.cube.exception.BICubeIndexException;
import com.finebi.cube.exception.BICubeRelationAbsentException;
import com.finebi.cube.exception.IllegalRelationPathException;
import com.finebi.cube.impl.pubsub.BIProcessor;
import com.finebi.cube.impl.pubsub.BIProcessorThreadManager;
import com.finebi.cube.message.IMessage;
import com.finebi.cube.relation.BITableSourceRelation;
import com.finebi.cube.structure.*;
import com.fr.bi.conf.log.BILogManager;
import com.fr.bi.conf.provider.BILogManagerProvider;
import com.fr.bi.conf.report.widget.RelationColumnKey;
import com.fr.bi.stable.constant.CubeConstant;
import com.fr.bi.stable.data.db.ICubeFieldSource;
import com.fr.bi.stable.data.source.CubeTableSource;
import com.fr.bi.stable.exception.BITablePathEmptyException;
import com.fr.bi.stable.gvi.GroupValueIndex;
import com.fr.bi.stable.gvi.GroupValueIndexOrHelper;
import com.fr.bi.stable.gvi.RoaringGroupValueIndex;
import com.fr.bi.stable.gvi.traversal.SingleRowTraversalAction;
import com.fr.bi.stable.io.newio.NIOConstant;
import com.fr.bi.stable.utils.program.BINonValueUtils;
import com.fr.bi.stable.utils.program.BIStringUtils;
import com.fr.fs.control.UserControl;
import com.fr.general.ComparatorUtils;
import com.fr.stable.bridge.StableFactory;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This class created on 2016/4/8.
 *
 * @author Connery
 * @since 4.0
 */
public class BITablePathIndexBuilder extends BIProcessor {
    protected Cube cube;
    protected CubeChooser cubeChooser;
    protected BICubeTablePath relationPath;
    private static final Logger logger = LoggerFactory.getLogger(BITablePathIndexBuilder.class);

    public BITablePathIndexBuilder(Cube cube, Cube integrityCube, BICubeTablePath relationPath) {
        this.cube = cube;
        this.cubeChooser = new CubeChooser(cube, integrityCube);
        this.relationPath = relationPath;
        initThreadPool();
    }

    @Override
    protected void initThreadPool() {
        executorService = BIProcessorThreadManager.getInstance().getExecutorService();
    }

    @Override
    public Object mainTask(IMessage lastReceiveMessage) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        logger.info(BIStringUtils.append("\n    ", logPath(), "start building path index main task"));
        buildRelationPathIndex();
        logger.info(BIStringUtils.append("\n    ", logPath(), "finish building path index main task,{} second "), stopwatch.elapsed(TimeUnit.SECONDS));
        return null;
    }

    @Override
    public void release() {
        cube.clear();
    }

    private String logPath() {
        return logPath(relationPath);
    }

    private String logPath(BICubeTablePath relationPath) {
        Integer count = 0;
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("The path ID:").append(BuildLogHelper.calculatePathID(relationPath));
            for (BICubeRelation relation : relationPath.getAllRelations()) {
                sb.append(BIStringUtils.append(
                        "\nRelation " + count++,
                        "\n       Primary table:", relation.getPrimaryTable().getSourceID(),
                        "\n       Primary field:", relation.getPrimaryField().getColumnName(),
                        "\n       Foreign table:", relation.getForeignTable().getSourceID(),
                        "\n       Foreign field:", relation.getForeignField().getColumnName(),
                        "\n"
                ));

            }
            return sb.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return "error path";
    }

    private void buildRelationPathIndex() {
        if (relationPath.size() >= 2) {
            BILogManager biLogManager = StableFactory.getMarkedObject(BILogManagerProvider.XML_TAG, BILogManager.class);
            biLogManager.logRelationStart(UserControl.getInstance().getSuperManagerID());
            long t = System.currentTimeMillis();
            CubeRelationEntityGetterService lastRelationEntity = null;
            CubeRelationEntityGetterService frontRelationPathReader = null;
            ICubeRelationEntityService targetPathEntity = null;
            RelationColumnKey columnKeyInfo = null;
            try {
                Stopwatch stopwatch = Stopwatch.createStarted();
                columnKeyInfo = getRelationColumnKeyInfo();
                int primaryRowCount = getPrimaryTableRowCount();
                lastRelationEntity = buildLastRelationReader();
                frontRelationPathReader = buildFrontRelationPathReader();
                targetPathEntity = buildTargetRelationPathWriter();
                GroupValueIndexOrHelper helper = new GroupValueIndexOrHelper();
                int[] reverse = new int[getJuniorTableRowCount()];
                Arrays.fill(reverse, NIOConstant.INTEGER.NULL_VALUE);
                for (int row = 0; row < primaryRowCount; row++) {
                    GroupValueIndex frontGroupValueIndex = frontRelationPathReader.getBitmapIndex(row);
                    GroupValueIndex resultGroupValueIndex = getTableLinkedOrGVI(frontGroupValueIndex, lastRelationEntity);
                    helper.add(resultGroupValueIndex);
                    targetPathEntity.addRelationIndex(row, resultGroupValueIndex);
                    initReverseIndex(reverse, row, resultGroupValueIndex);
                    if (CubeConstant.LOG_SEPERATOR_ROW != 0 && row % CubeConstant.LOG_SEPERATOR_ROW == 0) {
                        logger.info(BIStringUtils.append("\n    ", logPath(), "read ", String.valueOf(row), " rows field value and time elapse:", String.valueOf(stopwatch.elapsed(TimeUnit.SECONDS)), " second"));
                    }
                }
                GroupValueIndex nullIndex = helper.compute().NOT(reverse.length);
                buildReverseIndex(targetPathEntity, reverse);
                targetPathEntity.addRelationNULLIndex(0, nullIndex);
                targetPathEntity.addVersion(System.currentTimeMillis());
                long costTime = System.currentTimeMillis() - t;
                try {
                    biLogManager.infoRelation(columnKeyInfo, costTime, UserControl.getInstance().getSuperManagerID());
                } catch (Exception e) {
                    BILoggerFactory.getLogger().error(e.getMessage());
                }
            } catch (Exception e) {
                try {
                    BILoggerFactory.getLogger().error(BIStringUtils.append("error path:", columnKeyInfo.createJSON().toString()) + e.getMessage(), e);
                    biLogManager.errorRelation(columnKeyInfo, e.getMessage(), UserControl.getInstance().getSuperManagerID());
                } catch (Exception e1) {
                    BILoggerFactory.getLogger().error(e1.getMessage());

                }
                throw BINonValueUtils.beyondControl(e.getMessage(), e);
            } finally {

                if (lastRelationEntity != null) {
                    lastRelationEntity.clear();
                }
                if (frontRelationPathReader != null) {
                    frontRelationPathReader.clear();
                }
                if (targetPathEntity != null) {
                    targetPathEntity.forceReleaseWriter();
                    targetPathEntity.clear();
                }
            }
        }
    }

    private void initReverseIndex(final int[] index, final int row, GroupValueIndex gvi) {
        gvi.Traversal(new SingleRowTraversalAction() {
            @Override
            public void actionPerformed(int rowIndex) {
                index[rowIndex] = row;
            }
        });
    }

    private void buildReverseIndex(ICubeRelationEntityService tableRelation, int[] index) {
        for (int i = 0; i < index.length; i++) {
            tableRelation.addReverseIndex(i, index[i]);
        }
    }

    public static GroupValueIndex getTableLinkedOrGVI(GroupValueIndex currentIndex, final ICubeIndexDataGetterService reader) {
        if (currentIndex != null) {

            final GroupValueIndex result = new RoaringGroupValueIndex();
            currentIndex.Traversal(new SingleRowTraversalAction() {
                @Override
                public void actionPerformed(int rowIndices) {
                    try {
                        result.or(reader.getBitmapIndex(rowIndices));
                    } catch (BICubeIndexException e) {
                        BILoggerFactory.getLogger().error(e.getMessage(), e);
                    }
                }
            });
            return result;
        }
        return null;
    }

    protected int getPrimaryTableRowCount() throws BITablePathEmptyException {
        ITableKey primaryTableKey = relationPath.getFirstRelation().getPrimaryTable();
        CubeTableEntityGetterService primaryTable = cubeChooser.getCubeTable(primaryTableKey);
        int rowCount = primaryTable.getRowCount();
        primaryTable.clear();
        return rowCount;
    }

    protected int getJuniorTableRowCount() throws BITablePathEmptyException {
        ITableKey foreignTableKey = relationPath.getLastRelation().getForeignTable();
        CubeTableEntityGetterService foreignTable = cubeChooser.getCubeTable(foreignTableKey);
        int rowCount = foreignTable.getRowCount();
        foreignTable.clear();
        return rowCount;
    }

    private CubeRelationEntityGetterService buildLastRelationReader() throws
            BICubeRelationAbsentException, BICubeColumnAbsentException, IllegalRelationPathException, BITablePathEmptyException {
        BICubeRelation lastRelation = relationPath.getLastRelation();
        ITableKey lastPrimaryKey = lastRelation.getPrimaryTable();
        try {
            BICubeTablePath relationPath = new BICubeTablePath();
            relationPath.addRelationAtHead(lastRelation);
            logger.info("get the path tail reader,the tail :" + logPath(relationPath));
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
        }
        return cubeChooser.getCubeRelation(lastPrimaryKey, lastRelation);
    }

    private ICubeRelationEntityService buildTargetRelationPathWriter() throws
            BICubeRelationAbsentException, BICubeColumnAbsentException, IllegalRelationPathException, BITablePathEmptyException {
        BICubeTablePath frontRelation = new BICubeTablePath();
        frontRelation.copyFrom(relationPath);
        ITableKey firstPrimaryKey = relationPath.getFirstRelation().getPrimaryTable();
        return (ICubeRelationEntityService) cube.getCubeRelation(firstPrimaryKey, frontRelation);
    }

    private CubeRelationEntityGetterService buildFrontRelationPathReader() throws
            BICubeRelationAbsentException, BICubeColumnAbsentException, IllegalRelationPathException, BITablePathEmptyException {
        ITableKey firstPrimaryKey = relationPath.getFirstRelation().getPrimaryTable();
        BICubeTablePath frontRelation = buildFrontRelation();
        try {
            logger.info("get the path tail reader,the tail :" + logPath(frontRelation));
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
        }
        return cubeChooser.getCubeRelation(firstPrimaryKey, frontRelation);
    }

    private BICubeTablePath buildFrontRelation() throws BITablePathEmptyException {
        BICubeTablePath frontRelation = new BICubeTablePath();
        frontRelation.copyFrom(relationPath);
        frontRelation.removeLastRelation();
        return frontRelation;
    }

    public RelationColumnKey getRelationColumnKeyInfo() {
        List<BITableSourceRelation> relations = new ArrayList<BITableSourceRelation>();
        ICubeFieldSource field = null;
        for (BICubeRelation relation : this.relationPath.getAllRelations()) {
            BITableSourceRelation tableRelation = null;
            try {
                tableRelation = getTableRelation(relation);
            } catch (Exception e) {
                BILoggerFactory.getLogger().error("get relationColumnKey failed! relation information used as listed:" + relation.getPrimaryTable().getSourceID() + "." + relation.getPrimaryField().getColumnName() + " to " + relation.getForeignTable().getSourceID() + "." + relation.getForeignField().getColumnName());
            }
            field = tableRelation.getPrimaryKey();
            relations.add(tableRelation);
        }
        return new RelationColumnKey(field, relations);
    }

    private BITableSourceRelation getTableRelation(BICubeRelation relation) {
        ICubeFieldSource primaryField = null;
        ICubeFieldSource foreignField = null;
        CubeTableSource primaryTable = null;
        CubeTableSource foreignTable = null;
        Set<CubeTableSource> allTableSource = getAllTableSource();
        for (CubeTableSource cubeTableSource : allTableSource) {
            if (ComparatorUtils.equals(relation.getPrimaryTable().getSourceID(), cubeTableSource.getSourceID())) {
                primaryTable = cubeTableSource;
                Set<CubeTableSource> primarySources = new HashSet<CubeTableSource>();
                primarySources.add(cubeTableSource);
                for (ICubeFieldSource iCubeFieldSource : primaryTable.getFacetFields(primarySources)) {
                    if (ComparatorUtils.equals(iCubeFieldSource.getFieldName(), relation.getPrimaryField().getColumnName())) {
                        primaryField = iCubeFieldSource;
                    }
                }
                break;
            }
        }
        for (CubeTableSource cubeTableSource : allTableSource) {
            if (ComparatorUtils.equals(relation.getForeignTable().getSourceID(), cubeTableSource.getSourceID())) {
                foreignTable = cubeTableSource;
                Set<CubeTableSource> foreignSource = new HashSet<CubeTableSource>();
                foreignSource.add(cubeTableSource);
                for (ICubeFieldSource iCubeFieldSource : foreignTable.getFacetFields(foreignSource)) {
                    if (ComparatorUtils.equals(iCubeFieldSource.getFieldName(), relation.getForeignField().getColumnName())) {
                        foreignField = iCubeFieldSource;
                    }
                }
                break;
            }
        }
        BITableSourceRelation biTableSourceRelation = new BITableSourceRelation(primaryField, foreignField, primaryTable, foreignTable);
        return biTableSourceRelation;
    }

    private Set<CubeTableSource> getAllTableSource() {
        Set<CubeTableSource> cubeTableSourceSet = new HashSet<CubeTableSource>();
        Set<IBusinessPackageGetterService> packs = BICubeConfigureCenter.getPackageManager().getAllPackages(UserControl.getInstance().getSuperManagerID());
        for (IBusinessPackageGetterService pack : packs) {
            Iterator<BIBusinessTable> tIt = pack.getBusinessTables().iterator();
            while (tIt.hasNext()) {
                BIBusinessTable table = tIt.next();
                cubeTableSourceSet.add(table.getTableSource());
            }
        }
        return cubeTableSourceSet;
    }

}
