package com.fashionstore.ui.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Generates a human silhouette image programmatically.
 * This is used as a fallback if we don't have an actual silhouette image file.
 */
public class SilhouetteGenerator {

    /**
     * Generates a simple human silhouette image
     * 
     * @param width  Width of the image
     * @param height Height of the image
     * @return Image containing a human silhouette
     */
    public static Image generateSilhouette(int width, int height) {
        // Create a canvas to draw on
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fill with transparent background
        gc.setFill(Color.TRANSPARENT);
        gc.fillRect(0, 0, width, height);

        // Set color for silhouette
        gc.setFill(Color.rgb(100, 100, 100, 0.3));
        gc.setStroke(Color.rgb(80, 80, 80, 0.4));
        gc.setLineWidth(1.5);

        // Calculate proportions
        int centerX = width / 2;
        int headRadius = width / 8;
        int neckWidth = width / 12;
        int shoulderWidth = width / 3;
        int hipWidth = width / 4;
        int legWidth = width / 12;

        // Head position
        int headY = height / 8 + headRadius;

        // Draw head
        gc.fillOval(centerX - headRadius, headY - headRadius, headRadius * 2, headRadius * 2);
        gc.strokeOval(centerX - headRadius, headY - headRadius, headRadius * 2, headRadius * 2);

        // Draw neck
        int neckTop = headY + headRadius - 5;
        int neckHeight = height / 12;
        gc.fillRect(centerX - neckWidth / 2, neckTop, neckWidth, neckHeight);
        gc.strokeRect(centerX - neckWidth / 2, neckTop, neckWidth, neckHeight);

        // Draw shoulders and torso
        int shoulderY = neckTop + neckHeight;
        int torsoHeight = height / 3;

        // Shoulders
        gc.beginPath();
        gc.moveTo(centerX - shoulderWidth / 2, shoulderY);
        gc.lineTo(centerX + shoulderWidth / 2, shoulderY);
        gc.lineTo(centerX + hipWidth / 2, shoulderY + torsoHeight);
        gc.lineTo(centerX - hipWidth / 2, shoulderY + torsoHeight);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // Draw arms
        int armLength = height / 3;
        int handY = shoulderY + armLength;

        // Left arm
        gc.beginPath();
        gc.moveTo(centerX - shoulderWidth / 2, shoulderY);
        gc.lineTo(centerX - shoulderWidth / 2 - width / 15, handY);
        gc.lineTo(centerX - shoulderWidth / 2 + width / 30, handY);
        gc.lineTo(centerX - shoulderWidth / 3, shoulderY + width / 30);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // Right arm
        gc.beginPath();
        gc.moveTo(centerX + shoulderWidth / 2, shoulderY);
        gc.lineTo(centerX + shoulderWidth / 2 + width / 15, handY);
        gc.lineTo(centerX + shoulderWidth / 2 - width / 30, handY);
        gc.lineTo(centerX + shoulderWidth / 3, shoulderY + width / 30);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // Draw legs
        int legTop = shoulderY + torsoHeight;
        int legHeight = height - legTop - height / 20;

        // Left leg
        gc.beginPath();
        gc.moveTo(centerX - hipWidth / 3, legTop);
        gc.lineTo(centerX - hipWidth / 3 - legWidth, legTop + legHeight);
        gc.lineTo(centerX - hipWidth / 3 + legWidth, legTop + legHeight);
        gc.lineTo(centerX - hipWidth / 6, legTop);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // Right leg
        gc.beginPath();
        gc.moveTo(centerX + hipWidth / 3, legTop);
        gc.lineTo(centerX + hipWidth / 3 + legWidth, legTop + legHeight);
        gc.lineTo(centerX + hipWidth / 3 - legWidth, legTop + legHeight);
        gc.lineTo(centerX + hipWidth / 6, legTop);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // Create image from canvas
        WritableImage image = new WritableImage(width, height);
        canvas.snapshot(null, image);

        return image;
    }
}