package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.ShoppingCart;
import com.fashionstore.models.Product;
import com.fashionstore.storage.DataManager;
import com.fashionstore.utils.SceneManager;
import com.fashionstore.utils.WindowManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Year;
import java.util.ResourceBundle;

public class CheckoutController implements Initializable {

    @FXML
    private Label totalItemsLabel;
    @FXML
    private Label orderTotalLabel;
    @FXML
    private TextField cardNumberField;
    @FXML
    private TextField cardholderField;
    @FXML
    private TextField cvvField;
    @FXML
    private ComboBox<String> expiryMonthCombo;
    @FXML
    private ComboBox<Integer> expiryYearCombo;
    @FXML
    private Button placeOrderButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button backToCartButton;
    @FXML
    private Label cardErrorLabel;
    @FXML
    private Label cvvErrorLabel;
    @FXML
    private Label expiryErrorLabel;
    @FXML
    private ToggleGroup paymentMethodGroup;
    @FXML
    private VBox paypalEmailContainer;
    @FXML
    private TextField paypalEmailField;
    @FXML
    private Label paypalEmailErrorLabel;
    @FXML
    private VBox storePickupContainer;
    @FXML
    private ComboBox<String> storeLocationCombo;
    @FXML
    private Label storeLocationErrorLabel;

    private DataManager dataManager;
    private ShoppingCart cart;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = FashionStoreApp.getDataManager();

        if (dataManager.getCurrentUser() == null) {
            SceneManager.loadScene("LoginView.fxml");
            return;
        }

        cart = dataManager.getCart(dataManager.getCurrentUser().getUserId());
        totalItemsLabel.setText(cart.getItemCount() + " items");
        orderTotalLabel.setText(currencyFormat.format(cart.getTotalPrice()));

        setupExpiryDateFields();
        setupStoreLocations();
        setupValidation();
        hideErrorLabels();

        // Set default payment method to Credit Card
        paymentMethodGroup.selectToggle(paymentMethodGroup.getToggles().get(0));

