package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/company_db";
    private static final String USER = "root"; // replace with your DB username
    private static final String PASSWORD = "1234"; // replace with your DB password

    public static Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver"); // MySQL 8+ driver
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
