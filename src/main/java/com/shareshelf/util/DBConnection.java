package com.shareshelf.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single place that knows how to open a database connection.
 *
 * Credentials are never hardcoded. They are resolved in this order (last wins):
 *   1. built-in defaults (localhost MySQL, user "root", empty password)
 *   2. src/main/resources/db.properties (git-ignored; copy db.properties.example)
 *   3. environment variables SHARESHELF_DB_URL, SHARESHELF_DB_USER, SHARESHELF_DB_PASSWORD
 */
public class DBConnection {

    private static final Logger LOG = Logger.getLogger(DBConnection.class.getName());

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/shareshelf?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final Properties CONFIG = loadConfig();

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "MySQL JDBC driver not found on the classpath", e);
        }
    }

    private DBConnection() {
    }

    private static Properties loadConfig() {
        Properties p = new Properties();
        p.setProperty("db.url", DEFAULT_URL);
        p.setProperty("db.user", "root");
        p.setProperty("db.password", "");

        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read db.properties, using defaults", e);
        }

        overrideFromEnv(p, "db.url", "SHARESHELF_DB_URL");
        overrideFromEnv(p, "db.user", "SHARESHELF_DB_USER");
        overrideFromEnv(p, "db.password", "SHARESHELF_DB_PASSWORD");
        return p;
    }

    private static void overrideFromEnv(Properties p, String key, String envName) {
        String value = System.getenv(envName);
        if (value != null) {
            p.setProperty(key, value);
        }
    }

    /** Opens a new connection. Callers must close it (use try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"),
                CONFIG.getProperty("db.password"));
    }

    /** Run this class directly to check that your database settings work. */
    public static void main(String[] args) {
        try (Connection connection = getConnection()) {
            if (connection != null) {
                System.out.println("Database connection established successfully.");
            } else {
                System.out.println("Failed to establish database connection.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
