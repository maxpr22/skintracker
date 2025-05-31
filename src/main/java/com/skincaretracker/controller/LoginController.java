package com.skincaretracker.controller;

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

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        // TODO: Implement actual authentication logic
        if (username.equals("demo") && password.equals("password")) {
            try {
                // Load the dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Dashboard.fxml"));
                Parent dashboardRoot = loader.load();
                
                Scene dashboardScene = new Scene(dashboardRoot);
                dashboardScene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
                
                // Get current stage and set new scene
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(dashboardScene);
                stage.setTitle("Skin Care Tracker - Dashboard");
                stage.show();
            } catch (IOException e) {
                showError("Error loading dashboard: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showError("Invalid username or password");
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            // Load the registration view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Register.fxml"));
            Parent registerRoot = loader.load();
            
            Scene registerScene = new Scene(registerRoot);
            registerScene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
            
            // Get current stage and set new scene
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(registerScene);
            stage.setTitle("Skin Care Tracker - Register");
            stage.show();
        } catch (IOException e) {
            showError("Error loading registration page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
