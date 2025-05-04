package com.fashionstore.utils;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;

public class WindowManager {
    private static final String FXML_PATH = "/fxml/";
    private static final String CSS_PATH = "/styles/application.css";

    /**
     * Opens the wardrobe window
     */
    public static void openWardrobeWindow() {
        openWindow("WardrobeView.fxml", "My Wardrobe", 900, 600);
    }

    /**
     * Opens the shopping cart window
     */
    public static void openCartWindow() {
        openWindow("CartView.fxml", "Shopping Cart", 850, 600);
    }

    /**
     * Opens the checkout window
     */
    public static void openCheckoutWindow() {
        openWindow("CheckoutView.fxml", "Checkout", 850, 700);
    }

    /**
     * Opens the outfits window
     */
    public static void openOutfitsWindow() {
        openWindow("my_outfits.fxml", "My Outfits", 900, 650);
    }

    /**
     * Opens the AI stylist window
     */
    public static void openAIStylistWindow() {
        // Make sure to provide the correct path to the FXML file
        openWindow("AIRecommendationsView.fxml", "AI Stylist", 900, 700);
    }

    /**
     * Opens the outfit creator window
     */
    public static void openOutfitCreatorWindow() {
        openWindow("OutfitCreatorView.fxml", "Create New Outfit", 950, 650);
    }

    /**
     * Opens a modal window with the specified parameters
     * 
     * @param fxmlFile The FXML file name
     * @param title    The window title
     * @param width    The window width
     * @param height   The window height
     */
    private static void openWindow(String fxmlFile, String title, int width, int height) {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(
                            WindowManager.class.getResource(FXML_PATH + fxmlFile),
                            "FXML file not found: " + fxmlFile));

            Parent root = loader.load();

            // Create and configure the stage
            Stage stage = new Stage();
            configureStage(stage, root, title, width, height);

            // Apply entry animation
            applyEntryAnimation(root, stage);

            // Show the window
            stage.show();

        } catch (IOException e) {
            handleWindowError(title, e);
        } catch (NullPointerException e) {
            handleWindowError(title, new IOException("Resource not found: " + fxmlFile, e));
        }
    }

    /**
     * Applies animation when opening a window
     */
    private static void applyEntryAnimation(Parent root, Stage stage) {
        // Set initial opacity
        root.setOpacity(0);

        // Create fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1.0);

        // Create scale animation
        root.setScaleX(0.97);
        root.setScaleY(0.97);

        Timeline scaleTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(root.scaleXProperty(), 0.97),
                        new KeyValue(root.scaleYProperty(), 0.97)),
                new KeyFrame(Duration.millis(350),
                        new KeyValue(root.scaleXProperty(), 1.0),
                        new KeyValue(root.scaleYProperty(), 1.0)));

        // Play animations when the stage is shown
        stage.setOnShown(event -> {
            fadeIn.play();
            scaleTimeline.play();
        });
    }

    /**
     * Configures a stage with common settings
     */
    private static void configureStage(Stage stage, Parent root, String title, int width, int height) {
        stage.setTitle(title);

        // Set modality and owner
        Window owner = SceneManager.getPrimaryStage();
        if (owner != null) {
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }

        // Create and style the scene
        Scene scene = new Scene(root, width, height);

        // Determine which theme to use based on parent window or default to light
        String currentTheme = "light";
        if (owner != null && owner instanceof Stage) {
            Stage ownerStage = (Stage) owner;
            if (ownerStage.getScene() != null &&
                    ownerStage.getScene().getRoot().getProperties().get("theme") != null) {
                currentTheme = (String) ownerStage.getScene().getRoot().getProperties().get("theme");
            }
        }

        try {
            // Apply the appropriate theme
            if ("dark".equals(currentTheme)) {
                scene.getStylesheets().add(
                        Objects.requireNonNull(
                                WindowManager.class.getResource("/styles/dark-theme.css"),
                                "Dark theme CSS file not found").toExternalForm());

                // Store theme in scene properties
                scene.getRoot().getProperties().put("theme", "dark");
            } else {
                scene.getStylesheets().add(
                        Objects.requireNonNull(
                                WindowManager.class.getResource("/styles/application.css"),
                                "CSS file not found").toExternalForm());

                // Store theme in scene properties
                scene.getRoot().getProperties().put("theme", "light");
            }
        } catch (NullPointerException e) {
            System.err.println("Warning: Stylesheet not loaded - " + e.getMessage());
        }

        stage.setScene(scene);
        stage.setMinWidth(width);
        stage.setMinHeight(height);
        stage.centerOnScreen();
    }

    /**
     * Handles window opening errors
     */
    private static void handleWindowError(String windowTitle, Exception e) {
        String errorMessage = String.format(
                "Could not open %s window: %s",
                windowTitle,
                e.getMessage());

        System.err.println(errorMessage);
        e.printStackTrace();

        SceneManager.showErrorAlert(
                "Window Error",
                errorMessage);
    }

    public static void refreshHomeView() {
        // Run on JavaFX application thread to avoid threading issues
        javafx.application.Platform.runLater(() -> {
            try {
                // Find all open windows and refresh them if they contain a HomeController or
                // StoreViewController
                for (javafx.stage.Window window : javafx.stage.Stage.getWindows()) {
                    if (window instanceof javafx.stage.Stage) {
                        javafx.stage.Stage stage = (javafx.stage.Stage) window;
                        javafx.scene.Scene scene = stage.getScene();

                        if (scene != null && scene.getRoot() != null) {
                            // Check if this is the home view
                            if (scene.getRoot().getId() != null && scene.getRoot().getId().equals("homeView")) {
                                Object controller = scene.getRoot().getUserData();
                                if (controller instanceof com.fashionstore.controllers.HomeController) {
                                    System.out.println("Refreshing HomeController");
                                    ((com.fashionstore.controllers.HomeController) controller).refreshView();
                                }
                            }

                            // Also refresh store view if open
                            if (scene.getRoot().getId() != null && scene.getRoot().getId().equals("storeView")) {
                                Object controller = scene.getRoot().getUserData();
                                if (controller instanceof com.fashionstore.controllers.StoreViewController) {
                                    System.out.println("Refreshing StoreViewController");
                                    ((com.fashionstore.controllers.StoreViewController) controller).refreshView();
                                }
                            }

                            // Note: Add more view refresh calls here if other controllers implement
                            // refreshView()
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error refreshing views: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}