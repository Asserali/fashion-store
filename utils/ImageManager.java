package com.fashionstore.utils;

import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ImageManager {
    // Fixed ImageManager implementation
    public static String saveProductImageToCustomDir(String productId, String directory) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                // Ensure target directory exists
                File dir = new File(directory);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // Create unique filename with original extension
                String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf('.'));
                String fileName = "product_" + productId + extension;

                // Get absolute path for the destination file
                File destFile = new File(dir, fileName);

                // Create parent directories if needed
                if (!destFile.getParentFile().exists()) {
                    destFile.getParentFile().mkdirs();
                }

                // Copy file
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Log success
                System.out.println("Image saved successfully to: " + destFile.getAbsolutePath());

                // Return path relative to resources folder for JavaFX
                return "/images/" + fileName;
            } catch (IOException e) {
                System.err.println("Failed to save image: " + e.getMessage());
                e.printStackTrace();
                SceneManager.showErrorAlert("Image Error", "Failed to save image: " + e.getMessage());
                return null;
            }
        }
        return null;
    }
}