package com.pabloncf.saas.tenant;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applyTenantContext(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applyTenantContext(connection);
        return connection;
    }

    private void applyTenantContext(Connection connection) throws SQLException {
        UUID tenant = TenantContext.getCurrentTenant();
        // Session-level set_config (false = not transaction-local) so the value persists
        // for all queries on this connection, with or without an explicit transaction.
        // We always write a value: empty string when no tenant is set, which the RLS
        // policy treats as "no access" (guarded by IS NOT NULL AND <> '').
        String value = tenant != null ? tenant.toString() : "";
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT set_config('app.current_tenant', ?, false)")) {
            ps.setString(1, value);
            ps.executeQuery().close();
        }
    }
}