        // Add listener for payment method changes
        paymentMethodGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            updatePaymentMethodView();
        });

        // Initialize payment method view
        updatePaymentMethodView();
    }

    private void setupStoreLocations() {
        storeLocationCombo.getItems().addAll(
                "New York - 5th Avenue",
                "Los Angeles - Beverly Hills",
                "Chicago - Magnificent Mile",
                "Miami - Lincoln Road",
                "San Francisco - Union Square");
    }

    private void updatePaymentMethodView() {
        // Clear all selection classes first using the direct reference
        paymentSection.getStyleClass().removeAll(
                "credit-card-selected",
                "paypal-selected",
                "store-pickup-selected");

        RadioButton selectedPayment = (RadioButton) paymentMethodGroup.getSelectedToggle();
        if (selectedPayment == null)
            return;

        String paymentMethod = selectedPayment.getText();

        // Add the appropriate selection class
        if ("Credit Card".equals(paymentMethod)) {
            paymentSection.getStyleClass().add("credit-card-selected");
        } else if ("PayPal".equals(paymentMethod)) {
            paymentSection.getStyleClass().add("paypal-selected");
        } else if ("In-Store Pickup".equals(paymentMethod)) {
            paymentSection.getStyleClass().add("store-pickup-selected");
        }

        // Rest of your method remains the same...
        boolean isCreditCard = "Credit Card".equals(paymentMethod);
        boolean isPayPal = "PayPal".equals(paymentMethod);
        boolean isStorePickup = "In-Store Pickup".equals(paymentMethod);

        cardNumberField.getParent().getParent().setVisible(isCreditCard);
        cardNumberField.getParent().getParent().setManaged(isCreditCard);
        paypalEmailContainer.setVisible(isPayPal);
        paypalEmailContainer.setManaged(isPayPal);
        storePickupContainer.setVisible(isStorePickup);
        storePickupContainer.setManaged(isStorePickup);
        hideErrorLabels();
    }

    @FXML
    private VBox paymentSection;

    private void hideErrorLabels() {
        cardErrorLabel.setVisible(false);
        cvvErrorLabel.setVisible(false);
        expiryErrorLabel.setVisible(false);
        paypalEmailErrorLabel.setVisible(false);
        storeLocationErrorLabel.setVisible(false);
    }

    private void setupExpiryDateFields() {
        // Setup month combo box
        expiryMonthCombo.getItems().clear();
        for (int i = 1; i <= 12; i++) {
            expiryMonthCombo.getItems().add(String.format("%02d", i));
        }

        // Setup year combo box with next 10 years
        int currentYear = Year.now().getValue();
        expiryYearCombo.getItems().clear();
        for (int i = currentYear; i <= currentYear + 10; i++) {
            expiryYearCombo.getItems().add(i);
        }

        // Default to current month/year
        int currentMonth = LocalDate.now().getMonthValue();
        expiryMonthCombo.setValue(String.format("%02d", currentMonth));
        expiryYearCombo.setValue(currentYear);
    }

    private void setupValidation() {
        // Card number validation with formatting
        cardNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            String formatted = formatCardNumber(newVal);
            if (!formatted.equals(newVal)) {
                cardNumberField.setText(formatted);
            }
            validateCardNumber();
        });

        // CVV validation
        cvvField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                cvvField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (newVal.length() > 4) {
                cvvField.setText(oldVal);
            }
            validateCVV();
        });

        // Expiry date validation
        expiryMonthCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateExpiryDate());
        expiryYearCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateExpiryDate());

        // PayPal email validation
        paypalEmailField.textProperty().addListener((obs, oldVal, newVal) -> validatePayPalEmail());

        // Store location validation
        storeLocationCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateStoreLocation());
    }

    private String formatCardNumber(String input) {
        String cleaned = input.replaceAll("[^0-9]", "");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < cleaned.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            if (i < 16) {
                formatted.append(cleaned.charAt(i));
            }
        }

        return formatted.toString();
    }

    private boolean validateStoreLocation() {
        if (storeLocationCombo.getValue() == null || storeLocationCombo.getValue().isEmpty()) {
            storeLocationErrorLabel.setText("Please select a store location");
            storeLocationErrorLabel.setVisible(true);
            return false;
        }
        storeLocationErrorLabel.setVisible(false);
        return true;
    }

    private boolean validateCardNumber() {
        String cardNumber = cardNumberField.getText().replaceAll(" ", "");

        if (cardNumber.isEmpty()) {
            cardErrorLabel.setText("Card number is required");
            cardErrorLabel.setVisible(true);
            return false;
        }

        if (cardNumber.length() != 16) {
            cardErrorLabel.setText("Card number must be 16 digits");
            cardErrorLabel.setVisible(true);
            return false;
        }

        if (!isValidLuhn(cardNumber)) {
            cardErrorLabel.setText("Invalid card number");
            cardErrorLabel.setVisible(true);
            return false;
        }

        cardErrorLabel.setVisible(false);
        return true;
    }

    private boolean validateCVV() {
        String cvv = cvvField.getText();

        if (cvv.isEmpty()) {
            cvvErrorLabel.setText("CVV is required");
            cvvErrorLabel.setVisible(true);
            return false;
        }

        if (cvv.length() < 3 || cvv.length() > 4) {
            cvvErrorLabel.setText("CVV must be 3-4 digits");
            cvvErrorLabel.setVisible(true);
            return false;
        }

        cvvErrorLabel.setVisible(false);
        return true;
    }

    private boolean validateExpiryDate() {
        if (expiryMonthCombo.getValue() == null || expiryYearCombo.getValue() == null) {
            expiryErrorLabel.setText("Expiry date is required");
            expiryErrorLabel.setVisible(true);
            return false;
        }

        int currentYear = Year.now().getValue();
        int currentMonth = LocalDate.now().getMonthValue();
        int selectedYear = expiryYearCombo.getValue();
        String selectedMonthStr = expiryMonthCombo.getValue();

        int selectedMonth;
        try {
            selectedMonth = Integer.parseInt(selectedMonthStr);
        } catch (NumberFormatException e) {
            expiryErrorLabel.setText("Invalid month format");
            expiryErrorLabel.setVisible(true);
            return false;
        }

        if (selectedYear < currentYear ||
                (selectedYear == currentYear && selectedMonth < currentMonth)) {
            expiryErrorLabel.setText("Card has expired");
            expiryErrorLabel.setVisible(true);
            return false;
        }

        expiryErrorLabel.setVisible(false);
        return true;
    }

    private boolean validatePayPalEmail() {
        String email = paypalEmailField.getText().trim();

        if (email.isEmpty()) {
            paypalEmailErrorLabel.setText("PayPal email is required");
            paypalEmailErrorLabel.setVisible(true);
            return false;
        }

        // Simple email validation pattern
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(emailRegex)) {
            paypalEmailErrorLabel.setText("Invalid email format");
            paypalEmailErrorLabel.setVisible(true);
            return false;
        }

        paypalEmailErrorLabel.setVisible(false);
        return true;
    }

    private boolean isValidLuhn(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    @FXML
    private void backToCart() {
        Stage stage = (Stage) backToCartButton.getScene().getWindow();
        stage.close();
        WindowManager.openCartWindow();
    }

    @FXML
    private void cancelCheckout() {
        if (SceneManager.showConfirmationAlert("Cancel Checkout",
                "Are you sure you want to cancel your checkout process?")) {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }

    @FXML
    private void placeOrder() {
        boolean isValid = true;

        RadioButton selectedPayment = (RadioButton) paymentMethodGroup.getSelectedToggle();
        if (selectedPayment == null) {
            SceneManager.showErrorAlert("Validation Error", "Please select a payment method.");
            return;
        }

        String paymentMethod = selectedPayment.getText();

        if ("Credit Card".equals(paymentMethod)) {
            if (!validateCardNumber())
                isValid = false;
            if (!validateCVV())
                isValid = false;
            if (!validateExpiryDate())
                isValid = false;
            if (cardholderField.getText().trim().isEmpty()) {
                isValid = false;
                SceneManager.showErrorAlert("Validation Error", "Cardholder name is required.");
            }
        } else if ("PayPal".equals(paymentMethod)) {
            if (!validatePayPalEmail())
                isValid = false;
        } else if ("In-Store Pickup".equals(paymentMethod)) {
            if (!validateStoreLocation())
                isValid = false;
        }

        if (!isValid) {
            SceneManager.showErrorAlert("Validation Error", "Please fix the highlighted errors.");
            return;
        }

        // Process the order based on payment method
        String confirmationMessage = processOrder(paymentMethod);

        // Show success message and close the checkout window
        SceneManager.showAlert("Order Placed", confirmationMessage);
        Stage stage = (Stage) placeOrderButton.getScene().getWindow();
        stage.close();
    }

    private String processOrder(String paymentMethod) {
        StringBuilder confirmation = new StringBuilder("Your order has been successfully placed!\n\n");

        if ("Credit Card".equals(paymentMethod)) {
            confirmation.append("Payment Method: Credit Card (ending with ")
                    .append(cardNumberField.getText().substring(cardNumberField.getText().length() - 4))
                    .append(")\n");
        } else if ("PayPal".equals(paymentMethod)) {
            confirmation.append("Payment Method: PayPal (")
                    .append(paypalEmailField.getText())
                    .append(")\n");
        } else if ("In-Store Pickup".equals(paymentMethod)) {
            confirmation.append("Pickup Method: In-Store\n")
                    .append("Location: ").append(storeLocationCombo.getValue()).append("\n")
                    .append("Please bring your order confirmation and ID when picking up.\n");
        }

        confirmation.append("\nOrder Total: ").append(currencyFormat.format(cart.getTotalPrice()));

        // Add purchased items to wardrobe and decrease stock
        if (dataManager.getCurrentUser() != null) {
            for (ShoppingCart.CartItem item : cart.getItems()) {
                Product product = item.getProduct();
                int quantity = item.getQuantity();

                // Decrease product stock for each item in the cart based on quantity
                int currentStock = product.getStockQuantity();
                int newStock = Math.max(0, currentStock - quantity);
                product.setStockQuantity(newStock);

                // Update the product in database to reflect stock change
                dataManager.updateProduct(product);

                // Add each product to the user's wardrobe
                dataManager.getCurrentUser().addToWardrobe(product.getProductId());
            }

            // Append purchased items to confirmation
            confirmation.append("\n\nItems added to your wardrobe:");
            for (ShoppingCart.CartItem item : cart.getItems()) {
                confirmation.append("\n- ").append(item.getProduct().getName());
                if (item.getQuantity() > 1) {
                    confirmation.append(" (").append(item.getQuantity()).append(")");
                }
            }
        }

        // Clear the cart
        cart.clear();
        dataManager.saveCart(cart);
        dataManager.saveAllData();

        // Update cart counter in HomeController
        updateHomeControllerCart();

        return confirmation.toString();
    }

    // Method to update HomeController's cart counter
    private void updateHomeControllerCart() {
        try {
            // Refresh all open views to show updated stock levels
            WindowManager.refreshHomeView();
        } catch (Exception e) {
            System.err.println("Failed to update views: " + e.getMessage());
        }
    }
}