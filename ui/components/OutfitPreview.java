package com.fashionstore.ui.components;

import com.fashionstore.controllers.MyOutfitsController;
import com.fashionstore.controllers.OutfitDetailController;
import com.fashionstore.models.Outfit;
import com.fashionstore.models.Product;
import com.fashionstore.storage.DataManager;
import com.fashionstore.utils.SceneManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OutfitPreview extends VBox {
    private static final int IMAGE_SIZE = 80;
    private static final int MAX_IMAGES = 3;
    private static final int SPACING = 5;
    private static final int MAX_NAME_WIDTH = 250;

    private final Outfit outfit;
    private final DataManager dataManager;

    public OutfitPreview(Outfit outfit, DataManager dataManager) {
        super(SPACING);
        this.outfit = outfit;
        this.dataManager = dataManager;

        System.out.println("OutfitPreview: Creating preview for outfit " + outfit.getName() +
                " (ID: " + outfit.getOutfitId() + ")");

        getStyleClass().add("outfit-preview");
        initializeUI();
        setupEventHandlers();
    }

    private void initializeUI() {
        Label nameLabel = createNameLabel();
        FlowPane imagesPane = createPreviewImages();
        Label countLabel = createCountLabel();

        getChildren().addAll(nameLabel, imagesPane, countLabel);
    }

    private Label createNameLabel() {
        Label label = new Label(outfit.getName());
        label.setMaxWidth(MAX_NAME_WIDTH);
        label.setWrapText(true);
        label.getStyleClass().add("outfit-name");
        return label;
    }

    private Label createCountLabel() {
        Label label = new Label(outfit.getProductIds().size() + " items");
        label.setAccessibleText("View all items in this outfit");
        label.getStyleClass().add("item-count");
        return label;
    }

    private FlowPane createPreviewImages() {
        FlowPane imagesPane = new FlowPane(SPACING, SPACING);
        imagesPane.setPrefWrapLength(MAX_IMAGES * (IMAGE_SIZE + SPACING));

        // Convert the Set to a List to avoid ClassCastException
        List<String> productIds = new ArrayList<>(outfit.getProductIds());
        int itemsToShow = Math.min(MAX_IMAGES, productIds.size());

        for (int i = 0; i < itemsToShow; i++) {
            Product product = dataManager.getProduct(productIds.get(i));
            if (product != null) {
                imagesPane.getChildren().add(createProductImageView(product));
            }
        }

        if (imagesPane.getChildren().isEmpty()) {
            imagesPane.getChildren().add(createPlaceholderImage());
        }

        return imagesPane;
    }

    private ImageView createProductImageView(Product product) {
        ImageView imageView = new ImageView();
        configureImageView(imageView);

        try {
            String imagePath = product.getImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                // Try to load from file system first
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    imageView.setImage(new Image(imageFile.toURI().toString()));
                    return imageView;
                }

                // Try to load from classpath resources
                try {
                    imageView.setImage(new Image(getClass().getResourceAsStream(imagePath)));
                    return imageView;
                } catch (Exception ignored) {
                }
            }

            // Fallback to placeholder
            imageView.setImage(loadResourceImage("/images/product-placeholder.png"));
        } catch (Exception e) {
            imageView.setImage(loadResourceImage("/images/broken-image.png"));
        }

        return imageView;
    }

    private ImageView createPlaceholderImage() {
        ImageView placeholder = new ImageView(loadResourceImage("/images/outfit-placeholder.png"));
        configureImageView(placeholder);
        return placeholder;
    }

    private void configureImageView(ImageView imageView) {
        imageView.setFitWidth(IMAGE_SIZE);
        imageView.setFitHeight(IMAGE_SIZE);
        imageView.setPreserveRatio(true);
    }

    private Image loadResourceImage(String path) {
        try {
            return new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream(path),
                    "Failed to load image: " + path));
        } catch (Exception e) {
            return createColorPlaceholder(Color.LIGHTGRAY);
        }
    }

    private Image createColorPlaceholder(Color color) {
        Rectangle placeholder = new Rectangle(IMAGE_SIZE, IMAGE_SIZE);
        placeholder.setFill(color);
        return placeholder.snapshot(null, null);
    }

    private void setupEventHandlers() {
        this.setOnMouseClicked(this::handleOutfitClick);
        this.setOnMouseEntered(e -> setStyle("-fx-background-color: #f0f0f0;"));
        this.setOnMouseExited(e -> setStyle("-fx-background-color: transparent;"));
    }

    private void handleOutfitClick(MouseEvent event) {
        try {
            System.out.println("OutfitPreview: User clicked on outfit " + outfit.getName() +
                    " (ID: " + outfit.getOutfitId() + ")");

            // Load the outfit detail view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OutfitDetailView.fxml"));
            Parent root = loader.load();

            // Get the controller and set the outfit
            OutfitDetailController controller = loader.getController();

            // Get the parent controller (if the stage userdata contains it)
            MyOutfitsController parentController = null;
            try {
                if (getScene() != null && getScene().getWindow() != null) {
                    Stage currentStage = (Stage) getScene().getWindow();
                    if (currentStage.getUserData() instanceof MyOutfitsController) {
                        parentController = (MyOutfitsController) currentStage.getUserData();
                    }
                }
            } catch (Exception e) {
                // If we can't get the parent controller, just continue without it
                System.err.println("Warning: Could not access parent controller: " + e.getMessage());
            }

            controller.setOutfit(outfit, parentController);

            // Create a new stage for the detail view
            Stage detailStage = new Stage();
            detailStage.setTitle("Outfit Details: " + outfit.getName());

            // Set the scene and apply CSS
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());

            detailStage.setScene(scene);
            detailStage.initModality(Modality.APPLICATION_MODAL);

            // Set owner if possible
            try {
                if (getScene() != null && getScene().getWindow() != null) {
                    detailStage.initOwner(getScene().getWindow());
                }
            } catch (Exception e) {
                // If we can't set the owner, just continue without it
                System.err.println("Warning: Could not set owner for detail stage: " + e.getMessage());
            }

            // Show the detail view
            detailStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            SceneManager.showErrorAlert("Error", "Failed to load outfit details: " + e.getMessage());
        }
    }

    public Outfit getOutfit() {
        return outfit;
    }
}