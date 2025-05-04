package com.fashionstore.models;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Product implements Serializable, Comparable<Product> {
    private static final long serialVersionUID = 1L;

    // Core attributes
    private String productId;
    private String name;
    private String description;
    private String brand;
    private String category;
    private String subcategory;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal cost;

    // Additional attributes
    private String gender;
    private String size;
    private String color;
    private String material;
    private String season;
    private String imagePath;
    private int stockQuantity;
    private final Date dateAdded;
    private Date lastUpdated;
    private Map<String, String> attributes;
    private boolean isFeatured;
    private boolean isVisible;

    // Rating attributes
    private double averageRating;
    private int reviewCount;

    public Product(String name, String category, BigDecimal price) {
        this.productId = "PROD-" + UUID.randomUUID().toString();
        this.name = Objects.requireNonNull(name, "Product name cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null").setScale(2, RoundingMode.HALF_UP);
        this.originalPrice = this.price;
        this.dateAdded = new Date();
        this.lastUpdated = new Date();
        this.attributes = new HashMap<>();
        this.stockQuantity = 0;
        this.reviewCount = 0;
        this.averageRating = 0.0;
        this.isFeatured = false;
        this.isVisible = true;
    }

    // Basic getters
    public String getProductId() {
        return productId;
    }

    // Add setter for productId to support database loading
    public void setProductId(String productId) {
        if (productId != null) {
            this.productId = productId;
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getBrand() {
        return brand;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public String getGender() {
        return gender;
    }

    public String getSize() {
        return size;
    }

    public String getColor() {
        return color;
    }

    public String getMaterial() {
        return material;
    }

    public String getSeason() {
        return season;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public Date getDateAdded() {
        return new Date(dateAdded.getTime());
    }

    public Date getLastUpdated() {
        return new Date(lastUpdated.getTime());
    }

    public Map<String, String> getAttributes() {
        return new HashMap<>(attributes);
    }

    public boolean isFeatured() {
        return isFeatured;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public boolean isOnSale() {
        return price.compareTo(originalPrice) < 0;
    }

    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Returns all attributes of this product
     * 
     * @return Map containing all attributes
     */
    public Map<String, String> getAllAttributes() {
        return new HashMap<>(attributes);
    }

    // Setters with validation
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Product name cannot be null");
        updateTimestamp();
    }

    public void setDescription(String description) {
        this.description = description;
        updateTimestamp();
    }

    public void setBrand(String brand) {
        this.brand = brand;
        updateTimestamp();
    }

    public void setCategory(String category) {
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        updateTimestamp();
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
        updateTimestamp();
    }

    public void setPrice(BigDecimal price) {
        this.price = Objects.requireNonNull(price, "Price cannot be null")
                .setScale(2, RoundingMode.HALF_UP);
        updateTimestamp();
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice != null ? originalPrice.setScale(2, RoundingMode.HALF_UP) : null;
        updateTimestamp();
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost != null ? cost.setScale(2, RoundingMode.HALF_UP) : null;
        updateTimestamp();
    }

    public void setGender(String gender) {
        this.gender = gender;
        updateTimestamp();
    }

    public void setSize(String size) {
        this.size = size;
        updateTimestamp();
    }

    public void setColor(String color) {
        this.color = color;
        updateTimestamp();
    }

    public void setMaterial(String material) {
        this.material = material;
        updateTimestamp();
    }

    public void setSeason(String season) {
        this.season = season;
        updateTimestamp();
    }

    public void setImagePath(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            this.imagePath = "/images/default-product.jpg";
            return;
        }

        // Normalize path (convert backslashes to forward slashes)
        String normalizedPath = imagePath.replace("\\", "/");

        // Remove src/main/resources prefix if present
        if (normalizedPath.contains("src/main/resources")) {
            normalizedPath = normalizedPath
                    .substring(normalizedPath.indexOf("src/main/resources") + "src/main/resources".length());
        }

        // Ensure path starts with slash for resource loading
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        // Log the normalized path
        System.out.println("Setting image path: " + normalizedPath);

        this.imagePath = normalizedPath;
        updateTimestamp();
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
        updateTimestamp();
    }

    public void setFeatured(boolean featured) {
        isFeatured = featured;
        updateTimestamp();
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
        updateTimestamp();
    }

    // Attribute management
    public void addAttribute(String key, String value) {
        attributes.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
        updateTimestamp();
    }

    // Alias for addAttribute to support DataManager loading
    public void setAttribute(String key, String value) {
        addAttribute(key, value);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
        updateTimestamp();
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    // Inventory management
    public void increaseStock(int quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be positive");
        stockQuantity += quantity;
        updateTimestamp();
    }

    public boolean decreaseStock(int quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be positive");
        if (stockQuantity >= quantity) {
            stockQuantity -= quantity;
            updateTimestamp();
            return true;
        }
        return false;
    }

    // Rating management
    public void addRating(int rating) {
        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        double totalRating = averageRating * reviewCount;
        reviewCount++;
        averageRating = (totalRating + rating) / reviewCount;
        updateTimestamp();
    }

    // Discount management
    public void applyDiscount(BigDecimal discountPercentage) {
        Objects.requireNonNull(discountPercentage, "Discount percentage cannot be null");
        if (discountPercentage.compareTo(BigDecimal.ZERO) < 0
                || discountPercentage.compareTo(new BigDecimal("1.00")) > 0) {
            throw new IllegalArgumentException("Discount must be between 0 and 1 (0% to 100%)");
        }
        price = originalPrice.multiply(BigDecimal.ONE.subtract(discountPercentage))
                .setScale(2, RoundingMode.HALF_UP);
        updateTimestamp();
    }

    public void removeDiscount() {
        price = originalPrice;
        updateTimestamp();
    }

    private void updateTimestamp() {
        this.lastUpdated = new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Product product = (Product) o;
        return productId.equals(product.productId);
    }

    @Override
    public int hashCode() {
        return productId.hashCode();
    }

    @Override
    public int compareTo(Product other) {
        return this.dateAdded.compareTo(other.dateAdded);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + productId + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", stock=" + stockQuantity +
                ", rating=" + averageRating +
                '}';
    }

    // Builder pattern for complex creation
    public static class Builder {
        private final String name;
        private final String category;
        private final BigDecimal price;
        private String description;
        private String brand;
        private String subcategory;
        private String gender;
        private String size;
        private String color;
        private String material;
        private String season;
        private String imagePath;
        private int stockQuantity;

        public Builder(String name, String category, BigDecimal price) {
            this.name = name;
            this.category = category;
            this.price = price;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder subcategory(String subcategory) {
            this.subcategory = subcategory;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder material(String material) {
            this.material = material;
            return this;
        }

        public Builder season(String season) {
            this.season = season;
            return this;
        }

        public Builder imagePath(String imagePath) {
            this.imagePath = imagePath;
            return this;
        }

        public Builder stockQuantity(int stockQuantity) {
            this.stockQuantity = stockQuantity;
            return this;
        }

        public Product build() {
            Product product = new Product(name, category, price);
            product.setDescription(description);
            product.setBrand(brand);
            product.setSubcategory(subcategory);
            product.setGender(gender);
            product.setSize(size);
            product.setColor(color);
            product.setMaterial(material);
            product.setSeason(season);
            product.setImagePath(imagePath);
            product.setStockQuantity(stockQuantity);
            return product;
        }
    }
}