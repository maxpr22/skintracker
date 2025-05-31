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

    private Dialog<Product> productDialog;
    private TextField productNameField;
    private TextArea productDescriptionField;
    private CheckBox allergicCheckBox;
    private HBox ratingStars;
    private int selectedRating = 0;
    private Product editingProduct = null;

    public void initialize() {
        if (!dbManager.isUserLoggedIn()) {
            showError("Користувач не увійшов в систему. Будь ласка, увійдіть спочатку.");
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
            currentStage.setTitle("Трекер догляду за шкірою - Головна панель");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Не вдалося завантажити головну панель: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Неочікувана помилка: " + e.getMessage());
        }
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        allergicColumn.setCellValueFactory(new PropertyValueFactory<>("allergic"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));

        allergicColumn.setCellFactory(CheckBoxTableCell.forTableColumn(allergicColumn));
        allergicColumn.setEditable(true);

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
            private final Button editButton = new Button("Редагувати");
            private final Button deleteButton = new Button("Видалити");
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
        filterComboBox.getItems().addAll("Всі", "Алергенні", "Неалергенні");
        filterComboBox.setValue("Всі");

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
                case "Алергенні" -> product.isAllergic();
                case "Неалергенні" -> !product.isAllergic();
                default -> true;
            };

            return matchesSearch && matchesFilter;
        });
    }

    @FXML
    private void showAddProductDialog() {
        editingProduct = null;
        resetDialogFields();
        productDialog.setHeaderText("Додати новий продукт");

        Optional<Product> result = productDialog.showAndWait();
        if (result.isPresent()) {
            addProduct(result.get());
        }
    }

    private void createProductDialog() {
        productDialog = new Dialog<>();
        productDialog.setTitle("Менеджер продуктів");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        productNameField = new TextField();
        productNameField.setPromptText("Назва продукту");

        productDescriptionField = new TextArea();
        productDescriptionField.setPromptText("Опис");
        productDescriptionField.setPrefRowCount(3);

        allergicCheckBox = new CheckBox("Позначити як алергенний");

        HBox ratingContainer = new HBox(10);
        ratingContainer.getChildren().add(new Label("Рейтинг:"));
        ratingStars = new HBox(5);
        setupRatingStars();
        ratingContainer.getChildren().add(ratingStars);

        content.getChildren().addAll(
                new Label("Назва продукту:"),
                productNameField,
                new Label("Опис:"),
                productDescriptionField,
                allergicCheckBox,
                ratingContainer
        );

        DialogPane dialogPane = productDialog.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        productDialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return validateAndCreateProduct();
            }
            return null;
        });

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!isValidInput()) {
                event.consume();
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
            return new Product(
                    editingProduct.getId(),
                    name,
                    description,
                    allergicCheckBox.isSelected(),
                    selectedRating
            );
        } else {
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
            showError("Назва продукту є обов'язковою");
            productNameField.requestFocus();
            return false;
        }

        if (name.length() > 100) {
            showError("Назва продукту занадто довга (максимум 100 символів)");
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
        editingProduct = product;

        productNameField.setText(product.getName());
        productDescriptionField.setText(product.getDescription());
        allergicCheckBox.setSelected(product.isAllergic());
        selectedRating = product.getRating();
        updateRatingStars();
        productDialog.setHeaderText("Редагувати продукт");

        Optional<Product> result = productDialog.showAndWait();
        if (result.isPresent()) {
            updateProduct(result.get());
        }
    }

    private void updateProduct(Product updatedProduct) {
        try {
            boolean success = dbManager.updateProduct(updatedProduct);
            if (success) {
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
                showInfo("Продукт успішно оновлено!");
            } else {
                showError("Не вдалося оновити продукт у базі даних");
                loadProductsFromDatabase();
            }
        } catch (Exception e) {
            showError("Помилка при оновленні продукту: " + e.getMessage());
            loadProductsFromDatabase();
        }
    }

    private void showDeleteConfirmation(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Видалення продукту");
        alert.setHeaderText("Видалити " + product.getName());
        alert.setContentText("Ви впевнені, що хочете видалити цей продукт? Це також видалить всі пов'язані нагадування.");

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
                showInfo("Продукт успішно видалено!");
            } else {
                showError("Не вдалося видалити продукт з бази даних");
            }
        } catch (Exception e) {
            showError("Помилка при видаленні продукту: " + e.getMessage());
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
                    productsTable.getSelectionModel().select(createdProduct);
                    productsTable.scrollTo(createdProduct);
                });
                showInfo("Продукт успішно додано!");
            } else {
                showError("Не вдалося додати продукт до бази даних");
            }
        } catch (Exception e) {
            showError("Помилка при додаванні продукту: " + e.getMessage());
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
            showError("Не вдалося завантажити продукти з бази даних: " + e.getMessage());
        }
    }

    @FXML
    private void refreshProducts() {
        loadProductsFromDatabase();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Помилка");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Успіх");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}