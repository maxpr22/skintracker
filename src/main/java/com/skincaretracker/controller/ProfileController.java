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
            showError("Користувач не увійшов в систему");
            return;
        }

        setupComboBoxes();
        loadUserProfile();
    }

    private void setupComboBoxes() {
        skinTypeComboBox.setItems(FXCollections.observableArrayList(
                "Нормальна",
                "Суха",
                "Жирна",
                "Комбінована",
                "Чутлива"
        ));

        reminderHourComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(0, 23).boxed().toList()
        ));
        reminderMinuteComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(0, 59).boxed().toList()
        ));
    }

    private void loadUserProfile() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:skincare_tracker.db")) {
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

                        emailNotificationsCheckBox.setSelected(rs.getBoolean("email_notifications"));
                        pushNotificationsCheckBox.setSelected(rs.getBoolean("push_notifications"));
                        reminderNotificationsCheckBox.setSelected(rs.getBoolean("reminder_notifications"));

                        String reminderTime = rs.getString("preferred_reminder_time");
                        if (reminderTime != null && reminderTime.contains(":")) {
                            String[] timeParts = reminderTime.split(":");
                            reminderHourComboBox.setValue(Integer.parseInt(timeParts[0]));
                            reminderMinuteComboBox.setValue(Integer.parseInt(timeParts[1]));
                        } else {
                            reminderHourComboBox.setValue(9);
                            reminderMinuteComboBox.setValue(0);
                        }
                    }
                }
            }

            loadSkinConcerns(conn);

        } catch (SQLException e) {
            System.err.println("Error loading user profile: " + e.getMessage());
            showError("Помилка завантаження профілю користувача");
        }
    }

    private void loadSkinConcerns(Connection conn) throws SQLException {
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
            showError("Будь ласка, заповніть всі обов'язкові поля");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Будь ласка, введіть дійсну електронну адресу");
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
                    currentUser.setUsername(username);
                    currentUser.setEmail(email);
                    showSuccess("Особисту інформацію успішно оновлено");
                } else {
                    showError("Не вдалося оновити особисту інформацію");
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                showError("Ім'я користувача або електронна пошта вже існують");
            } else {
                System.err.println("Error updating personal info: " + e.getMessage());
                showError("Помилка оновлення особистої інформації");
            }
        }
    }

    @FXML
    private void updateSkinConcerns() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:skincare_tracker.db")) {
            String deleteSql = "DELETE FROM skin_concerns WHERE user_id = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setLong(1, currentUser.getId());
                deleteStmt.executeUpdate();
            }

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

            showSuccess("Проблеми шкіри успішно оновлено");

        } catch (SQLException e) {
            System.err.println("Error updating skin concerns: " + e.getMessage());
            showError("Помилка оновлення проблем шкіри");
        }
    }

    @FXML
    private void updateNotificationPreferences() {
        if (reminderNotificationsCheckBox.isSelected() &&
                (reminderHourComboBox.getValue() == null ||
                        reminderMinuteComboBox.getValue() == null)) {
            showError("Будь ласка, виберіть час нагадування");
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
                    showSuccess("Налаштування сповіщень успішно оновлено");
                } else {
                    showError("Не вдалося оновити налаштування сповіщень");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating notification preferences: " + e.getMessage());
            showError("Помилка оновлення налаштувань сповіщень");
        }
    }

    @FXML
    private void changePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Будь ласка, заповніть всі поля паролів");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Нові паролі не співпадають");
            return;
        }

        if (!isValidPassword(newPassword)) {
            showError("Пароль повинен містити щонайменше 8 символів, одну цифру, " +
                    "одну велику літеру та один спеціальний символ");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:skincare_tracker.db")) {
            String verifySql = "SELECT password FROM users WHERE id = ?";
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                verifyStmt.setLong(1, currentUser.getId());

                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        if (!currentPassword.equals(storedPassword)) {
                            showError("Поточний пароль неправильний");
                            return;
                        }
                    } else {
                        showError("Користувача не знайдено");
                        return;
                    }
                }
            }

            String updateSql = "UPDATE users SET password = ? WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, newPassword);
                updateStmt.setLong(2, currentUser.getId());

                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected > 0) {
                    showSuccess("Пароль успішно змінено");
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                } else {
                    showError("Не вдалося змінити пароль");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            showError("Помилка зміни пароля");
        }
    }

    @FXML
    private void deleteAccount() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Видалити акаунт");
        confirmDialog.setHeaderText("Ви впевнені, що хочете видалити свій акаунт?");
        confirmDialog.setContentText("Цю дію неможливо скасувати. Всі ваші дані будуть безповоротно видалені.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:skincare_tracker.db")) {
                conn.setAutoCommit(false);

                try {
                    String deleteRemindersSql = "DELETE FROM reminders WHERE user_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteRemindersSql)) {
                        stmt.setLong(1, currentUser.getId());
                        stmt.executeUpdate();
                    }

                    String deleteProductsSql = "DELETE FROM products WHERE user_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteProductsSql)) {
                        stmt.setLong(1, currentUser.getId());
                        stmt.executeUpdate();
                    }

                    String deleteConcernsSql = "DELETE FROM skin_concerns WHERE user_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteConcernsSql)) {
                        stmt.setLong(1, currentUser.getId());
                        stmt.executeUpdate();
                    }

                    String deleteUserSql = "DELETE FROM users WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteUserSql)) {
                        stmt.setLong(1, currentUser.getId());
                        int rowsAffected = stmt.executeUpdate();

                        if (rowsAffected > 0) {
                            conn.commit();
                            databaseManager.setCurrentUser(null);
                            showSuccess("Акаунт успішно видалено");
                        } else {
                            conn.rollback();
                            showError("Не вдалося видалити акаунт");
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
                showError("Помилка видалення акаунта");
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
        alert.setTitle("Помилка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успіх");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}