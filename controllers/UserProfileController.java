package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.User;
import com.fashionstore.storage.DataManager;
import com.fashionstore.utils.PasswordUtil;
import com.fashionstore.utils.SceneManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class UserProfileController implements Initializable {

    @FXML
    private Label usernameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private Label createdDateLabel;
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private CheckBox darkModeCheckbox;
    @FXML
    private Button deactivateAccountButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Label preferencesStatusLabel;

    private DataManager dataManager;
    private User currentUser;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = FashionStoreApp.getDataManager();
        currentUser = dataManager.getCurrentUser();

        if (currentUser == null) {
            SceneManager.loadScene("LoginView.fxml");
            return;
        }

        // Populate user information
        usernameLabel.setText(currentUser.getUsername());
        emailLabel.setText(currentUser.getEmail());
        firstNameField.setText(currentUser.getFirstName());
        lastNameField.setText(currentUser.getLastName());

        if (currentUser.getDateRegistered() != null) {
            createdDateLabel.setText(dateFormat.format(currentUser.getDateRegistered()));
        } else {
            createdDateLabel.setText("N/A");
        }

        // Set dark mode checkbox based on user preference
        darkModeCheckbox.setSelected(currentUser.isDarkModeEnabled());

        // Disable deactivate button if it's an admin
        if (currentUser.getUsername().equals("admin")) {
            deactivateAccountButton.setDisable(true);
        }

        // Apply the appropriate theme
        applyTheme();
    }

    @FXML
    private void saveProfileChanges() {
        boolean hasChanges = false;
        boolean hasPasswordChange = false;

        // Check if first name or last name changed
        if (!firstNameField.getText().equals(currentUser.getFirstName())) {
            currentUser.setFirstName(firstNameField.getText());
            hasChanges = true;
        }

        if (!lastNameField.getText().equals(currentUser.getLastName())) {
            currentUser.setLastName(lastNameField.getText());
            hasChanges = true;
        }

        // Check if password fields are filled
        if (!currentPasswordField.getText().isEmpty() && !newPasswordField.getText().isEmpty()) {
            // Verify current password
            if (currentUser.verifyPassword(currentPasswordField.getText())) {
                try {
                    // Hash the new password and set it
                    String hashedPassword = PasswordUtil.hashPassword(newPasswordField.getText());
                    currentUser.setPasswordHash(hashedPassword);
                    hasChanges = true;
                    hasPasswordChange = true;

                    // Clear password fields
                    currentPasswordField.clear();
                    newPasswordField.clear();
                } catch (Exception e) {
                    statusLabel.setText("Error: Failed to update password");
                    return;
                }
            } else {
                statusLabel.setText("Error: Current password is incorrect");
                return;
            }
        } else if (!currentPasswordField.getText().isEmpty() || !newPasswordField.getText().isEmpty()) {
            // One password field is filled but not the other
            statusLabel.setText("Error: Please fill both password fields to change password");
            return;
        }

        if (hasChanges) {
            // Save changes
            dataManager.saveAllData();

            if (hasPasswordChange) {
                statusLabel.setText("Profile and password updated successfully");
            } else {
                statusLabel.setText("Profile updated successfully");
            }
        } else {
            statusLabel.setText("No changes made");
        }
    }

    @FXML
    private void savePreferences() {
        // Save the dark mode preference
        dataManager.saveUserDarkModePreference(currentUser.getUserId(), darkModeCheckbox.isSelected());
        preferencesStatusLabel.setText("Preferences saved successfully");
    }

    @FXML
    private void toggleDarkMode() {
        // Apply theme based on checkbox
        applyTheme();

        // Save preference
        dataManager.saveUserDarkModePreference(currentUser.getUserId(), darkModeCheckbox.isSelected());
    }

    private void applyTheme() {
        Scene scene = darkModeCheckbox.getScene();
        if (scene == null)
            return;

        // Clear all existing stylesheets first
        scene.getStylesheets().clear();

        if (darkModeCheckbox.isSelected()) {
            // Apply dark theme
            scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
            scene.getRoot().getProperties().put("theme", "dark");
        } else {
            // Apply light theme
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            scene.getRoot().getProperties().put("theme", "light");
        }
    }

    @FXML
    private void handleDeactivateAccount() {
        // Show confirmation dialog
        boolean confirm = SceneManager.showConfirmationDialog(
                "Confirm Deactivation",
                "Are you sure you want to deactivate your account?",
                "Your account will be hidden for 7 days after which it will be permanently deleted if not reactivated.");

        if (confirm) {
            // Deactivate the account
            dataManager.deactivateUser(currentUser.getUserId());

            // Show confirmation and log the user out
            SceneManager.showAlert("Account Deactivated",
                    "Your account has been deactivated. You can reactivate it within 7 days by logging in.");

            // Log out
            dataManager.setCurrentUser(null);
            SceneManager.loadScene("LoginView.fxml");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.loadScene("HomeView.fxml");
    }
}