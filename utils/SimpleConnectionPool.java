package com.fashionstore.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

/**
 * A simple database connection pool implementation
 * that doesn't rely on external libraries.
 */
public class SimpleConnectionPool {
    private static final int DEFAULT_POOL_SIZE = 10;
    private static String DB_URL = "jdbc:mysql://localhost:3306/fashionstore";
    private static String DB_USER = "Asser";
    private static String DB_PASSWORD = "00990099";
    private static String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    private final List<Connection> connectionPool;
    private final List<Connection> usedConnections = new ArrayList<>();
    private static SimpleConnectionPool instance;

    private SimpleConnectionPool(int poolSize) throws SQLException {
        connectionPool = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            connectionPool.add(createConnection());
        }
    }

    public static synchronized SimpleConnectionPool getInstance() throws SQLException {
        if (instance == null) {
            loadProperties();
            try {
                Class.forName(DB_DRIVER);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Database driver not found: " + e.getMessage());
            }

            instance = new SimpleConnectionPool(DEFAULT_POOL_SIZE);
        }
        return instance;
    }

    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream is = SimpleConnectionPool.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (is != null) {
                props.load(is);
                DB_URL = props.getProperty("db.url", DB_URL);
                DB_USER = props.getProperty("db.user", DB_USER);
                DB_PASSWORD = props.getProperty("db.password", DB_PASSWORD);
                DB_DRIVER = props.getProperty("db.driver", DB_DRIVER);
                System.out.println("Loaded database configuration from properties file");
            } else {
                System.out.println("Using default database configuration");
            }
        } catch (IOException e) {
            System.err.println("Could not load database properties: " + e.getMessage());
            System.out.println("Using default database configuration");
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connectionPool.isEmpty()) {
            if (usedConnections.size() < DEFAULT_POOL_SIZE) {
                connectionPool.add(createConnection());
            } else {
                throw new SQLException("Connection pool exhausted");
            }
        }

        Connection connection = connectionPool.remove(connectionPool.size() - 1);

        // Validate connection
        if (!connection.isValid(1)) {
            connection = createConnection();
        }

        usedConnections.add(connection);
        return connection;
    }

    public synchronized boolean releaseConnection(Connection connection) {
        connectionPool.add(connection);
        return usedConnections.remove(connection);
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void close(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    getInstance().releaseConnection(conn);
                }
            } catch (SQLException e) {
                System.err.println("Error releasing connection: " + e.getMessage());
            }
        }
    }

    public int getSize() {
        return connectionPool.size() + usedConnections.size();
    }

    public int getAvailable() {
        return connectionPool.size();
    }

    public static void createDatabaseIfNotExists() {
        Connection conn = null;
        Connection tempConn = null;

        try {
            // Load properties
            loadProperties();

            // Load driver
            try {
                Class.forName(DB_DRIVER);
            } catch (ClassNotFoundException e) {
                System.err.println("Database driver not found: " + e.getMessage());
                return;
            }

            // Connect to MySQL server without specifying a database
            String baseUrl = DB_URL.substring(0, DB_URL.lastIndexOf('/'));
            tempConn = DriverManager.getConnection(baseUrl, DB_USER, DB_PASSWORD);

            // Extract database name from URL
            String dbName = DB_URL.substring(DB_URL.lastIndexOf('/') + 1);

            // Create database if not exists
            try (java.sql.Statement stmt = tempConn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
                System.out.println("Database created or already exists: " + dbName);
            }

            // Close temporary connection
            tempConn.close();

            // Now connect to the fashionstore database and create tables
            conn = getInstance().getConnection();

            // Create tables
            createTables(conn);

            // Run migrations for existing databases
            migrateDatabaseSchema(conn);

            System.out.println("Database schema created successfully");

        } catch (SQLException e) {
            System.err.println("Error creating database schema: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release connections back to the pool
            if (conn != null) {
                close(conn);
            }

            if (tempConn != null) {
                try {
                    tempConn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing temporary connection: " + e.getMessage());
                }
            }
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (java.sql.Statement stmt = conn.createStatement()) {
            // Create users table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "user_id VARCHAR(50) PRIMARY KEY, " +
                            "username VARCHAR(50) NOT NULL UNIQUE, " +
                            "email VARCHAR(100) NOT NULL, " +
                            "password_hash VARCHAR(255) NOT NULL, " +
                            "first_name VARCHAR(50), " +
                            "last_name VARCHAR(50), " +
                            "date_registered TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "last_login TIMESTAMP NULL, " +
                            "is_deactivated BOOLEAN DEFAULT FALSE, " +
                            "deactivation_date TIMESTAMP NULL, " +
                            "is_dark_mode BOOLEAN DEFAULT FALSE, " +
                            "is_banned BOOLEAN DEFAULT FALSE, " +
                            "ban_reason VARCHAR(255) NULL, " +
                            "ban_expiration TIMESTAMP NULL" +
                            ")");

            // Create products table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS products (" +
                            "product_id VARCHAR(50) PRIMARY KEY, " +
                            "name VARCHAR(100) NOT NULL, " +
                            "description TEXT, " +
                            "brand VARCHAR(50), " +
                            "category VARCHAR(50), " +
                            "subcategory VARCHAR(50), " +
                            "price DECIMAL(10,2) NOT NULL, " +
                            "original_price DECIMAL(10,2), " +
                            "cost DECIMAL(10,2), " +
                            "gender VARCHAR(20), " +
                            "size VARCHAR(20), " +
                            "color VARCHAR(30), " +
                            "material VARCHAR(50), " +
                            "season VARCHAR(20), " +
                            "image_path VARCHAR(255), " +
                            "stock_quantity INT DEFAULT 0, " +
                            "date_added TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "last_updated TIMESTAMP NULL, " +
                            "is_featured BOOLEAN DEFAULT FALSE, " +
                            "average_rating DOUBLE DEFAULT 0, " +
                            "review_count INT DEFAULT 0, " +
                            "is_visible BOOLEAN DEFAULT TRUE" +
                            ")");

            // Create outfits table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS outfits (" +
                            "outfit_id VARCHAR(50) PRIMARY KEY, " +
                            "user_id VARCHAR(50) NOT NULL, " +
                            "name VARCHAR(100) NOT NULL, " +
                            "description TEXT, " +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "last_modified TIMESTAMP NULL, " +
                            "ai_generated BOOLEAN DEFAULT FALSE, " +
                            "style_rating DOUBLE DEFAULT 0, " +
                            "likes_count INT DEFAULT 0, " +
                            "season VARCHAR(20), " +
                            "occasion VARCHAR(20), " +
                            "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                            ")");

            // Create outfit_products junction table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS outfit_products (" +
                            "outfit_id VARCHAR(50), " +
                            "product_id VARCHAR(50), " +
                            "PRIMARY KEY (outfit_id, product_id), " +
                            "FOREIGN KEY (outfit_id) REFERENCES outfits(outfit_id) ON DELETE CASCADE, " +
                            "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE" +
                            ")");

            // Create wardrobe_items junction table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS wardrobe_items (" +
                            "user_id VARCHAR(50), " +
                            "product_id VARCHAR(50), " +
                            "PRIMARY KEY (user_id, product_id), " +
                            "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                            "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE" +
                            ")");

            // Create style_preferences table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS style_preferences (" +
                            "preference_id VARCHAR(50) PRIMARY KEY, " +
                            "user_id VARCHAR(50) NOT NULL, " +
                            "preference_type VARCHAR(50) NOT NULL, " +
                            "preference_value VARCHAR(50) NOT NULL, " +
                            "preference_weight DOUBLE DEFAULT 1.0, " +
                            "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                            ")");

            // Create product_attributes table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS product_attributes (" +
                            "product_id VARCHAR(50), " +
                            "attribute_name VARCHAR(50), " +
                            "attribute_value VARCHAR(255), " +
                            "PRIMARY KEY (product_id, attribute_name), " +
                            "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE" +
                            ")");

            // Create shopping_carts table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS shopping_carts (" +
                            "cart_id VARCHAR(50) PRIMARY KEY, " +
                            "user_id VARCHAR(50) NOT NULL UNIQUE, " +
                            "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                            ")");

            // Create cart_items table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS cart_items (" +
                            "cart_id VARCHAR(50), " +
                            "product_id VARCHAR(50), " +
                            "quantity INT DEFAULT 1, " +
                            "PRIMARY KEY (cart_id, product_id), " +
                            "FOREIGN KEY (cart_id) REFERENCES shopping_carts(cart_id) ON DELETE CASCADE, " +
                            "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE" +
                            ")");

            // Create outfit_tags table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS outfit_tags (" +
                            "outfit_id VARCHAR(50), " +
                            "tag VARCHAR(50), " +
                            "PRIMARY KEY (outfit_id, tag), " +
                            "FOREIGN KEY (outfit_id) REFERENCES outfits(outfit_id) ON DELETE CASCADE" +
                            ")");
        }
    }

    /**
     * Run migrations to update existing database schema
     */
    private static void migrateDatabaseSchema(Connection conn) {
        try {
            // Check for each column we might need to add
            
            // Check if is_visible column exists in products table
            boolean isVisibleExists = false;
            try (java.sql.ResultSet rs = conn.getMetaData().getColumns(
                    null, null, "products", "is_visible")) {
                isVisibleExists = rs.next();
            }

            // Add is_visible column if it doesn't exist
            if (!isVisibleExists) {
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                            "ALTER TABLE products " +
                                    "ADD COLUMN is_visible BOOLEAN DEFAULT TRUE");
                    System.out.println("Added is_visible column to products table");
                }
            }
            
            // Check if new user-related columns exist
            boolean isDeactivatedExists = false;
            try (java.sql.ResultSet rs = conn.getMetaData().getColumns(
                    null, null, "users", "is_deactivated")) {
                isDeactivatedExists = rs.next();
            }
            
            boolean deactivationDateExists = false;
            try (java.sql.ResultSet rs = conn.getMetaData().getColumns(
                    null, null, "users", "deactivation_date")) {
                deactivationDateExists = rs.next();
            }
            
            boolean isDarkModeExists = false;
            try (java.sql.ResultSet rs = conn.getMetaData().getColumns(
                    null, null, "users", "is_dark_mode")) {
                isDarkModeExists = rs.next();
            }
            
            boolean isBannedExists = false;
            try (java.sql.ResultSet rs = conn.getMetaData().getColumns(
                    null, null, "users", "is_banned")) {
                isBannedExists = rs.next();
            }
            
            boolean banReasonExists = false;
            try (java.sql.ResultSet rs = conn.getMetaData().getColumns(
                    null, null, "users", "ban_reason")) {
                banReasonExists = rs.next();
            }
            
            boolean banExpirationExists = false;
            try (java.sql.ResultSet rs = conn.getMetaData().getColumns(
                    null, null, "users", "ban_expiration")) {
                banExpirationExists = rs.next();
            }
            
            // Add user columns if they don't exist
            try (java.sql.Statement stmt = conn.createStatement()) {
                if (!isDeactivatedExists) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN is_deactivated BOOLEAN DEFAULT FALSE");
                    System.out.println("Added is_deactivated column to users table");
                }
                
                if (!deactivationDateExists) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN deactivation_date TIMESTAMP NULL");
                    System.out.println("Added deactivation_date column to users table");
                }
                
                if (!isDarkModeExists) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN is_dark_mode BOOLEAN DEFAULT FALSE");
                    System.out.println("Added is_dark_mode column to users table");
                }
                
                if (!isBannedExists) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN is_banned BOOLEAN DEFAULT FALSE");
                    System.out.println("Added is_banned column to users table");
                }
                
                if (!banReasonExists) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN ban_reason VARCHAR(255) NULL");
                    System.out.println("Added ban_reason column to users table");
                }
                
                if (!banExpirationExists) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN ban_expiration TIMESTAMP NULL");
                    System.out.println("Added ban_expiration column to users table");
                }
            } catch (SQLException e) {
                System.err.println("Error adding columns to users table: " + e.getMessage());
            }
            
        } catch (SQLException e) {
            System.err.println("Error migrating database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}