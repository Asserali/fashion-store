package com.fashionstore.ui.components;

import com.fashionstore.models.Product;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;

public class StoreItemView extends VBox {

    private static final double IMAGE_WIDTH = 260;
    private static final double IMAGE_HEIGHT = 260;
    private static final double CORNER_RADIUS = 12;

    private final Product product;
    private final Button purchaseButton;
    private final ImageView imageView;
    private final StackPane imageContainer;

    public StoreItemView(Product product) {
        this.product = product;
        this.imageView = new ImageView();
        this.imageContainer = new StackPane();
        this.purchaseButton = createPurchaseButton();

        setupContainer();
        configureImageView();
        VBox detailsBox = createDetailsBox();
        getChildren().addAll(imageContainer, detailsBox);

        detailsBox.getChildren().add(purchaseButton);

        // Set out-of-stock status after everything is initialized
        if (product.getStockQuantity() <= 0) {
            purchaseButton.setDisable(true);
            purchaseButton.setText("Out of Stock");
        }

        setupInteractiveEffects();
    }

    private void setupContainer() {
        setMinWidth(240);
        setPrefWidth(280);
        setMaxWidth(320);
        setMinHeight(380);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(0));
        setSpacing(0);
        getStyleClass().addAll("store-item", "product-card");
        VBox.setVgrow(this, Priority.ALWAYS);

