package me.scholtes.ilfriends.data;

import com.zaxxer.hikari.HikariDataSource;
import me.scholtes.ilfriends.ILFriends;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class MySQLDataBase {

    private HikariDataSource dataSource;
    private ILFriends plugin;

    /**
     * Initializes and connects to the database
     * @param plugin Instance of the plugin
     */
    public MySQLDataBase(ILFriends plugin) {
        this.plugin = plugin;
        dataSource = new HikariDataSource();

        String address = plugin.getConfig().getString("database.address");
        String databaseName = plugin.getConfig().getString("database.db_name");
        String username = plugin.getConfig().getString("database.username");
        String password = plugin.getConfig().getString("database.password");
        String port = plugin.getConfig().getString("database.port");

        dataSource.setMaximumPoolSize(10);
        dataSource.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        dataSource.addDataSourceProperty("serverName", address);
        dataSource.addDataSourceProperty("port", port);
        dataSource.addDataSourceProperty("databaseName", databaseName);
        dataSource.addDataSourceProperty("user", username);
        dataSource.addDataSourceProperty("password", password);
        dataSource.addDataSourceProperty("useSSL", false);

        createTable();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Creates the new tables for the database
     * The friends table is for storing friend relations
     * The requests table is for storing friend requests
     */
    private void createTable() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
            Connection con = null;

            String createFriendTable = "CREATE TABLE IF NOT EXISTS friends (" +
                    "id int NOT NULL AUTO_INCREMENT," +
                    "player1 VARCHAR(36) NOT NULL," +
                    "player2 VARCHAR(36) NOT NULL," +
                    "date DATE NOT NULL," +
                    "PRIMARY KEY (id)" +
                    ");";

            String createRequestsTable = "CREATE TABLE IF NOT EXISTS requests (" +
                    "id int NOT NULL AUTO_INCREMENT," +
                    "sender VARCHAR(36) NOT NULL," +
                    "receiver VARCHAR(36) NOT NULL," +
                    "PRIMARY KEY (id)" +
                    ");";

            PreparedStatement preparedStatement = null;

            try {
                con = dataSource.getConnection();
                preparedStatement = con.prepareStatement(createFriendTable);
                preparedStatement.execute();
                preparedStatement = con.prepareStatement(createRequestsTable);
                preparedStatement.execute();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error while creating the table");
                e.printStackTrace();
            } finally {
                close(con, preparedStatement);
            }
        });
    }


    /**
     * Closes the connection and the prepared statement
     *
     * @param con The connection to close
     * @param preparedStatement The prepared statement to close
     */
    public static void close(Connection con, PreparedStatement preparedStatement) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if(preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
