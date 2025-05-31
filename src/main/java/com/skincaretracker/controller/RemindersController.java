package com.skincaretracker.controller;

import com.skincaretracker.model.Product;
import com.skincaretracker.model.Reminder;
import com.skincaretracker.util.DatabaseManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.IntStream;
import javafx.scene.control.cell.CheckBoxTableCell;

public class RemindersController {
    @FXML
    private TableView<Reminder> remindersTable;

    @FXML
    private TableColumn<Reminder, Product> productColumn;

    @FXML
    private TableColumn<Reminder, String> messageColumn;

    @FXML
    private TableColumn<Reminder, LocalDateTime> dateTimeColumn;

    @FXML
    private TableColumn<Reminder, Boolean> completedColumn;

    @FXML
    private TableColumn<Reminder, Void> actionsColumn;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private DatePicker dateFilter;

    // Dialog components - not in FXML
    private Dialog<Reminder> reminderDialog;
    private ComboBox<Product> productComboBox;
    private TextArea messageField;
    private DatePicker datePicker;
    private ComboBox<Integer> hourComboBox;
    private ComboBox<Integer> minuteComboBox;
    private CheckBox repeatCheckBox;
    private VBox repeatOptionsBox;
    private ComboBox<String> repeatFrequencyComboBox;
    private DatePicker endDatePicker;

    private final ObservableList<Reminder> reminders = FXCollections.observableArrayList();
    private final ObservableList<Reminder> allReminders = FXCollections.observableArrayList();
    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        // Перевіряємо чи користувач авторизований
        if (!dbManager.isUserLoggedIn()) {
            showError("User not logged in. Please log in first.");
            return;
        }

