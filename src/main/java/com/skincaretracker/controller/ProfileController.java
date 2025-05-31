package com.skincaretracker.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
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

    @FXML
    public void initialize() {
        setupComboBoxes();
        loadUserProfile(); // Load dummy data for now
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
        // TODO: Replace with actual user data loading
        usernameField.setText("demo_user");
        emailField.setText("demo@example.com");
        skinTypeComboBox.setValue("Normal");
        
        // Set some default skin concerns
        acneCheckBox.setSelected(true);
        drynessCheckBox.setSelected(false);
        sensitivityCheckBox.setSelected(true);
        agingCheckBox.setSelected(false);
        pigmentationCheckBox.setSelected(true);
        
        // Set default notification preferences
        emailNotificationsCheckBox.setSelected(true);
        pushNotificationsCheckBox.setSelected(true);
        reminderNotificationsCheckBox.setSelected(true);
        
        // Set default reminder time
        reminderHourComboBox.setValue(9);
        reminderMinuteComboBox.setValue(0);
    }

    @FXML
    private void updatePersonalInfo() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String skinType = skinTypeComboBox.getValue();

        if (username.isEmpty() || email.isEmpty() || skinType == null) {
            showError("Please fill in all required fields");
            return;
        }

        // TODO: Validate email format
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        // TODO: Update user information in database
        showSuccess("Personal information updated successfully");
    }

    @FXML
    private void updateSkinConcerns() {
        // TODO: Save skin concerns to database
        showSuccess("Skin concerns updated successfully");
    }

    @FXML
    private void updateNotificationPreferences() {
        if (reminderNotificationsCheckBox.isSelected() && 
            (reminderHourComboBox.getValue() == null || 
             reminderMinuteComboBox.getValue() == null)) {
            showError("Please select reminder time");
            return;
        }

        // TODO: Save notification preferences to database
        showSuccess("Notification preferences updated successfully");
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

        // TODO: Verify current password and update with new password in database
        showSuccess("Password changed successfully");
        
        // Clear password fields
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    @FXML
    private void deleteAccount() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Account");
        confirmDialog.setHeaderText("Are you sure you want to delete your account?");
        confirmDialog.setContentText("This action cannot be undone. All your data will be permanently deleted.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: Delete user account and all associated data
            // TODO: Redirect to login page
            showSuccess("Account deleted successfully");
        }
    }

    private boolean isValidEmail(String email) {
        // Basic email validation
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidPassword(String password) {
        // Password must be at least 8 characters long and contain at least one number,
        // one uppercase letter, and one special character
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
