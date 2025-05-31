package com.skincaretracker.controller;

import com.skincaretracker.model.User;
import com.skincaretracker.util.DatabaseManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Будь-ласка заповніть всі поля");
            return;
        }

        try {
            User user = dbManager.getUser(username, password);

            if (user != null) {
                dbManager.setCurrentUser(user);

                navigateToDashboard();
            } else {
                showError("Неправильне ім'я користувача або пароль");
            }
        } catch (Exception e) {
            showError("Помилка входу" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Register.fxml"));
            Parent registerRoot = loader.load();

            Scene registerScene = new Scene(registerRoot);

            var cssResource = getClass().getResource("/style/style.css");
            if (cssResource != null) {
                registerScene.getStylesheets().add(cssResource.toExternalForm());
            }

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(registerScene);
            stage.setTitle("Трекер для догляду за шкірою - Реєстрація");
            stage.show();
        } catch (IOException e) {
            showError("Помилка при завантаженні сторінки реєстрації " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Відновлення паролю");
        alert.setHeaderText("Відновлення паролю");
        alert.setContentText("Відновлення паролю буде в наступному апдейті.\n" +
                "Зверніться до адміністратора якщо ви забули свій пароль.");
        alert.showAndWait();
    }

    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Dashboard.fxml"));
            Parent dashboardRoot = loader.load();

            Scene dashboardScene = new Scene(dashboardRoot);

            var cssResource = getClass().getResource("/style/style.css");
            if (cssResource != null) {
                dashboardScene.getStylesheets().add(cssResource.toExternalForm());
            }

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(dashboardScene);
            stage.setTitle("Трекер для догляду за шкірою - Статистика");
            stage.show();

            clearFields();

        } catch (IOException e) {
            showError("Помилка при завантаженні статистики " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        hideError();
    }

    @FXML
    private void onUsernameTyped() {
        if (errorLabel.isVisible()) {
            hideError();
        }
    }

    @FXML
    private void onPasswordTyped() {
        if (errorLabel.isVisible()) {
            hideError();
        }
    }

    @FXML
    private void handleEnterPressed(ActionEvent event) {
        handleLogin(event);
    }
}