        setupTable();
        setupFilters();
        setupDialog();
        loadRemindersFromDatabase();
    }

    @FXML
    private void goToDashboard() {
        try {
            Stage currentStage = (Stage) remindersTable.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            var cssResource = getClass().getResource("/style/style.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            currentStage.setScene(scene);
            currentStage.setTitle("Skincare Tracker - Dashboard");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not load dashboard: " + e.getMessage());
        }
    }

    private void setupTable() {
        productColumn.setCellValueFactory(new PropertyValueFactory<>("product"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        dateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        completedColumn.setCellValueFactory(new PropertyValueFactory<>("completed"));

        // Custom cell factories
        productColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setText(null);
                } else {
                    setText(product.getName());
                }
            }
        });

        dateTimeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime dateTime, boolean empty) {
                super.updateItem(dateTime, empty);
                if (empty || dateTime == null) {
                    setText(null);
                } else {
                    setText(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }
            }
        });

        // Налаштування checkbox для completed колонки
        completedColumn.setCellFactory(column -> new CheckBoxTableCell<>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    CheckBox checkBox = (CheckBox) getGraphic();
                    if (checkBox != null) {
                        checkBox.setOnAction(event -> {
                            Reminder reminder = getTableView().getItems().get(getIndex());
                            reminder.setCompleted(checkBox.isSelected());
                            updateReminderInDatabase(reminder);
                        });
                    }
                }
            }
        });

        setupActionsColumn();
        remindersTable.setItems(reminders);
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttons = new HBox(5, editButton, deleteButton);

            {
                editButton.setOnAction(event -> {
                    Reminder reminder = getTableView().getItems().get(getIndex());
                    showEditDialog(reminder);
                });

                deleteButton.setOnAction(event -> {
                    Reminder reminder = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation(reminder);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("All", "Pending", "Completed");
        statusFilter.setValue("All");

        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterReminders());
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterReminders());
    }

    private void setupDialog() {
        reminderDialog = new Dialog<>();
        reminderDialog.setTitle("Reminder");
        reminderDialog.setHeaderText("Add New Reminder");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Initialize components
        productComboBox = new ComboBox<>();
        messageField = new TextArea();
        datePicker = new DatePicker();
        hourComboBox = new ComboBox<>();
        minuteComboBox = new ComboBox<>();
        repeatCheckBox = new CheckBox("Repeat Reminder");
        repeatOptionsBox = new VBox(5);
        repeatFrequencyComboBox = new ComboBox<>();
        endDatePicker = new DatePicker();

        // Setup components
        messageField.setPrefRowCount(3);
        messageField.setPromptText("Reminder Message");

        hourComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(0, 23).boxed().toList()
        ));
        minuteComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(0, 59).boxed().toList()
        ));

        repeatFrequencyComboBox.getItems().addAll("Daily", "Weekly", "Monthly");

        // Setup repeat options visibility
        repeatOptionsBox.getChildren().addAll(repeatFrequencyComboBox, endDatePicker);
        repeatOptionsBox.setVisible(false);
        repeatOptionsBox.setManaged(false);

        repeatCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            repeatOptionsBox.setVisible(newVal);
            repeatOptionsBox.setManaged(newVal);
        });

        // Add components to grid
        grid.add(new Label("Product:"), 0, 0);
        grid.add(productComboBox, 1, 0);
        grid.add(new Label("Message:"), 0, 1);
        grid.add(messageField, 1, 1);
        grid.add(new Label("Date:"), 0, 2);
        grid.add(datePicker, 1, 2);

        HBox timeBox = new HBox(5);
        timeBox.getChildren().addAll(hourComboBox, new Label(":"), minuteComboBox);
        grid.add(new Label("Time:"), 0, 3);
        grid.add(timeBox, 1, 3);

        grid.add(repeatCheckBox, 1, 4);
        grid.add(repeatOptionsBox, 1, 5);

        reminderDialog.getDialogPane().setContent(grid);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        reminderDialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        reminderDialog.setResultConverter(this::convertDialogResult);
    }

    private void loadProductsIntoComboBox() {
        try {
            var products = dbManager.getCurrentUserProducts();
            productComboBox.setItems(FXCollections.observableArrayList(products));
        } catch (Exception e) {
            showError("Error loading products: " + e.getMessage());
        }
    }

    private Reminder convertDialogResult(ButtonType buttonType) {
        if (buttonType.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;

        Product selectedProduct = productComboBox.getValue();
        String message = messageField.getText().trim();
        LocalDate date = datePicker.getValue();
        Integer hour = hourComboBox.getValue();
        Integer minute = minuteComboBox.getValue();

        if (selectedProduct == null || message.isEmpty() || date == null ||
                hour == null || minute == null) {
            showError("Please fill in all required fields");
            return null;
        }

        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of(hour, minute));
        if (dateTime.isBefore(LocalDateTime.now())) {
            showError("Reminder time must be in the future");
            return null;
        }

        return new Reminder(0L, selectedProduct, message, dateTime);
    }

    @FXML
    private void showAddReminderDialog() {
        resetDialogFields();
        loadProductsIntoComboBox();
        reminderDialog.setHeaderText("Add New Reminder");

        Optional<Reminder> result = reminderDialog.showAndWait();
        result.ifPresent(this::addReminderToDatabase);
    }

    private void showEditDialog(Reminder reminder) {
        reminderDialog.setHeaderText("Edit Reminder");
        loadProductsIntoComboBox();

        productComboBox.setValue(reminder.getProduct());
        messageField.setText(reminder.getMessage());
        datePicker.setValue(reminder.getDateTime().toLocalDate());
        hourComboBox.setValue(reminder.getDateTime().getHour());
        minuteComboBox.setValue(reminder.getDateTime().getMinute());

        Optional<Reminder> result = reminderDialog.showAndWait();
        result.ifPresent(updatedReminder -> {
            reminder.setProduct(updatedReminder.getProduct());
            reminder.setMessage(updatedReminder.getMessage());
            reminder.setDateTime(updatedReminder.getDateTime());
            updateReminderInDatabase(reminder);
        });

        reminderDialog.setHeaderText("Add New Reminder");
    }

    private void resetDialogFields() {
        productComboBox.setValue(null);
        messageField.clear();
        datePicker.setValue(LocalDate.now());
        hourComboBox.setValue(null);
        minuteComboBox.setValue(null);
        repeatCheckBox.setSelected(false);
        repeatFrequencyComboBox.setValue(null);
        endDatePicker.setValue(null);
    }

    @FXML
    private void clearFilters() {
        statusFilter.setValue("All");
        dateFilter.setValue(null);
    }

    @FXML
    private void showNotificationSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notification Settings");
        alert.setHeaderText(null);
        alert.setContentText("Notification settings will be implemented in a future update.");
        alert.showAndWait();
    }

    private void filterReminders() {
        String status = statusFilter.getValue();
        LocalDate date = dateFilter.getValue();

        ObservableList<Reminder> filteredList = allReminders.filtered(reminder -> {
            boolean matchesStatus = switch (status) {
                case "Pending" -> !reminder.isCompleted();
                case "Completed" -> reminder.isCompleted();
                default -> true;
            };

            boolean matchesDate = date == null ||
                    reminder.getDateTime().toLocalDate().equals(date);

            return matchesStatus && matchesDate;
        });

        reminders.setAll(filteredList);
    }

    private void showDeleteConfirmation(Reminder reminder) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Reminder");
        alert.setHeaderText("Delete Reminder for " + reminder.getProduct().getName());
        alert.setContentText("Are you sure you want to delete this reminder?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteReminderFromDatabase(reminder);
        }
    }

    // Database operations
    private void loadRemindersFromDatabase() {
        try {
            var remindersFromDb = dbManager.getCurrentUserReminders();
            allReminders.setAll(remindersFromDb);
            filterReminders(); // Apply current filters
        } catch (Exception e) {
            showError("Error loading reminders: " + e.getMessage());
        }
    }

    private void addReminderToDatabase(Reminder reminder) {
        try {
            Reminder createdReminder = dbManager.createReminderForCurrentUser(
                    reminder.getProduct().getId(),
                    reminder.getMessage(),
                    reminder.getDateTime()
            );

            if (createdReminder != null) {
                allReminders.add(createdReminder);
                filterReminders();
                showInfo("Reminder added successfully!");
            } else {
                showError("Failed to create reminder");
            }
        } catch (Exception e) {
            showError("Error adding reminder: " + e.getMessage());
        }
    }

    private void updateReminderInDatabase(Reminder reminder) {
        try {
            boolean updated = dbManager.updateReminder(reminder);
            if (updated) {
                remindersTable.refresh();
                showInfo("Reminder updated successfully!");
            } else {
                showError("Failed to update reminder");
            }
        } catch (Exception e) {
            showError("Error updating reminder: " + e.getMessage());
        }
    }

    private void deleteReminderFromDatabase(Reminder reminder) {
        try {
            boolean deleted = dbManager.deleteReminder(reminder.getId());
            if (deleted) {
                allReminders.remove(reminder);
                filterReminders();
                showInfo("Reminder deleted successfully!");
            } else {
                showError("Failed to delete reminder");
            }
        } catch (Exception e) {
            showError("Error deleting reminder: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Метод для оновлення даних ззовні (наприклад, після повернення з іншого екрана)
    public void refreshData() {
        loadRemindersFromDatabase();
    }
}