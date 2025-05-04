package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.Outfit;
import com.fashionstore.models.User;
import com.fashionstore.storage.DataManager;
import com.fashionstore.ui.components.OutfitPreview;
import com.fashionstore.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MyOutfitsController implements Initializable {
    @FXML
    private FlowPane outfitsPane;

    private DataManager dataManager;
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            dataManager = FashionStoreApp.getDataManager();
            currentUser = dataManager.getCurrentUser();

            System.out.println("MyOutfitsController.initialize: Current user: " +
                    (currentUser != null ? currentUser.getUsername() : "null"));

            loadOutfits();
        } catch (Exception e) {
            SceneManager.showErrorAlert("Initialization Error",
                    "Failed to initialize My Outfits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadOutfits() {
        outfitsPane.getChildren().clear();

        if (currentUser != null) {
            try {
                System.out.println("MyOutfitsController.loadOutfits: Loading outfits for user: " +
                        currentUser.getUsername() + " (ID: " + currentUser.getUserId() + ")");

                // Try to set this controller as userData, but only if the scene is available
                if (outfitsPane.getScene() != null && outfitsPane.getScene().getWindow() != null) {
                    Stage stage = (Stage) outfitsPane.getScene().getWindow();
                    stage.setUserData(this);
                    System.out.println("MyOutfitsController.loadOutfits: Set this controller as stage userData");
                } else {
                    System.out.println("MyOutfitsController.loadOutfits: Scene or window not available yet");
                    // If scene is not available yet, set it up to run later when scene is ready
                    outfitsPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null) {
                            // Wait for the window to be initialized
                            newScene.windowProperty().addListener((windowObs, oldWindow, newWindow) -> {
                                if (newWindow != null) {
                                    ((Stage) newWindow).setUserData(this);
                                    System.out.println(
                                            "MyOutfitsController.loadOutfits: Set this controller as stage userData (delayed)");
                                }
                            });

                            // In case the window is already available
                            if (newScene.getWindow() != null) {
                                ((Stage) newScene.getWindow()).setUserData(this);
                                System.out.println(
                                        "MyOutfitsController.loadOutfits: Set this controller as stage userData (via new scene)");
                            }
                        }
                    });
                }

                List<Outfit> outfits = dataManager.getUserOutfitsWithProducts(currentUser.getUserId());
                System.out.println("MyOutfitsController.loadOutfits: Loaded " + outfits.size() + " outfits");

                // Filter out empty outfits (with no products)
                List<Outfit> nonEmptyOutfits = outfits.stream()
                        .filter(outfit -> !outfit.getProductIds().isEmpty())
                        .collect(java.util.stream.Collectors.toList());

                System.out.println("MyOutfitsController.loadOutfits: Filtered " +
                        (outfits.size() - nonEmptyOutfits.size()) + " empty outfits");

                if (nonEmptyOutfits.isEmpty()) {
                    Label emptyLabel = new Label("You don't have any outfits yet.");
                    outfitsPane.getChildren().add(emptyLabel);
                    System.out.println("MyOutfitsController.loadOutfits: No outfits found");
                } else {
                    for (Outfit outfit : nonEmptyOutfits) {
                        System.out.println("MyOutfitsController.loadOutfits: Adding outfit " + outfit.getName() +
                                " (ID: " + outfit.getOutfitId() + ")");
                        OutfitPreview preview = new OutfitPreview(outfit, dataManager);
                        outfitsPane.getChildren().add(preview);
                    }
                }
            } catch (Exception e) {
                System.err.println("MyOutfitsController.loadOutfits: Error loading outfits: " + e.getMessage());
                SceneManager.showErrorAlert("Loading Error",
                        "Failed to load outfits: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("MyOutfitsController.loadOutfits: No current user");
            Label noUserLabel = new Label("Please log in to view your outfits.");
            outfitsPane.getChildren().add(noUserLabel);
        }
    }

    /**
     * Handles the Back to Shopping button click
     */
    @FXML
    public void backToShopping() {
        // Close this window
        javafx.stage.Stage stage = (javafx.stage.Stage) outfitsPane.getScene().getWindow();
        stage.close();
    }

    /**
     * Sets the application to dark mode
     */
    @FXML
    public void setDarkMode() {
        try {
            if (outfitsPane.getScene() != null) {
                // Remove light theme if present
                outfitsPane.getScene().getStylesheets().removeIf(
                        style -> style.contains("application.css"));

                // Add dark theme stylesheet
                String darkThemePath = getClass().getResource("/styles/dark-theme.css").toExternalForm();
                if (!outfitsPane.getScene().getStylesheets().contains(darkThemePath)) {
                    outfitsPane.getScene().getStylesheets().add(darkThemePath);
                }

                // Store theme preference (this could be saved to user preferences)
                outfitsPane.getScene().getRoot().getProperties().put("theme", "dark");

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
            if (outfitsPane.getScene() != null) {
                // Remove dark theme if present
                outfitsPane.getScene().getStylesheets().removeIf(
                        style -> style.contains("dark-theme.css"));

                // Add light theme stylesheet
                String lightThemePath = getClass().getResource("/styles/application.css").toExternalForm();
                if (!outfitsPane.getScene().getStylesheets().contains(lightThemePath)) {
                    outfitsPane.getScene().getStylesheets().add(lightThemePath);
                }

                // Store theme preference (this could be saved to user preferences)
                outfitsPane.getScene().getRoot().getProperties().put("theme", "light");

                SceneManager.showAlert("Theme Changed", "Light mode has been activated.");
            }
        } catch (Exception e) {
            SceneManager.showErrorAlert("Theme Error", "Failed to apply light theme: " + e.getMessage());
            e.printStackTrace();
        }
    }
}