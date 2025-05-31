package com.skincaretracker.controller;

import com.skincaretracker.model.User;
import com.skincaretracker.model.Product;
import com.skincaretracker.model.Reminder;
import com.skincaretracker.util.DatabaseManager;

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
import java.util.List;

public class DashboardController {
    @FXML
    private StackPane contentArea;

    @FXML
    private Label userNameLabel;

    @FXML
    private Text productCount;

    @FXML
    private Text reminderCount;

    private DatabaseManager dbManager;

    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        loadUserData();
        updateCounts();
    }

    private void loadUserData() {
        User currentUser = dbManager.getCurrentUser();
        if (currentUser != null && userNameLabel != null) {
            userNameLabel.setText("Вітаємо, " + currentUser.getUsername());
        } else if (userNameLabel != null) {
            userNameLabel.setText("Вітаємо, Гість");
            handleUserNotLoggedIn();
        }
    }

    private void updateCounts() {
        try {
            if (!dbManager.isUserLoggedIn()) {
                setCountsToZero();
                return;
            }

            List<Product> products = dbManager.getCurrentUserProducts();
            if (productCount != null) {
                productCount.setText(String.valueOf(products.size()));
            }

            List<Reminder> reminders = dbManager.getCurrentUserReminders();
            if (reminderCount != null) {
                long activeReminders = reminders.stream()
                        .filter(reminder -> !reminder.isCompleted())
                        .count();
                reminderCount.setText(String.valueOf(activeReminders));
            }

        } catch (Exception e) {
            System.err.println("Помилка при оновленні кількості: " + e.getMessage());
            setCountsToZero();
            showError("Помилка при завантаженні данних" + e.getMessage());
        }
    }

    private void setCountsToZero() {
        if (productCount != null) {
            productCount.setText("0");
        }
        if (reminderCount != null) {
            reminderCount.setText("0");
        }
    }

    private void handleUserNotLoggedIn() {
        System.out.println("Користувач не залогінений");
    }

    public void refreshDashboard() {
        loadUserData();
        updateCounts();
    }

    @FXML
    private void showProducts() {
        if (!checkUserLoggedIn()) return;
        loadView("/view/Products.fxml", "products view");
    }

    @FXML
    private void showReminders() {
        if (!checkUserLoggedIn()) return;
        loadView("/view/Reminders.fxml", "reminders view");
    }

    @FXML
    private void showProfile() {
        if (!checkUserLoggedIn()) return;
        loadView("/view/Profile.fxml", "profile view");
    }

    private boolean checkUserLoggedIn() {
        if (!dbManager.isUserLoggedIn()) {
            showWarning("Будь-ласка зареєструйтесь для доступу.");
            return false;
        }
        return true;
    }

    private void loadView(String fxmlPath, String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof RefreshableController) {
                ((RefreshableController) controller).refresh();
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            showError("Помилка завантаження " + viewName + ": " + e.getMessage());
        } catch (Exception e) {
            showError("Непередбачувана помилка завантаження " + viewName + ": " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            dbManager.setCurrentUser(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Parent loginRoot = loader.load();

            Scene loginScene = new Scene(loginRoot);

            var cssResource = getClass().getResource("/style/style.css");
            if (cssResource != null) {
                loginScene.getStylesheets().add(cssResource.toExternalForm());
            }

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(loginScene);
            stage.setTitle("Трекер для догляду за шкірою - Вхід");
            stage.show();

            System.out.println("Користувач увійшов успішно");
        } catch (IOException e) {
            showError("Помилка під час входу: " + e.getMessage());
        } catch (Exception e) {
            showError("Непередбачувана помилка під час виходу: " + e.getMessage());
        }
    }

    private void showError(String message) {
        System.err.println("Помилка: " + message);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Помилка");
        alert.setHeaderText("Трапилась помилка");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        System.out.println("Попередження: " + message);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Попередження");
        alert.setHeaderText("В доступі відмовлено");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public interface RefreshableController {
        void refresh();
    }
}