package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.User;
import com.fashionstore.storage.DataManager;
import com.fashionstore.utils.PasswordUtil;
import com.fashionstore.utils.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;
    @FXML
    private Button adminLoginButton;
    @FXML
    private Label errorLabel;

    private DataManager dataManager;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = FashionStoreApp.getDataManager();
        errorLabel.setVisible(false);

        // Add enter key handler to password field
        passwordField.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        // Special admin login check - always allow admin/admin
        if (username.equals("admin") && password.equals("admin")) {
            System.out.println("Admin login successful with hardcoded credentials");
            SceneManager.loadScene("AdminView.fxml");
            return;
        }

        User user = dataManager.getUserByUsername(username);
        if (user == null) {
            System.out.println("Login failed: User not found: " + username);
            showError("Invalid username or password");
            return;
        }

        System.out.println("Found user: " + username);

        // Use the new verification method in User class
        if (user.verifyPassword(password)) {
            System.out.println("Login successful for user: " + username);

            // Check if user is banned
            if (user.isBanned()) {
                // Check if the ban has expired
                if (user.isBanExpired()) {
                    // Automatically remove expired ban
                    user.unbanUser();
                    dataManager.saveAllData();

                    // Allow login to proceed
                    System.out.println("User's ban has expired, allowing login");
                } else {
                    // Ban is still active
                    String banMessage = "Your account has been banned. Reason: " + user.getBanReason();

                    // Add ban duration info if it's temporary
                    int daysLeft = user.getDaysLeftOnBan();
                    if (daysLeft > 0) {
                        banMessage += "\nYour ban will expire in " + daysLeft + " day" + (daysLeft == 1 ? "" : "s")
                                + ".";
                    } else if (daysLeft == -1) {
                        banMessage += "\nThis is a permanent ban.";
                    }

                    showError(banMessage);
                    return;
                }
            }

            // Check if account is deactivated
            if (user.isDeactivated()) {
                // If the account is deactivated but within the grace period
                if (!user.isDeactivationPeriodExpired()) {
                    int daysLeft = user.getDaysLeftUntilPermanentDeletion();
                    boolean confirm = SceneManager.showConfirmationDialog(
                            "Reactivate Account",
                            "Your account is currently deactivated.",
                            "Would you like to reactivate it? You have " + daysLeft +
                                    " day" + (daysLeft == 1 ? "" : "s") + " left before permanent deletion.");

                    if (confirm) {
                        // Reactivate the account
                        dataManager.reactivateUser(user.getUserId());
                    } else {
                        // User does not want to reactivate
                        return;
                    }
                } else {
                    // Deactivation period has expired
                    showError("This account has been permanently deleted.");
                    return;
                }
            }

            user.updateLastLogin();
            dataManager.setCurrentUser(user);

            // If it's admin, go to admin view, otherwise go to home view
            if (username.equals("admin")) {
                SceneManager.loadScene("AdminView.fxml");
            } else {
                SceneManager.loadScene("HomeView.fxml");
            }
            return;
        }

        // If we get here, password verification failed
        System.out.println("Login failed: Invalid password for user: " + username);
        showError("Invalid username or password");
    }

    @FXML
    private void showRegistration() {
        SceneManager.loadScene("RegisterView.fxml");
    }

    @FXML
    private void showAdminLogin() {
        // Clear existing fields
        usernameField.setText("admin");
        passwordField.clear();
        errorLabel.setVisible(false);

        // Set focus to password field
        Platform.runLater(() -> passwordField.requestFocus());
    }

    @FXML
    private void handleExit() {
        boolean confirm = SceneManager.showConfirmationDialog(
                "Exit Application",
                "Are you sure you want to exit?",
                "The application will close.");

        if (confirm) {
            Platform.exit();
        }
    }

    /**
     * Sets the application to dark mode
     */
    @FXML
    public void setDarkMode() {
        try {
            if (loginButton.getScene() != null) {
                // Remove light theme if present
                loginButton.getScene().getStylesheets().removeIf(
                        style -> style.contains("application.css"));

                // Add dark theme stylesheet
                String darkThemePath = getClass().getResource("/styles/dark-theme.css").toExternalForm();
                if (!loginButton.getScene().getStylesheets().contains(darkThemePath)) {
                    loginButton.getScene().getStylesheets().add(darkThemePath);
                }

                // Store theme preference
                loginButton.getScene().getRoot().getProperties().put("theme", "dark");
            }
        } catch (Exception e) {
            System.err.println("Failed to apply dark theme: " + e.getMessage());
        }
    }

    /**
     * Sets the application to light mode
     */
    @FXML
    public void setLightMode() {
        try {
            if (loginButton.getScene() != null) {
                // Remove dark theme if present
                loginButton.getScene().getStylesheets().removeIf(
                        style -> style.contains("dark-theme.css"));

                // Add light theme stylesheet
                String lightThemePath = getClass().getResource("/styles/application.css").toExternalForm();
                if (!loginButton.getScene().getStylesheets().contains(lightThemePath)) {
                    loginButton.getScene().getStylesheets().add(lightThemePath);
                }

                // Store theme preference
                loginButton.getScene().getRoot().getProperties().put("theme", "light");
            }
        } catch (Exception e) {
            System.err.println("Failed to apply light theme: " + e.getMessage());
        }
    }

    /**
     * Opens about dialog
     */
    @FXML
    public void openAbout() {
        SceneManager.showAlert("About Fashion Store",
                "Fashion Store Application\nVersion 1.0\n" +
                        "A virtual fashion store experience allowing users to create and manage outfits.");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}