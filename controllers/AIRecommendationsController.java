package com.fashionstore.controllers;

import com.fashionstore.ai.OutfitMatcher;
import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.Outfit;
import com.fashionstore.models.Product;
import com.fashionstore.storage.DataManager;
import com.fashionstore.ui.components.ClothingItemView;
import com.fashionstore.utils.SceneManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AIRecommendationsController implements Initializable {

    @FXML
    private VBox recommendationsContainer;
    @FXML
    private Button generateButton;

    private DataManager dataManager;
    private OutfitMatcher outfitMatcher;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = FashionStoreApp.getDataManager();
        outfitMatcher = new OutfitMatcher();

        if (dataManager.getCurrentUser() == null) {
            return;
        }
    }

    @FXML
    private void generateRecommendations() {
        recommendationsContainer.getChildren().clear();

        if (dataManager.getCurrentUser() == null) {
            return;
        }

        // Get user's wardrobe items
        List<Product> wardrobeItems = dataManager.getUserWardrobe(
                dataManager.getCurrentUser().getUserId());

        if (wardrobeItems.isEmpty()) {
            showEmptyWardrobeMessage();
            return;
        }

        // Generate 3 outfit recommendations
        for (int i = 0; i < 3; i++) {
            Outfit recommendation = outfitMatcher.generateOutfit(
                    dataManager.getCurrentUser(), wardrobeItems);

            if (recommendation != null && !recommendation.getProductIds().isEmpty()) {
                addOutfitRecommendation(recommendation, i + 1);
            }
        }
    }

    private void addOutfitRecommendation(Outfit outfit, int index) {
        VBox outfitBox = new VBox(10);
        outfitBox.setAlignment(Pos.CENTER_LEFT);
        outfitBox.setPadding(new Insets(15));
        outfitBox.getStyleClass().add("recommendation-box");

        // Outfit header
        Label titleLabel = new Label("Recommendation #" + index + ": " + outfit.getName());
        titleLabel.getStyleClass().add("recommendation-title");
        outfitBox.getChildren().add(titleLabel);

        // Items display
        FlowPane itemsPane = new FlowPane(10, 10);
        itemsPane.setAlignment(Pos.CENTER_LEFT);

        for (String productId : outfit.getProductIds()) {
            Product product = dataManager.getProduct(productId);
            if (product != null) {
                ClothingItemView itemView = new ClothingItemView(product);
                itemsPane.getChildren().add(itemView);
            }
        }

        outfitBox.getChildren().add(itemsPane);

        // Action buttons
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button saveButton = new Button("Save Outfit");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(e -> saveRecommendation(outfit));

        actionBox.getChildren().add(saveButton);
        outfitBox.getChildren().add(actionBox);

        // Add to container
        recommendationsContainer.getChildren().add(outfitBox);

        // Add separator unless it's the last recommendation
        if (index < 3) {
            recommendationsContainer.getChildren().add(new Separator());
        }
    }

    private void saveRecommendation(Outfit outfit) {
        // Save the outfit to user's collection
        dataManager.addOutfit(outfit);
        dataManager.getCurrentUser().addOutfit(outfit.getOutfitId());
        dataManager.saveAllData();

        SceneManager.showAlert("Outfit Saved",
                "The outfit recommendation has been saved to your collection!");
    }

    private void showEmptyWardrobeMessage() {
        Label emptyLabel = new Label("Your wardrobe is empty! Add some items to get AI recommendations.");
        emptyLabel.getStyleClass().add("warning-text");
        emptyLabel.setPadding(new Insets(20));

        recommendationsContainer.getChildren().add(emptyLabel);
    }

    /**
     * Handles the Back to Shopping button click
     */
    @FXML
    public void backToShopping() {
        // Close this window
        javafx.stage.Stage stage = (javafx.stage.Stage) recommendationsContainer.getScene().getWindow();
        stage.close();
    }
}