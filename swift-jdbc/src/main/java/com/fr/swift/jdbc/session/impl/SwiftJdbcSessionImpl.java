package com.fr.swift.jdbc.session.impl;

import com.fr.swift.db.Schema;
import com.fr.swift.jdbc.rpc.RpcCaller;
import com.fr.swift.jdbc.session.SwiftJdbcSession;
import com.fr.swift.jdbc.statement.SwiftStatement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author yee
 * @date 2018/8/27
 */
public class SwiftJdbcSessionImpl implements SwiftJdbcSession {

    private RpcCaller.SelectRpcCaller selectRpcCaller;
    private RpcCaller.MaintenanceRpcCaller maintenanceRpcCaller;
    private Schema schema;

    SwiftJdbcSessionImpl(Schema schema, RpcCaller.SelectRpcCaller selectRpcCaller, RpcCaller.MaintenanceRpcCaller maintenanceRpcCaller) {
        this.selectRpcCaller = selectRpcCaller;
        this.maintenanceRpcCaller = maintenanceRpcCaller;
        this.schema = schema;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return new SwiftStatement(schema, selectRpcCaller, maintenanceRpcCaller);
    }

    @Override
    public PreparedStatement preparedStatement(String sql) throws SQLException {
        return null;
    }
}
