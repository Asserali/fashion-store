package com.fashionstore.models;

import java.io.Serializable;
import java.util.*;

public class Outfit implements Serializable, Comparable<Outfit> {
    private static final long serialVersionUID = 2L; // Updated for new version

    private String outfitId;
    private final String userId;
    private String name;
    private String description;
    private final Date createdAt;
    private Date lastModified;
    private boolean aiGenerated;
    private double styleRating; // 0-5 rating
    private int likesCount;
    private final Set<String> productIds; // Using Set to avoid duplicates
    private OutfitSeason season;
    private OutfitOccasion occasion;
    private List<String> tags;

    public enum OutfitSeason {
        SPRING, SUMMER, FALL, WINTER, ALL_SEASON
    }

    public enum OutfitOccasion {
        CASUAL, FORMAL, WORK, SPORT, PARTY, DATE, TRAVEL
    }

    public Outfit(String userId, String name) {
        this.outfitId = "OUTFIT-" + UUID.randomUUID().toString();
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.name = Objects.requireNonNull(name, "Outfit name cannot be null");
        this.createdAt = new Date();
        this.lastModified = new Date();
        this.productIds = new LinkedHashSet<>(); // Preserves insertion order
        this.tags = new ArrayList<>();
        this.styleRating = 0.0;
        this.likesCount = 0;
        this.season = OutfitSeason.ALL_SEASON;
        this.occasion = OutfitOccasion.CASUAL;
    }

    // Core Accessors
    public String getOutfitId() { return outfitId; }
    
    // Add setter for outfitId to support database loading
    public void setOutfitId(String outfitId) {
        if (outfitId != null) {
            this.outfitId = outfitId;
        }
    }
    
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Date getCreatedAt() { return new Date(createdAt.getTime()); }
    public Date getLastModified() { return new Date(lastModified.getTime()); }
    public boolean isAiGenerated() { return aiGenerated; }
    public double getStyleRating() { return styleRating; }
    public int getLikesCount() { return likesCount; }
    public Set<String> getProductIds() { return new LinkedHashSet<>(productIds); }
    public OutfitSeason getSeason() { return season; }
    public OutfitOccasion getOccasion() { return occasion; }
    public List<String> getTags() { return new ArrayList<>(tags); }
    public int getItemCount() { return productIds.size(); }
    public boolean isEmpty() { return productIds.isEmpty(); }

    // Core Mutators
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Outfit name cannot be null");
        updateModified();
    }

    public void setDescription(String description) {
        this.description = description;
        updateModified();
    }

    public void setAiGenerated(boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
        updateModified();
    }

    public void setStyleRating(double styleRating) {
        this.styleRating = Math.max(0.0, Math.min(5.0, styleRating));
        updateModified();
    }

    public void setSeason(OutfitSeason season) {
        this.season = Objects.requireNonNull(season);
        updateModified();
    }

    public void setOccasion(OutfitOccasion occasion) {
        this.occasion = Objects.requireNonNull(occasion);
        updateModified();
    }

    // Product Management
    public boolean addProduct(String productId) {
        Objects.requireNonNull(productId, "Product ID cannot be null");
        boolean added = productIds.add(productId);
        if (added) updateModified();
        return added;
    }

    public boolean removeProduct(String productId) {
        boolean removed = productIds.remove(productId);
        if (removed) updateModified();
        return removed;
    }

    public boolean containsProduct(String productId) {
        return productIds.contains(productId);
    }

    public void clearProducts() {
        if (!productIds.isEmpty()) {
            productIds.clear();
            updateModified();
        }
    }

    // Tag Management
    public void addTag(String tag) {
        Objects.requireNonNull(tag, "Tag cannot be null");
        if (!tags.contains(tag)) {
            tags.add(tag);
            updateModified();
        }
    }

    public boolean removeTag(String tag) {
        boolean removed = tags.remove(tag);
        if (removed) updateModified();
        return removed;
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    // Social Features
    public void incrementLikes() {
        likesCount++;
        updateModified();
    }

    public void updateRating(int newRating) {
        if (newRating < 0 || newRating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
        this.styleRating = newRating;
        updateModified();
    }

    // Utility Methods
    private void updateModified() {
        this.lastModified = new Date();
    }

    public String toShortString() {
        return name + " (" + productIds.size() + " items)";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("outfitId", outfitId);
        map.put("name", name);
        map.put("itemCount", productIds.size());
        map.put("createdAt", createdAt);
        map.put("season", season.name());
        map.put("occasion", occasion.name());
        return map;
    }

    // Comparison and Equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Outfit outfit = (Outfit) o;
        return outfitId.equals(outfit.outfitId);
    }

    @Override
    public int hashCode() {
        return outfitId.hashCode();
    }

    @Override
    public int compareTo(Outfit other) {
        return this.createdAt.compareTo(other.createdAt);
    }

    @Override
    public String toString() {
        return "Outfit{" +
                "id='" + outfitId + '\'' +
                ", name='" + name + '\'' +
                ", items=" + productIds.size() +
                ", created=" + createdAt +
                ", rating=" + styleRating +
                ", season=" + season +
                ", occasion=" + occasion +
                '}';
    }

    // Builder Pattern for complex creation
    public static class Builder {
        private final String userId;
        private final String name;
        private String description;
        private boolean aiGenerated = false;
        private OutfitSeason season = OutfitSeason.ALL_SEASON;
        private OutfitOccasion occasion = OutfitOccasion.CASUAL;
        private Set<String> productIds = new HashSet<>();
        private List<String> tags = new ArrayList<>();

        public Builder(String userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder aiGenerated(boolean aiGenerated) {
            this.aiGenerated = aiGenerated;
            return this;
        }

        public Builder season(OutfitSeason season) {
            this.season = season;
            return this;
        }

        public Builder occasion(OutfitOccasion occasion) {
            this.occasion = occasion;
            return this;
        }

        public Builder addProduct(String productId) {
            this.productIds.add(productId);
            return this;
        }

        public Builder addTag(String tag) {
            this.tags.add(tag);
            return this;
        }

        public Outfit build() {
            Outfit outfit = new Outfit(userId, name);
            outfit.setDescription(description);
            outfit.setAiGenerated(aiGenerated);
            outfit.setSeason(season);
            outfit.setOccasion(occasion);
            productIds.forEach(outfit::addProduct);
            tags.forEach(outfit::addTag);
            return outfit;
        }
    }
}