package org.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgresConnector {
    private final static String URL = "jdbc:postgresql://localhost:5432/school";
    private final static String USER = "postgres";
    private final static String PASSWORD = "1";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
