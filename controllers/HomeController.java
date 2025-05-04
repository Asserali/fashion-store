package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.Product;
import com.fashionstore.models.ShoppingCart;
import com.fashionstore.models.User;
import com.fashionstore.storage.DataManager;
import com.fashionstore.ui.components.StoreItemView;
import com.fashionstore.utils.SceneManager;
import com.fashionstore.utils.WindowManager;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HomeController implements Initializable {
    @FXML
    private Button cartButton;
    @FXML
    private Label userLabel;
    @FXML
    private Button wardrobeButton;
    @FXML
    private Button outfitsButton;
    @FXML
    private Button aiButton;
    @FXML
    private Button logoutButton;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ComboBox<String> priceFilter;
    @FXML
    private Button searchButton;
    @FXML
    private FlowPane storeItemsPane;
    @FXML
    private Label itemCountLabel;

    private DataManager dataManager;
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = FashionStoreApp.getDataManager();
        currentUser = dataManager.getCurrentUser();

        Platform.runLater(() -> {
            if (storeItemsPane.getScene() != null) {
                storeItemsPane.getScene().getRoot().setId("homeView");
                storeItemsPane.getScene().getRoot().setUserData(this);
            }
        });

        if (currentUser == null) {
            SceneManager.loadScene("LoginView.fxml");
            return;
        }

        userLabel.setText("Welcome, " + currentUser.getUsername());

        // Get only visible products directly
        List<Product> storeItems = dataManager.getVisibleProducts();
        if (storeItems.isEmpty()) {
            storeItems = new ArrayList<>();
        }

        setupFilters(storeItems);
        displayStoreItems(storeItems);

        categoryFilter.setOnAction(e -> handleSearch());
        priceFilter.setOnAction(e -> handleSearch());

        Platform.runLater(this::maximizeWindow);
        Platform.runLater(this::setupResponsiveLayout);
        optimizeLayoutForMaximizedWindow();
    }

    private void setupResponsiveLayout() {
        storeItemsPane.prefWrapLengthProperty().bind(
                storeItemsPane.widthProperty());

        storeItemsPane.getScene().widthProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.doubleValue();
            if (width > 1600) {
                storeItemsPane.setHgap(25);
                storeItemsPane.setPrefWidth(width - 40);
            } else {
                storeItemsPane.setHgap(20);
            }
        });
    }

    private void maximizeWindow() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setMaximized(true);
            storeItemsPane.getParent().getStyleClass().add("page-transition");
        } catch (Exception e) {
            System.err.println("Failed to maximize window: " + e.getMessage());
        }
    }

    private void setupFilters(List<Product> storeItems) {
        if (categoryFilter.getItems() == null || categoryFilter.getItems().isEmpty()) {
            categoryFilter.getItems().add("All Categories");
            categoryFilter.setValue("All Categories");
        }

        if (priceFilter.getItems() == null || priceFilter.getItems().isEmpty()) {
            priceFilter.getItems().addAll(
                    "All Prices",
                    "Under $50",
                    "$50 - $100",
                    "$100 - $200",
                    "Over $200");
            priceFilter.setValue("All Prices");
        }

        for (Product item : storeItems) {
            if (item.getCategory() != null && !item.getCategory().isEmpty() &&
                    !categoryFilter.getItems().contains(item.getCategory())) {
                categoryFilter.getItems().add(item.getCategory());
            }
        }
    }

    private void displayStoreItems(List<Product> items) {
        storeItemsPane.getChildren().clear();
        itemCountLabel.setText("Showing " + items.size() + " items");

        // Prepare all item views
        List<StoreItemView> itemViews = new ArrayList<>();
        for (Product item : items) {
            StoreItemView itemView = new StoreItemView(item);
            itemView.setOnPurchase(e -> handlePurchase(item));
            itemView.setOpacity(0);
            itemViews.add(itemView);
        }

        // Add all items to the flow pane first (invisible)
        storeItemsPane.getChildren().addAll(itemViews);

        // Create timeline for staggered animation
        Timeline timeline = new Timeline();

        // Add keyframes for each item with staggered delays
        for (int i = 0; i < itemViews.size(); i++) {
            StoreItemView itemView = itemViews.get(i);

            // Calculate delay based on position (capped at reasonable max)
            double delayMs = Math.min(i, 20) * 40;

            // Add keyframe for fade-in and slight upward movement
            KeyFrame kf = new KeyFrame(
                    Duration.millis(delayMs),
                    e -> {
                        // Add animation class that includes transform and other effects
                        itemView.getStyleClass().add("item-fade-in");
                        // Animate opacity directly
                        Timeline itemTimeline = new Timeline(
                                new KeyFrame(Duration.ZERO, new KeyValue(itemView.opacityProperty(), 0)),
                                new KeyFrame(Duration.millis(400), new KeyValue(itemView.opacityProperty(), 1)));
                        itemTimeline.play();
                    });

            timeline.getKeyFrames().add(kf);
        }

        // Play the timeline
        timeline.play();
    }

    private void optimizeLayoutForMaximizedWindow() {
        storeItemsPane.getStyleClass().add("flow-pane-maximized");
        storeItemsPane.prefWrapLengthProperty().bind(
                storeItemsPane.widthProperty());

        storeItemsPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.doubleValue();
            if (width > 1600) {
                storeItemsPane.setHgap(30);
                storeItemsPane.setVgap(40);
            } else if (width > 1200) {
                storeItemsPane.setHgap(25);
                storeItemsPane.setVgap(30);
            } else {
                storeItemsPane.setHgap(20);
                storeItemsPane.setVgap(20);
            }
        });
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();
        String category = categoryFilter.getValue();
        String priceRange = priceFilter.getValue();

        // Start with only visible products
        List<Product> allItems = dataManager.getVisibleProducts();
        List<Product> filteredItems = new ArrayList<>(allItems);

        if (searchText != null && !searchText.isEmpty()) {
            filteredItems = filteredItems.stream()
                    .filter(item -> (item.getName() != null && item.getName().toLowerCase().contains(searchText)) ||
                            (item.getBrand() != null && item.getBrand().toLowerCase().contains(searchText)) ||
                            (item.getDescription() != null && item.getDescription().toLowerCase().contains(searchText)))
                    .collect(Collectors.toList());
        }

        if (category != null && !category.equals("All Categories")) {
            filteredItems = filteredItems.stream()
                    .filter(item -> category.equals(item.getCategory()))
                    .collect(Collectors.toList());
        }

        if (priceRange != null && !priceRange.equals("All Prices")) {
            filteredItems = filteredItems.stream()
                    .filter(item -> {
                        BigDecimal price = item.getPrice();
                        BigDecimal fifty = new BigDecimal("50.0");
                        BigDecimal hundred = new BigDecimal("100.0");
                        BigDecimal twoHundred = new BigDecimal("200.0");

                        switch (priceRange) {
                            case "Under $50":
                                return price.compareTo(fifty) < 0;
                            case "$50 - $100":
                                return price.compareTo(fifty) >= 0 && price.compareTo(hundred) <= 0;
                            case "$100 - $200":
                                return price.compareTo(hundred) > 0 && price.compareTo(twoHundred) <= 0;
                            case "Over $200":
                                return price.compareTo(twoHundred) > 0;
                            default:
                                return true;
                        }
                    })
                    .collect(Collectors.toList());
        }

        displayStoreItems(filteredItems);
    }

    private void handlePurchase(Product product) {
        // Check if item is in stock
        if (product.getStockQuantity() <= 0) {
            SceneManager.showAlert("Out of Stock",
                    "Sorry, this item is currently out of stock.");
            return;
        }

        // Make sure the product has an image path before adding to cart
        if (product.getImagePath() == null || product.getImagePath().isEmpty()) {
            product.setImagePath("/images/default-product.jpg");
            // Update product in database to ensure image path is saved
            dataManager.updateProduct(product);
        }

        ShoppingCart cart = dataManager.getCart(currentUser.getUserId());
        cart.addItem(product);
        dataManager.saveCart(cart);
        dataManager.saveAllData();

        SceneManager.showAlert("Added to Cart",
                product.getName() + " has been added to your shopping cart.");

        // Refresh all open views
        WindowManager.refreshHomeView();
    }

    @FXML
    private void openWardrobe() {
        WindowManager.openWardrobeWindow();
    }

    @FXML
    private void openOutfits() {
        try {
            if (currentUser == null) {
                SceneManager.showAlert("Login Required", "Please login to view your outfits.");
                return;
            }

            if (dataManager.getUserOutfits(currentUser.getUserId()).isEmpty()) {
                SceneManager.showAlert("No Outfits", "You don't have any saved outfits yet.");
                return;
            }

            WindowManager.openOutfitsWindow();
        } catch (Exception e) {
            SceneManager.showErrorAlert("Error", "Failed to open outfits: " + e.getMessage());
        }
    }

    @FXML
    private void openAIStylist() {
        try {
            WindowManager.openAIStylistWindow();
        } catch (Exception e) {
            SceneManager.showAlert("Not Implemented", "AI Stylist feature coming soon!");
        }
    }

    @FXML
    private void handleLogout() {
        dataManager.setCurrentUser(null);
        SceneManager.loadScene("LoginView.fxml");
    }

    @FXML
    private void openCart() {
        WindowManager.openCartWindow();
    }

    @FXML
    public void refreshView() {
        // Reload products
        List<Product> storeItems = dataManager.getVisibleProducts();
        setupFilters(storeItems);
        displayStoreItems(storeItems);
    }

    /**
     * Sets the application to dark mode
     */
    @FXML
    public void setDarkMode() {
        try {
            if (storeItemsPane.getScene() != null) {
                // Remove light theme if present
                storeItemsPane.getScene().getStylesheets().removeIf(
                        style -> style.contains("application.css"));

                // Add dark theme stylesheet
                String darkThemePath = getClass().getResource("/styles/dark-theme.css").toExternalForm();
                if (!storeItemsPane.getScene().getStylesheets().contains(darkThemePath)) {
                    storeItemsPane.getScene().getStylesheets().add(darkThemePath);
                }

                // Store theme preference (this could be saved to user preferences)
                storeItemsPane.getScene().getRoot().getProperties().put("theme", "dark");

                SceneManager.showAlert("Theme Changed", "Dark mode has been activated.");
            }
        } catch (Exception e) {
            SceneManager.showErrorAlert("Theme Error", "Failed to apply dark theme: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets the application to light mode
     */
    @FXML
    public void setLightMode() {
        try {
            if (storeItemsPane.getScene() != null) {
                // Remove dark theme if present
                storeItemsPane.getScene().getStylesheets().removeIf(
                        style -> style.contains("dark-theme.css"));

                // Add light theme stylesheet
                String lightThemePath = getClass().getResource("/styles/application.css").toExternalForm();
                if (!storeItemsPane.getScene().getStylesheets().contains(lightThemePath)) {
                    storeItemsPane.getScene().getStylesheets().add(lightThemePath);
                }

                // Store theme preference (this could be saved to user preferences)
                storeItemsPane.getScene().getRoot().getProperties().put("theme", "light");

                SceneManager.showAlert("Theme Changed", "Light mode has been activated.");
            }
        } catch (Exception e) {
            SceneManager.showErrorAlert("Theme Error", "Failed to apply light theme: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens user account settings
     */
    @FXML
    public void openUserAccount() {
        SceneManager.loadScene("UserProfileView.fxml");
    }

    /**
     * Handles exit menu option
     */
    @FXML
    public void handleExit() {
        boolean confirm = SceneManager.showConfirmationDialog(
                "Exit Application",
                "Are you sure you want to exit?",
                "Any unsaved changes will be lost.");

        if (confirm) {
            Stage stage = (Stage) storeItemsPane.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Opens application preferences
     */
    @FXML
    public void openPreferences() {
        SceneManager.showAlert("Preferences", "Application preferences will be implemented in a future update.");
    }

    /**
     * Opens notification settings
     */
    @FXML
    public void openNotificationSettings() {
        SceneManager.showAlert("Notification Settings",
                "Notification settings will be implemented in a future update.");
    }

    /**
     * Opens about dialog
     */
    @FXML
    public void openAbout() {
        SceneManager.showAlert("About Fashion Store",
                "Fashion Store Application\nVersion 1.0\n" +
                        "A virtual fashion store experience allowing users to create and manage outfits.");
    }

    /**
     * Opens help dialog
     */
    @FXML
    public void openHelp() {
        SceneManager.showAlert("Help",
                "Fashion Store Help:\n\n" +
                        "- Browse products in the main store view\n" +
                        "- Add items to your wardrobe\n" +
                        "- Create outfits from your wardrobe items\n" +
                        "- Use the AI Stylist for outfit recommendations\n\n" +
                        "For more help, please refer to the user manual.");
    }
}