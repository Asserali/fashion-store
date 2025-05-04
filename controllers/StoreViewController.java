package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.Product;
import com.fashionstore.storage.DataManager;
import com.fashionstore.ui.components.StoreItemView;
import com.fashionstore.utils.SceneManager;
import com.fashionstore.utils.WindowManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class StoreViewController implements Initializable {

    @FXML
    private FlowPane storeItemsPane;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private Label itemCountLabel; // Optional - can add this to your FXML if desired

    private DataManager dataManager;
    private List<Product> storeItems;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = FashionStoreApp.getDataManager();

        if (dataManager.getCurrentUser() == null) {
            SceneManager.loadScene("LoginView.fxml");
            return;
        }

        // Force reload all data to ensure we have the latest
        dataManager.loadAllData();

        // Set ID and userData for window refresh support
        javafx.application.Platform.runLater(() -> {
            if (storeItemsPane.getScene() != null) {
                storeItemsPane.getScene().getRoot().setId("storeView");
                storeItemsPane.getScene().getRoot().setUserData(this);
            }
        });

        // Load store items (only visible products)
        storeItems = dataManager.getVisibleProducts();
        System.out.println("StoreViewController initialized with " + storeItems.size() + " products");

        // Set up filters
        setupFilters();

        // Display items
        displayStoreItems();
    }

    private void setupFilters() {
        // Add categories
        categoryFilter.getItems().add("All Categories");

        // Get unique categories
        storeItems.stream()
                .map(Product::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .sorted() // Sort categories alphabetically
                .forEach(category -> categoryFilter.getItems().add(category));

        categoryFilter.setValue("All Categories");
        categoryFilter.setOnAction(e -> displayStoreItems());
    }

    private void displayStoreItems() {
        storeItemsPane.getChildren().clear();

        String category = categoryFilter.getValue();

        // Start with already-filtered visible products
        List<Product> filteredItems = storeItems;

        // Then apply category filter if needed
        if (category != null && !category.equals("All Categories")) {
            filteredItems = filteredItems.stream()
                    .filter(item -> category.equals(item.getCategory()))
                    .collect(Collectors.toList());
        }

        for (Product item : filteredItems) {
            StoreItemView itemView = new StoreItemView(item);

            // Handle out-of-stock products
            if (item.getStockQuantity() <= 0) {
                itemView.markAsOutOfStock();
            }

            itemView.setOnPurchase(e -> handlePurchase(item));
            storeItemsPane.getChildren().add(itemView);
        }

        // Update item count if label exists
        if (itemCountLabel != null) {
            itemCountLabel.setText("Showing " + filteredItems.size() + " items");
        }
    }

    private void handlePurchase(Product product) {
        // Check if item is already in wardrobe
        List<Product> wardrobe = dataManager.getUserWardrobe(dataManager.getCurrentUser().getUserId());
        boolean alreadyOwned = wardrobe.stream()
                .anyMatch(p -> p.getProductId().equals(product.getProductId()));

        if (alreadyOwned) {
            SceneManager.showAlert("Already Owned",
                    "You already have this item in your wardrobe.");
            return;
        }

        // Check if item is in stock
        if (product.getStockQuantity() <= 0) {
            SceneManager.showAlert("Out of Stock",
                    "Sorry, this item is currently out of stock.");
            return;
        }

        // Make sure the product has an image path before adding to wardrobe
        if (product.getImagePath() == null || product.getImagePath().isEmpty()) {
            product.setImagePath("/images/default-product.jpg");
        }

        // Decrease stock quantity by 1
        product.setStockQuantity(product.getStockQuantity() - 1);

        // Update product in database to reflect stock change
        dataManager.updateProduct(product);

        // Add to user's wardrobe
        dataManager.getCurrentUser().addToWardrobe(product.getProductId());

        // Important: Save data after modifications
        dataManager.saveAllData();

        SceneManager.showAlert("Purchase Successful",
                "Item added to your wardrobe: " + product.getName());

        // Refresh the current view
        refreshView();

        // Refresh other open views
        WindowManager.refreshHomeView();
    }

    // Refresh the view (can be called from outside)
    public void refreshView() {
        System.out.println("StoreViewController: Refreshing view");
        // Force reload all data from the database
        dataManager.loadAllData();
        // Get fresh product list (only visible)
        storeItems = dataManager.getVisibleProducts();
        System.out.println("StoreViewController: Refreshed with " + storeItems.size() + " products");
        setupFilters();
        displayStoreItems();
    }
}