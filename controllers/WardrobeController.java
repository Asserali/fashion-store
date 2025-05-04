package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.Product;
import com.fashionstore.storage.DataManager;
import com.fashionstore.ui.components.ClothingItemView;
import com.fashionstore.utils.WindowManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class WardrobeController implements Initializable {

    @FXML
    private FlowPane wardrobeItemsPane;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ComboBox<String> colorFilter;
    @FXML
    private ComboBox<String> seasonFilter;
    @FXML
    private Label totalItemsLabel;
    @FXML
    private Button createOutfitButton;

    private DataManager dataManager;
    private List<Product> wardrobeItems;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = FashionStoreApp.getDataManager();

        if (dataManager.getCurrentUser() == null) {
            return;
        }

        // Load wardrobe items
        wardrobeItems = dataManager.getUserWardrobe(dataManager.getCurrentUser().getUserId());

        // Setup filter dropdowns
        setupFilters();

        // Display items
        displayWardrobeItems();
    }

    private void setupFilters() {
        // Extract unique categories
        Set<String> categories = new HashSet<>();
        Set<String> colors = new HashSet<>();
        Set<String> seasons = new HashSet<>();

        for (Product item : wardrobeItems) {
            if (item.getCategory() != null && !item.getCategory().isEmpty()) {
                categories.add(item.getCategory());
            }
            if (item.getColor() != null && !item.getColor().isEmpty()) {
                colors.add(item.getColor());
            }
            if (item.getSeason() != null && !item.getSeason().isEmpty()) {
                seasons.add(item.getSeason());
            }
        }

        // Add options to comboboxes
        categoryFilter.getItems().add("All Categories");
        categoryFilter.getItems().addAll(categories);
        categoryFilter.setValue("All Categories");

        colorFilter.getItems().add("All Colors");
        colorFilter.getItems().addAll(colors);
        colorFilter.setValue("All Colors");

        seasonFilter.getItems().add("All Seasons");
        seasonFilter.getItems().addAll(seasons);
        seasonFilter.setValue("All Seasons");
    }

    private void displayWardrobeItems() {
        wardrobeItemsPane.getChildren().clear();

        String categoryVal = categoryFilter.getValue();
        String colorVal = colorFilter.getValue();
        String seasonVal = seasonFilter.getValue();

        // Filter items
        List<Product> filteredItems = wardrobeItems.stream()
                .filter(item -> categoryVal == null || "All Categories".equals(categoryVal) ||
                        categoryVal.equals(item.getCategory()))
                .filter(item -> colorVal == null || "All Colors".equals(colorVal) ||
                        colorVal.equals(item.getColor()))
                .filter(item -> seasonVal == null || "All Seasons".equals(seasonVal) ||
                        seasonVal.equals(item.getSeason()))
                .collect(Collectors.toList());

        // Create UI components for each item
        for (Product item : filteredItems) {
            ClothingItemView itemView = new ClothingItemView(item);
            wardrobeItemsPane.getChildren().add(itemView);
        }

        // Update count
        totalItemsLabel.setText("Total Items: " + filteredItems.size());
    }

    @FXML
    private void applyFilter() {
        displayWardrobeItems();
    }

    @FXML
    private void clearFilter() {
        categoryFilter.setValue("All Categories");
        colorFilter.setValue("All Colors");
        seasonFilter.setValue("All Seasons");
        displayWardrobeItems();
    }

    /**
     * Opens the outfit creator window when the "Create Outfit" button is clicked
     */
    @FXML
    public void createOutfit() {
        // Check if there are items in the wardrobe
        if (wardrobeItems == null || wardrobeItems.isEmpty()) {
            com.fashionstore.utils.SceneManager.showAlert(
                    "Empty Wardrobe",
                    "Your wardrobe is empty. Add some clothes to your wardrobe before creating an outfit.");
            return;
        }

        // Open the outfit creator window
        WindowManager.openOutfitCreatorWindow();
    }

    /**
     * Handles the Back to Shopping button click
     */
    @FXML
    public void backToShopping() {
        // Close this window
        javafx.stage.Stage stage = (javafx.stage.Stage) wardrobeItemsPane.getScene().getWindow();
        stage.close();
    }
}