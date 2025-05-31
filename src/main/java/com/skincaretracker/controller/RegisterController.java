package com.skincaretracker.controller;

import com.skincaretracker.model.User;
import com.skincaretracker.util.DatabaseManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    private DatabaseManager databaseManager;

    public void initialize() {
        databaseManager = DatabaseManager.getInstance();
        hideError();
    }

    @FXML
    public void handleCreateAccount(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Будь ласка, заповніть всі поля");
            return;
        }

        if (username.length() < 3) {
            showError("Логін повинен містити принаймні 3 символи");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Введіть коректну email адресу");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Паролі не співпадають");
            return;
        }

        if (password.length() < 6) {
            showError("Пароль повинен містити принаймні 6 символів");
            return;
        }

        User newUser = databaseManager.createUser(username, email, password);

        if (newUser != null) {
            showSuccess("Акаунт успішно створено!");

            clearFields();

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> handleBackToLogin(event));
                } catch (InterruptedException e) {
                    javafx.application.Platform.runLater(() -> handleBackToLogin(event));
                }
            }).start();

        } else {
            if (isUserExists(username, email)) {
                showError("Користувач з таким логіном або email вже існує");
            } else {
                showError("Помилка при створенні акаунту. Спробуйте ще раз");
            }
        }
    }

    @FXML
    public void handleBackToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
            stage.setScene(scene);

        } catch (Exception e) {
            System.err.println("Error loading login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #ff4444;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #4CAF50;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void clearFields() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    private boolean isUserExists(String username, String email) {
        try {
            User testUser1 = databaseManager.getUser(username, "dummy");
            User testUser2 = databaseManager.getUser("dummy", email);
            return testUser1 != null || testUser2 != null;
        } catch (Exception e) {
            return false;
        }
    }
}