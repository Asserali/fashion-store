package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.Outfit;
import com.fashionstore.models.Product;
import com.fashionstore.storage.DataManager;
import com.fashionstore.ui.components.ClothingItemView;
import com.fashionstore.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the outfit detail view.
 * Shows all items in an outfit and provides options to edit, delete, or "wear"
 * the outfit.
 */
public class OutfitDetailController implements Initializable {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Label outfitNameLabel;
    @FXML
    private Label outfitInfoLabel;
    @FXML
    private Label outfitDescriptionLabel;
    @FXML
    private FlowPane outfitItemsPane;
    @FXML
    private Button deleteButton;
    @FXML
    private Button editButton;

    private Outfit outfit;
    private DataManager dataManager;
    private MyOutfitsController parentController;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dataManager = FashionStoreApp.getDataManager();
    }

    /**
     * Sets the outfit to display and loads its details
     */
    public void setOutfit(Outfit outfit, MyOutfitsController parentController) {
        this.outfit = outfit;
        this.parentController = parentController;
        loadOutfitDetails();
    }

    /**
     * Loads all outfit details and populates the view
     */
    private void loadOutfitDetails() {
        if (outfit == null) {
            SceneManager.showErrorAlert("Error", "No outfit to display");
            return;
        }

        // Set basic outfit information
        outfitNameLabel.setText(outfit.getName());
        outfitInfoLabel.setText(String.format("Created on: %s | %s | %s",
                DATE_FORMAT.format(outfit.getCreatedAt()),
                outfit.getSeason().toString().replace("_", " "),
                outfit.getOccasion().toString().replace("_", " ")));

        // Set description if available
        if (outfit.getDescription() != null && !outfit.getDescription().isEmpty()) {
            outfitDescriptionLabel.setText(outfit.getDescription());
        } else {
            outfitDescriptionLabel.setText("No description available");
        }

        // Load all products
        loadOutfitItems();
    }

    /**
     * Loads all clothing items in the outfit
     */
    private void loadOutfitItems() {
        outfitItemsPane.getChildren().clear();

        List<Product> products = dataManager.getProductsForOutfit(outfit);

        if (products.isEmpty()) {
            Label emptyLabel = new Label("No items in this outfit");
            outfitItemsPane.getChildren().add(emptyLabel);

            // Prompt user to delete empty outfit
            boolean deleteEmptyOutfit = SceneManager.showConfirmationDialog(
                    "Empty Outfit",
                    "This outfit has no items.",
                    "Would you like to delete this empty outfit?");

            if (deleteEmptyOutfit) {
                System.out.println("User chose to delete empty outfit: " + outfit.getOutfitId());
                deleteOutfit(); // Call existing delete method
                return;
            }

            return;
        }

        // Create and add clothing item views
        for (Product product : products) {
            ClothingItemView itemView = new ClothingItemView(product, false);
            itemView.setOnMouseClicked(e -> {
                // Show product details when clicked
                SceneManager.showAlert("Product Details",
                        String.format("%s\nCategory: %s\nPrice: $%.2f",
                                product.getName(),
                                product.getCategory(),
                                product.getPrice()));
            });
            outfitItemsPane.getChildren().add(itemView);
        }
    }

    /**
     * Handles the back button - returns to outfits list
     */
    @FXML
    private void goBack() {
        closeWindow();
    }

    /**
     * Handles the delete button - removes the current outfit
     */
    @FXML
    private void deleteOutfit() {
        if (outfit == null) {
            System.err.println("Cannot delete: outfit is null");
            SceneManager.showErrorAlert("Error", "No outfit to delete.");
            return;
        }

        System.out.println("Attempting to delete outfit: " + outfit.getOutfitId() + " - " + outfit.getName());

        // Ask for confirmation
        boolean confirmed = SceneManager.showConfirmationDialog(
                "Confirm Delete",
                "Are you sure you want to delete this outfit?",
                "This action cannot be undone.");

        if (confirmed) {
            System.out.println("Delete confirmed by user. Proceeding...");

            try {
                // Delete the outfit
                boolean success = dataManager.removeOutfit(outfit.getOutfitId());
                System.out.println("DataManager.removeOutfit returned: " + success);

                if (success) {
                    // Also remove it from the user's collection
                    if (dataManager.getCurrentUser() != null) {
                        System.out.println("Removing from current user's collection");
                        dataManager.getCurrentUser().removeOutfit(outfit.getOutfitId());
                        dataManager.saveAllData();
                        System.out.println("Changes saved to data store");
                    } else {
                        System.err.println(
                                "Warning: CurrentUser is null, outfit removed from global store but not from user collection");
                    }

                    // Show confirmation and close
                    SceneManager.showAlert("Success", "Outfit deleted successfully!");

                    // Refresh parent controller if available
                    if (parentController != null) {
                        try {
                            System.out.println("Refreshing parent controller");
                            parentController.loadOutfits();
                        } catch (Exception e) {
                            System.err.println("Warning: Failed to refresh outfits: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("Warning: ParentController is null, cannot refresh outfit list");
                    }

                    // Close this window
                    System.out.println("Closing detail window");
                    closeWindow();
                } else {
                    System.err.println("Error: dataManager.removeOutfit returned false");
                    SceneManager.showErrorAlert("Error", "Failed to delete outfit.");
                }
            } catch (Exception e) {
                System.err.println("Exception during outfit deletion: " + e.getMessage());
                e.printStackTrace();
                SceneManager.showErrorAlert("Error", "An error occurred while deleting the outfit: " + e.getMessage());
            }
        } else {
            System.out.println("Delete canceled by user");
        }
    }

    /**
     * Opens the outfit creator to edit this outfit
     */
    @FXML
    private void editOutfit() {
        if (outfit == null) {
            System.err.println("Cannot edit: outfit is null");
            SceneManager.showErrorAlert("Error", "No outfit to edit.");
            return;
        }

        System.out.println("Attempting to edit outfit: " + outfit.getOutfitId() + " - " + outfit.getName());

        // Launch the outfit creator in edit mode
        SceneManager.showOutfitCreatorForEdit(outfit, result -> {
            System.out.println("Edit result: " + result);
            if (result) {
                try {
                    // Reload outfit details if edited successfully
                    System.out.println("Reloading outfit after edit");
                    outfit = dataManager.getOutfit(outfit.getOutfitId());
                    loadOutfitDetails();

                    // Refresh parent controller if available
                    if (parentController != null) {
                        System.out.println("Refreshing parent controller");
                        parentController.loadOutfits();
                    } else {
                        System.err.println("Warning: ParentController is null, cannot refresh outfit list");
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Error updating outfit details: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Closes the window
     */
    private void closeWindow() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }
}