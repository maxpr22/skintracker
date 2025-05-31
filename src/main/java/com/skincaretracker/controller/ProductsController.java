package com.skincaretracker.controller;

import com.skincaretracker.model.Product;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Optional;

public class ProductsController {
    @FXML
    private TableView<Product> productsTable;

    @FXML
    private TableColumn<Product, String> nameColumn;

    @FXML
    private TableColumn<Product, String> descriptionColumn;

    @FXML
    private TableColumn<Product, Boolean> allergicColumn;

    @FXML
    private TableColumn<Product, Integer> ratingColumn;

    @FXML
    private TableColumn<Product, Void> actionsColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterComboBox;

    @FXML
    private Button backToDashboardButton;

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;

    // Dialog components - создаются программно
    private Dialog<Product> addProductDialog;
    private TextField productNameField;
    private TextArea productDescriptionField;
    private CheckBox allergicCheckBox;
    private HBox ratingStars;
    private int selectedRating = 0;

    public void initialize() {
        setupTable();
        setupFilters();
        createAddDialog();
        loadDummyData();
    }

    @FXML
    private void goBackToDashboard() {
        try {
            // Загружаем FXML файл дашборда (правильный путь)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Dashboard.fxml"));
            Parent dashboardRoot = loader.load();

            // Получаем текущее окно
            Stage currentStage = (Stage) backToDashboardButton.getScene().getWindow();

            // Создаем новую сцену с дашбордом
            Scene dashboardScene = new Scene(dashboardRoot);

            // Добавляем CSS стили если они есть
            var cssResource = getClass().getResource("/style/style.css");
            if (cssResource != null) {
                dashboardScene.getStylesheets().add(cssResource.toExternalForm());
            }

            // Устанавливаем новую сцену
            currentStage.setScene(dashboardScene);
            currentStage.setTitle("Skin Care Tracker - Dashboard");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load dashboard: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Unexpected error: " + e.getMessage());
        }
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        allergicColumn.setCellValueFactory(new PropertyValueFactory<>("allergic"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));

        // Configure allergic column to use checkboxes
        allergicColumn.setCellFactory(CheckBoxTableCell.forTableColumn(allergicColumn));
        allergicColumn.setEditable(true);

        // Configure rating column to show stars
        ratingColumn.setCellFactory(column -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer rating, boolean empty) {
                super.updateItem(rating, empty);
                if (empty || rating == null) {
                    setText(null);
                } else {
                    setText("★".repeat(Math.max(0, rating)) + "☆".repeat(Math.max(0, 5 - rating)));
                }
            }
        });

        // Configure actions column
        setupActionsColumn();

        // Setup filtered list
        filteredProducts = new FilteredList<>(products);
        productsTable.setItems(filteredProducts);
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(column -> new TableCell<Product, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttons = new HBox(5, editButton, deleteButton);

            {
                editButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    showEditDialog(product);
                });

                deleteButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation(product);
                });

                editButton.setStyle("-fx-font-size: 10px;");
                deleteButton.setStyle("-fx-font-size: 10px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
    }

    private void setupFilters() {
        filterComboBox.getItems().addAll("All", "Allergic", "Non-Allergic");
        filterComboBox.setValue("All");

        // Add listeners for filtering
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterProducts());
        filterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> filterProducts());
    }

    private void filterProducts() {
        String searchText = searchField.getText().toLowerCase();
        String filter = filterComboBox.getValue();

        filteredProducts.setPredicate(product -> {
            if (product == null) return false;

            boolean matchesSearch = searchText.isEmpty() ||
                    product.getName().toLowerCase().contains(searchText) ||
                    product.getDescription().toLowerCase().contains(searchText);

            boolean matchesFilter = switch (filter) {
                case "Allergic" -> product.isAllergic();
                case "Non-Allergic" -> !product.isAllergic();
                default -> true;
            };

            return matchesSearch && matchesFilter;
        });
    }

    @FXML
    private void showAddProductDialog() {
        resetDialogFields();

        Optional<Product> result = addProductDialog.showAndWait();
        result.ifPresent(this::addProduct);
    }

    private void createAddDialog() {
        addProductDialog = new Dialog<>();
        addProductDialog.setTitle("Product Manager");
        addProductDialog.setHeaderText("Add New Product");

        // Create dialog content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        productNameField = new TextField();
        productNameField.setPromptText("Product Name");

        productDescriptionField = new TextArea();
        productDescriptionField.setPromptText("Description");
        productDescriptionField.setPrefRowCount(3);

        allergicCheckBox = new CheckBox("Mark as Allergic");

        // Rating stars
        HBox ratingContainer = new HBox(10);
        ratingContainer.getChildren().add(new Label("Rating:"));
        ratingStars = new HBox(5);
        setupRatingStars();
        ratingContainer.getChildren().add(ratingStars);

        content.getChildren().addAll(
                new Label("Product Name:"),
                productNameField,
                new Label("Description:"),
                productDescriptionField,
                allergicCheckBox,
                ratingContainer
        );

        DialogPane dialogPane = addProductDialog.getDialogPane();
        dialogPane.setContent(content);

        // Add buttons
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Set result converter
        addProductDialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String name = productNameField.getText().trim();
                String description = productDescriptionField.getText().trim();

                if (name.isEmpty()) {
                    showError("Product name is required");
                    return null;
                }

                return new Product(
                        System.currentTimeMillis(),
                        name,
                        description,
                        allergicCheckBox.isSelected(),
                        selectedRating
                );
            }
            return null;
        });
    }

    private void setupRatingStars() {
        ratingStars.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            final int rating = i;
            Label star = new Label("☆");
            star.setStyle("-fx-font-size: 18px; -fx-cursor: hand; -fx-text-fill: #ffd700;");
            star.setOnMouseClicked(e -> {
                selectedRating = rating;
                updateRatingStars();
            });
            star.setOnMouseEntered(e -> star.setStyle("-fx-font-size: 18px; -fx-cursor: hand; -fx-text-fill: #ffed4a;"));
            star.setOnMouseExited(e -> star.setStyle("-fx-font-size: 18px; -fx-cursor: hand; -fx-text-fill: #ffd700;"));
            ratingStars.getChildren().add(star);
        }
    }

    private void updateRatingStars() {
        for (int i = 0; i < ratingStars.getChildren().size(); i++) {
            Label star = (Label) ratingStars.getChildren().get(i);
            star.setText(i < selectedRating ? "★" : "☆");
        }
    }

    private void resetDialogFields() {
        productNameField.setText("");
        productDescriptionField.setText("");
        allergicCheckBox.setSelected(false);
        selectedRating = 0;
        updateRatingStars();
        addProductDialog.setHeaderText("Add New Product");
    }

    private void showEditDialog(Product product) {
        productNameField.setText(product.getName());
        productDescriptionField.setText(product.getDescription());
        allergicCheckBox.setSelected(product.isAllergic());
        selectedRating = product.getRating();
        updateRatingStars();
        addProductDialog.setHeaderText("Edit Product");

        Optional<Product> result = addProductDialog.showAndWait();
        result.ifPresent(updatedProduct -> {
            product.setName(updatedProduct.getName());
            product.setDescription(updatedProduct.getDescription());
            product.setAllergic(updatedProduct.isAllergic());
            product.setRating(updatedProduct.getRating());
            productsTable.refresh();
        });
    }

    private void showDeleteConfirmation(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Product");
        alert.setHeaderText("Delete " + product.getName());
        alert.setContentText("Are you sure you want to delete this product?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            products.remove(product);
        }
    }

    private void addProduct(Product product) {
        products.add(product);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadDummyData() {
        products.addAll(
                new Product(1L, "Face Cleanser", "Gentle daily cleanser for sensitive skin", false, 4),
                new Product(2L, "Moisturizer", "Hydrating cream with hyaluronic acid", false, 5),
                new Product(3L, "Sunscreen", "SPF 50 broad spectrum protection", true, 3),
                new Product(4L, "Serum", "Vitamin C brightening serum", false, 4),
                new Product(5L, "Toner", "Alcohol-free balancing toner", true, 2)
        );
    }
}