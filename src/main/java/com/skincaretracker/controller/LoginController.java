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
        // Скрываем error label при инициализации
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Валидация полей
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        try {
            // Попытка аутентификации через базу данных
            User user = dbManager.getUser(username, password);

            if (user != null) {
                // Устанавливаем текущего пользователя в DatabaseManager
                dbManager.setCurrentUser(user);

                // Переходим к дашборду
                navigateToDashboard();
            } else {
                showError("Invalid username or password");
            }
        } catch (Exception e) {
            showError("Error during login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            // Загружаем экран регистрации
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Register.fxml"));
            Parent registerRoot = loader.load();

            Scene registerScene = new Scene(registerRoot);

            // Добавляем стили если есть
            var cssResource = getClass().getResource("/style/style.css");
            if (cssResource != null) {
                registerScene.getStylesheets().add(cssResource.toExternalForm());
            }

            // Получаем текущую сцену и устанавливаем новую
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(registerScene);
            stage.setTitle("Skin Care Tracker - Register");
            stage.show();
        } catch (IOException e) {
            showError("Error loading registration page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        // Пока что показываем информационное сообщение
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password Recovery");
        alert.setHeaderText("Password Recovery");
        alert.setContentText("Password recovery functionality will be implemented in a future update.\n" +
                "Please contact support if you need to reset your password.");
        alert.showAndWait();
    }

    private void navigateToDashboard() {
        try {
            // Загружаем дашборд
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Dashboard.fxml"));
            Parent dashboardRoot = loader.load();

            Scene dashboardScene = new Scene(dashboardRoot);

            // Добавляем стили если есть
            var cssResource = getClass().getResource("/style/style.css");
            if (cssResource != null) {
                dashboardScene.getStylesheets().add(cssResource.toExternalForm());
            }

            // Получаем текущую сцену и устанавливаем новую
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(dashboardScene);
            stage.setTitle("Skin Care Tracker - Dashboard");
            stage.show();

            // Очищаем поля после успешного входа
            clearFields();

        } catch (IOException e) {
            showError("Error loading dashboard: " + e.getMessage());
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

    // Методы для очистки ошибок при вводе пользователя
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

    // Метод для обработки Enter в полях ввода
    @FXML
    private void handleEnterPressed(ActionEvent event) {
        handleLogin(event);
    }
}