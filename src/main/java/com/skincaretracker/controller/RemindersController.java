package com.skincaretracker.controller;

import com.skincaretracker.model.Product;
import com.skincaretracker.model.Reminder;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.IntStream;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;

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
    
    @FXML
    private Dialog<Reminder> reminderDialog;
    
    @FXML
    private ComboBox<Product> productComboBox;
    
    @FXML
    private TextArea messageField;
    
    @FXML
    private DatePicker datePicker;
    
    @FXML
    private ComboBox<Integer> hourComboBox;
    
    @FXML
    private ComboBox<Integer> minuteComboBox;
    
    @FXML
    private CheckBox repeatCheckBox;
    
    @FXML
    private VBox repeatOptionsBox;
    
    @FXML
    private ComboBox<String> repeatFrequencyComboBox;
    
    @FXML
    private DatePicker endDatePicker;

    private final ObservableList<Reminder> reminders = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        setupDialog();
        loadDummyData(); // TODO: Replace with actual data loading
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

        completedColumn.setCellFactory(column -> new CheckBoxTableCell<>());

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
        // Setup time ComboBoxes
        hourComboBox.setItems(FXCollections.observableArrayList(
            IntStream.rangeClosed(0, 23).boxed().toList()
        ));
        minuteComboBox.setItems(FXCollections.observableArrayList(
            IntStream.rangeClosed(0, 59).boxed().toList()
        ));

        // Setup product ComboBox
        productComboBox.setItems(FXCollections.observableArrayList(
            // TODO: Replace with actual product loading
            new Product(1L, "Face Cleanser", "Gentle daily cleanser", false, 4),
            new Product(2L, "Moisturizer", "Hydrating cream", false, 5)
        ));

        // Setup repeat options
        repeatFrequencyComboBox.getItems().addAll(
            "Daily", "Weekly", "Monthly"
        );

        repeatCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            repeatOptionsBox.setVisible(newVal);
            repeatOptionsBox.setManaged(newVal);
        });

        reminderDialog.setResultConverter(this::convertDialogResult);
    }

    private Reminder convertDialogResult(ButtonType buttonType) {
        if (buttonType != ButtonType.OK) return null;

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

        return new Reminder(
            System.currentTimeMillis(),
            selectedProduct,
            message,
            dateTime
        );
    }

    @FXML
    private void showAddReminderDialog() {
        resetDialogFields();
        Optional<Reminder> result = reminderDialog.showAndWait();
        result.ifPresent(this::addReminder);
    }

    private void showEditDialog(Reminder reminder) {
        reminderDialog.setHeaderText("Edit Reminder");
        
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
            remindersTable.refresh();
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
        // TODO: Implement notification settings dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notification Settings");
        alert.setHeaderText(null);
        alert.setContentText("Notification settings will be implemented in a future update.");
        alert.showAndWait();
    }

    private void filterReminders() {
        String status = statusFilter.getValue();
        LocalDate date = dateFilter.getValue();

        ObservableList<Reminder> filteredList = reminders.filtered(reminder -> {
            boolean matchesStatus = switch (status) {
                case "Pending" -> !reminder.isCompleted();
                case "Completed" -> reminder.isCompleted();
                default -> true;
            };

            boolean matchesDate = date == null || 
                                reminder.getDateTime().toLocalDate().equals(date);

            return matchesStatus && matchesDate;
        });

        remindersTable.setItems(filteredList);
    }

    private void showDeleteConfirmation(Reminder reminder) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Reminder");
        alert.setHeaderText("Delete Reminder for " + reminder.getProduct().getName());
        alert.setContentText("Are you sure you want to delete this reminder?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            reminders.remove(reminder);
        }
    }

    private void addReminder(Reminder reminder) {
        reminders.add(reminder);
        filterReminders();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadDummyData() {
        // Add some sample reminders
        Product product1 = new Product(1L, "Face Cleanser", "Gentle daily cleanser", false, 4);
        Product product2 = new Product(2L, "Moisturizer", "Hydrating cream", false, 5);

        reminders.addAll(
            new Reminder(1L, product1, "Morning cleanse", LocalDateTime.now().plusHours(1)),
            new Reminder(2L, product2, "Evening moisturizer", LocalDateTime.now().plusDays(1)),
            new Reminder(3L, product1, "Deep cleanse", LocalDateTime.now().plusDays(2))
        );
    }
}
