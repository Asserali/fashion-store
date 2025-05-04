package com.fashionstore.controllers;

import com.fashionstore.ai.OutfitRecommender;
import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.Outfit;
import com.fashionstore.models.Product;
import com.fashionstore.storage.DataManager;
import com.fashionstore.ui.components.BodyCanvas;
import com.fashionstore.ui.components.ClothingItemView;
import com.fashionstore.utils.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OutfitCreatorController implements Initializable {

    @FXML
    private BorderPane rootPane;
    @FXML
    private FlowPane wardrobeItemsPane;
    @FXML
    private FlowPane outfitPreviewPane;
    @FXML
    private TextField outfitNameField;
    @FXML
    private BodyCanvas bodyCanvas;
    @FXML
    private Button saveButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button closeButton;
    @FXML
    private Button generateSuggestionsButton;
    @FXML
    private Label styleRatingLabel;
    @FXML
    private Label filterResultsLabel;
    @FXML
    private VBox aiSuggestionsPane;

    // Category filter buttons
    @FXML
    private Button allCategoryButton;
    @FXML
    private Button topsButton;
    @FXML
    private Button bottomsButton;
    @FXML
    private Button shoesButton;
    @FXML
    private Button accessoriesButton;

    private DataManager dataManager;
    private List<Product> wardrobeItems = new ArrayList<>();
    private String currentCategory = "all"; // Track current category filter

    // AI components
    private OutfitRecommender recommender;
    private final DecimalFormat ratingFormat = new DecimalFormat("#.#");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = FashionStoreApp.getDataManager();

        if (dataManager.getCurrentUser() == null) {
            SceneManager.showErrorAlert("Error", "You must be logged in to create outfits");
            return;
        }

        // Initialize AI recommender
        recommender = new OutfitRecommender();

        // Load wardrobe items
        loadWardrobeItems();

        // Initialize with default "all" category
        filterCategory("all");

        // Add debug information
        System.out.println("OutfitCreatorController initialized");
        System.out.println("Loaded " + wardrobeItems.size() + " wardrobe items");
    }

    /**
     * Handles category filtering
     */
    @FXML
    private void filterCategory() {
        // Get the button that was clicked
        Button clicked = (Button) rootPane.getScene().getFocusOwner();
        String category = (String) clicked.getUserData();
        filterCategory(category);
    }

    /**
     * Filters wardrobe items by category
     */
    private void filterCategory(String category) {
        // Update current category
        this.currentCategory = category;

        // Update button styling
        updateCategoryButtons();

        // Clear and repopulate wardrobe items pane
        wardrobeItemsPane.getChildren().clear();

        // Filter items by category
        List<Product> filteredItems;
        if ("all".equals(category)) {
            filteredItems = wardrobeItems;
            filterResultsLabel.setText("Showing all items (" + filteredItems.size() + ")");
        } else {
            filteredItems = wardrobeItems.stream()
                    .filter(p -> categoryMatches(p, category))
                    .collect(Collectors.toList());
            filterResultsLabel.setText("Showing " + category + " (" + filteredItems.size() + ")");
        }

        // Create UI components for filtered items
        for (Product item : filteredItems) {
            ClothingItemView itemView = new ClothingItemView(item);
            itemView.setOnMouseClicked(e -> toggleItemSelection(itemView, e));
            wardrobeItemsPane.getChildren().add(itemView);
        }
    }

    /**
     * Checks if a product matches a category
     */
    private boolean categoryMatches(Product product, String category) {
        String productCategory = product.getCategory().toLowerCase();

        switch (category) {
            case "tops":
                return productCategory.contains("top") ||
                        productCategory.contains("shirt") ||
                        productCategory.contains("jacket") ||
                        productCategory.contains("sweater");
            case "bottoms":
                return productCategory.contains("bottom") ||
                        productCategory.contains("pant") ||
                        productCategory.contains("jean") ||
                        productCategory.contains("skirt") ||
                        productCategory.contains("short");
            case "shoes":
                return productCategory.contains("shoe") ||
                        productCategory.contains("boot") ||
                        productCategory.contains("sneaker") ||
                        productCategory.contains("sandal");
            case "accessories":
                return productCategory.contains("accessory") ||
                        productCategory.contains("accessories") ||
                        productCategory.contains("hat") ||
                        productCategory.contains("jewelry") ||
                        productCategory.contains("watch") ||
                        productCategory.contains("belt") ||
                        productCategory.contains("bag") ||
                        productCategory.contains("scarf");
            default:
                return true;
        }
    }

    /**
     * Updates the category button styling
     */
    private void updateCategoryButtons() {
        // Remove selected class from all buttons
        allCategoryButton.getStyleClass().remove("category-selected");
        topsButton.getStyleClass().remove("category-selected");
        bottomsButton.getStyleClass().remove("category-selected");
        shoesButton.getStyleClass().remove("category-selected");
        accessoriesButton.getStyleClass().remove("category-selected");

        // Add selected class to current category button
        switch (currentCategory) {
            case "all":
                allCategoryButton.getStyleClass().add("category-selected");
                break;
            case "tops":
                topsButton.getStyleClass().add("category-selected");
                break;
            case "bottoms":
                bottomsButton.getStyleClass().add("category-selected");
                break;
            case "shoes":
                shoesButton.getStyleClass().add("category-selected");
                break;
            case "accessories":
                accessoriesButton.getStyleClass().add("category-selected");
                break;
        }
    }

    private void loadWardrobeItems() {
        // Get user's wardrobe items
        wardrobeItems = dataManager.getUserWardrobe(dataManager.getCurrentUser().getUserId());
        System.out.println("Loaded " + wardrobeItems.size() + " wardrobe items");
    }

    /**
     * Toggles selection of an item and updates the outfit
     */
    private void toggleItemSelection(ClothingItemView itemView, MouseEvent event) {
        // Toggle selection
        itemView.setSelected(!itemView.isSelected());

        Product product = itemView.getProduct();

        // Try to place it on the body canvas if selected
        if (itemView.isSelected()) {
            boolean placed = bodyCanvas.placeProductAutomatically(product);
            if (!placed) {
                System.out.println("Could not place product automatically: " + product.getName());
            }
        }

        // Update preview and UI state
        updateOutfitPreview();
        calculateStyleRating();

        // Log debug info
        Map<String, Product> placedProducts = bodyCanvas.getPlacedProducts();
        System.out.println("Current outfit has " + placedProducts.size() + " items");
    }

    /**
     * Updates the outfit preview pane with currently selected items
     */
    private void updateOutfitPreview() {
        outfitPreviewPane.getChildren().clear();

        // Get all products from the body canvas
        Map<String, Product> placedProducts = bodyCanvas.getPlacedProducts();

        // Create compact view for each product
        for (Product product : placedProducts.values()) {
            ClothingItemView previewItem = new ClothingItemView(product);
            previewItem.setDraggable(false); // Disable dragging in preview
            previewItem.setMaxWidth(120);
            previewItem.setOnMouseClicked(e -> {
                // Find matching item in wardrobe and deselect it
                for (Node node : wardrobeItemsPane.getChildren()) {
                    if (node instanceof ClothingItemView) {
                        ClothingItemView wardrobeItem = (ClothingItemView) node;
                        if (wardrobeItem.getProduct().getProductId().equals(product.getProductId())) {
                            wardrobeItem.setSelected(false);
                            break;
                        }
                    }
                }

                // Remove from body canvas
                bodyCanvas.clearAll();

                // Refresh
                updateOutfitPreview();
                calculateStyleRating();
            });
            outfitPreviewPane.getChildren().add(previewItem);
        }
    }

    /**
     * Calculates and displays the style rating for the current outfit
     */
    private void calculateStyleRating() {
        // Get all products from the body canvas
        Map<String, Product> placedProducts = bodyCanvas.getPlacedProducts();

        if (placedProducts.isEmpty()) {
            styleRatingLabel.setText("Style Rating: -");
            return;
        }

        // Create a temporary outfit for rating
        Outfit tempOutfit = new Outfit(dataManager.getCurrentUser().getUserId(), "Temp");
        for (Product product : placedProducts.values()) {
            tempOutfit.addProduct(product.getProductId());
        }

        // Calculate rating
        List<Product> allProducts = new ArrayList<>(placedProducts.values());
        double rating = recommender.calculateOutfitRating(tempOutfit, allProducts, dataManager.getCurrentUser());

        // Update rating display
        styleRatingLabel.setText("Style Rating: " + ratingFormat.format(rating) + "/5.0");
    }

    /**
     * Generates an AI outfit suggestion
     */
    @FXML
    private void generateAiOutfit() {
        System.out.println("Generating AI outfit suggestion");

        // Clear existing outfit
        clearOutfit();

        // Generate outfit recommendation
        List<Outfit> recommendations = recommender.generateRecommendations(
                dataManager.getCurrentUser(),
                wardrobeItems,
                1);

        if (recommendations.isEmpty()) {
            SceneManager.showAlert("No Recommendations",
                    "Could not generate outfit recommendations. Please add more items to your wardrobe.");
            return;
        }

        // Get the first recommendation
        Outfit recommendation = recommendations.get(0);

        // Set outfit name
        outfitNameField.setText(recommendation.getName());

        // Add items to body canvas
        for (String productId : recommendation.getProductIds()) {
            Product product = findProductById(productId);
            if (product != null) {
                // Place on body canvas
                bodyCanvas.placeProductAutomatically(product);

                // Select in wardrobe view
                for (Node node : wardrobeItemsPane.getChildren()) {
                    if (node instanceof ClothingItemView) {
                        ClothingItemView itemView = (ClothingItemView) node;
                        if (itemView.getProduct().getProductId().equals(productId)) {
                            itemView.setSelected(true);
                            break;
                        }
                    }
                }
            }
        }

        // Update the preview
        updateOutfitPreview();
        calculateStyleRating();
    }

    /**
     * Finds a product by ID
     */
    private Product findProductById(String productId) {
        for (Product product : wardrobeItems) {
            if (product.getProductId().equals(productId)) {
                return product;
            }
        }
        return null;
    }

    /**
     * Initializes this controller for editing an existing outfit
     */
    public void initForEdit(Outfit outfitToEdit) {
        // Store the outfit being edited
        Outfit existingOutfit = outfitToEdit;

        // Set the name
        outfitNameField.setText(existingOutfit.getName());

        // Clear any current outfit
        clearOutfit();

        // Add all products from the existing outfit
        if (existingOutfit.getProductIds() != null && !existingOutfit.getProductIds().isEmpty()) {
            for (String productId : existingOutfit.getProductIds()) {
                Product product = findProductById(productId);
                if (product != null) {
                    // Place product on body canvas
                    bodyCanvas.placeProductAutomatically(product);

                    // Select in wardrobe view
                    for (Node node : wardrobeItemsPane.getChildren()) {
                        if (node instanceof ClothingItemView) {
                            ClothingItemView itemView = (ClothingItemView) node;
                            if (itemView.getProduct().getProductId().equals(productId)) {
                                itemView.setSelected(true);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Update the preview and rating
        updateOutfitPreview();
        calculateStyleRating();

        // Update the save button to indicate editing mode
        saveButton.setText("Update Outfit");
    }

    // Override saveOutfit method to handle edit mode
    @FXML
    private void saveOutfit() {
        String outfitName = outfitNameField.getText().trim();

        if (outfitName.isEmpty()) {
            SceneManager.showErrorAlert("Missing Information", "Please provide a name for your outfit.");
            return;
        }

        // Get products from body canvas
        Map<String, Product> placedProducts = bodyCanvas.getPlacedProducts();

        if (placedProducts.isEmpty()) {
            SceneManager.showErrorAlert("Empty Outfit", "Please add at least one item to your outfit.");
            return;
        }

        // Create or update outfit
        Outfit outfit;
        boolean isEditMode = saveButton.getText().equals("Update Outfit");

        if (isEditMode) {
            // Get the existing outfit from the stage's user data
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Object userData = stage.getUserData();

            if (userData instanceof Outfit) {
                outfit = (Outfit) userData;
                outfit.setName(outfitName);
                outfit.clearProducts(); // Remove existing products
            } else {
                // If we can't find existing outfit, create new
                outfit = new Outfit(dataManager.getCurrentUser().getUserId(), outfitName);
            }
        } else {
            // Create new outfit
            outfit = new Outfit(dataManager.getCurrentUser().getUserId(), outfitName);
        }

        // Add each product
        for (Product product : placedProducts.values()) {
            outfit.addProduct(product.getProductId());
        }

        // Save the outfit
        dataManager.addOutfit(outfit);
        dataManager.getCurrentUser().addOutfit(outfit.getOutfitId());
        dataManager.saveAllData();

        // Show confirmation and clear
        if (isEditMode) {
            SceneManager.showAlert("Success", "Your outfit has been updated!");
        } else {
            SceneManager.showAlert("Success", "Your outfit has been saved!");
        }

        clearOutfit();

        // Close the window and send back success callback if in edit mode
        Stage stage = (Stage) closeButton.getScene().getWindow();
        if (isEditMode) {
            // Get the callback from scene's userData
            Scene scene = stage.getScene();
            if (scene != null && scene.getUserData() instanceof java.util.function.Consumer) {
                @SuppressWarnings("unchecked")
                java.util.function.Consumer<Boolean> callback = (java.util.function.Consumer<Boolean>) scene
                        .getUserData();
                // Call the callback with success = true
                callback.accept(true);
            }
        }

        stage.close();

        // Refresh the outfits display if possible
        if (stage.getOwner() != null) {
            Object controller = stage.getOwner().getUserData();
            if (controller instanceof MyOutfitsController) {
                ((MyOutfitsController) controller).loadOutfits();
            }
        }
    }

    @FXML
    private void clearOutfit() {
        // Clear body canvas
        bodyCanvas.clearAll();

        // Clear preview pane
        outfitPreviewPane.getChildren().clear();

        // Clear name field
        outfitNameField.clear();

        // Clear style rating
        styleRatingLabel.setText("Style Rating: -");

        // Deselect all items in the wardrobe pane
        for (Node node : wardrobeItemsPane.getChildren()) {
            if (node instanceof ClothingItemView) {
                ((ClothingItemView) node).setSelected(false);
            }
        }
    }

    @FXML
    private void closeWindow() {
        // Get the window that contains this controller
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Refreshes the view - can be called from outside
     */
    public void refreshView() {
        // Reload wardrobe items
        loadWardrobeItems();

        // Refresh the current category
        filterCategory(currentCategory);
    }
}