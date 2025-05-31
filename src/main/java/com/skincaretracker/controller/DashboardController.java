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

    /**
     * Загружает данные текущего пользователя
     */
    private void loadUserData() {
        User currentUser = dbManager.getCurrentUser();
        if (currentUser != null && userNameLabel != null) {
            userNameLabel.setText("Welcome, " + currentUser.getUsername());
        } else if (userNameLabel != null) {
            userNameLabel.setText("Welcome, Guest");
            // Если пользователь не авторизован, возможно стоит перенаправить на логин
            handleUserNotLoggedIn();
        }
    }

    /**
     * Обновляет счетчики продуктов и напоминаний
     */
    private void updateCounts() {
        try {
            if (!dbManager.isUserLoggedIn()) {
                setCountsToZero();
                return;
            }

            // Получаем продукты текущего пользователя
            List<Product> products = dbManager.getCurrentUserProducts();
            if (productCount != null) {
                productCount.setText(String.valueOf(products.size()));
            }

            // Получаем напоминания текущего пользователя
            List<Reminder> reminders = dbManager.getCurrentUserReminders();
            if (reminderCount != null) {
                // Считаем только активные (незавершенные) напоминания
                long activeReminders = reminders.stream()
                        .filter(reminder -> !reminder.isCompleted())
                        .count();
                reminderCount.setText(String.valueOf(activeReminders));
            }

        } catch (Exception e) {
            System.err.println("Error updating counts: " + e.getMessage());
            setCountsToZero();
            showError("Error loading dashboard data: " + e.getMessage());
        }
    }

    /**
     * Устанавливает счетчики в ноль
     */
    private void setCountsToZero() {
        if (productCount != null) {
            productCount.setText("0");
        }
        if (reminderCount != null) {
            reminderCount.setText("0");
        }
    }

    /**
     * Обрабатывает случай, когда пользователь не авторизован
     */
    private void handleUserNotLoggedIn() {
        // Можно добавить логику для перенаправления на страницу логина
        // или показать предупреждение
        System.out.println("Warning: No user logged in on dashboard");
    }

    /**
     * Обновляет дашборд (может быть вызван извне после изменений данных)
     */
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

    /**
     * Проверяет, авторизован ли пользователь
     */
    private boolean checkUserLoggedIn() {
        if (!dbManager.isUserLoggedIn()) {
            showWarning("Please log in to access this feature.");
            return false;
        }
        return true;
    }

    /**
     * Вспомогательный метод для загрузки представлений
     */
    private void loadView(String fxmlPath, String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Если контроллер загруженного представления имеет метод для обновления,
            // можно его вызвать
            Object controller = loader.getController();
            if (controller instanceof RefreshableController) {
                ((RefreshableController) controller).refresh();
            }

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
            // Очищаем текущего пользователя в DatabaseManager
            dbManager.setCurrentUser(null);

            // Загружаем представление логина
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Parent loginRoot = loader.load();

            Scene loginScene = new Scene(loginRoot);

            // Проверка существования CSS файла
            var cssResource = getClass().getResource("/style/style.css");
            if (cssResource != null) {
                loginScene.getStylesheets().add(cssResource.toExternalForm());
            }

            // Получаем текущую сцену и устанавливаем новую
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(loginScene);
            stage.setTitle("Skin Care Tracker - Login");
            stage.show();

            System.out.println("User logged out successfully");
        } catch (IOException e) {
            showError("Error returning to login: " + e.getMessage());
        } catch (Exception e) {
            showError("Unexpected error during logout: " + e.getMessage());
        }
    }

    /**
     * Показывает сообщение об ошибке
     */
    private void showError(String message) {
        System.err.println("Error: " + message);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Показывает предупреждение
     */
    private void showWarning(String message) {
        System.out.println("Warning: " + message);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText("Access Restricted");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Интерфейс для контроллеров, которые можно обновлять
     */
    public interface RefreshableController {
        void refresh();
    }
}