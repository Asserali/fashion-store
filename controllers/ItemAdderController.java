package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.Product;
import com.fashionstore.storage.DataManager;
import com.fashionstore.utils.ImageManager;
import com.fashionstore.utils.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;

public class ItemAdderController {

    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private TextField priceField;
    @FXML
    private TextField stockField;
    @FXML
    private TextField colorField;
    @FXML
    private TextField sizeField;
    @FXML
    private TextField brandField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private Button saveButton;
    @FXML
    private Label errorLabel;
    @FXML
    private Label imageLabel;

    private DataManager dataManager;
    private String selectedImagePath = null;
    private String editingProductId;
    private String originalImagePath = null; // Store the original image path when editing
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        dataManager = FashionStoreApp.getDataManager();

        // Initialize category dropdown
        categoryComboBox.getItems().addAll(
                "Tops", "Bottoms", "Shoes", "Outerwear", "Accessories");
        categoryComboBox.setValue("Tops");

        // Hide error label initially
        errorLabel.setVisible(false);
    }

    @FXML
    private void chooseImage(ActionEvent event) {
        String tempId = "temp_" + System.currentTimeMillis();
        String newImagePath = ImageManager.saveProductImageToCustomDir(tempId, "src/main/resources/images/");

        if (newImagePath != null) {
            selectedImagePath = newImagePath;
            imageLabel.setText("Image selected");
            SceneManager.showAlert("Image Selected", "Image has been selected successfully.");
        } else {
            // Keep original image if selection was canceled
            if (isEditMode) {
                imageLabel.setText("Using original image");
            } else {
                imageLabel.setText("No image selected (will use default)");
            }
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            if (!validateInputs()) {
                errorLabel.setVisible(true);
                return;
            }
            errorLabel.setVisible(false);

            Product product = createProductFromFields();

            // Check if we're editing an existing product
            if (isEditMode && editingProductId != null && !editingProductId.isEmpty()) {
                // Preserve the original ID when editing
                product.setProductId(editingProductId);

                // Set product visibility (preserve it from original product)
                Product originalProduct = dataManager.getProduct(editingProductId);
                if (originalProduct != null) {
                    product.setVisible(originalProduct.isVisible());
                }

                System.out.println("Updating product: " + editingProductId);
                System.out.println("Image path: " + product.getImagePath());

                dataManager.updateProduct(product);
                SceneManager.showAlert("Success", "Product updated successfully!");
            } else {
                // Make new product visible by default
                product.setVisible(true);

                // Add as a new product
                dataManager.addProduct(product);
                SceneManager.showAlert("Success", "Product added successfully!");
            }

            // Save all data to ensure changes are persisted
            dataManager.saveAllData();

            // Refresh the admin view if needed
            Stage stage = (Stage) saveButton.getScene().getWindow();
            Object userData = stage.getUserData();
            if (userData instanceof AdminController) {
                ((AdminController) userData).refreshProductTable();
            }

            closeWindow();
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
            errorLabel.setVisible(true);
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        try {
            String name = nameField.getText().trim();
            String category = categoryComboBox.getValue();
            String priceText = priceField.getText().trim();
            String stockText = stockField.getText().trim();
            String description = descriptionField.getText().trim();

            if (name.isEmpty()) {
                errorLabel.setText("Product name cannot be empty");
                return false;
            }
            if (category == null || category.isEmpty()) {
                errorLabel.setText("Please select a category");
                return false;
            }
            if (priceText.isEmpty()) {
                errorLabel.setText("Price cannot be empty");
                return false;
            }
            if (stockText.isEmpty()) {
                errorLabel.setText("Stock quantity cannot be empty");
                return false;
            }
            if (description.isEmpty()) {
                errorLabel.setText("Description cannot be empty");
                return false;
            }

            try {
                BigDecimal price = new BigDecimal(priceText);
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    errorLabel.setText("Price must be greater than zero");
                    return false;
                }

                int stock = Integer.parseInt(stockText);
                if (stock < 0) {
                    errorLabel.setText("Stock must be zero or positive");
                    return false;
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Please enter valid numbers for price and stock");
                return false;
            }

            return true;
        } catch (NullPointerException e) {
            errorLabel.setText("System error: Form not properly initialized");
            e.printStackTrace();
            return false;
        }
    }

    private Product createProductFromFields() {
        // Existing field processing
        String name = nameField.getText().trim();
        String category = categoryComboBox.getValue();
        BigDecimal price = new BigDecimal(priceField.getText().trim());
        int stock = Integer.parseInt(stockField.getText().trim());
        String color = colorField.getText().trim();
        String size = sizeField.getText().trim();
        String brand = brandField.getText().trim();
        String description = descriptionField.getText().trim();

        Product product = new Product(name, category, price);
        product.setDescription(description);
        product.setStockQuantity(stock);
        product.setColor(color.isEmpty() ? null : color);
        product.setSize(size.isEmpty() ? null : size);
        product.setBrand(brand.isEmpty() ? null : brand);

        // Improved image path handling
        if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
            product.setImagePath(selectedImagePath);
            System.out.println("Setting selected image path: " + selectedImagePath);
        } else if (isEditMode && originalImagePath != null && !originalImagePath.isEmpty()) {
            // When editing a product and no new image is selected, preserve the original
            // image
            product.setImagePath(originalImagePath);
            System.out.println("Preserving original image path: " + originalImagePath);
        } else {
            // Default image for new products when no image is selected
            product.setImagePath("/images/default-product.jpg");
            System.out.println("Using default image path");
        }

        return product;
    }

    private boolean verifyImageExists(String path) {
        try {
            // First try to load as resource
            if (getClass().getResource(path) != null) {
                return true;
            }

            // Fallback to file system check (for development)
            File file = new File("src/main/resources" + path);
            return file.exists();
        } catch (Exception e) {
            return false;
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    public void loadProductForEditing(Product product) {
        if (product == null) {
            SceneManager.showErrorAlert("Error", "Cannot load product: Product object is null");
            return;
        }

        System.out.println("Loading product for editing: " + product.getProductId() + " - " + product.getName());
        System.out.println("Image path: " + product.getImagePath());

        // Set edit mode flag
        isEditMode = true;

        // Populate form fields with product data
        nameField.setText(product.getName());
        categoryComboBox.setValue(product.getCategory());
        priceField.setText(product.getPrice().toString());
        stockField.setText(String.valueOf(product.getStockQuantity()));
        colorField.setText(product.getColor() != null ? product.getColor() : "");
        sizeField.setText(product.getSize() != null ? product.getSize() : "");
        brandField.setText(product.getBrand() != null ? product.getBrand() : "");
        descriptionField.setText(product.getDescription() != null ? product.getDescription() : "");

        // Store the product ID for updating
        this.editingProductId = product.getProductId();

        // Store the original image path
        this.originalImagePath = product.getImagePath();

        // Update image label
        if (originalImagePath != null && !originalImagePath.isEmpty()) {
            imageLabel.setText("Using existing image");
        } else {
            imageLabel.setText("No image (will use default)");
        }
    }
}