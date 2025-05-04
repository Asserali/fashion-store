package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.Product;
import com.fashionstore.models.User;
import com.fashionstore.models.Outfit;
import com.fashionstore.storage.DataManager;
import com.fashionstore.utils.SceneManager;
import com.fashionstore.utils.WindowManager;
import com.fashionstore.utils.PasswordUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.stream.Collectors;

public class AdminController {

    @FXML
    private TableView<Product> productTable;
    @FXML
    private TableColumn<Product, String> nameColumn;
    @FXML
    private TableColumn<Product, String> categoryColumn;
    @FXML
    private TableColumn<Product, BigDecimal> priceColumn;
    @FXML
    private TableColumn<Product, Integer> stockColumn;
    @FXML
    private TableColumn<Product, Boolean> visibilityColumn;
    @FXML
    private Label statusLabel;

    private DataManager dataManager;

    public void initialize() {
        // Get data manager from main application
        dataManager = FashionStoreApp.getDataManager();

        // Just to be safe, reload all data
        dataManager.loadAllData();

        // Initialize table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        visibilityColumn.setCellValueFactory(new PropertyValueFactory<>("visible"));

        // Set visibility column to display "Visible" or "Hidden"
        visibilityColumn.setCellFactory(column -> new TableCell<Product, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Visible" : "Hidden");
                }
            }
        });

        // Load product data
        refreshProductTable();
    }

    @FXML
    private void backToLogin() {
        SceneManager.loadScene("LoginView.fxml");
    }

    @FXML
    private void openItemAdder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItemAdder.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Add New Product");

            // Create scene
            Scene scene = new Scene(root);

            // Get current theme from the main window
            String currentTheme = "light"; // Default theme
            if (productTable.getScene() != null &&
                    productTable.getScene().getRoot().getProperties().get("theme") != null) {
                currentTheme = (String) productTable.getScene().getRoot().getProperties().get("theme");
            }

            // Apply appropriate theme CSS
            if ("dark".equals(currentTheme)) {
                URL darkCssUrl = getClass().getResource("/styles/dark-theme.css");
                if (darkCssUrl != null) {
                    scene.getStylesheets().add(darkCssUrl.toExternalForm());
                }

                // Store the theme preference in the new scene
                scene.getRoot().getProperties().put("theme", "dark");
            } else {
                // Default to light theme
                URL cssUrl = getClass().getResource("/styles/application.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }

                // Store the theme preference in the new scene
                scene.getRoot().getProperties().put("theme", "light");
            }

            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setUserData(this);
            stage.showAndWait(); // Use showAndWait to ensure refresh after dialog closes

            // Refresh data after dialog closes (even without explicit callback)
            refreshProductTable();
        } catch (IOException e) {
            SceneManager.showErrorAlert("Error", "Failed to load item adder: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void editSelectedItem() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                System.out.println("Editing product: " + selected.getProductId() + " - " + selected.getName());
                System.out.println("Current image path: " + selected.getImagePath());

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItemAdder.fxml"));
                Parent root = loader.load();

                ItemAdderController controller = loader.getController();
                controller.loadProductForEditing(selected);

                Stage stage = new Stage();
                stage.setTitle("Edit Product");

                // Create scene
                Scene scene = new Scene(root);

                // Get current theme from the main window
                String currentTheme = "light"; // Default theme
                if (productTable.getScene() != null &&
                        productTable.getScene().getRoot().getProperties().get("theme") != null) {
                    currentTheme = (String) productTable.getScene().getRoot().getProperties().get("theme");
                }

                // Apply appropriate theme CSS
                if ("dark".equals(currentTheme)) {
                    URL darkCssUrl = getClass().getResource("/styles/dark-theme.css");
                    if (darkCssUrl != null) {
                        scene.getStylesheets().add(darkCssUrl.toExternalForm());
                    }

                    // Store the theme preference in the new scene
                    scene.getRoot().getProperties().put("theme", "dark");
                } else {
                    // Default to light theme
                    URL cssUrl = getClass().getResource("/styles/application.css");
                    if (cssUrl != null) {
                        scene.getStylesheets().add(cssUrl.toExternalForm());
                    }

                    // Store the theme preference in the new scene
                    scene.getRoot().getProperties().put("theme", "light");
                }

                stage.setScene(scene);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setUserData(this);

                // Show dialog and wait for it to close
                stage.showAndWait();

                // After dialog closes, force refresh
                System.out.println("Dialog closed, refreshing data...");
                dataManager.loadAllData(); // Reload all data from database
                refreshProductTable();

                // Also refresh other views
                WindowManager.refreshHomeView();
            } catch (IOException e) {
                SceneManager.showErrorAlert("Error", "Failed to load editor: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            SceneManager.showAlert("No Selection", "Please select a product to edit.");
        }
    }

    @FXML
    private void removeSelectedItem() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Use SceneManager's confirmation alert for consistent styling
            SceneManager.showConfirmationAlert(
                    "Confirm Deletion",
                    "Are you sure you want to delete " + selected.getName()
                            + "?\n\nThis will also remove it from all user wardrobes, outfits, and shopping carts.",
                    () -> {
                        // On confirm
                        System.out.println("Deleting product: " + selected.getProductId() + " - " + selected.getName());

                        try {
                            // Delete the product
                            dataManager.deleteProduct(selected.getProductId());

                            // Reload all data from database to ensure consistency
                            dataManager.loadAllData();

                            // Refresh views
                            refreshProductTable();
                            WindowManager.refreshHomeView();

                            setStatus("Product \"" + selected.getName() + "\" has been removed from the system.");
                        } catch (Exception e) {
                            SceneManager.showErrorAlert("Error", "Failed to delete product: " + e.getMessage());
                            e.printStackTrace();
                        }
                    },
                    null); // No action on cancel
        } else {
            SceneManager.showAlert("No Selection", "Please select a product to remove.");
        }
    }

    @FXML
    private void toggleProductVisibility() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Toggle visibility
            boolean newVisibility = !selected.isVisible();
            selected.setVisible(newVisibility);

            // Update product in database
            dataManager.updateProduct(selected);
            dataManager.saveAllData();

            // Refresh views
            refreshProductTable();
            WindowManager.refreshHomeView();

            String statusMessage = "Product \"" + selected.getName() + "\" is now " +
                    (newVisibility ? "visible" : "hidden") + " in store";
            setStatus(statusMessage);
        } else {
            SceneManager.showAlert("No Selection", "Please select a product to toggle visibility.");
        }
    }

    @FXML
    public void refreshView() {
        dataManager.loadAllData();
        refreshProductTable();
        setStatus("Data refreshed from database");
    }

    @FXML
    public void setLightMode() {
        try {
            if (productTable.getScene() != null) {
                // Remove dark theme if present
                productTable.getScene().getStylesheets().removeIf(
                        style -> style.contains("dark-theme.css"));

                // Add light theme stylesheet
                String lightThemePath = getClass().getResource("/styles/application.css").toExternalForm();
                if (!productTable.getScene().getStylesheets().contains(lightThemePath)) {
                    productTable.getScene().getStylesheets().add(lightThemePath);
                }

                // Store theme preference
                productTable.getScene().getRoot().getProperties().put("theme", "light");

                setStatus("Light theme applied");
            }
        } catch (Exception e) {
            System.err.println("Failed to apply light theme: " + e.getMessage());
        }
    }

    @FXML
    public void setDarkMode() {
        try {
            if (productTable.getScene() != null) {
                // Remove light theme if present
                productTable.getScene().getStylesheets().removeIf(
                        style -> style.contains("application.css"));

                // Add dark theme stylesheet
                String darkThemePath = getClass().getResource("/styles/dark-theme.css").toExternalForm();
                if (!productTable.getScene().getStylesheets().contains(darkThemePath)) {
                    productTable.getScene().getStylesheets().add(darkThemePath);
                }

                // Store theme preference
                productTable.getScene().getRoot().getProperties().put("theme", "dark");

                setStatus("Dark theme applied");
            }
        } catch (Exception e) {
            System.err.println("Failed to apply dark theme: " + e.getMessage());
        }
    }

    @FXML
    public void openAbout() {
        SceneManager.showAlert("About Fashion Store - Admin",
                "Fashion Store Admin Panel\nVersion 1.0\n" +
                        "Provides administrative functions for managing the store inventory, users, and analytics.");
    }

    @FXML
    public void handleExit() {
        boolean confirm = SceneManager.showConfirmationDialog(
                "Exit Admin Panel",
                "Are you sure you want to exit the admin panel?",
                "You will be returned to the login screen.");

        if (confirm) {
            backToLogin();
        }
    }

    @FXML
    public void exportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Store Data");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName("fashion_store_data_" +
                java.time.LocalDate.now().toString() + ".json");

        Stage stage = (Stage) productTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                // Export all store data to JSON
                exportStoreDataToJson(file);

                // Show success message
                setStatus("Store data exported to " + file.getAbsolutePath());
                SceneManager.showAlert("Export Successful",
                        "Store data has been exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                SceneManager.showErrorAlert("Export Error",
                        "Failed to export store data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Exports all store data to a JSON file
     * 
     * @param file The file to export to
     * @throws IOException If there's an error writing to the file
     */
    private void exportStoreDataToJson(File file) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // Export products
        json.append("  \"products\": [\n");
        List<Product> products = dataManager.getAllProducts();
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            json.append("    {\n");
            json.append("      \"productId\": \"").append(escapeJson(product.getProductId())).append("\",\n");
            json.append("      \"name\": \"").append(escapeJson(product.getName())).append("\",\n");
            json.append("      \"category\": \"").append(escapeJson(product.getCategory())).append("\",\n");
            json.append("      \"description\": \"").append(escapeJson(product.getDescription())).append("\",\n");
            json.append("      \"price\": ").append(product.getPrice()).append(",\n");
            json.append("      \"stockQuantity\": ").append(product.getStockQuantity()).append(",\n");
            json.append("      \"visible\": ").append(product.isVisible()).append(",\n");
            json.append("      \"imagePath\": \"").append(escapeJson(product.getImagePath())).append("\"\n");
            json.append("    }").append(i < products.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ],\n");

        // Export users (excluding sensitive data)
        json.append("  \"users\": [\n");
        List<User> users = dataManager.getAllUsers();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            json.append("    {\n");
            json.append("      \"userId\": \"").append(escapeJson(user.getUserId())).append("\",\n");
            json.append("      \"username\": \"").append(escapeJson(user.getUsername())).append("\",\n");
            json.append("      \"email\": \"").append(escapeJson(user.getEmail())).append("\",\n");
            json.append("      \"firstName\": \"").append(escapeJson(user.getFirstName())).append("\",\n");
            json.append("      \"lastName\": \"").append(escapeJson(user.getLastName())).append("\",\n");

            // Include wardrobe items
            json.append("      \"wardrobeItems\": [");
            List<String> wardrobeItems = user.getWardrobeItemIds();
            for (int j = 0; j < wardrobeItems.size(); j++) {
                json.append("\"").append(escapeJson(wardrobeItems.get(j))).append("\"")
                        .append(j < wardrobeItems.size() - 1 ? ", " : "");
            }
            json.append("],\n");

            // Include outfits
            json.append("      \"outfits\": [");
            List<String> outfits = user.getOutfitIds();
            for (int j = 0; j < outfits.size(); j++) {
                json.append("\"").append(escapeJson(outfits.get(j))).append("\"")
                        .append(j < outfits.size() - 1 ? ", " : "");
            }
            json.append("]\n");

            json.append("    }").append(i < users.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ],\n");

        // Export outfits
        json.append("  \"outfits\": [\n");
        List<Outfit> outfits = new ArrayList<>();
        for (User user : users) {
            List<Outfit> userOutfits = dataManager.getUserOutfits(user.getUserId());
            outfits.addAll(userOutfits);
        }

        for (int i = 0; i < outfits.size(); i++) {
            Outfit outfit = outfits.get(i);
            json.append("    {\n");
            json.append("      \"outfitId\": \"").append(escapeJson(outfit.getOutfitId())).append("\",\n");
            json.append("      \"name\": \"").append(escapeJson(outfit.getName())).append("\",\n");
            json.append("      \"createdBy\": \"").append(escapeJson(outfit.getUserId())).append("\",\n");

            if (outfit.getCreatedAt() != null) {
                json.append("      \"dateCreated\": \"").append(outfit.getCreatedAt().toString()).append("\",\n");
            } else {
                json.append("      \"dateCreated\": null,\n");
            }

            // Include product IDs - convert Set to List if needed
            json.append("      \"productIds\": [");
            List<String> productIds = new ArrayList<>(outfit.getProductIds());
            for (int j = 0; j < productIds.size(); j++) {
                json.append("\"").append(escapeJson(productIds.get(j))).append("\"")
                        .append(j < productIds.size() - 1 ? ", " : "");
            }
            json.append("]\n");

            json.append("    }").append(i < outfits.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ]\n");

        json.append("}");

        // Write to file
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(json.toString());
        }
    }

    /**
     * Escapes a string for JSON format
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @FXML
    public void importData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Store Data");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        Stage stage = (Stage) productTable.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // Confirm import as it will overwrite existing data
            boolean confirm = SceneManager.showConfirmationDialog(
                    "Confirm Import",
                    "Importing data will overwrite existing data. Continue?",
                    "This operation cannot be undone.");

            if (confirm) {
                try {
                    // Show a simple message for now since actual import would require
                    // a complex parser implementation
                    setStatus("Selected import file: " + file.getAbsolutePath());
                    SceneManager.showAlert("Import Data",
                            "Data import functionality will be implemented in a future version.\n\n" +
                                    "Selected file: " + file.getAbsolutePath());

                    /*
                     * // The actual implementation would parse the JSON file and update the
                     * database
                     * // This would be complex and require careful handling of references between
                     * entities
                     * importStoreDataFromJson(file);
                     * dataManager.loadAllData();
                     * refreshProductTable();
                     * 
                     * setStatus("Store data imported from " + file.getAbsolutePath());
                     * SceneManager.showAlert("Import Successful",
                     * "Store data has been imported from:\n" + file.getAbsolutePath());
                     */
                } catch (Exception e) {
                    SceneManager.showErrorAlert("Import Error",
                            "Failed to import store data: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    public void showUserManagement() {
        try {
            // Create a dialog/stage for user management
            Stage userManagementStage = new Stage();
            userManagementStage.setTitle("User Management");
            userManagementStage.initModality(Modality.WINDOW_MODAL);
            userManagementStage.initOwner(productTable.getScene().getWindow());

            // Create a VBox as the root container
            VBox root = new VBox(10);
            root.setPadding(new Insets(20));
            root.setMinWidth(800);
            root.setMinHeight(600);

            // Add a header
            Label headerLabel = new Label("User Management");
            headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            headerLabel.getStyleClass().add("header-title");

            // Create a TableView for users
            TableView<User> userTable = new TableView<>();
            userTable.setPrefHeight(400);
            userTable.setPlaceholder(new Label("No users found"));

            // Create columns
            TableColumn<User, String> idColumn = new TableColumn<>("User ID");
            idColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
            idColumn.setPrefWidth(150);

            TableColumn<User, String> usernameColumn = new TableColumn<>("Username");
            usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
            usernameColumn.setPrefWidth(150);

            TableColumn<User, String> emailColumn = new TableColumn<>("Email");
            emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
            emailColumn.setPrefWidth(250);

            TableColumn<User, String> lastLoginColumn = new TableColumn<>("Last Login");
            lastLoginColumn.setCellValueFactory(data -> {
                User user = data.getValue();
                return new SimpleStringProperty(
                        user.getLastLogin() != null ? user.getLastLogin().toString() : "Never");
            });
            lastLoginColumn.setPrefWidth(200);

            TableColumn<User, String> statusColumn = new TableColumn<>("Status");
            statusColumn.setCellValueFactory(data -> {
                User user = data.getValue();
                String status = "";

                if (user.isBanned()) {
                    int daysLeft = user.getDaysLeftOnBan();
                    if (daysLeft > 0) {
                        status = "Banned (" + daysLeft + " days left)";
                    } else if (daysLeft == -1) {
                        status = "Permanently Banned";
                    } else {
                        status = "Ban Expired";
                    }
                } else if (user.isDeactivated()) {
                    status = "Deactivated";
                } else {
                    status = "Active";
                }

                return new SimpleStringProperty(status);
            });

            // Add a special cell factory to apply color styling to the status
            statusColumn.setCellFactory(column -> new TableCell<User, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);

                        // Apply style based on status
                        if (item.contains("Banned")) {
                            setStyle("-fx-text-fill: #cc0000;"); // Red for banned
                        } else if (item.equals("Deactivated")) {
                            setStyle("-fx-text-fill: #cc6600;"); // Orange for deactivated
                        } else if (item.equals("Active")) {
                            setStyle("-fx-text-fill: #009900;"); // Green for active
                        } else {
                            setStyle("");
                        }
                    }
                }
            });

            statusColumn.setPrefWidth(150);

            // Add columns to table
            userTable.getColumns().addAll(idColumn, usernameColumn, emailColumn, lastLoginColumn, statusColumn);

            // Load users
            List<User> users = dataManager.getAllUsers();
            userTable.setItems(FXCollections.observableArrayList(users));

            // Create button bar
            HBox buttonBar = new HBox(10);
            buttonBar.setAlignment(Pos.CENTER_RIGHT);

            Button addUserBtn = new Button("Add User");
            addUserBtn.getStyleClass().add("action-button");
            addUserBtn.setOnAction(e -> {
                // Create a dialog for adding a new user
                Stage addUserStage = new Stage();
                addUserStage.setTitle("Add New User");
                addUserStage.initModality(Modality.WINDOW_MODAL);
                addUserStage.initOwner(userManagementStage);

                // Create form layout
                GridPane form = new GridPane();
                form.setHgap(10);
                form.setVgap(10);
                form.setPadding(new Insets(20));

                // Username field
                Label usernameLabel = new Label("Username:");
                TextField usernameField = new TextField();
                usernameField.setPromptText("Enter username");
                form.add(usernameLabel, 0, 0);
                form.add(usernameField, 1, 0);

                // Email field
                Label emailLabel = new Label("Email:");
                TextField emailField = new TextField();
                emailField.setPromptText("Enter email");
                form.add(emailLabel, 0, 1);
                form.add(emailField, 1, 1);

                // Password field
                Label passwordLabel = new Label("Password:");
                PasswordField passwordField = new PasswordField();
                passwordField.setPromptText("Enter password");
                form.add(passwordLabel, 0, 2);
                form.add(passwordField, 1, 2);

                // First name field
                Label firstNameLabel = new Label("First Name:");
                TextField firstNameField = new TextField();
                firstNameField.setPromptText("Enter first name");
                form.add(firstNameLabel, 0, 3);
                form.add(firstNameField, 1, 3);

                // Last name field
                Label lastNameLabel = new Label("Last Name:");
                TextField lastNameField = new TextField();
                lastNameField.setPromptText("Enter last name");
                form.add(lastNameLabel, 0, 4);
                form.add(lastNameField, 1, 4);

                // Error label
                Label errorLabel = new Label();
                errorLabel.setTextFill(javafx.scene.paint.Color.RED);
                form.add(errorLabel, 0, 5, 2, 1);

                // Button bar
                HBox formButtonBar = new HBox(10);
                formButtonBar.setAlignment(Pos.CENTER_RIGHT);

                Button saveBtn = new Button("Save");
                saveBtn.getStyleClass().add("action-button");
                saveBtn.setOnAction(saveEvent -> {
                    // Validate inputs
                    if (usernameField.getText().trim().isEmpty()) {
                        errorLabel.setText("Username is required");
                        return;
                    }

                    if (emailField.getText().trim().isEmpty()) {
                        errorLabel.setText("Email is required");
                        return;
                    }

                    if (passwordField.getText().trim().isEmpty()) {
                        errorLabel.setText("Password is required");
                        return;
                    }

                    // Check if username already exists
                    if (dataManager.getUserByUsername(usernameField.getText().trim()) != null) {
                        errorLabel.setText("Username already exists");
                        return;
                    }

                    try {
                        // Create new user
                        User newUser = new User(
                                usernameField.getText().trim(),
                                emailField.getText().trim(),
                                passwordField.getText().trim() // The User constructor will handle hashing now
                        );

                        // Set additional properties
                        if (!firstNameField.getText().trim().isEmpty()) {
                            newUser.setFirstName(firstNameField.getText().trim());
                        }

                        if (!lastNameField.getText().trim().isEmpty()) {
                            newUser.setLastName(lastNameField.getText().trim());
                        }

                        // Add user to the system
                        dataManager.addUser(newUser);
                        dataManager.saveAllData();

                        // Refresh the user table
                        userTable.setItems(FXCollections.observableArrayList(dataManager.getAllUsers()));

                        // Close the dialog
                        addUserStage.close();

                        // Show success message
                        setStatus("User '" + newUser.getUsername() + "' created successfully");
                    } catch (Exception ex) {
                        errorLabel.setText("Error creating user: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });

                Button cancelBtn = new Button("Cancel");
                cancelBtn.setOnAction(cancelEvent -> addUserStage.close());

                formButtonBar.getChildren().addAll(saveBtn, cancelBtn);
                form.add(formButtonBar, 0, 6, 2, 1);

                // Create scene and show dialog
                Scene scene = new Scene(form);

                // Apply the same theme as the parent window
                if (productTable.getScene().getStylesheets().stream()
                        .anyMatch(s -> s.contains("dark-theme.css"))) {
                    scene.getStylesheets().add(
                            getClass().getResource("/styles/dark-theme.css").toExternalForm());
                } else {
                    scene.getStylesheets().add(
                            getClass().getResource("/styles/application.css").toExternalForm());
                }

                addUserStage.setScene(scene);
                addUserStage.showAndWait();
            });

            Button removeUserBtn = new Button("Remove User");
            removeUserBtn.getStyleClass().add("delete-button");
            removeUserBtn.setOnAction(e -> {
                User selectedUser = userTable.getSelectionModel().getSelectedItem();
                if (selectedUser != null) {
                    // Prevent admin user deletion
                    if (selectedUser.getUsername().equals("admin")) {
                        SceneManager.showAlert("Cannot Delete Admin",
                                "The default admin account cannot be deleted for system security.");
                        return;
                    }

                    boolean confirm = SceneManager.showConfirmationDialog(
                            "Confirm Delete",
                            "Are you sure you want to delete user " + selectedUser.getUsername() + "?",
                            "This will remove all their data including wardrobes and outfits.");

                    if (confirm) {
                        try {
                            // Remove outfits created by this user
                            List<Outfit> userOutfits = dataManager.getUserOutfits(selectedUser.getUserId());
                            for (Outfit outfit : userOutfits) {
                                dataManager.removeOutfit(outfit.getOutfitId());
                            }

                            // Remove user from data manager
                            boolean removed = dataManager.removeUser(selectedUser.getUserId());

                            if (removed) {
                                // Refresh the user table
                                userTable.setItems(FXCollections.observableArrayList(dataManager.getAllUsers()));

                                // Show success message
                                setStatus("User '" + selectedUser.getUsername() + "' deleted successfully");
                            } else {
                                SceneManager.showErrorAlert("Error Deleting User",
                                        "Failed to delete user from database");
                            }
                        } catch (Exception ex) {
                            SceneManager.showErrorAlert("Error Deleting User",
                                    "Failed to delete user: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                } else {
                    SceneManager.showAlert("No Selection", "Please select a user to remove");
                }
            });

            Button banUserBtn = new Button("Ban User");
            banUserBtn.getStyleClass().add("warning-button");
            banUserBtn.setOnAction(e -> {
                User selectedUser = userTable.getSelectionModel().getSelectedItem();
                if (selectedUser != null) {
                    // Prevent admin user banning
                    if (selectedUser.getUsername().equals("admin")) {
                        SceneManager.showAlert("Cannot Ban Admin",
                                "The default admin account cannot be banned.");
                        return;
                    }

                    // Create a dialog for banning the user
                    Stage banUserStage = new Stage();
                    banUserStage.setTitle("Ban User: " + selectedUser.getUsername());
                    banUserStage.initModality(Modality.WINDOW_MODAL);
                    banUserStage.initOwner(userManagementStage);

                    // Create form layout
                    GridPane form = new GridPane();
                    form.setHgap(10);
                    form.setVgap(10);
                    form.setPadding(new Insets(20));

                    // Reason field
                    Label reasonLabel = new Label("Ban Reason:");
                    TextField reasonField = new TextField();
                    reasonField.setPromptText("Enter reason for ban");
                    form.add(reasonLabel, 0, 0);
                    form.add(reasonField, 1, 0);

                    // Ban type radio buttons
                    Label banTypeLabel = new Label("Ban Type:");
                    form.add(banTypeLabel, 0, 1);

                    ToggleGroup banTypeGroup = new ToggleGroup();
                    RadioButton permanentRadio = new RadioButton("Permanent");
                    permanentRadio.setToggleGroup(banTypeGroup);
                    permanentRadio.setSelected(true);
                    RadioButton temporaryRadio = new RadioButton("Temporary");
                    temporaryRadio.setToggleGroup(banTypeGroup);

                    HBox radioBox = new HBox(10, permanentRadio, temporaryRadio);
                    form.add(radioBox, 1, 1);

                    // Duration field (for temporary bans)
                    Label durationLabel = new Label("Duration (days):");
                    form.add(durationLabel, 0, 2);

                    Spinner<Integer> durationSpinner = new Spinner<>(1, 365, 7);
                    durationSpinner.setEditable(true);
                    durationSpinner.setDisable(true); // Initially disabled (permanent ban)
                    form.add(durationSpinner, 1, 2);

                    // Enable/disable duration spinner based on radio selection
                    temporaryRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        durationSpinner.setDisable(!newVal);
                    });

                    // Error label
                    Label errorLabel = new Label();
                    errorLabel.setTextFill(javafx.scene.paint.Color.RED);
                    errorLabel.setVisible(false);
                    form.add(errorLabel, 0, 3, 2, 1);

                    // Buttons
                    HBox formButtonBar = new HBox(10);
                    formButtonBar.setAlignment(Pos.CENTER_RIGHT);

                    Button confirmBtn = new Button("Confirm Ban");
                    confirmBtn.getStyleClass().add("warning-button");
                    confirmBtn.setOnAction(event -> {
                        // Validate input
                        if (reasonField.getText().trim().isEmpty()) {
                            errorLabel.setText("Please enter a reason for the ban");
                            errorLabel.setVisible(true);
                            return;
                        }

                        boolean success;
                        String reason = reasonField.getText().trim();

                        if (permanentRadio.isSelected()) {
                            // Permanent ban
                            success = dataManager.banUser(selectedUser.getUserId(), reason);
                        } else {
                            // Temporary ban
                            int days = durationSpinner.getValue();
                            success = dataManager.banUserTemporarily(selectedUser.getUserId(), reason, days);
                        }

                        if (success) {
                            // Reload all users to get fresh user data with updated ban status
                            List<User> updatedUsers = dataManager.getAllUsers();

                            // Refresh the user table
                            userTable.setItems(FXCollections.observableArrayList(updatedUsers));
                            userTable.refresh(); // Explicitly refresh the table view
                            banUserStage.close();

                            // Show success message
                            setStatus("User '" + selectedUser.getUsername() + "' has been banned");
                        } else {
                            errorLabel.setText("Failed to ban user");
                            errorLabel.setVisible(true);
                        }
                    });

                    Button cancelBtn = new Button("Cancel");
                    cancelBtn.setOnAction(event -> banUserStage.close());

                    formButtonBar.getChildren().addAll(confirmBtn, cancelBtn);
                    form.add(formButtonBar, 0, 4, 2, 1);

                    // Create scene and show dialog
                    Scene scene = new Scene(form);

                    // Apply the same theme as the parent window
                    if (productTable.getScene().getStylesheets().stream()
                            .anyMatch(s -> s.contains("dark-theme.css"))) {
                        scene.getStylesheets().add(
                                getClass().getResource("/styles/dark-theme.css").toExternalForm());
                    } else {
                        scene.getStylesheets().add(
                                getClass().getResource("/styles/application.css").toExternalForm());
                    }

                    banUserStage.setScene(scene);
                    banUserStage.showAndWait();
                } else {
                    SceneManager.showAlert("No Selection", "Please select a user to ban");
                }
            });

            Button unbanUserBtn = new Button("Unban User");
            unbanUserBtn.getStyleClass().add("action-button");
            unbanUserBtn.setOnAction(e -> {
                User selectedUser = userTable.getSelectionModel().getSelectedItem();
                if (selectedUser != null) {
                    if (!selectedUser.isBanned()) {
                        SceneManager.showAlert("Not Banned", "This user is not currently banned.");
                        return;
                    }

                    boolean confirm = SceneManager.showConfirmationDialog(
                            "Confirm Unban",
                            "Are you sure you want to unban user " + selectedUser.getUsername() + "?",
                            "This will restore their account access immediately.");

                    if (confirm) {
                        boolean success = dataManager.unbanUser(selectedUser.getUserId());
                        if (success) {
                            // Reload all users to get fresh user data with updated ban status
                            List<User> updatedUsers = dataManager.getAllUsers();

                            // Refresh the user table
                            userTable.setItems(FXCollections.observableArrayList(updatedUsers));
                            userTable.refresh(); // Explicitly refresh the table view

                            // Show success message
                            setStatus("User '" + selectedUser.getUsername() + "' has been unbanned");
                        } else {
                            SceneManager.showErrorAlert("Error", "Failed to unban user");
                        }
                    }
                } else {
                    SceneManager.showAlert("No Selection", "Please select a user to unban");
                }
            });

            Button closeBtn = new Button("Close");
            closeBtn.setOnAction(e -> userManagementStage.close());

            buttonBar.getChildren().addAll(addUserBtn, removeUserBtn, banUserBtn, unbanUserBtn, closeBtn);

            // Add all elements to root
            root.getChildren().addAll(headerLabel, userTable, buttonBar);

            // Create the scene and apply styles
            Scene scene = new Scene(root);

            // Apply the same theme as the parent window
            if (productTable.getScene().getStylesheets().stream()
                    .anyMatch(s -> s.contains("dark-theme.css"))) {
                scene.getStylesheets().add(
                        getClass().getResource("/styles/dark-theme.css").toExternalForm());
            } else {
                scene.getStylesheets().add(
                        getClass().getResource("/styles/application.css").toExternalForm());
            }

            userManagementStage.setScene(scene);
            userManagementStage.show();

        } catch (Exception e) {
            SceneManager.showErrorAlert("Error", "Failed to open User Management: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void showAnalytics() {
        try {
            // Create a dialog/stage for analytics
            Stage analyticsStage = new Stage();
            analyticsStage.setTitle("Store Analytics");
            analyticsStage.initModality(Modality.WINDOW_MODAL);
            analyticsStage.initOwner(productTable.getScene().getWindow());

            // Create a BorderPane as the root container
            BorderPane root = new BorderPane();
            root.setPadding(new Insets(20));
            root.setMinWidth(900);
            root.setMinHeight(700);

            // Add a header to the top
            Label headerLabel = new Label("Store Analytics Dashboard");
            headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            headerLabel.getStyleClass().add("header-title");

            HBox headerBox = new HBox(headerLabel);
            headerBox.setAlignment(Pos.CENTER_LEFT);
            headerBox.setPadding(new Insets(0, 0, 20, 0));
            root.setTop(headerBox);

            // Create tabs for different analytics
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            // Add the inventory analytics tab
            tabPane.getTabs().add(createInventoryTab());

            // Add the sales projection tab
            tabPane.getTabs().add(createSalesProjectionTab());

            // Add the user analytics tab
            tabPane.getTabs().add(createUserAnalyticsTab());

            // Add the outfit trends tab
            tabPane.getTabs().add(createOutfitTrendsTab());

            root.setCenter(tabPane);

            // Add a bottom button bar
            HBox buttonBar = new HBox(10);
            buttonBar.setAlignment(Pos.CENTER_RIGHT);
            buttonBar.setPadding(new Insets(20, 0, 0, 0));

            Button exportBtn = new Button("Export Report");
            exportBtn.getStyleClass().add("action-button");
            exportBtn.setOnAction(e -> {
                try {
                    // Configure file chooser for CSV export
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Export Analytics Report");
                    fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
                    fileChooser.setInitialFileName("fashion_store_analytics_" +
                            java.time.LocalDate.now().toString() + ".csv");

                    // Show save dialog
                    Stage stage = (Stage) analyticsStage.getScene().getWindow();
                    File file = fileChooser.showSaveDialog(stage);

                    if (file != null) {
                        // Generate the report
                        generateAnalyticsReport(file);

                        // Show success message
                        setStatus("Analytics report exported to " + file.getAbsolutePath());
                        SceneManager.showAlert("Export Successful",
                                "Analytics report has been exported to:\n" + file.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    SceneManager.showErrorAlert("Export Error",
                            "Failed to export analytics report: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });

            Button closeBtn = new Button("Close");
            closeBtn.setOnAction(e -> analyticsStage.close());

            buttonBar.getChildren().addAll(exportBtn, closeBtn);
            root.setBottom(buttonBar);

            // Create the scene and apply styles
            Scene scene = new Scene(root);

            // Apply the same theme as the parent window
            if (productTable.getScene().getStylesheets().stream()
                    .anyMatch(s -> s.contains("dark-theme.css"))) {
                scene.getStylesheets().add(
                        getClass().getResource("/styles/dark-theme.css").toExternalForm());
            } else {
                scene.getStylesheets().add(
                        getClass().getResource("/styles/application.css").toExternalForm());
            }

            analyticsStage.setScene(scene);
            analyticsStage.show();

        } catch (Exception e) {
            SceneManager.showErrorAlert("Error", "Failed to open Analytics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates the inventory analytics tab with charts and stats
     */
    private Tab createInventoryTab() {
        Tab tab = new Tab("Inventory");
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Get inventory data
        List<Product> products = dataManager.getAllProducts();
        Map<String, Object> metrics = com.fashionstore.utils.AnalyticsService.getInventoryMetrics(products);
        Map<String, Integer> categoryData = com.fashionstore.utils.AnalyticsService.getCategoryDistribution(products);
        Map<String, Integer> stockLevelData = com.fashionstore.utils.AnalyticsService
                .getStockLevelDistribution(products);
        Map<String, Integer> priceRangeData = com.fashionstore.utils.AnalyticsService
                .getPriceRangeDistribution(products);

        // Create metrics grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(50);
        statsGrid.setVgap(15);
        statsGrid.setPadding(new Insets(20));
        statsGrid.setStyle("-fx-background-color: rgba(50, 50, 50, 0.1); -fx-background-radius: 5;");

        // Add metrics
        addStatisticToGrid(statsGrid, 0, "Total Products:",
                String.valueOf(metrics.get("totalProducts")));
        addStatisticToGrid(statsGrid, 1, "Total Stock:",
                String.valueOf(metrics.get("totalStock")));
        addStatisticToGrid(statsGrid, 2, "Average Price:",
                String.format("$%.2f", metrics.get("averagePrice")));
        addStatisticToGrid(statsGrid, 3, "Inventory Value:",
                String.format("$%.2f", metrics.get("totalValue")));
        addStatisticToGrid(statsGrid, 4, "Low Stock Items:",
                String.valueOf(metrics.get("lowStockCount")));

        // Add metrics to content
        content.getChildren().add(statsGrid);

        // Create category distribution table
        Label categoryChartTitle = new Label("Category Distribution");
        categoryChartTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TableView<Map.Entry<String, Integer>> categoryTable = createDataTable(categoryData, "Category", "Count");
        categoryTable.setMinHeight(300);
        categoryTable.setMaxHeight(300);

        VBox categorySection = new VBox(10, categoryChartTitle, categoryTable);
        content.getChildren().add(categorySection);

        // Create stock level table
        Label stockChartTitle = new Label("Stock Level Distribution");
        stockChartTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TableView<Map.Entry<String, Integer>> stockTable = createDataTable(stockLevelData, "Stock Level", "Count");
        stockTable.setMinHeight(300);
        stockTable.setMaxHeight(300);

        VBox stockSection = new VBox(10, stockChartTitle, stockTable);
        content.getChildren().add(stockSection);

        // Create price range chart
        Label priceChartTitle = new Label("Price Range Distribution");
        priceChartTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        BarChart<String, Number> priceChart = createBarChart(
                priceRangeData,
                "Price Range",
                "Number of Products");
        priceChart.setMinHeight(300);
        priceChart.setMaxHeight(300);

        VBox priceSection = new VBox(10, priceChartTitle, priceChart);
        content.getChildren().add(priceSection);

        scrollPane.setContent(content);
        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Creates a data table from the given data
     */
    private <K, V> TableView<Map.Entry<K, V>> createDataTable(Map<K, V> data, String keyColumnName,
            String valueColumnName) {
        TableView<Map.Entry<K, V>> table = new TableView<>();

        // Create columns
        TableColumn<Map.Entry<K, V>, String> keyColumn = new TableColumn<>(keyColumnName);
        keyColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey().toString()));
        keyColumn.setPrefWidth(200);

        TableColumn<Map.Entry<K, V>, String> valueColumn = new TableColumn<>(valueColumnName);
        valueColumn
                .setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getValue().toString()));
        valueColumn.setPrefWidth(100);

        table.getColumns().addAll(keyColumn, valueColumn);

        // Add data
        ObservableList<Map.Entry<K, V>> items = FXCollections.observableArrayList(data.entrySet());
        table.setItems(items);

        return table;
    }

    /**
     * Creates the sales projection tab with charts
     */
    private Tab createSalesProjectionTab() {
        Tab tab = new Tab("Sales Forecast");
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Get projection data
        Map<String, Double> projectionData = com.fashionstore.utils.AnalyticsService.getRevenueProjection();

        // Create chart title
        Label projectionTitle = new Label("Revenue Forecast (Next 6 Months)");
        projectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Create line chart for projections
        LineChart<String, Number> projectionChart = createLineChart(
                projectionData,
                "Month",
                "Projected Revenue ($)");
        projectionChart.setMinHeight(400);
        projectionChart.setTitle("Monthly Revenue Projection");

        content.getChildren().addAll(projectionTitle, projectionChart);

        // Add recommendations section
        Label recTitle = new Label("Inventory Recommendations");
        recTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        recTitle.setPadding(new Insets(20, 0, 10, 0));

        VBox recommendationsBox = new VBox(10);
        recommendationsBox.setPadding(new Insets(15));
        recommendationsBox.setStyle("-fx-background-color: rgba(50, 50, 50, 0.1); -fx-background-radius: 5;");

        List<Product> lowStockItems = dataManager.getAllProducts().stream()
                .filter(p -> p.getStockQuantity() < 5 && p.getStockQuantity() > 0)
                .limit(5)
                .collect(Collectors.toList());

        Label recLabel = new Label("Based on current inventory levels and projected sales, consider restocking:");
        recLabel.setWrapText(true);

        recommendationsBox.getChildren().add(recLabel);

        if (lowStockItems.isEmpty()) {
            recommendationsBox.getChildren().add(new Label("• No items currently need restocking"));
        } else {
            for (Product product : lowStockItems) {
                Label itemLabel = new Label("• " + product.getName() + " (Current Stock: " +
                        product.getStockQuantity() + ")");
                recommendationsBox.getChildren().add(itemLabel);
            }
        }

        content.getChildren().addAll(recTitle, recommendationsBox);

        tab.setContent(content);
        return tab;
    }

    /**
     * Creates the user analytics tab with charts
     */
    private Tab createUserAnalyticsTab() {
        Tab tab = new Tab("User Analytics");
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Get user data
        List<User> users = dataManager.getAllUsers();
        Map<java.time.LocalDate, Integer> registrationTrend = com.fashionstore.utils.AnalyticsService
                .getUserRegistrationTrend(users, 30);

        // Create user statistics section
        Label userStatsTitle = new Label("User Statistics");
        userStatsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        GridPane userStatsGrid = new GridPane();
        userStatsGrid.setHgap(50);
        userStatsGrid.setVgap(15);
        userStatsGrid.setPadding(new Insets(20));
        userStatsGrid.setStyle("-fx-background-color: rgba(50, 50, 50, 0.1); -fx-background-radius: 5;");

        addStatisticToGrid(userStatsGrid, 0, "Total Users:", String.valueOf(users.size()));

        // Count recent users (last 30 days)
        long recentUsers = users.stream()
                .filter(u -> u.getLastLogin() != null)
                .filter(u -> {
                    Date thirtyDaysAgo = new Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L);
                    return u.getLastLogin().after(thirtyDaysAgo);
                })
                .count();

        addStatisticToGrid(userStatsGrid, 1, "Active Users (Last 30 Days):", String.valueOf(recentUsers));

        // Calculate average wardrobe size
        double avgWardrobeSize = users.stream()
                .mapToDouble(u -> u.getWardrobeItemIds().size())
                .average()
                .orElse(0.0);

        addStatisticToGrid(userStatsGrid, 2, "Average Wardrobe Size:",
                String.format("%.1f items", avgWardrobeSize));

        content.getChildren().addAll(userStatsTitle, userStatsGrid);

        // Create registration trend chart
        Label trendTitle = new Label("User Registration Trend (Last 30 Days)");
        trendTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        trendTitle.setPadding(new Insets(20, 0, 10, 0));

        // We need to convert LocalDate to String for the chart
        Map<String, Number> chartData = new LinkedHashMap<>();
        registrationTrend
                .forEach((date, count) -> chartData.put(date.getMonthValue() + "/" + date.getDayOfMonth(), count));

        BarChart<String, Number> registrationChart = createBarChart(
                chartData,
                "Date",
                "New Users");
        registrationChart.setMinHeight(400);
        registrationChart.setCategoryGap(0);

        content.getChildren().addAll(trendTitle, registrationChart);

        tab.setContent(content);
        return tab;
    }

    /**
     * Creates the outfit trends tab with charts and trending outfits
     */
    private Tab createOutfitTrendsTab() {
        Tab tab = new Tab("Outfit Trends");
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Get outfit data - collect all users' outfits
        List<Outfit> outfits = new ArrayList<>();
        for (User user : dataManager.getAllUsers()) {
            for (String outfitId : user.getOutfitIds()) {
                Outfit outfit = dataManager.getOutfit(outfitId);
                if (outfit != null) {
                    outfits.add(outfit);
                }
            }
        }

        // Create outfit statistics section
        Label outfitStatsTitle = new Label("Outfit Statistics");
        outfitStatsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        GridPane outfitStatsGrid = new GridPane();
        outfitStatsGrid.setHgap(50);
        outfitStatsGrid.setVgap(15);
        outfitStatsGrid.setPadding(new Insets(20));
        outfitStatsGrid.setStyle("-fx-background-color: rgba(50, 50, 50, 0.1); -fx-background-radius: 5;");

        addStatisticToGrid(outfitStatsGrid, 0, "Total Outfits:", String.valueOf(outfits.size()));

        // Count AI-generated outfits
        long aiOutfits = outfits.stream()
                .filter(Outfit::isAiGenerated)
                .count();

        // Avoid division by zero
        double aiPercentage = outfits.isEmpty() ? 0 : (double) aiOutfits / outfits.size() * 100;

        addStatisticToGrid(outfitStatsGrid, 1, "AI-Generated Outfits:",
                String.valueOf(aiOutfits) + " (" + String.format("%.1f%%", aiPercentage) + ")");

        // Calculate average items per outfit
        double avgItems = outfits.stream()
                .mapToDouble(o -> o.getProductIds().size())
                .average()
                .orElse(0.0);

        addStatisticToGrid(outfitStatsGrid, 2, "Average Items per Outfit:",
                String.format("%.1f items", avgItems));

        content.getChildren().addAll(outfitStatsTitle, outfitStatsGrid);

        // Create trending outfits section
        Label trendingTitle = new Label("Top Trending Outfits");
        trendingTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        trendingTitle.setPadding(new Insets(20, 0, 10, 0));

        // Get trending outfits using our service
        List<Map.Entry<Outfit, Integer>> trendingOutfits = com.fashionstore.utils.AnalyticsService
                .getTopTrendingOutfits(outfits, 5);

        VBox trendingOutfitsBox = new VBox(10);
        trendingOutfitsBox.setPadding(new Insets(15));
        trendingOutfitsBox.setStyle("-fx-background-color: rgba(50, 50, 50, 0.1); -fx-background-radius: 5;");

        if (trendingOutfits.isEmpty()) {
            trendingOutfitsBox.getChildren().add(new Label("No outfits available to analyze"));
        } else {
            for (Map.Entry<Outfit, Integer> entry : trendingOutfits) {
                Outfit outfit = entry.getKey();
                Integer score = entry.getValue();

                HBox outfitRow = new HBox(15);
                outfitRow.setAlignment(Pos.CENTER_LEFT);

                Label nameLabel = new Label(outfit.getName());
                nameLabel.setStyle("-fx-font-weight: bold;");

                Label itemsLabel = new Label(outfit.getProductIds().size() + " items");

                Label occasionLabel = new Label("Occasion: " + outfit.getOccasion());

                Label scoreLabel = new Label("Trend Score: " + score);
                scoreLabel.setStyle("-fx-text-fill: #2a5885;");

                outfitRow.getChildren().addAll(nameLabel, itemsLabel, occasionLabel, scoreLabel);
                trendingOutfitsBox.getChildren().add(outfitRow);
            }
        }

        content.getChildren().addAll(trendingTitle, trendingOutfitsBox);

        // Create season popularity table
        Label seasonTitle = new Label("Outfit Season Popularity");
        seasonTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        seasonTitle.setPadding(new Insets(20, 0, 10, 0));

        // Count outfits by season
        Map<String, Integer> seasonData = new LinkedHashMap<>();
        for (Outfit.OutfitSeason season : Outfit.OutfitSeason.values()) {
            seasonData.put(season.name(), 0);
        }

        for (Outfit outfit : outfits) {
            String season = outfit.getSeason().name();
            seasonData.put(season, seasonData.getOrDefault(season, 0) + 1);
        }

        TableView<Map.Entry<String, Integer>> seasonTable = createDataTable(seasonData, "Season", "Count");
        seasonTable.setMinHeight(300);
        seasonTable.setMaxHeight(300);

        content.getChildren().addAll(seasonTitle, seasonTable);

        scrollPane.setContent(content);
        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Creates a pie chart from the given data
     */
    private <K, V extends Number> TableView<Map.Entry<K, V>> createPieChart(Map<K, V> data, String title) {
        // Replace with table view since PieChart is not available
        return createDataTable(data, "Category", "Value");
    }

    /**
     * Creates a bar chart from the given data
     */
    private BarChart<String, Number> createBarChart(
            Map<String, ? extends Number> data,
            String xAxisLabel,
            String yAxisLabel) {

        // Create axes
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xAxisLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);

        // Create the chart
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);

        // Create a data series
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // Add data to series
        for (Map.Entry<String, ? extends Number> entry : data.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        chart.getData().add(series);
        return chart;
    }

    /**
     * Creates a line chart from the given data
     */
    private LineChart<String, Number> createLineChart(
            Map<String, ? extends Number> data,
            String xAxisLabel,
            String yAxisLabel) {

        // Create axes
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xAxisLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);

        // Create the chart
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);

        // Create a data series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Projected Revenue");

        // Add data to series
        for (Map.Entry<String, ? extends Number> entry : data.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        chart.getData().add(series);
        return chart;
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    public void saveProduct(Product product) {
        if (product.getProductId() != null) {
            // If product has an ID, use updateProduct
            System.out.println("Saving updated product: " + product.getProductId());
            dataManager.updateProduct(product);
            setStatus("Product \"" + product.getName() + "\" updated successfully");
        } else {
            // If new product, use addProduct
            System.out.println("Saving new product");
            dataManager.addProduct(product);
            setStatus("New product \"" + product.getName() + "\" added successfully");
        }
        dataManager.saveAllData(); // Explicit save after addition/modification
        refreshProductTable();

        // Also refresh store views
        WindowManager.refreshHomeView();
    }

    public void refreshProductTable() {
        try {
            // Get fresh data from the data manager
            ObservableList<Product> products = FXCollections.observableArrayList(dataManager.getAllProducts());

            // Sort by name for better usability
            products.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

            // Update the table
            productTable.setItems(products);
            productTable.refresh();

            System.out.println("Product table refreshed with " + products.size() + " products");
        } catch (Exception e) {
            System.err.println("Error refreshing product table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates and exports an analytics report to a CSV file
     * 
     * @param file The file to export to
     */
    private void generateAnalyticsReport(File file) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
            // Write CSV header
            writer.println("Fashion Store Analytics Report," + java.time.LocalDateTime.now());
            writer.println();

            // Product statistics
            List<Product> products = dataManager.getAllProducts();
            writer.println("PRODUCT STATISTICS");
            writer.println("Total Products," + products.size());

            int totalStock = products.stream().mapToInt(Product::getStockQuantity).sum();
            writer.println("Total Stock," + totalStock);

            double totalValue = products.stream()
                    .mapToDouble(p -> p.getPrice().doubleValue() * p.getStockQuantity())
                    .sum();
            writer.println("Inventory Value,$" + String.format("%.2f", totalValue));

            long lowStockCount = products.stream().filter(p -> p.getStockQuantity() < 5).count();
            writer.println("Low Stock Items," + lowStockCount);

            writer.println("Most Popular Category," + getMostPopularCategory(products));
            writer.println();

            // Category breakdown
            writer.println("CATEGORY BREAKDOWN");
            Map<String, Long> categoryCounts = products.stream()
                    .filter(p -> p.getCategory() != null && !p.getCategory().isEmpty())
                    .collect(java.util.stream.Collectors.groupingBy(
                            Product::getCategory, java.util.stream.Collectors.counting()));

            writer.println("Category,Product Count,Percentage");
            for (Map.Entry<String, Long> entry : categoryCounts.entrySet()) {
                double percentage = (double) entry.getValue() / products.size() * 100;
                writer.println(entry.getKey() + "," + entry.getValue() + "," +
                        String.format("%.1f%%", percentage));
            }
            writer.println();

            // User statistics
            List<User> users = dataManager.getAllUsers();
            writer.println("USER STATISTICS");
            writer.println("Total Users," + users.size());

            // Average wardrobe size
            double avgWardrobeSize = users.stream()
                    .mapToDouble(u -> u.getWardrobeItemIds().size())
                    .average()
                    .orElse(0);
            writer.println("Average Wardrobe Size," + String.format("%.1f", avgWardrobeSize));

            // Average outfits per user
            double avgOutfitsPerUser = users.stream()
                    .mapToDouble(u -> u.getOutfitIds().size())
                    .average()
                    .orElse(0);
            writer.println("Average Outfits Per User," + String.format("%.1f", avgOutfitsPerUser));
            writer.println();

            // Detailed product listing
            writer.println("PRODUCT INVENTORY DETAILS");
            writer.println("Product ID,Name,Category,Price,Stock,Value");
            for (Product product : products) {
                double productValue = product.getPrice().doubleValue() * product.getStockQuantity();
                writer.println(
                        product.getProductId() + "," +
                                escapeCSV(product.getName()) + "," +
                                escapeCSV(product.getCategory()) + "," +
                                "$" + product.getPrice() + "," +
                                product.getStockQuantity() + "," +
                                "$" + String.format("%.2f", productValue));
            }
        } catch (java.io.FileNotFoundException e) {
            throw new RuntimeException("Failed to create report file: " + e.getMessage(), e);
        }
    }

    /**
     * Escapes a string for CSV format (adds quotes if needed and escapes quotes)
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            // Replace quotes with double quotes
            value = value.replace("\"", "\"\"");
            // Wrap in quotes
            return "\"" + value + "\"";
        }

        return value;
    }

    // Helper method to find the most popular category
    private String getMostPopularCategory(List<Product> products) {
        Map<String, Long> categoryCounts = products.stream()
                .filter(p -> p.getCategory() != null && !p.getCategory().isEmpty())
                .collect(Collectors.groupingBy(
                        Product::getCategory, Collectors.counting()));

        if (categoryCounts.isEmpty()) {
            return "N/A";
        }

        return categoryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    // Helper method to add statistics to a grid
    private void addStatisticToGrid(GridPane grid, int row, String label, String value) {
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label valueLabel = new Label(value);

        grid.add(nameLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }
}
