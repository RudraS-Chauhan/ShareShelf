package com.shareshelf.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/shareshelf";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "password";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
    
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