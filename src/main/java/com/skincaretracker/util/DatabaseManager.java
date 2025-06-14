package com.skincaretracker.util;

import com.skincaretracker.model.User;
import com.skincaretracker.model.Product;
import com.skincaretracker.model.Reminder;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:skincare_tracker.db";
    private static DatabaseManager instance;
    private User currentUser; // Добавляем управление текущим пользователем

    private DatabaseManager() {
        initializeDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // Методы для управления текущим пользователем
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isUserLoggedIn() {
        return currentUser != null;
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            createTables(conn);
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables(Connection conn) throws SQLException {
        // Users table
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                skin_type TEXT,
                preferred_reminder_time TEXT,
                email_notifications BOOLEAN DEFAULT 1,
                push_notifications BOOLEAN DEFAULT 1,
                reminder_notifications BOOLEAN DEFAULT 1
            )
        """;

        // Skin concerns table
        String createSkinConcernsTable = """
            CREATE TABLE IF NOT EXISTS skin_concerns (
                user_id INTEGER,
                concern TEXT,
                PRIMARY KEY (user_id, concern),
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """;

        // Products table
        String createProductsTable = """
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                name TEXT NOT NULL,
                description TEXT,
                allergic BOOLEAN DEFAULT 0,
                rating INTEGER CHECK (rating >= 0 AND rating <= 5),
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """;

        // Reminders table
        String createRemindersTable = """
            CREATE TABLE IF NOT EXISTS reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                product_id INTEGER,
                message TEXT,
                datetime TEXT,
                completed BOOLEAN DEFAULT 0,
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (product_id) REFERENCES products(id)
            )
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createSkinConcernsTable);
            stmt.execute(createProductsTable);
            stmt.execute(createRemindersTable);
        }
    }

    // User operations
    public User createUser(String username, String email, String password) {
        String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password); // In production, this should be hashed

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new User(generatedKeys.getLong(1), username, email, password);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return null;
        }
    }

    public User getUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // In production, this should be hashed

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("password")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user: " + e.getMessage());
        }
        return null;
    }

    // Product operations
    public List<Product> getUserProducts(long userId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("allergic"),
                            rs.getInt("rating")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user products: " + e.getMessage());
        }
        return products;
    }

    public Product createProduct(long userId, String name, String description, boolean allergic, int rating) {
        String sql = "INSERT INTO products (user_id, name, description, allergic, rating) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, name);
            pstmt.setString(3, description);
            pstmt.setBoolean(4, allergic);
            pstmt.setInt(5, rating);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating product failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Product(
                            generatedKeys.getLong(1),
                            name,
                            description,
                            allergic,
                            rating
                    );
                } else {
                    throw new SQLException("Creating product failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating product: " + e.getMessage());
            return null;
        }
    }

    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET name = ?, description = ?, allergic = ?, rating = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setBoolean(3, product.isAllergic());
            pstmt.setInt(4, product.getRating());
            pstmt.setLong(5, product.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating product: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteProduct(long productId) {
        // Сначала удаляем все связанные напоминания
        deleteRemindersByProductId(productId);

        String sql = "DELETE FROM products WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, productId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting product: " + e.getMessage());
            return false;
        }
    }

    // Reminder operations
    public List<Reminder> getUserReminders(long userId) {
        List<Reminder> reminders = new ArrayList<>();
        String sql = """
            SELECT r.*, p.* FROM reminders r 
            JOIN products p ON r.product_id = p.id 
            WHERE r.user_id = ?
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product(
                            rs.getLong("product_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("allergic"),
                            rs.getInt("rating")
                    );

                    Reminder reminder = new Reminder(
                            rs.getLong("id"),
                            product,
                            rs.getString("message"),
                            LocalDateTime.parse(rs.getString("datetime"))
                    );
                    reminder.setCompleted(rs.getBoolean("completed"));
                    reminders.add(reminder);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user reminders: " + e.getMessage());
        }
        return reminders;
    }

    public Reminder createReminder(long userId, long productId, String message, LocalDateTime dateTime) {
        String sql = "INSERT INTO reminders (user_id, product_id, message, datetime) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, userId);
            pstmt.setLong(2, productId);
            pstmt.setString(3, message);
            pstmt.setString(4, dateTime.toString());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating reminder failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // Get the product for the reminder
                    Product product = getProduct(productId);
                    if (product != null) {
                        return new Reminder(
                                generatedKeys.getLong(1),
                                product,
                                message,
                                dateTime
                        );
                    }
                }
                throw new SQLException("Creating reminder failed, no ID obtained.");
            }
        } catch (SQLException e) {
            System.err.println("Error creating reminder: " + e.getMessage());
            return null;
        }
    }

    public boolean updateReminder(Reminder reminder) {
        String sql = "UPDATE reminders SET message = ?, datetime = ?, completed = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, reminder.getMessage());
            pstmt.setString(2, reminder.getDateTime().toString());
            pstmt.setBoolean(3, reminder.isCompleted());
            pstmt.setLong(4, reminder.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating reminder: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteReminder(long reminderId) {
        String sql = "DELETE FROM reminders WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, reminderId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting reminder: " + e.getMessage());
            return false;
        }
    }

    private boolean deleteRemindersByProductId(long productId) {
        String sql = "DELETE FROM reminders WHERE product_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, productId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error deleting reminders by product ID: " + e.getMessage());
            return false;
        }
    }

    private Product getProduct(long productId) {
        String sql = "SELECT * FROM products WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, productId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("allergic"),
                            rs.getInt("rating")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting product: " + e.getMessage());
        }
        return null;
    }

    // Удобные методы для текущего пользователя
    public List<Product> getCurrentUserProducts() {
        if (currentUser == null) return new ArrayList<>();
        return getUserProducts(currentUser.getId());
    }

    public List<Reminder> getCurrentUserReminders() {
        if (currentUser == null) return new ArrayList<>();
        return getUserReminders(currentUser.getId());
    }

    public Product createProductForCurrentUser(String name, String description, boolean allergic, int rating) {
        if (currentUser == null) return null;
        return createProduct(currentUser.getId(), name, description, allergic, rating);
    }

    public Reminder createReminderForCurrentUser(long productId, String message, LocalDateTime dateTime) {
        if (currentUser == null) return null;
        return createReminder(currentUser.getId(), productId, message, dateTime);
    }
}