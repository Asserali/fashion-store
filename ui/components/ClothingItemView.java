package com.fashionstore.ui.components;

import com.fashionstore.models.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.io.File;

public class ClothingItemView extends VBox {

    private Product product;
    private boolean isSelected = false;
    private boolean isDraggable = true;

    public ClothingItemView(Product product) {
        this(product, true);
    }

    public ClothingItemView(Product product, boolean draggable) {
        this.product = product;
        this.isDraggable = draggable;

        setPrefWidth(150);
        setMaxWidth(150);
        setMinHeight(200);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10));
        getStyleClass().add("clothing-item");

        // Add image view
        ImageView imageView = createImageView();

        // Create labels
        Label nameLabel = new Label(product.getName());
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(130);
        nameLabel.getStyleClass().add("clothing-item-name");

        Label infoLabel = new Label(product.getCategory() +
                (product.getColor() != null ? " • " + product.getColor() : ""));
        infoLabel.getStyleClass().add("clothing-item-info");

        // Add components to this container
        getChildren().addAll(imageView, nameLabel, infoLabel);

        // Add click handler
        setOnMouseClicked(e -> toggleSelection());

        // Set up drag-and-drop if enabled
        if (isDraggable) {
            setupDragAndDrop();
        }
    }

    private ImageView createImageView() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(130);
        imageView.setFitHeight(130);
        imageView.setPreserveRatio(true);

        // Try to load the image using the path stored in the product
        String imagePath = product.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                // First try to load as a resource (typical for application images)
                java.io.InputStream resourceStream = getClass().getResourceAsStream(imagePath);
                if (resourceStream != null) {
                    Image image = new Image(resourceStream);
                    imageView.setImage(image);
                    return imageView;
                }

                // Fallback to file system for development environment
                File imageFile = new File("src/main/resources" + imagePath);
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView.setImage(image);
                    return imageView;
                }

                // Last attempt - try absolute path (should rarely be needed)
                File absoluteFile = new File(imagePath);
                if (absoluteFile.exists()) {
                    Image image = new Image(absoluteFile.toURI().toString());
                    imageView.setImage(image);
                    return imageView;
                }

                // If we got here, no image could be loaded
                createPlaceholder(imageView);
            } catch (Exception e) {
                System.err.println("Error loading image for product " + product.getProductId() + ": " + e.getMessage());
                createPlaceholder(imageView);
            }
        } else {
            createPlaceholder(imageView);
        }

        return imageView;
    }

    private void createPlaceholder(ImageView imageView) {
        // Create a colored rectangle as placeholder
        Rectangle placeholder = new Rectangle(130, 130);
        Rectangle clip = new Rectangle(130, 130);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        placeholder.setClip(clip);

        // Use product color if available, or a default
        if (product.getColor() != null && !product.getColor().isEmpty()) {
            try {
                Color color = Color.web(mapColorNameToHex(product.getColor()));
                placeholder.setFill(color);
            } catch (Exception e) {
                placeholder.setFill(Color.LIGHTGRAY);
            }
        } else {
            placeholder.setFill(Color.LIGHTGRAY);
        }

        // Add initial letter in the center
        if (product.getName() != null && !product.getName().isEmpty()) {
            Label initialLabel = new Label(product.getName().substring(0, 1).toUpperCase());
            initialLabel.setStyle("-fx-font-size: 42px; -fx-text-fill: white; -fx-font-weight: bold;");

            // Create a group with the rectangle and label
            javafx.scene.Group group = new javafx.scene.Group(placeholder, initialLabel);

            // Position the label in the center
            initialLabel.setLayoutX(placeholder.getWidth() / 2 - 15);
            initialLabel.setLayoutY(placeholder.getHeight() / 2 - 25);

            // Create snapshot from the group
            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(javafx.scene.paint.Color.TRANSPARENT);
            imageView.setImage(group.snapshot(params, null));
        } else {
            // Just use the rectangle if no name
            imageView.setImage(placeholder.snapshot(null, null));
        }
    }

    private String mapColorNameToHex(String colorName) {
        // Basic color mapping
        switch (colorName.toLowerCase()) {
            case "black":
                return "#000000";
            case "white":
                return "#FFFFFF";
            case "red":
                return "#FF0000";
            case "green":
                return "#00FF00";
            case "blue":
                return "#0000FF";
            case "yellow":
                return "#FFFF00";
            case "purple":
                return "#800080";
            case "orange":
                return "#FFA500";
            case "pink":
                return "#FFC0CB";
            case "gray":
            case "grey":
                return "#808080";
            case "brown":
                return "#A52A2A";
            case "navy":
                return "#000080";
            default:
                return "#DDDDDD";
        }
    }

    /**
     * Sets up drag-and-drop capability for this item
     */
    private void setupDragAndDrop() {
        // Set up drag detection
        setOnDragDetected(event -> {
            // Start drag-and-drop
            Dragboard db = startDragAndDrop(TransferMode.COPY);

            // Put a string on dragboard to identify our drag
            ClipboardContent content = new ClipboardContent();
            content.putString(product.getProductId());
            db.setContent(content);

            // Create a snapshot of this item for drag visual
            db.setDragView(snapshot(null, null));

            // Add a selected look during drag
            getStyleClass().add("clothing-item-dragging");

            event.consume();
        });

        // Handle drag done to reset UI
        setOnDragDone(event -> {
            getStyleClass().remove("clothing-item-dragging");
            event.consume();
        });
    }

    public Product getProduct() {
        return product;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
        if (selected) {
            getStyleClass().add("clothing-item-selected");
        } else {
            getStyleClass().remove("clothing-item-selected");
        }
    }

    public void toggleSelection() {
        setSelected(!isSelected);
    }

    /**
     * Makes this item non-draggable (for items displayed in preview only)
     */
    public void setDraggable(boolean draggable) {
        this.isDraggable = draggable;
        if (draggable) {
            setupDragAndDrop();
        } else {
            // Remove drag handlers
            setOnDragDetected(null);
            setOnDragDone(null);
        }
    }

    /**
     * Creates a smaller version of this clothing item view (for drop zones)
     */
    public ClothingItemView createCompactView() {
        ClothingItemView compact = new ClothingItemView(product, false);
        compact.setPrefWidth(100);
        compact.setMaxWidth(100);
        compact.setMinHeight(120);
        compact.setPadding(new Insets(5));
        compact.getStyleClass().add("clothing-item-compact");
        return compact;
    }
}