        // Add drop shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(4);
        dropShadow.setOffsetX(0);
        dropShadow.setOffsetY(2);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.1));
        setEffect(dropShadow);
    }

    private void configureImageView() {
        imageView.setPreserveRatio(false); // Allow cropping
        imageView.setSmooth(true);
        imageView.setFitWidth(IMAGE_WIDTH);
        imageView.setFitHeight(IMAGE_HEIGHT);

        imageContainer.getChildren().add(imageView);
        imageContainer.setMinSize(IMAGE_WIDTH, IMAGE_HEIGHT);
        imageContainer.setPrefSize(IMAGE_WIDTH, IMAGE_HEIGHT);
        imageContainer.getStyleClass().add("image-container");

        applyClippingMask();
        loadProductImage();
    }

    private void applyClippingMask() {
        Rectangle clip = new Rectangle(IMAGE_WIDTH, IMAGE_HEIGHT);
        clip.setArcWidth(CORNER_RADIUS);
        clip.setArcHeight(CORNER_RADIUS);
        imageView.setClip(clip);
    }

    private void loadProductImage() {
        try {
            String imagePath = product.getImagePath();
            System.out.println("Loading image: " + imagePath);

            if (imagePath == null || imagePath.isEmpty()) {
                imagePath = "/images/default-product.png";
            }

            // Try loading from resources (works in JAR)
            InputStream resourceStream = getClass().getResourceAsStream(imagePath);
            if (resourceStream != null) {
                Image image = new Image(resourceStream);
                processAndDisplayImage(image);
                return;
            }

            // Fallback to file system (works in development)
            File imageFile = new File("src/main/resources" + imagePath);
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                processAndDisplayImage(image);
                return;
            }

            System.err.println("Image not found at: " + imagePath);
            createPlaceholder();
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            createPlaceholder();
        }
    }

    private void processAndDisplayImage(Image image) {
        // Calculate scaling factors
        double widthRatio = IMAGE_WIDTH / image.getWidth();
        double heightRatio = IMAGE_HEIGHT / image.getHeight();
        double scale = Math.max(widthRatio, heightRatio);

        // Center the image viewport
        double viewportWidth = IMAGE_WIDTH / scale;
        double viewportHeight = IMAGE_HEIGHT / scale;
        double viewportX = (image.getWidth() - viewportWidth) / 2;
        double viewportY = (image.getHeight() - viewportHeight) / 2;

        // Apply viewport for cropping
        imageView.setViewport(new javafx.geometry.Rectangle2D(
                viewportX, viewportY, viewportWidth, viewportHeight));

        setImageWithAnimation(image);
    }

    private void setImageWithAnimation(Image image) {
        // Clear any existing placeholder
        imageContainer.getChildren().removeIf(node -> node instanceof Rectangle || node instanceof Label);

        // Configure animations
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), imageView);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1.0);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), imageView);
        scaleIn.setFromX(0.95);
        scaleIn.setFromY(0.95);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);

        // Set image after clearing previous
        imageView.setImage(image);

        // Play animations
        fadeIn.play();
        scaleIn.play();
    }

    private void createPlaceholder() {
        Rectangle placeholder = new Rectangle(IMAGE_WIDTH, IMAGE_HEIGHT);
        placeholder.setArcWidth(CORNER_RADIUS);
        placeholder.setArcHeight(CORNER_RADIUS);

        if (product.getColor() != null && !product.getColor().isEmpty()) {
            try {
                placeholder.setFill(Color.web(mapColorNameToHex(product.getColor())));
            } catch (Exception e) {
                placeholder.setStyle("-fx-fill: linear-gradient(to bottom right, #f0f0f0, #d0d0d0);");
            }
        } else {
            placeholder.setStyle("-fx-fill: linear-gradient(to bottom right, #f0f0f0, #d0d0d0);");
        }

        Label initialLabel = new Label(product.getName().substring(0, 1).toUpperCase());
        initialLabel.getStyleClass().add("placeholder-initial");

        imageContainer.getChildren().clear();
        imageContainer.getChildren().addAll(placeholder, initialLabel);
    }

    private VBox createDetailsBox() {
        VBox detailsBox = new VBox(8);
        detailsBox.setPadding(new Insets(15, 10, 15, 10));
        detailsBox.setAlignment(Pos.TOP_LEFT);
        detailsBox.getStyleClass().add("product-details");

        Label nameLabel = new Label(product.getName());
        nameLabel.setWrapText(true);
        nameLabel.maxWidthProperty().bind(widthProperty().subtract(20));
        nameLabel.getStyleClass().add("item-name");

        Label priceLabel = new Label(formatPrice(product.getPrice()));
        priceLabel.getStyleClass().add("item-price");

        // Add stock information
        Label stockLabel = createStockLabel();

        Label infoLabel = new Label(buildInfoText());
        infoLabel.setWrapText(true);
        infoLabel.maxWidthProperty().bind(widthProperty().subtract(20));
        infoLabel.getStyleClass().add("item-info");

        Region spacer = new Region();
        spacer.setMinHeight(8);
        VBox.setVgrow(spacer, Priority.ALWAYS);

        detailsBox.getChildren().addAll(nameLabel, priceLabel, stockLabel, infoLabel, spacer);
        return detailsBox;
    }

    private Label createStockLabel() {
        Label stockLabel = new Label();
        int stockQty = product.getStockQuantity();

        if (stockQty <= 0) {
            stockLabel.setText("Out of Stock");
            stockLabel.getStyleClass().add("stock-out");
            // Don't set purchase button properties here, do it in the constructor after
            // initialization
        } else if (stockQty < 5) {
            stockLabel.setText("Low Stock: " + stockQty + " left");
            stockLabel.getStyleClass().add("stock-low");
        } else {
            stockLabel.setText("In Stock: " + stockQty);
            stockLabel.getStyleClass().add("stock-available");
        }

        return stockLabel;
    }

    private Button createPurchaseButton() {
        Button button = new Button("Add to Cart");
        button.getStyleClass().add("add-to-cart-button");
        return button;
    }

    private String formatPrice(BigDecimal price) {
        return String.format("$%.2f", price);
    }

    private String buildInfoText() {
        StringBuilder info = new StringBuilder();

        if (product.getBrand() != null && !product.getBrand().isEmpty()) {
            info.append(product.getBrand());
        }

        if (product.getCategory() != null && !product.getCategory().isEmpty()) {
            if (info.length() > 0)
                info.append(" • ");
            info.append(product.getCategory());
        }

        if (product.getColor() != null && !product.getColor().isEmpty()) {
            if (info.length() > 0)
                info.append(" • ");
            info.append(product.getColor());
        }

        return info.toString();
    }

    private void setDefaultGradient(Rectangle placeholder) {
        placeholder.setStyle("-fx-fill: linear-gradient(to bottom right, #f0f0f0, #d0d0d0);");
    }

    private String mapColorNameToHex(String colorName) {
        // Extended color mapping with more fashion colors
        switch (colorName.toLowerCase()) {
            case "black":
                return "#000000";
            case "white":
                return "#FFFFFF";
            case "red":
                return "#E53935";
            case "green":
                return "#43A047";
            case "blue":
                return "#1E88E5";
            case "yellow":
                return "#FDD835";
            case "purple":
                return "#8E24AA";
            case "orange":
                return "#FF9800";
            case "pink":
                return "#EC407A";
            case "gray":
            case "grey":
                return "#757575";
            case "brown":
                return "#795548";
            case "navy":
                return "#1A237E";
            case "beige":
                return "#F5F5DC";
            case "teal":
                return "#009688";
            case "olive":
                return "#808000";
            case "maroon":
                return "#800000";
            case "khaki":
                return "#F0E68C";
            case "coral":
                return "#FF7F50";
            case "turquoise":
                return "#40E0D0";
            case "lavender":
                return "#E6E6FA";
            default:
                return "#DDDDDD";
        }
    }

    private void setupInteractiveEffects() {
        // Mouse enter effect
        setOnMouseEntered(e -> {
            DropShadow enhancedShadow = new DropShadow();
            enhancedShadow.setRadius(10);
            enhancedShadow.setOffsetY(3);
            enhancedShadow.setColor(Color.rgb(0, 0, 0, 0.2));

            // Create smooth animation for shadow change
            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), this);
            fadeIn.setFromValue(0.97);
            fadeIn.setToValue(1.0);

            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(250), this);
            scaleUp.setFromX(1.0);
            scaleUp.setFromY(1.0);
            scaleUp.setToX(1.02);
            scaleUp.setToY(1.02);

            setEffect(enhancedShadow);
            fadeIn.play();
            scaleUp.play();
        });

        // Mouse exit effect
        setOnMouseExited(e -> {
            DropShadow normalShadow = new DropShadow();
            normalShadow.setRadius(4);
            normalShadow.setOffsetY(2);
            normalShadow.setColor(Color.rgb(0, 0, 0, 0.1));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), this);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.97);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(250), this);
            scaleDown.setFromX(1.02);
            scaleDown.setFromY(1.02);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);

            setEffect(normalShadow);
            fadeOut.play();
            scaleDown.play();
        });

        // Button hover effect
        purchaseButton.setOnMouseEntered(e -> purchaseButton.getStyleClass().add("button-hover"));
        purchaseButton.setOnMouseExited(e -> purchaseButton.getStyleClass().remove("button-hover"));
    }

    public Product getProduct() {
        return product;
    }

    public void setOnPurchase(EventHandler<ActionEvent> handler) {
        purchaseButton.setOnAction(handler);
    }

    /**
     * Marks this product view as out of stock, disabling the purchase button
     * and updating visual indicators.
     */
    public void markAsOutOfStock() {
        if (purchaseButton != null) {
            purchaseButton.setDisable(true);
            purchaseButton.setText("Out of Stock");
        }

        // Add a visual overlay to the image if not already done
        if (imageContainer != null &&
                !imageContainer.getChildren().stream()
                        .anyMatch(node -> node.getId() != null && node.getId().equals("outOfStockOverlay"))) {
            Rectangle overlay = new Rectangle(IMAGE_WIDTH, IMAGE_HEIGHT);
            overlay.setId("outOfStockOverlay");
            overlay.setFill(Color.rgb(0, 0, 0, 0.5)); // Semi-transparent black

            Label outOfStockLabel = new Label("OUT OF STOCK");
            outOfStockLabel.getStyleClass().add("out-of-stock-label");
            outOfStockLabel.setTextFill(Color.WHITE);
            outOfStockLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

            imageContainer.getChildren().addAll(overlay, outOfStockLabel);
        }
    }
}