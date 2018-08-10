package com.fr.swift.query.info.bean.parser;

import com.fr.swift.config.TestConfDb;
import com.fr.swift.config.indexing.TableIndexingConf;
import com.fr.swift.config.indexing.impl.SwiftColumnIndexingConf;
import com.fr.swift.config.indexing.impl.SwiftTableIndexingConf;
import com.fr.swift.config.service.IndexingConfService;
import com.fr.swift.context.SwiftContext;
import com.fr.swift.db.Database;
import com.fr.swift.db.impl.SwiftDatabase;
import com.fr.swift.query.info.bean.element.DimensionBean;
import com.fr.swift.query.info.bean.element.MetricBean;
import com.fr.swift.query.info.bean.element.SortBean;
import com.fr.swift.query.info.bean.query.DetailQueryInfoBean;
import com.fr.swift.query.info.bean.query.GroupQueryInfoBean;
import com.fr.swift.query.info.bean.query.QueryInfoBeanFactory;
import com.fr.swift.query.info.detail.DetailQueryInfo;
import com.fr.swift.query.info.group.GroupQueryInfo;
import com.fr.swift.query.query.QueryType;
import com.fr.swift.query.sort.SortType;
import com.fr.swift.resource.ResourceUtils;
import com.fr.swift.source.DataSource;
import com.fr.swift.source.SourceKey;
import com.fr.swift.source.alloter.impl.line.LineAllotRule;
import com.fr.swift.source.db.QueryDBSource;
import com.fr.swift.test.Preparer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * Created by Lyon on 2018/6/7.
 */
public class QueryInfoParserTest {

    private final Database db = SwiftDatabase.getInstance();
    private static IndexingConfService service;

    @BeforeClass
    public static void boot() throws Exception {
        Preparer.prepareCubeBuild();
        TestConfDb.setConfDb(SwiftTableIndexingConf.class, SwiftColumnIndexingConf.class);
        service = SwiftContext.get().getBean(IndexingConfService.class);
        SourceKey a = new SourceKey("DEMO_CONTRACT");
        TableIndexingConf tableConf = new SwiftTableIndexingConf(a, new LineAllotRule(1024));
        service.setTableConf(tableConf);
    }

    @Test
    public void testGroupQueryInfoBean() {
        String path = ResourceUtils.getFileAbsolutePath("json");
        String filePath = path + File.separator + "group.json";
        assertTrue(new File(filePath).exists());
        GroupQueryInfoBean queryBean = null;
        try {
            queryBean = (GroupQueryInfoBean) new QueryInfoBeanFactory().create(new File(filePath).toURI().toURL());
        } catch (IOException e) {
            fail();
        }
        List<DimensionBean> dimensionBeans = queryBean.getDimensionBeans();
        assertEquals(1, dimensionBeans.size());
        assertEquals("合同类型", dimensionBeans.get(0).getColumn());
        assertEquals("合同类型-转义", dimensionBeans.get(0).getName());
        List<MetricBean> metricBeans = queryBean.getMetricBeans();
        assertEquals(1, metricBeans.size());
        assertEquals("购买数量", metricBeans.get(0).getColumn());
        assertEquals("购买数量-转义", metricBeans.get(0).getName());
        GroupQueryInfo info = (GroupQueryInfo) QueryInfoParser.parse(queryBean);
        assertEquals(1, info.getDimensions().size());
        assertEquals(1, info.getMetrics().size());
    }

    @Test
    public void testDetailQueryInfoBean() throws SQLException {
        DataSource dataSource = new QueryDBSource("select * from DEMO_CONTRACT", "DEMO_CONTRACT");
        if (!db.existsTable(new SourceKey("DEMO_CONTRACT"))) {
            db.createTable(new SourceKey("DEMO_CONTRACT"), dataSource.getMetadata());
        }
        String path = ResourceUtils.getFileAbsolutePath("json");
        String filePath = path + File.separator + "detail.json";
        assertTrue(new File(filePath).exists());
        DetailQueryInfoBean queryBean = null;
        try {
            queryBean = (DetailQueryInfoBean) new QueryInfoBeanFactory().create(new File(filePath).toURI().toURL());
        } catch (IOException e) {
            fail();
        }
        assertEquals(QueryType.DETAIL, queryBean.getQueryType());
        assertEquals(4, queryBean.getDimensionBeans().size());
        List<DimensionBean> dimensionBeanList = queryBean.getDimensionBeans();
        assertEquals("合同类型", dimensionBeanList.get(0).getColumn());
        assertEquals("购买数量", dimensionBeanList.get(1).getColumn());
        assertEquals("总金额", dimensionBeanList.get(2).getColumn());
        assertEquals("购买的产品", dimensionBeanList.get(3).getColumn());
        List<SortBean> sortBeans = queryBean.getSortBeans();
        assertEquals(1, sortBeans.size());
        assertEquals("购买数量", sortBeans.get(0).getColumn());
        assertEquals(SortType.DESC, sortBeans.get(0).getType());

        DetailQueryInfo info = (DetailQueryInfo) QueryInfoParser.parse(queryBean);
        assertEquals(4, info.getDimensions().size());
        assertEquals(1, info.getSorts().size());
    }
}