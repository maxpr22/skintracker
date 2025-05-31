package com.skincaretracker.controller;

import com.skincaretracker.model.User;
import com.skincaretracker.util.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class ProfileController {
    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private ComboBox<String> skinTypeComboBox;

    @FXML
    private CheckBox acneCheckBox;

    @FXML
    private CheckBox drynessCheckBox;

    @FXML
    private CheckBox sensitivityCheckBox;

    @FXML
    private CheckBox agingCheckBox;

    @FXML
    private CheckBox pigmentationCheckBox;

    @FXML
    private CheckBox emailNotificationsCheckBox;

    @FXML
    private CheckBox pushNotificationsCheckBox;

    @FXML
    private CheckBox reminderNotificationsCheckBox;

    @FXML
    private ComboBox<Integer> reminderHourComboBox;

    @FXML
    private ComboBox<Integer> reminderMinuteComboBox;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    private DatabaseManager databaseManager;
    private User currentUser;

    @FXML
    public void initialize() {
        databaseManager = DatabaseManager.getInstance();
        currentUser = databaseManager.getCurrentUser();

        if (currentUser == null) {
            showError("No user logged in");
            return;
        }

        setupComboBoxes();
        loadUserProfile();
    }

    private void setupComboBoxes() {
        // Setup skin type options
        skinTypeComboBox.setItems(FXCollections.observableArrayList(
                "Normal",
                "Dry",
                "Oily",
                "Combination",
                "Sensitive"
        ));

        // Setup time ComboBoxes
        reminderHourComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(0, 23).boxed().toList()
        ));
        reminderMinuteComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(0, 59).boxed().toList()
        ));
    }

    private void loadUserProfile() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:skincare_tracker.db")) {
            // Load basic user info
            String userSql = "SELECT * FROM users WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(userSql)) {
                pstmt.setLong(1, currentUser.getId());

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        usernameField.setText(rs.getString("username"));
                        emailField.setText(rs.getString("email"));

                        String skinType = rs.getString("skin_type");
                        if (skinType != null) {
                            skinTypeComboBox.setValue(skinType);
                        }

                        // Load notification preferences
                        emailNotificationsCheckBox.setSelected(rs.getBoolean("email_notifications"));
                        pushNotificationsCheckBox.setSelected(rs.getBoolean("push_notifications"));
                        reminderNotificationsCheckBox.setSelected(rs.getBoolean("reminder_notifications"));

                        // Load reminder time
                        String reminderTime = rs.getString("preferred_reminder_time");
                        if (reminderTime != null && reminderTime.contains(":")) {
                            String[] timeParts = reminderTime.split(":");
                            reminderHourComboBox.setValue(Integer.parseInt(timeParts[0]));
                            reminderMinuteComboBox.setValue(Integer.parseInt(timeParts[1]));
                        } else {
                            // Set default time
                            reminderHourComboBox.setValue(9);
                            reminderMinuteComboBox.setValue(0);
                        }
                    }
                }
            }

            // Load skin concerns
            loadSkinConcerns(conn);

        } catch (SQLException e) {
            System.err.println("Error loading user profile: " + e.getMessage());
            showError("Error loading user profile");
        }
    }

    private void loadSkinConcerns(Connection conn) throws SQLException {
        // Reset all checkboxes
        acneCheckBox.setSelected(false);
        drynessCheckBox.setSelected(false);
        sensitivityCheckBox.setSelected(false);
        agingCheckBox.setSelected(false);
        pigmentationCheckBox.setSelected(false);

        String concernsSql = "SELECT concern FROM skin_concerns WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(concernsSql)) {
            pstmt.setLong(1, currentUser.getId());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String concern = rs.getString("concern");
                    switch (concern.toLowerCase()) {
                        case "acne" -> acneCheckBox.setSelected(true);
                        case "dryness" -> drynessCheckBox.setSelected(true);
                        case "sensitivity" -> sensitivityCheckBox.setSelected(true);
                        case "aging" -> agingCheckBox.setSelected(true);
                        case "pigmentation" -> pigmentationCheckBox.setSelected(true);
                    }
                }
            }
        }
    }

    @FXML
    private void updatePersonalInfo() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String skinType = skinTypeComboBox.getValue();

        if (username.isEmpty() || email.isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:skincare_tracker.db")) {
            String sql = "UPDATE users SET username = ?, email = ?, skin_type = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, skinType);
                pstmt.setLong(4, currentUser.getId());

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Update current user object
                    currentUser.setUsername(username);
                    currentUser.setEmail(email);
                    showSuccess("Personal information updated successfully");
                } else {
                    showError("Failed to update personal information");
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                showError("Username or email already exists");
            } else {
                System.err.println("Error updating personal info: " + e.getMessage());
                showError("Error updating personal information");
            }
        }
    }

    @FXML
    private void updateSkinConcerns() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:skincare_tracker.db")) {
            // First, delete existing skin concerns
            String deleteSql = "DELETE FROM skin_concerns WHERE user_id = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setLong(1, currentUser.getId());
                deleteStmt.executeUpdate();
            }

            // Then, insert new skin concerns
            List<String> selectedConcerns = new ArrayList<>();
            if (acneCheckBox.isSelected()) selectedConcerns.add("acne");
            if (drynessCheckBox.isSelected()) selectedConcerns.add("dryness");
            if (sensitivityCheckBox.isSelected()) selectedConcerns.add("sensitivity");
            if (agingCheckBox.isSelected()) selectedConcerns.add("aging");
            if (pigmentationCheckBox.isSelected()) selectedConcerns.add("pigmentation");

            if (!selectedConcerns.isEmpty()) {
                String insertSql = "INSERT INTO skin_concerns (user_id, concern) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    for (String concern : selectedConcerns) {
                        insertStmt.setLong(1, currentUser.getId());
                        insertStmt.setString(2, concern);
                        insertStmt.executeUpdate();
                    }
                }
            }

            showSuccess("Skin concerns updated successfully");

        } catch (SQLException e) {
            System.err.println("Error updating skin concerns: " + e.getMessage());
            showError("Error updating skin concerns");
        }
    }

    @FXML
    private void updateNotificationPreferences() {
        if (reminderNotificationsCheckBox.isSelected() &&
                (reminderHourComboBox.getValue() == null ||
                        reminderMinuteComboBox.getValue() == null)) {
            showError("Please select reminder time");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:skincare_tracker.db")) {
            String reminderTime = null;
            if (reminderNotificationsCheckBox.isSelected()) {
                reminderTime = String.format("%02d:%02d",
                        reminderHourComboBox.getValue(),
                        reminderMinuteComboBox.getValue());
            }

            String sql = """
                UPDATE users SET 
                email_notifications = ?, 
                push_notifications = ?, 
                reminder_notifications = ?, 
                preferred_reminder_time = ? 
                WHERE id = ?
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBoolean(1, emailNotificationsCheckBox.isSelected());
                pstmt.setBoolean(2, pushNotificationsCheckBox.isSelected());
                pstmt.setBoolean(3, reminderNotificationsCheckBox.isSelected());
                pstmt.setString(4, reminderTime);
                pstmt.setLong(5, currentUser.getId());

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    showSuccess("Notification preferences updated successfully");
                } else {
                    showError("Failed to update notification preferences");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating notification preferences: " + e.getMessage());
            showError("Error updating notification preferences");
        }
    }

    @FXML
    private void changePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all password fields");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("New passwords do not match");
            return;
        }

        if (!isValidPassword(newPassword)) {
            showError("Password must be at least 8 characters long and contain at least one number, " +
                    "one uppercase letter, and one special character");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:skincare_tracker.db")) {
            // First, verify current password
            String verifySql = "SELECT password FROM users WHERE id = ?";
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                verifyStmt.setLong(1, currentUser.getId());

                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        if (!currentPassword.equals(storedPassword)) {
                            showError("Current password is incorrect");
                            return;
                        }
                    } else {
                        showError("User not found");
                        return;
                    }
                }
            }

            // Update password
            String updateSql = "UPDATE users SET password = ? WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, newPassword); // В продакшені треба хешувати
                updateStmt.setLong(2, currentUser.getId());

                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected > 0) {
                    showSuccess("Password changed successfully");
                    // Clear password fields
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                } else {
                    showError("Failed to change password");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            showError("Error changing password");
        }
    }

    @FXML
    private void deleteAccount() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Account");
        confirmDialog.setHeaderText("Are you sure you want to delete your account?");
        confirmDialog.setContentText("This action cannot be undone. All your data will be permanently deleted.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:skincare_tracker.db")) {
                conn.setAutoCommit(false); // Start transaction

                try {
                    // Delete in correct order due to foreign key constraints

                    // Delete reminders
                    String deleteRemindersSql = "DELETE FROM reminders WHERE user_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteRemindersSql)) {
                        stmt.setLong(1, currentUser.getId());
                        stmt.executeUpdate();
                    }

                    // Delete products
                    String deleteProductsSql = "DELETE FROM products WHERE user_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteProductsSql)) {
                        stmt.setLong(1, currentUser.getId());
                        stmt.executeUpdate();
                    }

                    // Delete skin concerns
                    String deleteConcernsSql = "DELETE FROM skin_concerns WHERE user_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteConcernsSql)) {
                        stmt.setLong(1, currentUser.getId());
                        stmt.executeUpdate();
                    }

                    // Delete user
                    String deleteUserSql = "DELETE FROM users WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteUserSql)) {
                        stmt.setLong(1, currentUser.getId());
                        int rowsAffected = stmt.executeUpdate();

                        if (rowsAffected > 0) {
                            conn.commit(); // Commit transaction

                            // Clear current user from DatabaseManager
                            databaseManager.setCurrentUser(null);

                            showSuccess("Account deleted successfully");

                            // TODO: Redirect to login page
                            // Platform.runLater(() -> {
                            //     try {
                            //         // Load login scene
                            //     } catch (Exception e) {
                            //         e.printStackTrace();
                            //     }
                            // });

                        } else {
                            conn.rollback();
                            showError("Failed to delete account");
                        }
                    }

                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                System.err.println("Error deleting account: " + e.getMessage());
                showError("Error deleting account");
            }
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}