package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.Outfit;
import com.fashionstore.storage.DataManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class OutfitsController implements Initializable {

    @FXML
    private FlowPane outfitsContainer;

    private DataManager dataManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dataManager = FashionStoreApp.getDataManager();
        loadOutfits();
    }

    private void loadOutfits() {
        outfitsContainer.getChildren().clear();

        List<Outfit> outfits = dataManager.getUserOutfits(dataManager.getCurrentUser().getUserId());
        for (Outfit outfit : outfits) {
            // Add UI components for each outfit (e.g., a custom OutfitView)
            // Example: outfitsContainer.getChildren().add(new OutfitView(outfit));
        }
    }

    @FXML
    private void openOutfitCreator() {
        com.fashionstore.utils.WindowManager.openOutfitCreatorWindow();
    }
}