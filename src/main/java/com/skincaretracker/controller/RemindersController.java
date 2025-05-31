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
        if (!dbManager.isUserLoggedIn()) {
            showError("Користувач не авторизований. Будь ласка, увійдіть в систему.");
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
            currentStage.setTitle("Skincare Tracker - Головна");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Не вдалося завантажити головну сторінку: " + e.getMessage());
        }
    }

    private void setupTable() {
        productColumn.setCellValueFactory(new PropertyValueFactory<>("product"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        dateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        completedColumn.setCellValueFactory(new PropertyValueFactory<>("completed"));

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
            private final Button editButton = new Button("Редагувати");
            private final Button deleteButton = new Button("Видалити");
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
        statusFilter.getItems().addAll("Всі", "Очікуючі", "Виконані");
        statusFilter.setValue("Всі");

        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterReminders());
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterReminders());
    }

    private void setupDialog() {
        reminderDialog = new Dialog<>();
        reminderDialog.setTitle("Нагадування");
        reminderDialog.setHeaderText("Додати нове нагадування");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        productComboBox = new ComboBox<>();
        messageField = new TextArea();
        datePicker = new DatePicker();
        hourComboBox = new ComboBox<>();
        minuteComboBox = new ComboBox<>();
        repeatCheckBox = new CheckBox("Повторювати нагадування");
        repeatOptionsBox = new VBox(5);
        repeatFrequencyComboBox = new ComboBox<>();
        endDatePicker = new DatePicker();

        messageField.setPrefRowCount(3);
        messageField.setPromptText("Текст нагадування");

        hourComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(0, 23).boxed().toList()
        ));
        minuteComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(0, 59).boxed().toList()
        ));

        repeatFrequencyComboBox.getItems().addAll("Щодня", "Щотижня", "Щомісяця");

        repeatOptionsBox.getChildren().addAll(repeatFrequencyComboBox, endDatePicker);
        repeatOptionsBox.setVisible(false);
        repeatOptionsBox.setManaged(false);

        repeatCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            repeatOptionsBox.setVisible(newVal);
            repeatOptionsBox.setManaged(newVal);
        });

        grid.add(new Label("Продукт:"), 0, 0);
        grid.add(productComboBox, 1, 0);
        grid.add(new Label("Повідомлення:"), 0, 1);
        grid.add(messageField, 1, 1);
        grid.add(new Label("Дата:"), 0, 2);
        grid.add(datePicker, 1, 2);

        HBox timeBox = new HBox(5);
        timeBox.getChildren().addAll(hourComboBox, new Label(":"), minuteComboBox);
        grid.add(new Label("Час:"), 0, 3);
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
            showError("Помилка завантаження продуктів: " + e.getMessage());
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
            showError("Будь ласка, заповніть всі обов'язкові поля");
            return null;
        }

        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of(hour, minute));
        if (dateTime.isBefore(LocalDateTime.now())) {
            showError("Час нагадування має бути в майбутньому");
            return null;
        }

        return new Reminder(0L, selectedProduct, message, dateTime);
    }

    @FXML
    private void showAddReminderDialog() {
        resetDialogFields();
        loadProductsIntoComboBox();
        reminderDialog.setHeaderText("Додати нове нагадування");

        Optional<Reminder> result = reminderDialog.showAndWait();
        result.ifPresent(this::addReminderToDatabase);
    }

    private void showEditDialog(Reminder reminder) {
        reminderDialog.setHeaderText("Редагувати нагадування");
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

        reminderDialog.setHeaderText("Додати нове нагадування");
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
        statusFilter.setValue("Всі");
        dateFilter.setValue(null);
    }

    @FXML
    private void showNotificationSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Налаштування сповіщень");
        alert.setHeaderText(null);
        alert.setContentText("Налаштування сповіщень будуть реалізовані в майбутньому оновленні.");
        alert.showAndWait();
    }

    private void filterReminders() {
        String status = statusFilter.getValue();
        LocalDate date = dateFilter.getValue();

        ObservableList<Reminder> filteredList = allReminders.filtered(reminder -> {
            boolean matchesStatus = switch (status) {
                case "Очікуючі" -> !reminder.isCompleted();
                case "Виконані" -> reminder.isCompleted();
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
        alert.setTitle("Видалити нагадування");
        alert.setHeaderText("Видалити нагадування для " + reminder.getProduct().getName());
        alert.setContentText("Ви впевнені, що хочете видалити це нагадування?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteReminderFromDatabase(reminder);
        }
    }

    private void loadRemindersFromDatabase() {
        try {
            var remindersFromDb = dbManager.getCurrentUserReminders();
            allReminders.setAll(remindersFromDb);
            filterReminders();
        } catch (Exception e) {
            showError("Помилка завантаження нагадувань: " + e.getMessage());
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
                showInfo("Нагадування успішно додано!");
            } else {
                showError("Не вдалося створити нагадування");
            }
        } catch (Exception e) {
            showError("Помилка додавання нагадування: " + e.getMessage());
        }
    }

    private void updateReminderInDatabase(Reminder reminder) {
        try {
            boolean updated = dbManager.updateReminder(reminder);
            if (updated) {
                remindersTable.refresh();
                showInfo("Нагадування успішно оновлено!");
            } else {
                showError("Не вдалося оновити нагадування");
            }
        } catch (Exception e) {
            showError("Помилка оновлення нагадування: " + e.getMessage());
        }
    }

    private void deleteReminderFromDatabase(Reminder reminder) {
        try {
            boolean deleted = dbManager.deleteReminder(reminder.getId());
            if (deleted) {
                allReminders.remove(reminder);
                filterReminders();
                showInfo("Нагадування успішно видалено!");
            } else {
                showError("Не вдалося видалити нагадування");
            }
        } catch (Exception e) {
            showError("Помилка видалення нагадування: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Помилка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Інформація");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void refreshData() {
        loadRemindersFromDatabase();
    }
}