package com.skincaretracker.controller;

import com.skincaretracker.model.Product;
import com.skincaretracker.util.DatabaseManager;
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
import javafx.application.Platform;
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

    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;

    // Dialog components
    private Dialog<Product> productDialog;
    private TextField productNameField;
    private TextArea productDescriptionField;
    private CheckBox allergicCheckBox;
    private HBox ratingStars;
    private int selectedRating = 0;
    private Product editingProduct = null; // Track which product we're editing

    public void initialize() {
        if (!dbManager.isUserLoggedIn()) {
            showError("User not logged in. Please login first.");
            goBackToDashboard();
            return;
        }

        setupTable();
        setupFilters();
        createProductDialog();
        loadProductsFromDatabase();
    }

    @FXML
    private void goBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Dashboard.fxml"));
            Parent dashboardRoot = loader.load();

            Stage currentStage = (Stage) backToDashboardButton.getScene().getWindow();
            Scene dashboardScene = new Scene(dashboardRoot);

            var cssResource = getClass().getResource("/style/style.css");
            if (cssResource != null) {
                dashboardScene.getStylesheets().add(cssResource.toExternalForm());
            }

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

        setupActionsColumn();

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
        editingProduct = null; // We're creating a new product
        resetDialogFields();
        productDialog.setHeaderText("Add New Product");

        Optional<Product> result = productDialog.showAndWait();
        if (result.isPresent()) {
            addProduct(result.get());
        }
    }

    private void createProductDialog() {
        productDialog = new Dialog<>();
        productDialog.setTitle("Product Manager");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        productNameField = new TextField();
        productNameField.setPromptText("Product Name");

        productDescriptionField = new TextArea();
        productDescriptionField.setPromptText("Description");
        productDescriptionField.setPrefRowCount(3);

        allergicCheckBox = new CheckBox("Mark as Allergic");

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

        DialogPane dialogPane = productDialog.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Improve validation and result conversion
        productDialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return validateAndCreateProduct();
            }
            return null;
        });

        // Add validation on OK button
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!isValidInput()) {
                event.consume(); // Prevent dialog from closing
            }
        });
    }

    private Product validateAndCreateProduct() {
        String name = productNameField.getText().trim();
        String description = productDescriptionField.getText().trim();

        if (!isValidInput()) {
            return null;
        }

        if (editingProduct != null) {
            // Return updated product with existing ID
            return new Product(
                    editingProduct.getId(),
                    name,
                    description,
                    allergicCheckBox.isSelected(),
                    selectedRating
            );
        } else {
            // Return new product (ID will be set by database)
            return new Product(
                    0L,
                    name,
                    description,
                    allergicCheckBox.isSelected(),
                    selectedRating
            );
        }
    }

    private boolean isValidInput() {
        String name = productNameField.getText().trim();

        if (name.isEmpty()) {
            showError("Product name is required");
            productNameField.requestFocus();
            return false;
        }

        if (name.length() > 100) { // Assuming max length
            showError("Product name is too long (max 100 characters)");
            productNameField.requestFocus();
            return false;
        }

        return true;
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
    }

    private void showEditDialog(Product product) {
        editingProduct = product; // Set the product we're editing

        productNameField.setText(product.getName());
        productDescriptionField.setText(product.getDescription());
        allergicCheckBox.setSelected(product.isAllergic());
        selectedRating = product.getRating();
        updateRatingStars();
        productDialog.setHeaderText("Edit Product");

        Optional<Product> result = productDialog.showAndWait();
        if (result.isPresent()) {
            updateProduct(result.get());
        }
    }

    private void updateProduct(Product updatedProduct) {
        try {
            boolean success = dbManager.updateProduct(updatedProduct);
            if (success) {
                // Find and update the product in our list
                for (int i = 0; i < products.size(); i++) {
                    Product p = products.get(i);
                    if (p.getId().equals(updatedProduct.getId())) {
                        products.set(i, updatedProduct);
                        break;
                    }
                }

                Platform.runLater(() -> {
                    productsTable.refresh();
                    filterProducts();
                });
                showInfo("Product updated successfully!");
            } else {
                showError("Failed to update product in database");
                // Reload from database to revert changes
                loadProductsFromDatabase();
            }
        } catch (Exception e) {
            showError("Error updating product: " + e.getMessage());
            loadProductsFromDatabase();
        }
    }

    private void showDeleteConfirmation(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Product");
        alert.setHeaderText("Delete " + product.getName());
        alert.setContentText("Are you sure you want to delete this product? This will also delete all associated reminders.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteProduct(product);
        }
    }

    private void deleteProduct(Product product) {
        try {
            boolean success = dbManager.deleteProduct(product.getId());
            if (success) {
                Platform.runLater(() -> {
                    products.remove(product);
                });
                showInfo("Product deleted successfully!");
            } else {
                showError("Failed to delete product from database");
            }
        } catch (Exception e) {
            showError("Error deleting product: " + e.getMessage());
        }
    }

    private void addProduct(Product product) {
        try {
            Product createdProduct = dbManager.createProductForCurrentUser(
                    product.getName(),
                    product.getDescription(),
                    product.isAllergic(),
                    product.getRating()
            );

            if (createdProduct != null) {
                Platform.runLater(() -> {
                    products.add(createdProduct);
                    filterProducts();
                    // Scroll to the new product
                    productsTable.getSelectionModel().select(createdProduct);
                    productsTable.scrollTo(createdProduct);
                });
                showInfo("Product added successfully!");
            } else {
                showError("Failed to add product to database");
            }
        } catch (Exception e) {
            showError("Error adding product: " + e.getMessage());
        }
    }

    private void loadProductsFromDatabase() {
        try {
            products.clear();
            products.addAll(dbManager.getCurrentUserProducts());

            Platform.runLater(() -> {
                filterProducts();
            });
        } catch (Exception e) {
            showError("Failed to load products from database: " + e.getMessage());
        }
    }

    @FXML
    private void refreshProducts() {
        loadProductsFromDatabase();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}