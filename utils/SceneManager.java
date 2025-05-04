package com.fashionstore.utils;

import com.fashionstore.controllers.OutfitCreatorController;
import com.fashionstore.models.Outfit;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class SceneManager {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    // Update your loadScene method in SceneManager.java
    public static void loadScene(String fxmlPath) {
        try {
            String fullPath = fxmlPath;
            if (!fxmlPath.startsWith("/fxml/")) {
                fullPath = "/fxml/" + fxmlPath;
            }

            System.out.println("Attempting to load: " + fullPath);

            URL fxmlUrl = SceneManager.class.getResource(fullPath);
            if (fxmlUrl == null) {
                throw new IOException("Cannot find FXML file: " + fullPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // Create new scene with loading overlay
            StackPane rootContainer = new StackPane();
            rootContainer.getStyleClass().add("root-container");
            rootContainer.getChildren().add(root);

            // Store the current maximized state
            boolean wasMaximized = primaryStage.isMaximized();

            // Store current theme if it exists
            String currentTheme = "light"; // Default theme
            if (primaryStage.getScene() != null &&
                    primaryStage.getScene().getRoot().getProperties().get("theme") != null) {
                currentTheme = (String) primaryStage.getScene().getRoot().getProperties().get("theme");
            }

            // Set a consistent minimum size for the window
            double minWidth = 1024;
            double minHeight = 768;

            Scene scene = new Scene(rootContainer);

            // Apply the appropriate theme based on previous setting
            if ("dark".equals(currentTheme)) {
                URL darkCssUrl = SceneManager.class.getResource("/styles/dark-theme.css");
                if (darkCssUrl != null) {
                    scene.getStylesheets().add(darkCssUrl.toExternalForm());
                }

                // Store the theme preference in the new scene
                scene.getRoot().getProperties().put("theme", "dark");

                // Set black title bar for dark theme
                try {
                    scene.getRoot().setStyle("-fx-background-color: #000000;");
                } catch (Exception e) {
                    System.err.println("Failed to set title bar color: " + e.getMessage());
                }
            } else {
                // Default to light theme
                URL cssUrl = SceneManager.class.getResource("/styles/application.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }

                // Store the theme preference in the new scene
                scene.getRoot().getProperties().put("theme", "light");
            }

            if (primaryStage == null) {
                throw new IllegalStateException("Primary stage has not been set.");
            }

            // Configure stage properties
            if (!primaryStage.isShowing()) {
                primaryStage.setMinWidth(minWidth);
                primaryStage.setMinHeight(minHeight);
                primaryStage.setWidth(minWidth);
                primaryStage.setHeight(minHeight);
            }

            // Set the scene
            primaryStage.setScene(scene);

            // Show window if not already showing
            if (!primaryStage.isShowing()) {
                primaryStage.centerOnScreen();
                primaryStage.show();
            }

            // Use Platform.runLater to ensure content is properly sized after stage is
            // shown
            Platform.runLater(() -> {
                // Restore maximized state if it was maximized before
                if (wasMaximized) {
                    primaryStage.setMaximized(true);
                }

                // Setup maximization handling for this scene
                updateForMaximized(primaryStage, root);

                // Apply fade-in animation
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                fadeIn.setFromValue(0.3);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error Loading View", "Could not load view: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    public static <T> T loadComponent(String fxmlPath) {
        try {
            if (!fxmlPath.startsWith("/fxml/")) {
                fxmlPath = "/fxml/" + fxmlPath;
            }

            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            loader.load();
            return loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error Loading Component", "Could not load component: " + fxmlPath);
            return null;
        }
    }

    public static Stage loadModal(String fxmlPath) {
        return loadModal(fxmlPath, null);
    }

    public static Stage loadModal(String fxmlPath, String title) {
        try {
            if (!fxmlPath.startsWith("/fxml/")) {
                fxmlPath = "/fxml/" + fxmlPath;
            }

            URL fxmlUrl = SceneManager.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("Cannot find FXML file: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            if (primaryStage != null) {
                modalStage.initOwner(primaryStage);
            }

            if (title != null) {
                modalStage.setTitle(title);
            }

            Scene scene = new Scene(root);

            // Get current theme from primary stage scene if available
            String currentTheme = "light"; // Default theme
            if (primaryStage != null &&
                    primaryStage.getScene() != null &&
                    primaryStage.getScene().getRoot().getProperties().get("theme") != null) {
                currentTheme = (String) primaryStage.getScene().getRoot().getProperties().get("theme");
            }

            // Apply appropriate theme CSS
            if ("dark".equals(currentTheme)) {
                URL darkCssUrl = SceneManager.class.getResource("/styles/dark-theme.css");
                if (darkCssUrl != null) {
                    scene.getStylesheets().add(darkCssUrl.toExternalForm());
                }

                // Store the theme preference in the new scene
                scene.getRoot().getProperties().put("theme", "dark");
            } else {
                // Default to light theme
                URL cssUrl = SceneManager.class.getResource("/styles/application.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }

                // Store the theme preference in the new scene
                scene.getRoot().getProperties().put("theme", "light");
            }

            modalStage.setScene(scene);

            // Set minimum size for consistency
            double minWidth = 600;
            double minHeight = 400;
            modalStage.setMinWidth(minWidth);
            modalStage.setMinHeight(minHeight);

            // Size to content but ensure minimum size
            modalStage.sizeToScene();
            if (modalStage.getWidth() < minWidth) {
                modalStage.setWidth(minWidth);
            }
            if (modalStage.getHeight() < minHeight) {
                modalStage.setHeight(minHeight);
            }

            modalStage.centerOnScreen();

            // Add resize listeners to prevent losing maximized state
            modalStage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    // When maximized, update the scene to fill the screen properly
                    Platform.runLater(() -> {
                        scene.getRoot().requestLayout();
                    });
                }
            });

            return modalStage;
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error Loading Modal", "Could not load modal: " + fxmlPath);
            return null;
        }
    }

    public static boolean showConfirmationAlert(String title, String message) {
        AtomicBoolean result = new AtomicBoolean(false);

        // Run on JavaFX thread and wait for result
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            Optional<ButtonType> buttonType = alert.showAndWait();
            result.set(buttonType.isPresent() && buttonType.get() == ButtonType.OK);
        });

        // Note: This still has the same threading issue as before
        // For a proper solution, you should use callbacks as in the other method
        return result.get();
    }

    public static void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Apply CSS to the dialog
            applyDialogStyles(alert, Alert.AlertType.INFORMATION);

            alert.showAndWait();
        });
    }

    public static void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Apply CSS to the dialog
            applyDialogStyles(alert, Alert.AlertType.ERROR);

            alert.showAndWait();
        });
    }

    public static void showConfirmationAlert(String title, String message, Runnable onConfirm, Runnable onCancel) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Apply CSS to the dialog
            applyDialogStyles(alert, Alert.AlertType.CONFIRMATION);

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (onConfirm != null)
                        onConfirm.run();
                } else {
                    if (onCancel != null)
                        onCancel.run();
                }
            });
        });
    }

    /**
     * Applies the current application theme's CSS to any dialog including alerts
     * 
     * @param alert     The alert or dialog to style
     * @param alertType The type of alert for additional styling
     */
    private static void applyDialogStyles(Alert alert, Alert.AlertType alertType) {
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();

        // Get current theme from primary stage scene if available
        String currentTheme = "light"; // Default theme
        if (primaryStage != null && primaryStage.getScene() != null &&
                primaryStage.getScene().getRoot().getProperties().get("theme") != null) {
            currentTheme = (String) primaryStage.getScene().getRoot().getProperties().get("theme");
        }

        // Apply appropriate theme CSS
        if ("dark".equals(currentTheme)) {
            URL darkCssUrl = SceneManager.class.getResource("/styles/dark-theme.css");
            if (darkCssUrl != null) {
                alert.getDialogPane().getStylesheets().add(darkCssUrl.toExternalForm());
            }
        } else {
            URL cssUrl = SceneManager.class.getResource("/styles/application.css");
            if (cssUrl != null) {
                alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
        }

        // Add dialog-specific class for additional styling
        alert.getDialogPane().getStyleClass().add("custom-alert");

        // Add alert type specific class
        if (alertType != null) {
            switch (alertType) {
                case CONFIRMATION:
                    alert.getDialogPane().getStyleClass().add("confirmation");
                    break;
                case INFORMATION:
                    alert.getDialogPane().getStyleClass().add("information");
                    break;
                case WARNING:
                    alert.getDialogPane().getStyleClass().add("warning");
                    break;
                case ERROR:
                    alert.getDialogPane().getStyleClass().add("error");
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Overloaded version that defaults to no specific alert type
     * 
     * @param alert The alert or dialog to style
     */
    private static void applyDialogStyles(Alert alert) {
        applyDialogStyles(alert, null);
    }

    /**
     * Updates the layout for maximized windows
     * 
     * @param stage The stage to update
     * @param root  The root element to update
     */
    public static void updateForMaximized(Stage stage, Parent root) {
        if (stage == null || root == null) {
            return;
        }

        // Add a listener for the maximized property
        stage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                if (newVal) {
                    // When maximized, apply the maximized styling
                    applyMaximizedStyles(root);
                } else {
                    // When restored, apply the normal styling
                    applyNormalStyles(root);
                }
            });
        });

        // Initial setup based on current maximized state
        if (stage.isMaximized()) {
            applyMaximizedStyles(root);
        } else {
            applyNormalStyles(root);
        }
    }

    /**
     * Apply styles for maximized window
     * 
     * @param root The root element to update
     */
    private static void applyMaximizedStyles(Parent root) {
        // Apply to FlowPane if found
        root.lookupAll(".flow-pane").forEach(node -> {
            node.getStyleClass().add("flow-pane-maximized");
        });

        // Request layout to ensure proper resizing
        root.requestLayout();
    }

    /**
     * Apply styles for normal (non-maximized) window
     * 
     * @param root The root element to update
     */
    private static void applyNormalStyles(Parent root) {
        // Remove maximized styles from FlowPane if found
        root.lookupAll(".flow-pane").forEach(node -> {
            node.getStyleClass().remove("flow-pane-maximized");
        });

        // Request layout to ensure proper resizing
        root.requestLayout();
    }

    public static boolean showConfirmationDialog(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        // Apply CSS to the dialog
        applyDialogStyles(alert, Alert.AlertType.CONFIRMATION);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Shows the outfit creator in edit mode.
     *
     * @param outfit   The outfit to edit
     * @param callback Consumer that receives a boolean indicating success/failure
     */
    public static void showOutfitCreatorForEdit(Outfit outfit, java.util.function.Consumer<Boolean> callback) {
        try {
            // Load the outfit creator view
            Stage stage = loadModal("OutfitCreatorView.fxml", "Edit Outfit");

            // Store the outfit and callback
            stage.setUserData(outfit); // Store the outfit for the controller to access

            // Get the controller
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/OutfitCreatorView.fxml"));
            Parent root = loader.load();
            OutfitCreatorController controller = loader.getController();

            // Set edit mode with the outfit
            controller.initForEdit(outfit);

            // Apply CSS
            Scene scene = new Scene(root);

            // Get current theme from primary stage scene
            String currentTheme = "light"; // Default theme
            if (primaryStage != null &&
                    primaryStage.getScene() != null &&
                    primaryStage.getScene().getRoot().getProperties().get("theme") != null) {
                currentTheme = (String) primaryStage.getScene().getRoot().getProperties().get("theme");
            }

            // Apply appropriate theme CSS
            if ("dark".equals(currentTheme)) {
                URL darkCssUrl = SceneManager.class.getResource("/styles/dark-theme.css");
                if (darkCssUrl != null) {
                    scene.getStylesheets().add(darkCssUrl.toExternalForm());
                }

                // Store the theme preference in the new scene
                scene.getRoot().getProperties().put("theme", "dark");
            } else {
                // Default to light theme
                URL cssUrl = SceneManager.class.getResource("/styles/application.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }

                // Store the theme preference in the new scene
                scene.getRoot().getProperties().put("theme", "light");
            }

            // Store callback in scene user data
            scene.setUserData(callback);

            // Set the scene
            stage.setScene(scene);

            // Set minimum size for consistency
            double minWidth = 800;
            double minHeight = 600;
            stage.setMinWidth(minWidth);
            stage.setMinHeight(minHeight);
            stage.centerOnScreen();

            // Show the stage
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Could not open outfit editor: " + e.getMessage());
            if (callback != null) {
                callback.accept(false);
            }
        }
    }
}