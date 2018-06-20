package com.fr.swift.service;

import com.fr.swift.Invoker;
import com.fr.swift.ProxyFactory;
import com.fr.swift.Result;
import com.fr.swift.URL;
import com.fr.swift.config.bean.SwiftServiceInfoBean;
import com.fr.swift.config.service.SwiftServiceInfoService;
import com.fr.swift.context.SwiftContext;
import com.fr.swift.event.analyse.SegmentLocationRpcEvent;
import com.fr.swift.exception.SwiftServiceException;
import com.fr.swift.frrpc.SwiftClusterService;
import com.fr.swift.invocation.SwiftInvocation;
import com.fr.swift.log.SwiftLoggers;
import com.fr.swift.query.builder.QueryBuilder;
import com.fr.swift.query.query.QueryInfo;
import com.fr.swift.query.session.AbstractSession;
import com.fr.swift.query.session.Session;
import com.fr.swift.query.session.SessionBuilder;
import com.fr.swift.query.session.factory.SessionFactory;
import com.fr.swift.rpc.annotation.RpcMethod;
import com.fr.swift.rpc.annotation.RpcService;
import com.fr.swift.rpc.annotation.RpcServiceType;
import com.fr.swift.rpc.client.AsyncRpcCallback;
import com.fr.swift.rpc.client.async.RpcFuture;
import com.fr.swift.rpc.server.RpcServer;
import com.fr.swift.segment.Incrementer;
import com.fr.swift.segment.SegmentDestination;
import com.fr.swift.segment.SegmentKey;
import com.fr.swift.segment.impl.SegmentLocationInfoImpl;
import com.fr.swift.segment.recover.SwiftSegmentRecovery;
import com.fr.swift.selector.ProxySelector;
import com.fr.swift.selector.UrlSelector;
import com.fr.swift.service.listener.SwiftServiceListenerHandler;
import com.fr.swift.source.SourceKey;
import com.fr.swift.source.SwiftResultSet;
import com.fr.swift.structure.Pair;
import com.fr.swift.util.concurrent.CommonExecutor;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author pony
 * @date 2017/10/10
 */
@RpcService(type = RpcServiceType.CLIENT_SERVICE, value = RealtimeService.class)
public class SwiftRealtimeService extends AbstractSwiftService implements RealtimeService, Serializable {
    private RpcServer server = SwiftContext.getInstance().getBean(RpcServer.class);

    @Override
    public void insert(SourceKey tableKey, SwiftResultSet resultSet) throws SQLException {
        SwiftLoggers.getLogger().info("insert");

        new Incrementer(tableKey).increment(resultSet);
        // TODO 这个还要处理realtime的destination并且更新给analyse

        // 更新给analyse
        URL masterURL = getMasterURL();
        ProxyFactory factory = ProxySelector.getInstance().getFactory();
        Invoker invoker = factory.getInvoker(null, SwiftServiceListenerHandler.class, masterURL, false);
        Result result = invoker.invoke(new SwiftInvocation(server.getMethodByName("rpcTrigger"), new Object[]{new SegmentLocationRpcEvent(new SegmentLocationInfoImpl(ServiceType.REAL_TIME, new HashMap<String, Pair<Integer, List<SegmentDestination>>>()))}));
        RpcFuture future = (RpcFuture) result.getValue();
        future.addCallback(new AsyncRpcCallback() {
            @Override
            public void success(Object result) {
                logger.info("rpcTrigger success! ");
            }

            @Override
            public void fail(Exception e) {
                logger.error("rpcTrigger error! ", e);
            }
        });
    }

    @Override
    @RpcMethod(methodName = "merge")
    public void merge(List<SegmentKey> tableKeys) {
        SwiftLoggers.getLogger().info("merge");
    }

    @Override
    @RpcMethod(methodName = "recover")
    public void recover(List<SegmentKey> tableKeys) {
        SwiftLoggers.getLogger().info("recover");
    }

    @Override
    @RpcMethod(methodName = "realTimeQuery")
    public <T extends SwiftResultSet> T query(final QueryInfo<T> queryInfo) throws SQLException {
        SessionFactory sessionFactory = SwiftContext.getInstance().getBean(SessionFactory.class);
        return sessionFactory.openSession(new SessionBuilder() {
            @Override
            public Session build(long cacheTimeout) {
                return new AbstractSession(cacheTimeout) {
                    @Override
                    protected <T extends SwiftResultSet> T query(QueryInfo<T> queryInfo) throws SQLException {
                        return QueryBuilder.buildQuery(queryInfo).getQueryResult();
                    }
                };
            }

            @Override
            public String getQueryId() {
                return queryInfo.getQueryId();
            }
        }).executeQuery(queryInfo);
    }

    @Override
    public boolean start() throws SwiftServiceException {
        super.start();

        recover0();

        return true;
    }

    private static void recover0() {
        CommonExecutor.get().submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    // 恢复所有realtime块
                    SwiftSegmentRecovery.getInstance().recoverAll();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.REAL_TIME;
    }

    private static final long serialVersionUID = 4719723736240190155L;

    public SwiftRealtimeService(String id) {
        super(id);
    }

    public SwiftRealtimeService() {
    }

    private URL getMasterURL() {
        List<SwiftServiceInfoBean> swiftServiceInfoBeans = SwiftContext.getInstance().getBean(SwiftServiceInfoService.class).getServiceInfoByService(SwiftClusterService.SERVICE);
        SwiftServiceInfoBean swiftServiceInfoBean = swiftServiceInfoBeans.get(0);
        return UrlSelector.getInstance().getFactory().getURL(swiftServiceInfoBean.getServiceInfo());
    }
}