package com.fr.swift.rpc;

import com.fr.swift.context.SwiftContext;
import com.fr.swift.log.SwiftLogger;
import com.fr.swift.log.SwiftLoggers;
import com.fr.swift.rpc.server.RpcServer;
import com.fr.swift.rpc.starter.RpcClientServiceStarter;
import com.fr.swift.rpc.starter.RpcServerServiceStarter;
import com.fr.third.springframework.context.ApplicationContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This class created on 2018/6/8
 *
 * @author Lucifer
 * @description
 * @since Advanced FineBI 5.0
 */
public class SwiftRpcService {

    private ApplicationContext context;
    private RpcServiceStarter serverStarter;
    private RpcServiceStarter clientStarter;

    private ScheduledExecutorService serverServiceExector = Executors.newScheduledThreadPool(1);
    private ScheduledExecutorService clientServiceExector = Executors.newScheduledThreadPool(1);

    private static final SwiftLogger LOGGER = SwiftLoggers.getLogger(RpcServer.class);

    private SwiftRpcService() {
        init();
    }

    public static final SwiftRpcService INSTANCE = new SwiftRpcService();

    public static SwiftRpcService getInstance() {
        return INSTANCE;
    }

    private void init() {
        context = SwiftContext.getInstance().getRpcContext();
    }

    public void startServerService() {
        synchronized (this.getClass()) {
            if (serverStarter == null) {
                serverStarter = new RpcServerServiceStarter(context);
            }
        }
        serverServiceExector.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    serverStarter.start();
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
        });
    }

    public synchronized void stopServerService() throws Exception {
        serverServiceExector.shutdown();
        if (serverStarter != null) {
            serverStarter.stop();
        }
    }

    public void startClientService() {
        synchronized (this.getClass()) {
            if (clientStarter == null) {
                clientStarter = new RpcClientServiceStarter(context);
            }
        }
        clientServiceExector.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    clientStarter.start();
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
        });
    }

    public synchronized void stopClientService() throws Exception {
        clientServiceExector.shutdown();
        if (clientStarter != null) {
            clientStarter.stop();
        }
    }
}
