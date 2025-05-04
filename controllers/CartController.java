package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.ShoppingCart;
import com.fashionstore.storage.DataManager;
import com.fashionstore.utils.SceneManager;
import com.fashionstore.utils.WindowManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.scene.control.SpinnerValueFactory;

public class CartController implements Initializable {

    @FXML
    private TableView<ShoppingCart.CartItem> cartItemsTable;
    @FXML
    private TableColumn<ShoppingCart.CartItem, String> itemColumn;
    @FXML
    private TableColumn<ShoppingCart.CartItem, String> priceColumn;
    @FXML
    private TableColumn<ShoppingCart.CartItem, Integer> quantityColumn;
    @FXML
    private TableColumn<ShoppingCart.CartItem, String> subtotalColumn;
    @FXML
    private TableColumn<ShoppingCart.CartItem, Button> actionsColumn;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private Label emptyCartLabel;
    @FXML
    private Button checkoutButton;
    @FXML
    private Button clearCartButton;
    @FXML
    private Button continueShoppingButton;
    @FXML
    private Button backToShoppingButton;

    private DataManager dataManager;
    private ShoppingCart cart;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("CartController: Initializing cart view");
        
        try {
            // Initialize the data manager
            initializeDataManager();
            System.out.println("CartController: Data manager initialized");
            
            // Load the cart data
            initializeCart();
            System.out.println("CartController: Cart initialized with " + 
                              (cart != null ? cart.getItems().size() : 0) + " items");
            
            // Set up the table columns
            setupTableColumns();
            System.out.println("CartController: Table columns configured");
            
            // Refresh the cart display
            refreshCartDisplay();
            System.out.println("CartController: Initial cart display refreshed");
            
        } catch (Exception e) {
            System.err.println("CartController: Initialization error: " + e.getMessage());
            e.printStackTrace();
            handleInitializationError(e);
        }
    }

    private void initializeDataManager() {
        dataManager = FashionStoreApp.getDataManager();
        if (dataManager == null || dataManager.getCurrentUser() == null) {
            SceneManager.loadScene("LoginView.fxml");
            throw new IllegalStateException("User not logged in");
        }
    }

    private void initializeCart() {
        cart = dataManager.getCart(dataManager.getCurrentUser().getUserId());
        if (cart == null) {
            cart = new ShoppingCart(dataManager.getCurrentUser().getUserId());
            dataManager.saveCart(cart);
        }
    }

    private void setupTableColumns() {
        itemColumn
                .setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProduct().getName()));

        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                currencyFormat.format(cellData.getValue().getProduct().getPrice())));

        quantityColumn.setCellValueFactory(
                cellData -> new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());

        quantityColumn.setCellFactory(createQuantityCellFactory());

        subtotalColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(currencyFormat.format(cellData.getValue().getTotalPrice())));

        actionsColumn.setCellFactory(createActionsCellFactory());
    }

    private Callback<TableColumn<ShoppingCart.CartItem, Integer>, TableCell<ShoppingCart.CartItem, Integer>> createQuantityCellFactory() {
        return column -> new TableCell<ShoppingCart.CartItem, Integer>() {
            private final Spinner<Integer> spinner = new Spinner<>(1, 100, 1);

            {
                spinner.setEditable(true);
                
                // Configure the spinner value factory to properly handle changes
                SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = 
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
                spinner.setValueFactory(valueFactory);
                
                // Add proper change listener
                spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                    if (newValue == null || oldValue == null || oldValue.equals(newValue)) {
                        return; // No actual change
                    }
                    
                    int index = getIndex();
                    if (index < 0 || index >= getTableView().getItems().size()) {
                        return; // Invalid index
                    }
                    
                    // Get the item from the table
                    ShoppingCart.CartItem item = getTableView().getItems().get(index);
                    if (item != null) {
                        System.out.println("Spinner changed for item: " + 
                                          (item.getProduct() != null ? item.getProduct().getName() : "null") + 
                                          " from " + oldValue + " to " + newValue);
                        
                        // Update the item quantity
                        item.setQuantity(newValue);
                        
                        // Save the cart state
                        dataManager.saveCart(cart);
                        
                        // Update the UI
                        javafx.application.Platform.runLater(() -> {
                            // Update subtotals and other UI elements
                            updateCart();
                            
                            // Force refresh of the table to update subtotals
                            getTableView().refresh();
                        });
                    }
                });
            }

            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                
                // Clear the graphic first
                setGraphic(null);
                
                if (empty || quantity == null) {
                    return;
                }
                
                // Set the current quantity value
                spinner.getValueFactory().setValue(quantity);
                
                // Set the spinner as the cell's graphic
                setGraphic(spinner);
            }
        };
    }

    private Callback<TableColumn<ShoppingCart.CartItem, Button>, TableCell<ShoppingCart.CartItem, Button>> createActionsCellFactory() {
        return column -> new TableCell<ShoppingCart.CartItem, Button>() {
            // Create a new button for each row
            private final Button button = new Button("Remove");

            @Override
            protected void updateItem(Button item, boolean empty) {
                super.updateItem(item, empty);
                
                // Always clear the graphic first
                setGraphic(null);
                
                if (empty) {
                    return;
                }
                
                // Style the button
                button.getStyleClass().add("remove-button");
                button.setMaxWidth(Double.MAX_VALUE);
                
                // Set the button action directly here
                button.setOnAction(event -> {
                    // Get the item directly from this cell's table row
                    int idx = getIndex();
                    System.out.println("Remove button clicked at index: " + idx);
                    
                    // Safety check the table view
                    if (getTableView() == null) {
                        System.err.println("TableView is null");
                        return;
                    }
                    
                    // Safety check the table's items
                    if (getTableView().getItems() == null) {
                        System.err.println("Items list is null");
                        return;
                    }
                    
                    // Get the item
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        ShoppingCart.CartItem cartItem = getTableView().getItems().get(idx);
                        if (cartItem != null && cartItem.getProduct() != null) {
                            String prodId = cartItem.getProduct().getProductId();
                            String prodName = cartItem.getProduct().getName();
                            int qty = cartItem.getQuantity();
                            
                            System.out.println("Removing item: " + prodName + 
                                              " (ID: " + prodId + ", quantity: " + qty + ")");
                            
                            // Use the dedicated removeFromCart method for more robust handling
                            removeFromCart(prodId);
                        }
                    }
                });
                
                // Add the button to the cell
                setGraphic(button);
            }
        };
    }

    /**
     * Completely rebuilds the cart display from the current cart data.
     */
    private void refreshCartDisplay() {
        System.out.println("CartController.refreshCartDisplay: Rebuilding cart display");
        
        try {
            // Get the latest cart items
            List<ShoppingCart.CartItem> items = cart.getItems();
            System.out.println("CartController.refreshCartDisplay: Cart has " + items.size() + " items");
            
            // Create a fresh observable list
            javafx.collections.ObservableList<ShoppingCart.CartItem> observableItems = 
                javafx.collections.FXCollections.observableArrayList(items);
            
            // Set the new items to the table
            cartItemsTable.setItems(observableItems);
            
            // Update labels, buttons, etc.
            updateCart();
            
            // Force a refresh
            cartItemsTable.refresh();
            
            System.out.println("CartController.refreshCartDisplay: Display updated with " + items.size() + " items");
        } catch (Exception e) {
            System.err.println("CartController.refreshCartDisplay: Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates all cart-related UI elements (total, buttons, labels)
     * and saves cart state to the data manager.
     */
    private void updateCart() {
        try {
            // Recalculate and display the total price
            totalAmountLabel.setText(currencyFormat.format(cart.getTotalPrice()));
            
            // Check if the cart is empty
            boolean isEmpty = cart.isEmpty();
            
            // Show/hide empty cart message
            emptyCartLabel.setVisible(isEmpty);
            
            // Enable/disable buttons based on cart state
            clearCartButton.setDisable(isEmpty);
            checkoutButton.setDisable(isEmpty);
            
            // Save cart changes to data store
            dataManager.saveCart(cart);
            
            // Log the cart status
            System.out.println("CartController.updateCart: Updated UI for cart with " + 
                              cart.getItems().size() + " items, total: " + 
                              currencyFormat.format(cart.getTotalPrice()));
        } catch (Exception e) {
            System.err.println("CartController.updateCart: Error updating cart UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Removes an item completely from the cart, regardless of quantity.
     * 
     * @param productId The ID of the product to remove
     */
    private void removeFromCart(String productId) {
        if (productId == null || productId.isEmpty()) {
            System.err.println("Cannot remove: null or empty product ID");
            return;
        }

        System.out.println("CartController.removeFromCart: Removing product ID: " + productId);
        
        try {
            // Remove the item from the cart
            boolean removed = cart.removeItem(productId);
            
            if (removed) {
                System.out.println("CartController.removeFromCart: Item successfully removed");
                
                // Save the updated cart
                dataManager.saveCart(cart);
                System.out.println("CartController.removeFromCart: Cart saved to data store");
                
                // Update the UI using refreshCartDisplay for a complete refresh
                refreshCartDisplay();
            } else {
                System.err.println("CartController.removeFromCart: Failed to remove item");
                
                // Try again with a fresh cart
                System.out.println("CartController.removeFromCart: Retrying with fresh cart");
                
                // Get the cart again from data store
                String userId = dataManager.getCurrentUser().getUserId();
                cart = dataManager.getCart(userId);
                
                // Try removal again
                if (cart.removeItem(productId)) {
                    System.out.println("CartController.removeFromCart: Second attempt successful");
                    dataManager.saveCart(cart);
                    
                    // Update UI
                    refreshCartDisplay();
                } else {
                    System.err.println("CartController.removeFromCart: Second attempt also failed");
                    
                    // Show an error message to the user
                    javafx.application.Platform.runLater(() -> {
                        SceneManager.showErrorAlert("Error", "Failed to remove item from cart. Please try again.");
                    });
                }
            }
            
        } catch (Exception e) {
            System.err.println("CartController.removeFromCart: Error: " + e.getMessage());
            e.printStackTrace();
            
            // Try to recover by refreshing the display
            try {
                refreshCartDisplay();
            } catch (Exception ex) {
                System.err.println("CartController.removeFromCart: Failed to refresh display: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void clearCart() {
        if (cart.isEmpty())
            return;

        // Use SceneManager for consistent styling
        SceneManager.showConfirmationAlert("Clear Cart",
                "Are you sure you want to remove all items?",
                () -> {
                    // On confirm
                    cart.clear();
                    cartItemsTable.getItems().clear();
                    updateCart();
                },
                null); // No action on cancel
    }

    @FXML
    private void continueShopping() {
        Button sourceButton = continueShoppingButton;
        if (backToShoppingButton != null) {
            sourceButton = backToShoppingButton;
        } else if (continueShoppingButton != null) {
            sourceButton = continueShoppingButton;
        }

        if (sourceButton != null && sourceButton.getScene() != null) {
            ((Stage) sourceButton.getScene().getWindow()).close();
        }
    }

    @FXML
    private void proceedToCheckout() {
        if (cart.isEmpty()) {
            SceneManager.showAlert("Empty Cart", "Your cart is empty. Add items before checkout.");
            return;
        }
        ((Stage) checkoutButton.getScene().getWindow()).close();
        WindowManager.openCheckoutWindow();
    }

    private void handleInitializationError(Exception e) {
        SceneManager.showErrorAlert("Initialization Error", "Failed to initialize cart: " + e.getMessage());
        e.printStackTrace();
    }
}