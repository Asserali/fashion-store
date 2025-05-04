package com.fashionstore.storage;

import com.fashionstore.models.Outfit;
import com.fashionstore.models.Product;
import com.fashionstore.models.ShoppingCart;
import com.fashionstore.models.User;
import com.fashionstore.models.StylePreference;
import com.fashionstore.utils.DatabaseUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.lang.reflect.Field;

public class DataManager {
    // Flag to track if data has been initialized
    private static boolean dataInitialized = false;

    // Data storage (maintained as cache for performance)
    private Map<String, User> users;
    private Map<String, Product> products;
    private Map<String, Outfit> outfits;
    private Map<String, ShoppingCart> carts;
    private User currentUser;

    public DataManager() {
        this.users = new HashMap<>();
        this.products = new HashMap<>();
        this.outfits = new HashMap<>();
        this.carts = new HashMap<>();

        // Initialize database schema if needed
        try {
            // Create database and tables if they don't exist
            DatabaseUtils.createDatabaseIfNotExists();
            
            // Verify that we can connect to the database
            Connection conn = null;
            try {
                conn = DatabaseUtils.getConnection();
                System.out.println("Database connection verified successfully.");
                
                // Test a simple query to make sure tables exist
                PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int userCount = rs.getInt(1);
                    System.out.println("Database ready: " + userCount + " users found in database.");
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                System.err.println("Error verifying database connection: " + e.getMessage());
                throw e; // Re-throw to ensure we don't proceed if database is not available
            } finally {
                DatabaseUtils.close(conn, null, null);
            }
        } catch (Exception e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
            e.printStackTrace();
            // Don't stop app initialization if database fails, it will use in-memory
        }
    }

    // Data loading methods
    public void loadAllData() {
        boolean hasExistingData = false;

        // Load users
        try {
            loadUsersFromDb();
            if (!users.isEmpty()) {
                System.out.println("Loaded " + users.size() + " users");
                hasExistingData = true;
            }
        } catch (SQLException e) {
            System.err.println("Error loading users: " + e.getMessage());
            e.printStackTrace();
        }

        // Load products
        try {
            loadProductsFromDb();
            if (!products.isEmpty()) {
                System.out.println("Loaded " + products.size() + " products");
                hasExistingData = true;
            }
        } catch (SQLException e) {
            System.err.println("Error loading products: " + e.getMessage());
            e.printStackTrace();
        }

        // Load outfits
        try {
            loadOutfitsFromDb();
            if (!outfits.isEmpty()) {
                System.out.println("Loaded " + outfits.size() + " outfits");
                hasExistingData = true;
            }
        } catch (SQLException e) {
            System.err.println("Error loading outfits: " + e.getMessage());
            e.printStackTrace();
        }

        // Load carts
        try {
            loadCartsFromDb();
            if (!carts.isEmpty()) {
                System.out.println("Loaded " + carts.size() + " shopping carts");
                hasExistingData = true;
            }
        } catch (SQLException e) {
            System.err.println("Error loading carts: " + e.getMessage());
            e.printStackTrace();
        }

        // Only initialize sample data if no existing data was found and it hasn't been
        // initialized before
        if (!hasExistingData && !dataInitialized) {
            System.out.println("No existing data found. Initializing sample data...");
            initializeSampleData();
            dataInitialized = true;
            // Save sample data immediately
            saveAllData();
        }
    }

    private void loadUsersFromDb() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtils.getConnection();

            // Load users
            ps = conn.prepareStatement("SELECT * FROM users");
            rs = ps.executeQuery();

