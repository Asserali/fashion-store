package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.User;
import com.fashionstore.storage.DataManager;
import com.fashionstore.utils.PasswordUtil;
import com.fashionstore.utils.SceneManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button registerButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label errorLabel;

    private DataManager dataManager;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = FashionStoreApp.getDataManager();
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate input
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        // Check if username already exists
        if (dataManager.getUserByUsername(username) != null) {
            showError("Username already exists");
            return;
        }

        // Create new user
        String hashedPassword = PasswordUtil.hashPassword(password);
        User newUser = new User(username, email, hashedPassword);

        // Set first and last name if provided
        if (!firstName.isEmpty()) {
            newUser.setFirstName(firstName);
        }

        if (!lastName.isEmpty()) {
            newUser.setLastName(lastName);
        }

        try {
            // Add the user to the database
            dataManager.addUser(newUser);
            
            // Double-check that the user was saved by trying to load it
            User savedUser = dataManager.getUserByUsername(username);
            if (savedUser == null) {
                showError("Failed to save user to database. Please try again or contact support.");
                return;
            }
            
            // Save all data to ensure changes are persisted
            dataManager.saveAllData();
            
            // Log the user info for debugging
            System.out.println("Registered new user: " + username);
            System.out.println("User ID: " + newUser.getUserId());
            System.out.println("First Name: " + (firstName.isEmpty() ? "N/A" : firstName));
            System.out.println("Last Name: " + (lastName.isEmpty() ? "N/A" : lastName));
            
            // Show success alert and navigate to login
            SceneManager.showAlert("Registration Successful", "You can now login with your credentials.");
            SceneManager.loadScene("LoginView.fxml");
        } catch (Exception e) {
            System.err.println("Error registering user: " + e.getMessage());
            e.printStackTrace();
            showError("Error registering user: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        SceneManager.loadScene("LoginView.fxml");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}