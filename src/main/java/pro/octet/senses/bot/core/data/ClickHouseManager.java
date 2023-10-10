/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.data;

import com.clickhouse.client.ClickHouseFormat;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.jdbc.ClickHouseConnection;
import com.clickhouse.jdbc.ClickHouseDataSource;
import com.clickhouse.jdbc.ClickHouseStatement;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * ClickHouse Manager
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class ClickHouseManager implements AutoCloseable {

    private transient ClickHouseDataSource dataSource;
    private transient ClickHouseConnection connection;
    private transient ClickHouseStatement statement;
    private final String taskName;

    public ClickHouseManager(Properties props, String jdbc, String taskName) throws SQLException {
        initDataSource(props, jdbc);
        getConnection();
        getStatement();
        this.taskName = taskName;
    }

    private void initDataSource(Properties props, String jdbc) throws SQLException {
        if (dataSource == null) {
            dataSource = new ClickHouseDataSource(jdbc, props);
        }
    }

    private ClickHouseConnection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = dataSource.getConnection();
        }
        return connection;
    }

    private ClickHouseStatement getStatement() throws SQLException {
        if (statement == null || statement.isClosed()) {
            statement = getConnection().createStatement();
        }
        return statement;
    }

    public void checkClickHouseStatus() {
        try {
            log.info("{} -> Check ClickHouse status, ClickHouse availability status: {}, ClickHouse connection status: {}.",
                    taskName, connection.isValid(3000), !connection.isClosed());
        } catch (SQLException e) {
            log.warn("", e);
        }
    }

    public boolean write(String tableName, StringBuffer data) throws SQLException {
        CompletableFuture<ClickHouseResponse> future = getStatement().write()
                .table(tableName)
                .data(new ByteArrayInputStream(data.toString().getBytes(StandardCharsets.UTF_8)))
                .format(ClickHouseFormat.JSONEachRow)
                .send();
        return future.isDone() && !future.isCompletedExceptionally();
    }

    @Override
    public void close() {
        try {
            if (statement != null) {
                statement.close();
                statement = null;
                log.debug("{} -> Closed ClickHouse statement.", taskName);
            }
        } catch (SQLException e) {
            log.warn("", e);
        }
        try {
            if (connection != null) {
                connection.close();
                connection = null;
                log.debug("{} -> Closed ClickHouse connection.", taskName);
            }
        } catch (SQLException e) {
            log.warn("", e);
        }
    }
}