            while (rs.next()) {
                String userId = rs.getString("user_id");
                String username = rs.getString("username");
                String email = rs.getString("email");
                String passwordHash = rs.getString("password_hash");

                User user = new User(username, email, passwordHash);
                // We need to set the user ID explicitly since we're reconstructing from DB
                user.setUserId(userId);

                // Set first and last name properly
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));

                // Handle date fields
                Timestamp dateRegistered = rs.getTimestamp("date_registered");
                if (dateRegistered != null) {
                    user.setDateRegistered(new Date(dateRegistered.getTime()));
                }

                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    user.setLastLogin(new Date(lastLogin.getTime()));
                }

                // Load deactivation status and date
                try {
                    boolean isDeactivated = rs.getBoolean("is_deactivated");
                    if (!rs.wasNull() && isDeactivated) {
                        // Call deactivateAccount which handles setting isDeactivated
                        user.deactivateAccount();

                        // Override the deactivation date with the one from the database
                        Timestamp deactivationDate = rs.getTimestamp("deactivation_date");
                        if (deactivationDate != null) {
                            // We don't need to explicitly set this as deactivateAccount() already sets it
                            // But we want to use the date from the database, not the current date
                            Field deactivationDateField = User.class.getDeclaredField("deactivationDate");
                            deactivationDateField.setAccessible(true);
                            deactivationDateField.set(user, new Date(deactivationDate.getTime()));
                        }
                    }
                } catch (SQLException e) {
                    // Column might not exist in older databases
                    System.out.println("Warning: Deactivation columns not found for user " + userId);
                } catch (Exception e) {
                    // This is for the reflection part - if it fails, just stick with the default
                    // deactivation date
                    System.out.println("Warning: Could not set deactivation date via reflection for " + userId);
                }

                // Load dark mode preference
                try {
                    boolean isDarkMode = rs.getBoolean("is_dark_mode");
                    if (!rs.wasNull()) {
                        user.setDarkModeEnabled(isDarkMode);
                    }
                } catch (SQLException e) {
                    // Column might not exist in older databases
                    System.out.println("Warning: Dark mode column not found for user " + userId);
                }

                // Load ban information
                try {
                    boolean isBanned = rs.getBoolean("is_banned");
                    if (!rs.wasNull() && isBanned) {
                        String banReason = rs.getString("ban_reason");
                        user.banUser(banReason);

                        // Set ban expiration if it exists
                        Timestamp banExpiration = rs.getTimestamp("ban_expiration");
                        if (banExpiration != null) {
                            try {
                                // We need to use reflection to set the ban expiration date directly
                                // because banUser() method sets it to null by default (permanent ban)
                                Field banExpirationField = User.class.getDeclaredField("banExpiration");
                                banExpirationField.setAccessible(true);
                                banExpirationField.set(user, new Date(banExpiration.getTime()));
                            } catch (Exception e) {
                                System.out
                                        .println("Warning: Could not set ban expiration via reflection for " + userId);
                            }
                        }
                    }
                } catch (SQLException e) {
                    // Column might not exist in older databases
                    System.out.println("Warning: Ban columns not found for user " + userId);
                }

                // Add to cache
                users.put(userId, user);
            }
            rs.close();
            ps.close();

            // Load wardrobe items for users
            for (User user : users.values()) {
                ps = conn.prepareStatement(
                        "SELECT product_id FROM wardrobe_items WHERE user_id = ?");
                ps.setString(1, user.getUserId());
                rs = ps.executeQuery();

                while (rs.next()) {
                    user.addToWardrobe(rs.getString("product_id"));
                }
                rs.close();
                ps.close();
            }

            // Load style preferences for users
            for (User user : users.values()) {
                ps = conn.prepareStatement(
                        "SELECT preference_type, preference_value, preference_weight " +
                                "FROM style_preferences WHERE user_id = ?");
                ps.setString(1, user.getUserId());
                rs = ps.executeQuery();

                while (rs.next()) {
                    StylePreference preference = new StylePreference(
                            rs.getString("preference_type"),
                            rs.getString("preference_value"),
                            rs.getDouble("preference_weight"));
                    user.addStylePreference(preference);
                }
                rs.close();
                ps.close();
            }

        } finally {
            DatabaseUtils.close(conn, ps, rs);
        }
    }

    private void loadProductsFromDb() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtils.getConnection();
            ps = conn.prepareStatement("SELECT * FROM products");
            rs = ps.executeQuery();

            while (rs.next()) {
                String productId = rs.getString("product_id");
                String name = rs.getString("name");
                String category = rs.getString("category");
                BigDecimal price = rs.getBigDecimal("price");

                Product product = new Product(name, category, price);
                // Set the product ID explicitly since we're reconstructing from DB
                product.setProductId(productId);

                product.setDescription(rs.getString("description"));
                product.setBrand(rs.getString("brand"));
                product.setSubcategory(rs.getString("subcategory"));
                product.setOriginalPrice(rs.getBigDecimal("original_price"));
                product.setCost(rs.getBigDecimal("cost"));
                product.setGender(rs.getString("gender"));
                product.setSize(rs.getString("size"));
                product.setColor(rs.getString("color"));
                product.setMaterial(rs.getString("material"));
                product.setSeason(rs.getString("season"));
                product.setImagePath(rs.getString("image_path"));
                product.setStockQuantity(rs.getInt("stock_quantity"));
                product.setFeatured(rs.getBoolean("is_featured"));

                // Default to true if column doesn't exist or is null
                boolean isVisible = true;
                try {
                    isVisible = rs.getBoolean("is_visible");
                    if (rs.wasNull()) {
                        isVisible = true; // Default to visible if NULL
                    }
                } catch (SQLException e) {
                    // Column doesn't exist, use default value
                    System.out.println("Warning: is_visible column not found, defaulting to true");
                }
                product.setVisible(isVisible);

                // Add to cache
                products.put(productId, product);
            }
            rs.close();
            ps.close();

            // Load product attributes
            for (Product product : products.values()) {
                ps = conn.prepareStatement(
                        "SELECT attribute_name, attribute_value FROM product_attributes " +
                                "WHERE product_id = ?");
                ps.setString(1, product.getProductId());
                rs = ps.executeQuery();

                while (rs.next()) {
                    product.setAttribute(
                            rs.getString("attribute_name"),
                            rs.getString("attribute_value"));
                }
                rs.close();
                ps.close();
            }

        } finally {
            DatabaseUtils.close(conn, ps, rs);
        }
    }

    private void loadOutfitsFromDb() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtils.getConnection();
            ps = conn.prepareStatement("SELECT * FROM outfits");
            rs = ps.executeQuery();

            while (rs.next()) {
                String outfitId = rs.getString("outfit_id");
                String userId = rs.getString("user_id");
                String name = rs.getString("name");

                Outfit outfit = new Outfit(userId, name);
                // Set the outfit ID explicitly since we're reconstructing from DB
                outfit.setOutfitId(outfitId);

                outfit.setDescription(rs.getString("description"));
                outfit.setAiGenerated(rs.getBoolean("ai_generated"));
                outfit.setStyleRating(rs.getDouble("style_rating"));

                String seasonStr = rs.getString("season");
                if (seasonStr != null) {
                    outfit.setSeason(Outfit.OutfitSeason.valueOf(seasonStr));
                }

                String occasionStr = rs.getString("occasion");
                if (occasionStr != null) {
                    outfit.setOccasion(Outfit.OutfitOccasion.valueOf(occasionStr));
                }

                // Add to cache
                outfits.put(outfitId, outfit);

                // Add to user's outfits list
                User user = users.get(userId);
                if (user != null) {
                    user.addOutfit(outfitId);
                }
            }
            rs.close();
            ps.close();

            // Load outfit products
            for (Outfit outfit : outfits.values()) {
                ps = conn.prepareStatement(
                        "SELECT product_id FROM outfit_products WHERE outfit_id = ?");
                ps.setString(1, outfit.getOutfitId());
                rs = ps.executeQuery();

                while (rs.next()) {
                    outfit.addProduct(rs.getString("product_id"));
                }
                rs.close();
                ps.close();
            }

            // Load outfit tags
            for (Outfit outfit : outfits.values()) {
                ps = conn.prepareStatement(
                        "SELECT tag FROM outfit_tags WHERE outfit_id = ?");
                ps.setString(1, outfit.getOutfitId());
                rs = ps.executeQuery();

                while (rs.next()) {
                    outfit.addTag(rs.getString("tag"));
                }
                rs.close();
                ps.close();
            }

        } finally {
            DatabaseUtils.close(conn, ps, rs);
        }
    }

    private void loadCartsFromDb() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtils.getConnection();

            // Get all carts
            ps = conn.prepareStatement("SELECT * FROM shopping_carts");
            rs = ps.executeQuery();

            while (rs.next()) {
                String cartId = rs.getString("cart_id");
                String userId = rs.getString("user_id");

                ShoppingCart cart = new ShoppingCart(userId);
                // Set the cart ID explicitly since we're reconstructing from DB
                cart.setCartId(cartId);
                carts.put(userId, cart);
            }
            rs.close();
            ps.close();

            // Load cart items
            for (ShoppingCart cart : carts.values()) {
                ps = conn.prepareStatement(
                        "SELECT ci.product_id, ci.quantity, p.* " +
                                "FROM cart_items ci " +
                                "JOIN products p ON ci.product_id = p.product_id " +
                                "WHERE ci.cart_id = ?");
                ps.setString(1, cart.getCartId());
                rs = ps.executeQuery();

                while (rs.next()) {
                    // For each cart item, we need the product and quantity
                    String productId = rs.getString("product_id");
                    int quantity = rs.getInt("quantity");

                    // Get the product from cache if it exists, or create a new one
                    Product product = products.get(productId);
                    if (product == null) {
                        String name = rs.getString("name");
                        String category = rs.getString("category");
                        BigDecimal price = rs.getBigDecimal("price");

                        product = new Product(name, category, price);
                        // Set the product ID explicitly since we're reconstructing from DB
                        product.setProductId(productId);
                        products.put(productId, product);
                    }

                    // Add product to cart with specified quantity
                    cart.addItem(product, quantity);
                }
                rs.close();
                ps.close();
            }

        } finally {
            DatabaseUtils.close(conn, ps, rs);
        }
    }

    // Data saving methods
    public void saveAllData() {
        try {
            saveUsers();
            saveProducts();
            saveOutfits();
            saveCarts();
        } catch (Exception e) {
            System.err.println("Error saving all data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveUsers() {
        Connection conn = null;
        PreparedStatement ps = null;
        boolean databaseSaved = false;

        try {
            conn = DatabaseUtils.getConnection();
            conn.setAutoCommit(false);

            // Make sure the users table has the new columns - using more compatible approach
            try {
                // Check if columns exist first, then add them if they don't
                java.sql.DatabaseMetaData dbm = conn.getMetaData();
                java.sql.ResultSet rs = dbm.getColumns(null, null, "users", "is_deactivated");
                boolean isDeactivatedExists = rs.next();
                rs.close();
                
                rs = dbm.getColumns(null, null, "users", "deactivation_date");
                boolean deactivationDateExists = rs.next();
                rs.close();
                
                rs = dbm.getColumns(null, null, "users", "is_dark_mode");
                boolean isDarkModeExists = rs.next();
                rs.close();
                
                rs = dbm.getColumns(null, null, "users", "is_banned");
                boolean isBannedExists = rs.next();
                rs.close();
                
                rs = dbm.getColumns(null, null, "users", "ban_reason");
                boolean banReasonExists = rs.next();
                rs.close();
                
                rs = dbm.getColumns(null, null, "users", "ban_expiration");
                boolean banExpirationExists = rs.next();
                rs.close();
                
                // Add each column individually if it doesn't exist
                if (!isDeactivatedExists) {
                    ps = conn.prepareStatement("ALTER TABLE users ADD COLUMN is_deactivated BOOLEAN DEFAULT FALSE");
                    ps.executeUpdate();
                    ps.close();
                }
                
                if (!deactivationDateExists) {
                    ps = conn.prepareStatement("ALTER TABLE users ADD COLUMN deactivation_date TIMESTAMP NULL");
                    ps.executeUpdate();
                    ps.close();
                }
                
                if (!isDarkModeExists) {
                    ps = conn.prepareStatement("ALTER TABLE users ADD COLUMN is_dark_mode BOOLEAN DEFAULT FALSE");
                    ps.executeUpdate();
                    ps.close();
                }
                
                if (!isBannedExists) {
                    ps = conn.prepareStatement("ALTER TABLE users ADD COLUMN is_banned BOOLEAN DEFAULT FALSE");
                    ps.executeUpdate();
                    ps.close();
                }
                
                if (!banReasonExists) {
                    ps = conn.prepareStatement("ALTER TABLE users ADD COLUMN ban_reason VARCHAR(255) NULL");
                    ps.executeUpdate();
                    ps.close();
                }
                
                if (!banExpirationExists) {
                    ps = conn.prepareStatement("ALTER TABLE users ADD COLUMN ban_expiration TIMESTAMP NULL");
                    ps.executeUpdate();
                    ps.close();
                }
                
                System.out.println("Successfully updated users table schema");
                
            } catch (SQLException e) {
                System.err.println("Warning: Could not update users table schema: " + e.getMessage());
                // Continue anyway, as we'll save to memory
            }

            // Clear existing style preferences to avoid duplicates
            ps = conn.prepareStatement("DELETE FROM style_preferences");
            ps.executeUpdate();
            ps.close();

            // Clear existing wardrobe items to avoid duplicates
            ps = conn.prepareStatement("DELETE FROM wardrobe_items");
            ps.executeUpdate();
            ps.close();

            for (User user : users.values()) {
                try {
                    // First, check if the user exists
                    ps = conn.prepareStatement("SELECT user_id FROM users WHERE user_id = ?");
                    ps.setString(1, user.getUserId());
                    boolean userExists = ps.executeQuery().next();
                    ps.close();
                    
                    if (userExists) {
                        // If user exists, do an UPDATE
                        ps = conn.prepareStatement(
                                "UPDATE users SET " +
                                        "username = ?, " +
                                        "email = ?, " +
                                        "password_hash = ?, " +
                                        "first_name = ?, " +
                                        "last_name = ?, " +
                                        "last_login = ?, " +
                                        "is_deactivated = ?, " +
                                        "deactivation_date = ?, " +
                                        "is_dark_mode = ?, " +
                                        "is_banned = ?, " +
                                        "ban_reason = ?, " +
                                        "ban_expiration = ? " +
                                        "WHERE user_id = ?");
                        
                        ps.setString(1, user.getUsername());
                        ps.setString(2, user.getEmail());
                        ps.setString(3, user.getPasswordHash());
                        ps.setString(4, user.getFirstName());
                        ps.setString(5, user.getLastName());
                        ps.setTimestamp(6, user.getLastLogin() != null ? new Timestamp(user.getLastLogin().getTime()) : null);
                        ps.setBoolean(7, user.isDeactivated());
                        ps.setTimestamp(8,
                                user.getDeactivationDate() != null ? new Timestamp(user.getDeactivationDate().getTime())
                                        : null);
                        ps.setBoolean(9, user.isDarkModeEnabled());
                        ps.setBoolean(10, user.isBanned());
                        ps.setString(11, user.getBanReason());
                        ps.setTimestamp(12,
                                user.getBanExpiration() != null ? new Timestamp(user.getBanExpiration().getTime()) : null);
                        ps.setString(13, user.getUserId());
                        
                    } else {
                        // If user doesn't exist, do an INSERT
                        ps = conn.prepareStatement(
                                "INSERT INTO users " +
                                        "(user_id, username, email, password_hash, first_name, last_name, date_registered, " +
                                        "last_login, is_deactivated, deactivation_date, is_dark_mode, is_banned, ban_reason, ban_expiration) " +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        
                        ps.setString(1, user.getUserId());
                        ps.setString(2, user.getUsername());
                        ps.setString(3, user.getEmail());
                        ps.setString(4, user.getPasswordHash());
                        ps.setString(5, user.getFirstName());
                        ps.setString(6, user.getLastName());
                        ps.setTimestamp(7,
                                user.getDateRegistered() != null ? new Timestamp(user.getDateRegistered().getTime()) : null);
                        ps.setTimestamp(8, user.getLastLogin() != null ? new Timestamp(user.getLastLogin().getTime()) : null);
                        ps.setBoolean(9, user.isDeactivated());
                        ps.setTimestamp(10,
                                user.getDeactivationDate() != null ? new Timestamp(user.getDeactivationDate().getTime())
                                        : null);
                        ps.setBoolean(11, user.isDarkModeEnabled());
                        ps.setBoolean(12, user.isBanned());
                        ps.setString(13, user.getBanReason());
                        ps.setTimestamp(14,
                                user.getBanExpiration() != null ? new Timestamp(user.getBanExpiration().getTime()) : null);
                    }
                    
                    ps.executeUpdate();
                    ps.close();
                    
                    // Log user saving
                    System.out.println("Saved user: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
                
                } catch (SQLException e) {
                    System.err.println("Error saving user " + user.getUsername() + ": " + e.getMessage());
                    throw e; // Rethrow to trigger rollback
                }

                // Insert wardrobe items
                if (!user.getWardrobeItemIds().isEmpty()) {
                    ps = conn.prepareStatement(
                            "INSERT INTO wardrobe_items (user_id, product_id) VALUES (?, ?)");

                    for (String productId : user.getWardrobeItemIds()) {
                        ps.setString(1, user.getUserId());
                        ps.setString(2, productId);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    ps.close();
                }

                // Insert style preferences
                if (!user.getStylePreferences().isEmpty()) {
                    ps = conn.prepareStatement(
                            "INSERT INTO style_preferences " +
                                    "(preference_id, user_id, preference_type, preference_value, preference_weight) " +
                                    "VALUES (?, ?, ?, ?, ?)");

                    for (StylePreference pref : user.getStylePreferences()) {
                        String prefId = UUID.randomUUID().toString();
                        ps.setString(1, prefId);
                        ps.setString(2, user.getUserId());
                        ps.setString(3, pref.getType());
                        ps.setString(4, pref.getValue());
                        ps.setDouble(5, pref.getWeight());
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    ps.close();
                }
            }

            conn.commit();
            databaseSaved = true;
            System.out.println("Saved " + users.size() + " users to database");

        } catch (SQLException e) {
            System.err.println("Error saving users to database: " + e.getMessage());
            e.printStackTrace();

            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null)
                    conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }

            DatabaseUtils.close(conn, ps, null);
        }

        // If we failed to save to the database, log the error
        if (!databaseSaved) {
            System.err.println("Failed to save users to database!");
        }
    }

    private void saveProducts() {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseUtils.getConnection();
            conn.setAutoCommit(false);

            // Clear existing product attributes to avoid duplicates
            ps = conn.prepareStatement("DELETE FROM product_attributes");
            ps.executeUpdate();
            ps.close();

            for (Product product : products.values()) {
                // Upsert product record
                ps = conn.prepareStatement(
                        "INSERT INTO products " +
                                "(product_id, name, description, brand, category, subcategory, price, " +
                                "original_price, cost, gender, size, color, material, season, " +
                                "image_path, stock_quantity, date_added, last_updated, is_featured, " +
                                "average_rating, review_count, is_visible) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE " +
                                "name = VALUES(name), " +
                                "description = VALUES(description), " +
                                "brand = VALUES(brand), " +
                                "category = VALUES(category), " +
                                "subcategory = VALUES(subcategory), " +
                                "price = VALUES(price), " +
                                "original_price = VALUES(original_price), " +
                                "cost = VALUES(cost), " +
                                "gender = VALUES(gender), " +
                                "size = VALUES(size), " +
                                "color = VALUES(color), " +
                                "material = VALUES(material), " +
                                "season = VALUES(season), " +
                                "image_path = VALUES(image_path), " +
                                "stock_quantity = VALUES(stock_quantity), " +
                                "last_updated = VALUES(last_updated), " +
                                "is_featured = VALUES(is_featured), " +
                                "average_rating = VALUES(average_rating), " +
                                "review_count = VALUES(review_count), " +
                                "is_visible = VALUES(is_visible)");

                ps.setString(1, product.getProductId());
                ps.setString(2, product.getName());
                ps.setString(3, product.getDescription());
                ps.setString(4, product.getBrand());
                ps.setString(5, product.getCategory());
                ps.setString(6, product.getSubcategory());
                ps.setBigDecimal(7, product.getPrice());
                ps.setBigDecimal(8, product.getOriginalPrice());
                ps.setBigDecimal(9, product.getCost());
                ps.setString(10, product.getGender());
                ps.setString(11, product.getSize());
                ps.setString(12, product.getColor());
                ps.setString(13, product.getMaterial());
                ps.setString(14, product.getSeason());
                ps.setString(15, product.getImagePath());
                ps.setInt(16, product.getStockQuantity());
                ps.setTimestamp(17,
                        product.getDateAdded() != null ? new Timestamp(product.getDateAdded().getTime()) : null);
                ps.setTimestamp(18,
                        product.getLastUpdated() != null ? new Timestamp(product.getLastUpdated().getTime()) : null);
                ps.setBoolean(19, product.isFeatured());
                ps.setDouble(20, product.getAverageRating());
                ps.setInt(21, product.getReviewCount());
                ps.setBoolean(22, product.isVisible());

                ps.executeUpdate();
                ps.close();

                // Insert product attributes
                Map<String, String> attributes = product.getAllAttributes();
                if (!attributes.isEmpty()) {
                    ps = conn.prepareStatement(
                            "INSERT INTO product_attributes (product_id, attribute_name, attribute_value) " +
                                    "VALUES (?, ?, ?)");

                    for (Map.Entry<String, String> entry : attributes.entrySet()) {
                        ps.setString(1, product.getProductId());
                        ps.setString(2, entry.getKey());
                        ps.setString(3, entry.getValue());
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    ps.close();
                }
            }

            conn.commit();
            System.out.println("Saved " + products.size() + " products to database");

        } catch (SQLException e) {
            System.err.println("Error saving products: " + e.getMessage());
            e.printStackTrace();

            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null)
                    conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }

            DatabaseUtils.close(conn, ps, null);
        }
    }

    private void saveOutfits() {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseUtils.getConnection();
            conn.setAutoCommit(false);

            // Filter out empty outfits
            List<Outfit> emptyOutfits = outfits.values().stream()
                    .filter(outfit -> outfit.getProductIds().isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            System.out.println("saveOutfits: Found " + emptyOutfits.size() + " empty outfits to remove");

            // Remove empty outfits from the collection
            for (Outfit emptyOutfit : emptyOutfits) {
                outfits.remove(emptyOutfit.getOutfitId());

                // Also remove from user's outfit list
                User user = users.get(emptyOutfit.getUserId());
                if (user != null) {
                    user.removeOutfit(emptyOutfit.getOutfitId());
                }

                System.out.println("saveOutfits: Removed empty outfit: " + emptyOutfit.getName() +
                        " (ID: " + emptyOutfit.getOutfitId() + ")");
            }

            // Clear existing outfit products and tags to avoid duplicates
            ps = conn.prepareStatement("DELETE FROM outfit_products");
            ps.executeUpdate();
            ps.close();

            ps = conn.prepareStatement("DELETE FROM outfit_tags");
            ps.executeUpdate();
            ps.close();

            for (Outfit outfit : outfits.values()) {
                // Upsert outfit record
                ps = conn.prepareStatement(
                        "INSERT INTO outfits " +
                                "(outfit_id, user_id, name, description, created_at, last_modified, " +
                                "ai_generated, style_rating, likes_count, season, occasion) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE " +
                                "name = VALUES(name), " +
                                "description = VALUES(description), " +
                                "last_modified = VALUES(last_modified), " +
                                "ai_generated = VALUES(ai_generated), " +
                                "style_rating = VALUES(style_rating), " +
                                "likes_count = VALUES(likes_count), " +
                                "season = VALUES(season), " +
                                "occasion = VALUES(occasion)");

                ps.setString(1, outfit.getOutfitId());
                ps.setString(2, outfit.getUserId());
                ps.setString(3, outfit.getName());
                ps.setString(4, outfit.getDescription());
                ps.setTimestamp(5,
                        outfit.getCreatedAt() != null ? new Timestamp(outfit.getCreatedAt().getTime()) : null);
                ps.setTimestamp(6,
                        outfit.getLastModified() != null ? new Timestamp(outfit.getLastModified().getTime()) : null);
                ps.setBoolean(7, outfit.isAiGenerated());
                ps.setDouble(8, outfit.getStyleRating());
                ps.setInt(9, outfit.getLikesCount());
                ps.setString(10, outfit.getSeason() != null ? outfit.getSeason().name() : null);
                ps.setString(11, outfit.getOccasion() != null ? outfit.getOccasion().name() : null);

                ps.executeUpdate();
                ps.close();

                // Insert outfit products
                if (!outfit.getProductIds().isEmpty()) {
                    ps = conn.prepareStatement(
                            "INSERT INTO outfit_products (outfit_id, product_id) VALUES (?, ?)");

                    for (String productId : outfit.getProductIds()) {
                        ps.setString(1, outfit.getOutfitId());
                        ps.setString(2, productId);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    ps.close();
                }

                // Insert outfit tags
                if (!outfit.getTags().isEmpty()) {
                    ps = conn.prepareStatement(
                            "INSERT INTO outfit_tags (outfit_id, tag) VALUES (?, ?)");

                    for (String tag : outfit.getTags()) {
                        ps.setString(1, outfit.getOutfitId());
                        ps.setString(2, tag);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    ps.close();
                }
            }

            conn.commit();
            System.out.println("Saved " + outfits.size() + " outfits to database");

        } catch (SQLException e) {
            System.err.println("Error saving outfits: " + e.getMessage());
            e.printStackTrace();

            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null)
                    conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }

            DatabaseUtils.close(conn, ps, null);
        }
    }

    private void saveCarts() {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseUtils.getConnection();
            conn.setAutoCommit(false);

            // Clear existing cart items to avoid duplicates
            ps = conn.prepareStatement("DELETE FROM cart_items");
            ps.executeUpdate();
            ps.close();

            for (ShoppingCart cart : carts.values()) {
                // Upsert cart record
                ps = conn.prepareStatement(
                        "INSERT INTO shopping_carts (cart_id, user_id) " +
                                "VALUES (?, ?) " +
                                "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)");

                ps.setString(1, cart.getCartId());
                ps.setString(2, cart.getUserId());

                ps.executeUpdate();
                ps.close();

                // Insert cart items
                if (!cart.getItems().isEmpty()) {
                    ps = conn.prepareStatement(
                            "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)");

                    for (ShoppingCart.CartItem item : cart.getItems()) {
                        ps.setString(1, cart.getCartId());
                        ps.setString(2, item.getProduct().getProductId());
                        ps.setInt(3, item.getQuantity());
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    ps.close();
                }
            }

            conn.commit();
            System.out.println("Saved " + carts.size() + " shopping carts to database");

        } catch (SQLException e) {
            System.err.println("Error saving carts: " + e.getMessage());
            e.printStackTrace();

            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null)
                    conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }

            DatabaseUtils.close(conn, ps, null);
        }
    }

    // User management
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getUser(String userId) {
        return users.get(userId);
    }

    public User getUserByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }

        // Special case for admin user
        if (username.equals("admin")) {
            // First check if we have a real admin user in the data store
            for (User user : users.values()) {
                if (user.getUsername().equals("admin")) {
                    System.out.println("Found admin user in the user store");
                    return user;
                }
            }

            // If not, create a temporary admin user with hardcoded credentials
            System.out.println("Creating temporary admin user");
            User adminUser = new User("admin", "admin@example.com", "admin");
            return adminUser;
        }

        // Normal case - find user by username
        for (User user : users.values()) {
            if (username.equals(user.getUsername())) {
                return user;
            }
        }

        return null;
    }

    public void addUser(User user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("User or user ID cannot be null");
        }
        users.put(user.getUserId(), user);
        saveUsers(); // Save immediately when a user is added
    }

    /**
     * Removes a user from the system
     * 
     * @param userId The ID of the user to remove
     * @return true if the user was removed, false otherwise
     */
    public boolean removeUser(String userId) {
        if (userId == null) {
            return false;
        }

        boolean databaseSuccess = false;
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseUtils.getConnection();
            conn.setAutoCommit(false);

            // Delete from database tables

            // 1. Remove user's style preferences
            ps = conn.prepareStatement("DELETE FROM style_preferences WHERE user_id = ?");
            ps.setString(1, userId);
            ps.executeUpdate();
            ps.close();

            // 2. Remove user's wardrobe items
            ps = conn.prepareStatement("DELETE FROM wardrobe_items WHERE user_id = ?");
            ps.setString(1, userId);
            ps.executeUpdate();
            ps.close();

            // 3. Get user's outfits to remove them
            ps = conn.prepareStatement("SELECT outfit_id FROM outfits WHERE user_id = ?");
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String outfitId = rs.getString("outfit_id");
                // Delete outfit products first
                PreparedStatement psOutfitProducts = conn.prepareStatement(
                        "DELETE FROM outfit_products WHERE outfit_id = ?");
                psOutfitProducts.setString(1, outfitId);
                psOutfitProducts.executeUpdate();
                psOutfitProducts.close();

                // Then delete the outfit
                PreparedStatement psOutfit = conn.prepareStatement(
                        "DELETE FROM outfits WHERE outfit_id = ?");
                psOutfit.setString(1, outfitId);
                psOutfit.executeUpdate();
                psOutfit.close();

                // Also remove from memory
                outfits.remove(outfitId);
            }
            rs.close();
            ps.close();

            // 4. Remove user's shopping cart
            ps = conn.prepareStatement("DELETE FROM cart_items WHERE user_id = ?");
            ps.setString(1, userId);
            ps.executeUpdate();
            ps.close();

            // 5. Finally delete the user
            ps = conn.prepareStatement("DELETE FROM users WHERE user_id = ?");
            ps.setString(1, userId);
            int usersDeleted = ps.executeUpdate();
            ps.close();

            // Commit changes
            conn.commit();
            databaseSuccess = true;

        } catch (SQLException e) {
            System.err.println("Error deleting user " + userId + " from database: " + e.getMessage());
            e.printStackTrace();

            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }

            // We'll still try to remove the user from memory

        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }

            DatabaseUtils.close(conn, ps, null);
        }

        // Always perform in-memory removal, even if database operations failed
        System.out.println("Removing user " + userId + " from in-memory storage");

        // Get user's outfits first to remove them
        User userToRemove = users.get(userId);
        if (userToRemove != null) {
            List<String> outfitIds = new ArrayList<>(userToRemove.getOutfitIds());
            for (String outfitId : outfitIds) {
                outfits.remove(outfitId);
                userToRemove.removeOutfit(outfitId);
            }
        }

        // Remove from memory
        User removedUser = users.remove(userId);
        carts.remove(userId);

        if (removedUser != null) {
            System.out.println("User removed from memory: " + removedUser.getUsername());

            // Try to save the changes to persist them
            try {
                saveUsers();
                saveOutfits();
            } catch (Exception e) {
                System.err.println("Error saving changes after user removal: " + e.getMessage());
            }

            return true;
        }

        return databaseSuccess;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    // Product management
    public void addProduct(Product product) {
        if (product == null || product.getProductId() == null) {
            throw new IllegalArgumentException("Product or product ID cannot be null");
        }
        products.put(product.getProductId(), product);
        saveProducts(); // Save immediately when a product is added
    }

    public Product getProduct(String productId) {
        return products.get(productId);
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    /**
     * Gets all visible products from the store
     * 
     * @return List of all visible products
     */
    public List<Product> getVisibleProducts() {
        return products.values().stream()
                .filter(product -> product != null && product.isVisible())
                .collect(Collectors.toList());
    }

    public List<Product> getProductsByIds(List<String> productIds) {
        if (productIds == null) {
            return new ArrayList<>();
        }

        return productIds.stream()
                .map(this::getProduct)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void updateProduct(Product product) {
        if (product == null || product.getProductId() == null) {
            throw new IllegalArgumentException("Product or product ID cannot be null");
        }
        products.put(product.getProductId(), product);
        saveProducts(); // Save immediately when a product is updated
    }

    public void deleteProduct(String productId) {
        if (productId == null) {
            return;
        }

        System.out.println("Deleting product with ID: " + productId);

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseUtils.getConnection();
            conn.setAutoCommit(false);

            // First delete the product from database tables to maintain referential
            // integrity

            // 1. Remove from product attributes
            ps = conn.prepareStatement("DELETE FROM product_attributes WHERE product_id = ?");
            ps.setString(1, productId);
            int attributesDeleted = ps.executeUpdate();
            System.out.println("Deleted " + attributesDeleted + " product attributes");
            ps.close();

            // 2. Remove from outfit products
            ps = conn.prepareStatement("DELETE FROM outfit_products WHERE product_id = ?");
            ps.setString(1, productId);
            int outfitProductsDeleted = ps.executeUpdate();
            System.out.println("Removed product from " + outfitProductsDeleted + " outfits");
            ps.close();

            // 3. Remove from wardrobe items
            ps = conn.prepareStatement("DELETE FROM wardrobe_items WHERE product_id = ?");
            ps.setString(1, productId);
            int wardrobeItemsDeleted = ps.executeUpdate();
            System.out.println("Removed product from " + wardrobeItemsDeleted + " wardrobes");
            ps.close();

            // 4. Remove from cart items
            ps = conn.prepareStatement("DELETE FROM cart_items WHERE product_id = ?");
            ps.setString(1, productId);
            int cartItemsDeleted = ps.executeUpdate();
            System.out.println("Removed product from " + cartItemsDeleted + " shopping carts");
            ps.close();

            // 5. Finally delete the product
            ps = conn.prepareStatement("DELETE FROM products WHERE product_id = ?");
            ps.setString(1, productId);
            int productsDeleted = ps.executeUpdate();
            System.out.println("Deleted " + productsDeleted + " products");
            ps.close();

            // Commit changes to database
            conn.commit();
            System.out.println("Database transaction committed successfully");

            // Now update the in-memory collections

            // Remove product from the products map
            Product removedProduct = products.remove(productId);
            System.out.println("Removed from in-memory products map: " + (removedProduct != null ? "yes" : "no"));

            // Remove the product from all user wardrobes
            int inMemoryWardrobesUpdated = 0;
            for (User user : users.values()) {
                if (user.getWardrobeItemIds().remove(productId)) {
                    inMemoryWardrobesUpdated++;
                }
            }
            System.out.println("Updated " + inMemoryWardrobesUpdated + " in-memory user wardrobes");

            // Remove the product from all outfits
            int inMemoryOutfitsUpdated = 0;
            for (Outfit outfit : outfits.values()) {
                if (outfit.getProductIds().remove(productId)) {
                    inMemoryOutfitsUpdated++;
                }
            }
            System.out.println("Updated " + inMemoryOutfitsUpdated + " in-memory outfits");

            // Remove the product from all shopping carts
            int inMemoryCartsUpdated = 0;
            for (ShoppingCart cart : carts.values()) {
                int cartSizeBefore = cart.getItemCount();
                cart.removeItem(productId);
                if (cart.getItemCount() < cartSizeBefore) {
                    inMemoryCartsUpdated++;
                }
            }
            System.out.println("Updated " + inMemoryCartsUpdated + " in-memory shopping carts");

            // Double-check the product is gone
            if (getProduct(productId) != null) {
                System.err.println("WARNING: Product still exists in memory after deletion!");
            } else {
                System.out.println("Product " + productId + " successfully removed from memory");
            }

            // Force a reload to ensure everything is in sync
            loadAllData();

            // Double-check after reload
            if (getProduct(productId) != null) {
                System.err.println("CRITICAL ERROR: Product reappeared after reload!");
            } else {
                System.out.println("Product " + productId + " confirmed deleted after reload");
            }

        } catch (SQLException e) {
            System.err.println("Error deleting product " + productId + ": " + e.getMessage());
            e.printStackTrace();

            try {
                if (conn != null)
                    conn.rollback();
                System.out.println("Transaction rolled back due to error");
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
                ex.printStackTrace();
            }
        } finally {
            try {
                if (conn != null)
                    conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
                e.printStackTrace();
            }

            DatabaseUtils.close(conn, ps, null);
        }
    }

    // Outfit management
    public void addOutfit(Outfit outfit) {
        if (outfit == null || outfit.getOutfitId() == null) {
            throw new IllegalArgumentException("Outfit or outfit ID cannot be null");
        }

        if (validateOutfit(outfit)) {
            outfits.put(outfit.getOutfitId(), outfit);
            User user = users.get(outfit.getUserId());
            if (user != null) {
                user.addOutfit(outfit.getOutfitId());
                saveUsers(); // Save user changes
            }
            saveOutfits(); // Save immediately when an outfit is added
        }
    }

    public Outfit getOutfit(String outfitId) {
        return outfits.get(outfitId);
    }

    public List<Outfit> getUserOutfits(String userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        return outfits.values().stream()
                .filter(outfit -> outfit.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Outfit> getUserOutfitsWithProducts(String userId) {
        List<Outfit> userOutfits = getUserOutfits(userId);
        List<Outfit> result = new ArrayList<>();

        System.out.println("getUserOutfitsWithProducts: Found " + userOutfits.size() +
                " outfits for user " + userId);

        // First, try to get outfits directly from database with their products
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtils.getConnection();

            // This query gets only outfits that have products
            ps = conn.prepareStatement(
                    "SELECT o.*, COUNT(op.product_id) as product_count " +
                            "FROM outfits o " +
                            "LEFT JOIN outfit_products op ON o.outfit_id = op.outfit_id " +
                            "WHERE o.user_id = ? " +
                            "GROUP BY o.outfit_id " +
                            "HAVING product_count > 0");
            ps.setString(1, userId);
            rs = ps.executeQuery();

            Set<String> validOutfitIds = new HashSet<>();
            while (rs.next()) {
                validOutfitIds.add(rs.getString("outfit_id"));
            }

            System.out.println("getUserOutfitsWithProducts: Found " + validOutfitIds.size() +
                    " valid outfits with products in database");

            // Filter our local outfit list to include only outfits with products
            userOutfits = userOutfits.stream()
                    .filter(outfit -> validOutfitIds.contains(outfit.getOutfitId()))
                    .collect(java.util.stream.Collectors.toList());

        } catch (SQLException e) {
            System.err.println("getUserOutfitsWithProducts: Database error: " + e.getMessage());
            // Continue with the old method if there's a database error
        } finally {
            DatabaseUtils.close(conn, ps, rs);
        }

        for (Outfit outfit : userOutfits) {
            // Make sure this outfit is in the outfits map
            outfits.put(outfit.getOutfitId(), outfit);

            Outfit detailed = new Outfit(outfit.getUserId(), outfit.getName());
            detailed.setOutfitId(outfit.getOutfitId()); // Ensure ID is preserved
            detailed.setDescription(outfit.getDescription());
            detailed.setAiGenerated(outfit.isAiGenerated());
            detailed.setStyleRating(outfit.getStyleRating());
            detailed.setSeason(outfit.getSeason());
            detailed.setOccasion(outfit.getOccasion());

            // Add all products
            for (String productId : outfit.getProductIds()) {
                detailed.addProduct(productId);
            }

            // Also ensure this outfit is in the outfits map with the correct ID
            outfits.put(detailed.getOutfitId(), detailed);

            System.out.println("getUserOutfitsWithProducts: Added outfit " + detailed.getName() +
                    " (ID: " + detailed.getOutfitId() + ") to result and outfits map");

            result.add(detailed);
        }

        return result;
    }

    public boolean validateOutfit(Outfit outfit) {
        if (outfit == null || outfit.getUserId() == null ||
                outfit.getName() == null || outfit.getName().isEmpty()) {
            return false;
        }

        for (String productId : outfit.getProductIds()) {
            if (!products.containsKey(productId)) {
                return false;
            }
        }

        return true;
    }

    public void updateOutfit(Outfit outfit) {
        if (outfit == null || outfit.getOutfitId() == null) {
            throw new IllegalArgumentException("Outfit or outfit ID cannot be null");
        }

        if (validateOutfit(outfit)) {
            outfits.put(outfit.getOutfitId(), outfit);
            saveOutfits(); // Save immediately when an outfit is updated
        }
    }

    /**
     * Removes an outfit and returns true if successful
     */
    public boolean removeOutfit(String outfitId) {
        if (outfitId == null) {
            System.err.println("removeOutfit: outfitId is null");
            return false;
        }

        System.out.println("removeOutfit: Trying to remove outfit with ID: " + outfitId);

        boolean outfitFound = false;
        Outfit outfitToRemove = null;

        // First check if it's in the outfits map
        if (outfits.containsKey(outfitId)) {
            System.out.println("removeOutfit: Found outfit in outfits map");
            outfitToRemove = outfits.get(outfitId);
            outfitFound = true;
        } else {
            // If not in the map, search through all users' outfits to find it
            System.out.println("removeOutfit: Outfit not found in outfits map, searching users...");
            for (User user : users.values()) {
                if (user.hasOutfit(outfitId)) {
                    System.out.println("removeOutfit: Found outfit in user " + user.getUsername() + "'s outfit list");

                    // Create a temporary outfit for removal purposes
                    outfitToRemove = new Outfit(user.getUserId(), "Temporary");
                    outfitToRemove.setOutfitId(outfitId);

                    outfitFound = true;
                    break;
                }
            }
        }

        if (!outfitFound || outfitToRemove == null) {
            System.err.println("removeOutfit: Could not find outfit with ID: " + outfitId + " anywhere");
            return false;
        }

        try {
            // First, delete from database to maintain referential integrity
            Connection conn = null;
            PreparedStatement ps = null;

            try {
                conn = DatabaseUtils.getConnection();
                conn.setAutoCommit(false);

                // Delete outfit products
                ps = conn.prepareStatement("DELETE FROM outfit_products WHERE outfit_id = ?");
                ps.setString(1, outfitId);
                int productsDeleted = ps.executeUpdate();
                System.out.println("removeOutfit: Deleted " + productsDeleted + " outfit products from database");
                ps.close();

                // Delete outfit tags
                ps = conn.prepareStatement("DELETE FROM outfit_tags WHERE outfit_id = ?");
                ps.setString(1, outfitId);
                int tagsDeleted = ps.executeUpdate();
                System.out.println("removeOutfit: Deleted " + tagsDeleted + " outfit tags from database");
                ps.close();

                // Delete the outfit itself
                ps = conn.prepareStatement("DELETE FROM outfits WHERE outfit_id = ?");
                ps.setString(1, outfitId);
                int outfitsDeleted = ps.executeUpdate();
                System.out.println("removeOutfit: Deleted " + outfitsDeleted + " outfits from database");
                ps.close();

                conn.commit();
                System.out.println("removeOutfit: Database changes committed");
            } catch (SQLException e) {
                System.err.println("removeOutfit: Database error: " + e.getMessage());
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        System.err.println("removeOutfit: Error rolling back: " + ex.getMessage());
                    }
                }
                // Continue with in-memory deletion despite DB error
            } finally {
                DatabaseUtils.close(conn, ps, null);
            }

            // Remove from user's outfit list
            String userId = outfitToRemove.getUserId();
            User user = users.get(userId);
            if (user != null) {
                System.out.println("removeOutfit: Found user: " + user.getUsername());
                boolean removed = user.removeOutfit(outfitId);
                System.out.println("removeOutfit: Removed from user's outfits: " + removed);
            } else {
                System.err.println("removeOutfit: User not found: " + userId);
                // Even if user not found, continue with removing from outfits map
            }

            // Remove from outfits map
            Outfit removedOutfit = outfits.remove(outfitId);
            System.out.println("removeOutfit: Removed from outfits map: " + (removedOutfit != null));

            // Save changes
            saveOutfits();
            saveUsers();
            System.out.println("removeOutfit: Changes saved to data store");

            return true;
        } catch (Exception e) {
            System.err.println("removeOutfit: Exception during outfit removal: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Wardrobe management
    public List<Product> getUserWardrobe(String userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        User user = users.get(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        return getProductsByIds(user.getWardrobeItemIds());
    }

    // Shopping cart management
    public ShoppingCart getCart(String userId) {
        if (userId == null) {
            return new ShoppingCart("guest");
        }

        ShoppingCart cart = carts.get(userId);
        if (cart == null) {
            cart = new ShoppingCart(userId);
            carts.put(userId, cart);
        }
        return cart;
    }

    public void saveCart(ShoppingCart cart) {
        if (cart == null || cart.getUserId() == null) {
            throw new IllegalArgumentException("Cart or user ID cannot be null");
        }
        carts.put(cart.getUserId(), cart);
        saveCarts(); // Save immediately when a cart is updated
    }

    // Sample data initialization
    public void initializeSampleData() {
        // Create admin user if it doesn't exist
        if (getUserByUsername("admin") == null) {
            User adminUser = new User("admin", "admin@example.com", "admin");
            users.put(adminUser.getUserId(), adminUser);
        }

        // Check if we already have sample products
        if (!products.isEmpty()) {
            return;
        }

        // Sample products
        Product tshirt = createSampleProduct("Basic T-Shirt", "Tops", "19.99", "black", "M");
        Product jeans = createSampleProduct("Slim Jeans", "Bottoms", "49.99", "blue", "32");
        Product shoes = createSampleProduct("Running Shoes", "Shoes", "79.99", "white", "10");
        Product jacket = createSampleProduct("Denim Jacket", "Outerwear", "89.99", "blue", "L");

        // Sample user (only create if no users exist)
        if (users.isEmpty()) {
            User sampleUser = new User("sampleuser", "user@example.com", "password123");
            users.put(sampleUser.getUserId(), sampleUser);

            // Sample outfits
            Outfit casualOutfit = new Outfit(sampleUser.getUserId(), "Casual Look");
            casualOutfit.addProduct(tshirt.getProductId());
            casualOutfit.addProduct(jeans.getProductId());
            outfits.put(casualOutfit.getOutfitId(), casualOutfit);
            sampleUser.addOutfit(casualOutfit.getOutfitId());

            Outfit sportyOutfit = new Outfit(sampleUser.getUserId(), "Sporty Look");
            sportyOutfit.addProduct(tshirt.getProductId());
            sportyOutfit.addProduct(shoes.getProductId());
            outfits.put(sportyOutfit.getOutfitId(), sportyOutfit);
            sampleUser.addOutfit(sportyOutfit.getOutfitId());
        }

        System.out.println("Sample data initialized");
    }

    private Product createSampleProduct(String name, String category, String price, String color, String size) {
        Product product = new Product(name, category, new BigDecimal(price));
        product.setColor(color);
        product.setSize(size);
        product.setDescription("Sample " + name + " in " + color);
        product.setStockQuantity(10); // Set some initial stock
        products.put(product.getProductId(), product);
        return product;
    }

    /**
     * Gets all products that are part of an outfit
     */
    public List<Product> getProductsForOutfit(Outfit outfit) {
        if (outfit == null || outfit.getProductIds() == null || outfit.getProductIds().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> productIds = new ArrayList<>(outfit.getProductIds());
        return getProductsByIds(productIds);
    }

    /**
     * Deactivates a user account temporarily.
     * The account can be reactivated within a week.
     *
     * @param userId The ID of the user to deactivate
     * @return true if the account was deactivated, false otherwise
     */
    public boolean deactivateUser(String userId) {
        User user = users.get(userId);
        if (user == null) {
            return false;
        }

        // Cannot deactivate admin
        if (user.getUsername().equals("admin")) {
            return false;
        }

        user.deactivateAccount();
        saveUsers(); // Save changes immediately
        return true;
    }

    /**
     * Reactivates a deactivated user account if it's within the grace period.
     *
     * @param userId The ID of the user to reactivate
     * @return true if the account was reactivated, false if it's expired or wasn't
     *         deactivated
     */
    public boolean reactivateUser(String userId) {
        User user = users.get(userId);
        if (user == null) {
            return false;
        }

        if (!user.isDeactivated()) {
            return false; // User wasn't deactivated
        }

        if (user.isDeactivationPeriodExpired()) {
            return false; // Deactivation period expired, cannot reactivate
        }

        user.reactivateAccount();
        saveUsers(); // Save changes immediately
        return true;
    }

    /**
     * Bans a user account permanently (admin action).
     *
     * @param userId The ID of the user to ban
     * @param reason The reason for the ban
     * @return true if the user was banned, false otherwise
     */
    public boolean banUser(String userId, String reason) {
        User user = users.get(userId);
        if (user == null) {
            return false;
        }

        // Cannot ban admin
        if (user.getUsername().equals("admin")) {
            return false;
        }

        user.banUser(reason);
        saveAllData(); // Save changes immediately
        return true;
    }

    /**
     * Bans a user account temporarily (admin action).
     *
     * @param userId The ID of the user to ban
     * @param reason The reason for the ban
     * @param days   Number of days for the temporary ban
     * @return true if the user was banned, false otherwise
     */
    public boolean banUserTemporarily(String userId, String reason, int days) {
        User user = users.get(userId);
        if (user == null) {
            return false;
        }

        // Cannot ban admin
        if (user.getUsername().equals("admin")) {
            return false;
        }

        user.banUserTemporarily(reason, days);
        saveAllData(); // Save changes immediately
        return true;
    }

    /**
     * Unbans a previously banned user.
     *
     * @param userId The ID of the user to unban
     * @return true if the user was unbanned, false otherwise
     */
    public boolean unbanUser(String userId) {
        User user = users.get(userId);
        if (user == null) {
            return false;
        }

        if (!user.isBanned()) {
            return false; // User wasn't banned
        }

        user.unbanUser();
        saveAllData(); // Save changes immediately
        return true;
    }

    /**
     * Permanently deletes deactivated accounts that have expired their grace
     * period.
     * Should be called periodically by the application.
     *
     * @return The number of accounts that were permanently deleted
     */
    public int cleanupExpiredAccounts() {
        int deletedCount = 0;

        List<String> expiredUserIds = users.values().stream()
                .filter(User::isDeactivated)
                .filter(User::isDeactivationPeriodExpired)
                .map(User::getUserId)
                .collect(java.util.stream.Collectors.toList());

        for (String userId : expiredUserIds) {
            if (removeUser(userId)) {
                deletedCount++;
            }
        }

        return deletedCount;
    }

    /**
     * Saves a user's UI preference for dark mode.
     *
     * @param userId          The ID of the user
     * @param darkModeEnabled Whether dark mode is enabled
     * @return true if the preference was saved, false otherwise
     */
    public boolean saveUserDarkModePreference(String userId, boolean darkModeEnabled) {
        User user = users.get(userId);
        if (user == null) {
            return false;
        }

        user.setDarkModeEnabled(darkModeEnabled);
        saveUsers(); // Save changes immediately
        return true;
    }

    /**
     * Gets whether a user has dark mode enabled.
     *
     * @param userId The ID of the user
     * @return true if dark mode is enabled, false otherwise
     */
    public boolean isUserDarkModeEnabled(String userId) {
        User user = users.get(userId);
        if (user == null) {
            return false; // Default to light mode
        }

        return user.isDarkModeEnabled();
    }
}