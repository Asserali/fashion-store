package com.fashionstore.ui.components;

import com.fashionstore.models.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;

/**
 * A component that displays a human silhouette with drop zones for clothing
 * items.
 * Users can drag and drop clothing items onto the body parts to create outfits.
 */
public class BodyCanvas extends VBox {
    // Body zones for different clothing types
    private final StackPane headZone;
    private final StackPane topZone;
    private final StackPane bottomZone;
    private final StackPane feetZone;
    private final StackPane accessoriesZone;

    // Map to track which product is placed in which zone
    private final Map<String, Product> placedProducts = new HashMap<>();

    // Silhouette image
    private final ImageView silhouetteView;

    /**
     * Creates a new BodyCanvas component
     */
    public BodyCanvas() {
        super(10);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));
        setMinWidth(300);
        setPrefWidth(300);
        setMaxWidth(400);
        getStyleClass().add("body-canvas");

        // Create the silhouette image
        silhouetteView = new ImageView();
        try {
            // Try to load image from resources
            Image silhouette = new Image(getClass().getResourceAsStream("/images/silhouette.png"));
            if (silhouette == null || silhouette.isError()) {
                // If file not found or empty, generate a silhouette programmatically
                silhouette = SilhouetteGenerator.generateSilhouette(200, 400);
                System.out.println("Using generated silhouette image");
            }
            silhouetteView.setImage(silhouette);
            silhouetteView.setFitHeight(400);
            silhouetteView.setPreserveRatio(true);
            silhouetteView.setOpacity(0.15); // Very light silhouette
        } catch (Exception e) {
            System.err.println("Could not load silhouette image: " + e.getMessage());
            // Generate a silhouette programmatically as fallback
            Image silhouette = SilhouetteGenerator.generateSilhouette(200, 400);
            silhouetteView.setImage(silhouette);
            silhouetteView.setFitHeight(400);
            silhouetteView.setPreserveRatio(true);
            silhouetteView.setOpacity(0.15);
        }

        // Create the zones with initial labels
        headZone = createZone("Head/Hat", "accessories");
        topZone = createZone("Top/Shirt", "tops");
        bottomZone = createZone("Bottom/Pants", "bottoms");
        feetZone = createZone("Shoes", "shoes");
        accessoriesZone = createZone("Accessories", "accessories");

        // Create the layout
        StackPane bodyStack = new StackPane();
        VBox zonesBox = new VBox(10);
        zonesBox.setAlignment(Pos.CENTER);
        zonesBox.getChildren().addAll(headZone, topZone, bottomZone, feetZone, accessoriesZone);

        bodyStack.getChildren().addAll(silhouetteView, zonesBox);

        Label titleLabel = new Label("Outfit Builder");
        titleLabel.getStyleClass().add("section-header");

        getChildren().addAll(titleLabel, bodyStack);

        // Setup drop handlers
        setupDropHandlers();
    }

    /**
     * Creates a drop zone for a specific body part
     */
    private StackPane createZone(String labelText, String categoryHint) {
        StackPane zone = new StackPane();
        zone.setPrefSize(250, 80);
        zone.getStyleClass().add("drop-zone");
        zone.setUserData(categoryHint); // Store category hint for validation

        Label label = new Label(labelText);
        label.getStyleClass().add("drop-zone-label");

        zone.getChildren().add(label);
        return zone;
    }

    /**
     * Sets up drop handlers for all zones
     */
    private void setupDropHandlers() {
        // Setup handlers for each zone
        setupZoneHandlers(headZone, "Head/Hat");
        setupZoneHandlers(topZone, "Top/Shirt");
        setupZoneHandlers(bottomZone, "Bottom/Pants");
        setupZoneHandlers(feetZone, "Shoes");
        setupZoneHandlers(accessoriesZone, "Accessories");
    }

    /**
     * Sets up handlers for a specific zone
     */
    private void setupZoneHandlers(StackPane zone, String zoneName) {
        // Visual feedback for drag over
        zone.setOnDragOver(event -> {
            if (event.getGestureSource() instanceof ClothingItemView &&
                    validateItemForZone((ClothingItemView) event.getGestureSource(), zone)) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                zone.getStyleClass().add("drop-zone-active");
            }
            event.consume();
        });

        // Remove highlight when drag exits
        zone.setOnDragExited(event -> {
            zone.getStyleClass().remove("drop-zone-active");
            event.consume();
        });

        // Handle the drop
        zone.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getGestureSource() instanceof ClothingItemView) {
                ClothingItemView source = (ClothingItemView) event.getGestureSource();
                if (validateItemForZone(source, zone)) {
                    addProductToZone(source.getProduct(), zone);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            zone.getStyleClass().remove("drop-zone-active");
            event.consume();
        });

        // Double-click to remove
        zone.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                clearZone(zone);
            }
        });
    }

    /**
     * Validates if an item can be placed in a zone based on category
     */
    private boolean validateItemForZone(ClothingItemView itemView, StackPane zone) {
        Product product = itemView.getProduct();
        String category = product.getCategory().toLowerCase();
        String zoneCategory = (String) zone.getUserData();

        // Simple validation rules (can be expanded)
        if (zone == headZone) {
            return category.contains("hat") || category.contains("cap") ||
                    category.contains("accessories") && (product.getName().toLowerCase().contains("hat") ||
                            product.getName().toLowerCase().contains("cap"));
        } else if (zone == topZone) {
            return category.contains("top") || category.contains("shirt") ||
                    category.contains("jacket") || category.contains("outerwear");
        } else if (zone == bottomZone) {
            return category.contains("bottom") || category.contains("pant") ||
                    category.contains("skirt") || category.contains("short");
        } else if (zone == feetZone) {
            return category.contains("shoe") || category.contains("boot") ||
                    category.contains("sneaker") || category.contains("sandal");
        } else if (zone == accessoriesZone) {
            return category.contains("accessor") || category.contains("jewelry") ||
                    category.contains("watch") || category.contains("belt") ||
                    category.contains("scarf") || category.contains("bag");
        }

        // Default fallback - allow drop if we can't determine
        return true;
    }

    /**
     * Adds a product to a drop zone
     */
    private void addProductToZone(Product product, StackPane zone) {
        // Clear the zone first
        clearZone(zone);

        // Create product view
        ClothingItemView itemView = new ClothingItemView(product);
        itemView.setMaxSize(230, 70);

        // Store in our map
        String zoneId = getZoneId(zone);
        placedProducts.put(zoneId, product);

        // Add to the zone
        zone.getChildren().add(itemView);
    }

    /**
     * Clears a zone (removes any product from it)
     */
    private void clearZone(StackPane zone) {
        // Keep only the first child (the label)
        if (zone.getChildren().size() > 1) {
            zone.getChildren().remove(1, zone.getChildren().size());
        }

        // Remove from our map
        String zoneId = getZoneId(zone);
        placedProducts.remove(zoneId);
    }

    /**
     * Gets a string identifier for a zone
     */
    private String getZoneId(StackPane zone) {
        if (zone == headZone)
            return "head";
        if (zone == topZone)
            return "top";
        if (zone == bottomZone)
            return "bottom";
        if (zone == feetZone)
            return "feet";
        if (zone == accessoriesZone)
            return "accessories";
        return "unknown";
    }

    /**
     * Gets all products currently placed on the body
     */
    public Map<String, Product> getPlacedProducts() {
        return new HashMap<>(placedProducts);
    }

    /**
     * Clears all zones
     */
    public void clearAll() {
        clearZone(headZone);
        clearZone(topZone);
        clearZone(bottomZone);
        clearZone(feetZone);
        clearZone(accessoriesZone);
        placedProducts.clear();
    }

    /**
     * Places a product on the appropriate zone based on its category
     */
    public boolean placeProductAutomatically(Product product) {
        String category = product.getCategory().toLowerCase();

        // Determine the appropriate zone
        StackPane targetZone = null;
        if (category.contains("hat") || category.contains("cap")) {
            targetZone = headZone;
        } else if (category.contains("top") || category.contains("shirt") ||
                category.contains("jacket") || category.contains("outerwear")) {
            targetZone = topZone;
        } else if (category.contains("bottom") || category.contains("pant") ||
                category.contains("skirt") || category.contains("short")) {
            targetZone = bottomZone;
        } else if (category.contains("shoe") || category.contains("boot") ||
                category.contains("sneaker") || category.contains("sandal")) {
            targetZone = feetZone;
        } else if (category.contains("accessor") || category.contains("jewelry") ||
                category.contains("watch") || category.contains("belt") ||
                category.contains("scarf") || category.contains("bag")) {
            targetZone = accessoriesZone;
        }

        // Place on the determined zone
        if (targetZone != null) {
            addProductToZone(product, targetZone);
            return true;
        }

        return false;
    }
}