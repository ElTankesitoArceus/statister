package com.github.eltankesitoarceus.stater.data.database;

import com.github.eltankesitoarceus.stater.Stater;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    static HikariDataSource dataSource;

    private static final String[] TABLE_NAMES = {"players", "stats"};
    static Logger logger = Logger.getLogger(String.valueOf(DatabaseManager.class));
    private static String prefix;

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        if (dataSource == null) {
            prepareDataSource();
        }
        return dataSource.getConnection();
    }

    private static void prepareDataSource() throws ClassNotFoundException {
        HikariConfig config = new HikariConfig();
        String databaseType = Stater.getPlugin().getPluginConfig().getString("database-type");
        if (databaseType.equalsIgnoreCase("sqlite")) {
            Class.forName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:plugins/Statister/database.db");
        } else if(databaseType.equalsIgnoreCase("mysql")) {
            String host = Stater.getPlugin().getPluginConfig().getString("host");
            int port = Stater.getPlugin().getPluginConfig().getInt("port");
            String database = Stater.getPlugin().getPluginConfig().getString("database");
            config.setJdbcUrl("jdbc:mysql://%s:%d/%s".formatted(host, port, database));
            config.setUsername(Stater.getPlugin().getPluginConfig().getString("username"));
            config.setPassword(Stater.getPlugin().getPluginConfig().getString("password"));
        }
        int poolSize = Stater.getPlugin().getPluginConfig().getInt("pool-size");
        config.setMaximumPoolSize(poolSize > 0 ? poolSize : 2);
        initializeDatabase();
    }

    private static void initializeDatabase() {
        prefix = Stater.getPlugin().getPluginConfig().getString("prefix");
        String[] tables = new String[TABLE_NAMES.length];
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            boolean propper = true;
            ResultSet rs;
            String[] table = {"TABLE"};
            for (int i = 0; i < TABLE_NAMES.length && propper; i++) {
                rs = meta.getTables(null, null, TABLE_NAMES[i], table);
                propper = rs.next();
                rs.close();
            }
            if (!propper) createBaseTables(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createBaseTables(Connection conn) {
        String[] createQueries = {
                "CREATE TABLE IF NOT EXISTS %splayers (ID INTEGER NOT NULL, NAME varchar(32) NOT NULL, PRIMARY KEY ( id ))",
                "CREATE TABLE IF NOT EXISTS %sstats (PLAYER_ID INTEGER NOT NULL, NAME VARCHAR(100) NOT NULL, VALUE VARCHAR(200))"
        };
        try (Statement st = conn.createStatement()) {
            for (String q : createQueries) {
                st.execute(q);
            }
        } catch (SQLException e) {
            logger.severe("Error while creating database tables");
            throw new RuntimeException(e);
        }
    }

    public static String getPrefix() {
        return prefix;
    }
}