package com.fashionstore.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database utility class that uses SimpleConnectionPool 
 * instead of HikariCP to avoid module system issues.
 */
public class DatabaseUtils {
    
    /**
     * Gets a database connection from the pool
     */
    public static Connection getConnection() throws SQLException {
        return SimpleConnectionPool.getInstance().getConnection();
    }
    
    /**
     * Closes database resources
     */
    public static void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException e) {
            System.err.println("Error closing ResultSet: " + e.getMessage());
        }
        
        try {
            if (ps != null) ps.close();
        } catch (SQLException e) {
            System.err.println("Error closing PreparedStatement: " + e.getMessage());
        }
        
        if (conn != null) {
            SimpleConnectionPool.close(conn);
        }
    }
    
    /**
     * Creates database and tables if they don't exist
     */
    public static void createDatabaseIfNotExists() {
        SimpleConnectionPool.createDatabaseIfNotExists();
    }
} 