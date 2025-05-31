package com.skincaretracker.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;

public class DashboardController {
    @FXML
    private StackPane contentArea;

    @FXML
    private Label userNameLabel;

    @FXML
    private Text productCount;

    @FXML
    private Text reminderCount;

    // Убрана аннотация @FXML - initialize() вызывается автоматически
    public void initialize() {
        // TODO: Load actual user data
        if (userNameLabel != null) {
            userNameLabel.setText("Welcome, Demo User");
        }
        updateCounts();
    }

    private void updateCounts() {
        // TODO: Get actual counts from database
        if (productCount != null) {
            productCount.setText("0");
        }
        if (reminderCount != null) {
            reminderCount.setText("0");
        }
    }

    @FXML
    private void showProducts() {
        loadView("/view/Products.fxml", "products view");
    }

    @FXML
    private void showReminders() {
        loadView("/view/Reminders.fxml", "reminders view");
    }

    @FXML
    private void showProfile() {
        loadView("/view/Profile.fxml", "profile view");
    }

    // Вспомогательный метод для загрузки представлений
    private void loadView(String fxmlPath, String viewName) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            showError("Error loading " + viewName + ": " + e.getMessage());
        } catch (Exception e) {
            showError("Unexpected error loading " + viewName + ": " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Load the login view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Parent loginRoot = loader.load();

            Scene loginScene = new Scene(loginRoot);

            // Проверка существования CSS файла
            var cssResource = getClass().getResource("/style/style.css");
            if (cssResource != null) {
                loginScene.getStylesheets().add(cssResource.toExternalForm());
            }

            // Get current stage and set new scene
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(loginScene);
            stage.setTitle("Skin Care Tracker - Login");
            stage.show();
        } catch (IOException e) {
            showError("Error returning to login: " + e.getMessage());
        } catch (Exception e) {
            showError("Unexpected error during logout: " + e.getMessage());
        }
    }

    private void showError(String message) {
        // Улучшенная обработка ошибок с диалоговым окном
        System.err.println(message);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
}