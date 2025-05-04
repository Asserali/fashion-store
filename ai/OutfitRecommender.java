package com.fashionstore.ai;

import com.fashionstore.models.Outfit;
import com.fashionstore.models.Product;
import com.fashionstore.models.StylePreference;
import com.fashionstore.models.User;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-based outfit recommendation engine that suggests outfits based on
 * color coordination, style matching, and season appropriateness.
 */
public class OutfitRecommender {

    // Color coordination maps
    private static final Map<String, List<String>> COMPLEMENTARY_COLORS = new HashMap<>();

    // Style coordination maps
    private static final Map<String, List<String>> STYLE_MATCHES = new HashMap<>();

    // Static initializer to set up color and style matching rules
    static {
        // Initialize complementary color pairings
        COMPLEMENTARY_COLORS.put("black", Arrays.asList("white", "gray", "red", "blue", "green", "pink", "yellow"));
        COMPLEMENTARY_COLORS.put("white", Arrays.asList("black", "navy", "blue", "red", "brown"));
        COMPLEMENTARY_COLORS.put("blue", Arrays.asList("white", "gray", "brown", "navy", "khaki"));
        COMPLEMENTARY_COLORS.put("red", Arrays.asList("white", "black", "gray", "navy", "khaki"));
        COMPLEMENTARY_COLORS.put("green", Arrays.asList("white", "black", "khaki", "brown"));
        COMPLEMENTARY_COLORS.put("yellow", Arrays.asList("black", "navy", "blue", "purple"));
        COMPLEMENTARY_COLORS.put("purple", Arrays.asList("white", "gray", "yellow"));
        COMPLEMENTARY_COLORS.put("pink", Arrays.asList("black", "white", "gray", "navy"));
        COMPLEMENTARY_COLORS.put("gray", Arrays.asList("black", "white", "navy", "red", "pink", "blue"));
        COMPLEMENTARY_COLORS.put("navy", Arrays.asList("white", "gray", "pink", "red", "yellow"));
        COMPLEMENTARY_COLORS.put("brown", Arrays.asList("white", "blue", "green", "khaki"));
        COMPLEMENTARY_COLORS.put("khaki", Arrays.asList("black", "navy", "brown", "green", "red"));

        // Initialize style matching rules
        STYLE_MATCHES.put("formal", Arrays.asList("formal", "business", "elegant"));
        STYLE_MATCHES.put("casual", Arrays.asList("casual", "everyday", "relaxed"));
        STYLE_MATCHES.put("sporty", Arrays.asList("sporty", "athletic", "active"));
        STYLE_MATCHES.put("trendy", Arrays.asList("trendy", "modern", "stylish"));
        STYLE_MATCHES.put("vintage", Arrays.asList("vintage", "retro", "classic"));
    }

    /**
     * Generates outfit recommendations based on user's wardrobe and preferences
     * 
     * @param user               The user to generate recommendations for
     * @param wardrobeItems      List of products in the user's wardrobe
     * @param maxRecommendations Maximum number of recommendations to generate
     * @return List of recommended outfits
     */
    public List<Outfit> generateRecommendations(
            User user,
            List<Product> wardrobeItems,
            int maxRecommendations) {

        // Make sure we have enough items to work with
        if (wardrobeItems.size() < 2) {
            return Collections.emptyList();
        }

        // Categorize wardrobe items
        Map<String, List<Product>> categorizedItems = categorizeWardrobeItems(wardrobeItems);

        List<Outfit> recommendations = new ArrayList<>();

        // Try to create enough outfit combinations
        for (int attempt = 0; attempt < maxRecommendations * 3
                && recommendations.size() < maxRecommendations; attempt++) {
            Outfit outfit = createOutfitCombination(user, categorizedItems);
            if (outfit != null && !outfitExists(recommendations, outfit)) {
                // Calculate and set style rating
                double rating = calculateOutfitRating(outfit, wardrobeItems, user);
                outfit.setStyleRating(rating);

                recommendations.add(outfit);
            }
        }

        // Sort recommendations by rating (highest first)
        recommendations.sort((o1, o2) -> Double.compare(o2.getStyleRating(), o1.getStyleRating()));

        return recommendations;
    }

    /**
     * Categorizes wardrobe items by their product category
     */
    private Map<String, List<Product>> categorizeWardrobeItems(List<Product> wardrobeItems) {
        Map<String, List<Product>> categorized = new HashMap<>();

        // Initialize common categories
        categorized.put("tops", new ArrayList<>());
        categorized.put("bottoms", new ArrayList<>());
        categorized.put("shoes", new ArrayList<>());
        categorized.put("accessories", new ArrayList<>());
        categorized.put("outerwear", new ArrayList<>());

        // Categorize each item
        for (Product product : wardrobeItems) {
            String category = product.getCategory().toLowerCase();

            if (category.contains("top") || category.contains("shirt") || category.contains("tee") ||
                    category.contains("blouse") || category.contains("sweater")) {
                categorized.get("tops").add(product);
            } else if (category.contains("bottom") || category.contains("pant") ||
                    category.contains("jean") || category.contains("skirt") ||
                    category.contains("short")) {
                categorized.get("bottoms").add(product);
            } else if (category.contains("shoe") || category.contains("boot") ||
                    category.contains("sneaker") || category.contains("sandal")) {
                categorized.get("shoes").add(product);
            } else if (category.contains("accessory") || category.contains("accessories") ||
                    category.contains("hat") || category.contains("bag") ||
                    category.contains("jewelry") || category.contains("watch") ||
                    category.contains("belt") || category.contains("scarf")) {
                categorized.get("accessories").add(product);
            } else if (category.contains("outerwear") || category.contains("jacket") ||
                    category.contains("coat") || category.contains("hoodie")) {
                categorized.get("outerwear").add(product);
            } else {
                // If we don't know where it goes, try to guess from the name
                String name = product.getName().toLowerCase();
                if (name.contains("shirt") || name.contains("top") || name.contains("tee")) {
                    categorized.get("tops").add(product);
                } else if (name.contains("pant") || name.contains("jean") || name.contains("skirt")) {
                    categorized.get("bottoms").add(product);
                } else if (name.contains("shoe") || name.contains("boot") || name.contains("sneaker")) {
                    categorized.get("shoes").add(product);
                } else {
                    // Default to accessories if we can't categorize
                    categorized.get("accessories").add(product);
                }
            }
        }

        return categorized;
    }

    /**
     * Creates a single outfit combination from the user's wardrobe items
     */
    private Outfit createOutfitCombination(User user, Map<String, List<Product>> categorizedItems) {
        Random random = new Random();

        // Make sure we have essential items
        if (categorizedItems.get("tops").isEmpty() || categorizedItems.get("bottoms").isEmpty()) {
            return null;
        }

        // Start building the outfit
        Outfit outfit = new Outfit(user.getUserId(), "AI Recommendation");
        outfit.setAiGenerated(true);
        outfit.setDescription("AI-generated outfit based on your style preferences and wardrobe items.");

        // Select a top
        Product top = getRandomItem(categorizedItems.get("tops"));
        outfit.addProduct(top.getProductId());

        // Select a bottom that coordinates with the top
        Product bottom = selectCoordinatingItem(top, categorizedItems.get("bottoms"));
        outfit.addProduct(bottom.getProductId());

        // Add shoes if available
        if (!categorizedItems.get("shoes").isEmpty()) {
            Product shoes = selectCoordinatingItem(bottom, categorizedItems.get("shoes"));
            outfit.addProduct(shoes.getProductId());
        }

        // Maybe add outerwear (30% chance)
        if (!categorizedItems.get("outerwear").isEmpty() && random.nextDouble() < 0.3) {
            Product outerwear = selectCoordinatingItem(top, categorizedItems.get("outerwear"));
            outfit.addProduct(outerwear.getProductId());
        }

        // Maybe add an accessory (50% chance)
        if (!categorizedItems.get("accessories").isEmpty() && random.nextDouble() < 0.5) {
            Product accessory = getRandomItem(categorizedItems.get("accessories"));
            outfit.addProduct(accessory.getProductId());
        }

        // Determine appropriate season
        determineOutfitSeason(outfit);

        // Determine appropriate occasion
        determineOutfitOccasion(outfit);

        return outfit;
    }

    /**
     * Gets a random item from a list
     */
    private Product getRandomItem(List<Product> items) {
        if (items.isEmpty())
            return null;
        return items.get(new Random().nextInt(items.size()));
    }

    /**
     * Selects an item that coordinates well with the reference item
     */
    private Product selectCoordinatingItem(Product referenceItem, List<Product> candidates) {
        if (candidates.isEmpty())
            return null;

        // Get color of the reference item
        String referenceColor = referenceItem.getColor() != null ? referenceItem.getColor().toLowerCase() : null;

        // If we don't have a color, just return a random item
        if (referenceColor == null) {
            return getRandomItem(candidates);
        }

        // Try to find complementary colors
        List<String> complementaryColors = COMPLEMENTARY_COLORS.getOrDefault(
                referenceColor, Collections.emptyList());

        // Filter items by complementary colors
        List<Product> matchingItems = candidates.stream()
                .filter(item -> item.getColor() != null &&
                        (complementaryColors.contains(item.getColor().toLowerCase()) ||
                                item.getColor().equalsIgnoreCase(referenceColor)))
                .collect(Collectors.toList());

        // If no matches, return random item
        if (matchingItems.isEmpty()) {
            return getRandomItem(candidates);
        }

        // Return a random matching item
        return getRandomItem(matchingItems);
    }

    /**
     * Determines the most appropriate season for the outfit
     */
    private void determineOutfitSeason(Outfit outfit) {
        // Simple logic based on number of layers and item types
        int itemCount = outfit.getProductIds().size();

        if (itemCount >= 4) {
            // Many layers suggests colder weather
            outfit.setSeason(Outfit.OutfitSeason.FALL);
        } else if (itemCount <= 2) {
            // Fewer layers suggests warmer weather
            outfit.setSeason(Outfit.OutfitSeason.SUMMER);
        } else {
            // Default to all-season
            outfit.setSeason(Outfit.OutfitSeason.ALL_SEASON);
        }
    }

    /**
     * Determines the most appropriate occasion for the outfit
     */
    private void determineOutfitOccasion(Outfit outfit) {
        // Default to casual
        outfit.setOccasion(Outfit.OutfitOccasion.CASUAL);
    }

    /**
     * Calculates a style rating for the outfit (0-5)
     */
    public double calculateOutfitRating(Outfit outfit, List<Product> allItems, User user) {
        // Base rating
        double rating = 3.0;

        // Convert outfit to products
        List<Product> outfitProducts = outfit.getProductIds().stream()
                .map(id -> findProductById(id, allItems))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Check color coordination
        if (hasGoodColorCoordination(outfitProducts)) {
            rating += 0.5;
        }

        // Check if it matches user preferences
        if (user.getStylePreferences() != null && !user.getStylePreferences().isEmpty()) {
            for (StylePreference preference : user.getStylePreferences()) {
                // Simple match - if any product matches a preference, increase rating
                if (outfitProducts.stream().anyMatch(p -> p.getCategory() != null &&
                        p.getCategory().toLowerCase().contains(preference.getValue().toLowerCase()))) {
                    rating += 0.25 * preference.getWeight();
                }
            }
        }

        // Ensure rating is within 0-5 range
        return Math.max(0, Math.min(5, rating));
    }

    /**
     * Checks if outfit has good color coordination
     */
    private boolean hasGoodColorCoordination(List<Product> products) {
        // Need at least 2 items to coordinate
        if (products.size() < 2) {
            return true;
        }

        // Count products with complementary colors
        int complementaryPairs = 0;

        for (int i = 0; i < products.size(); i++) {
            String color1 = products.get(i).getColor();
            if (color1 == null)
                continue;

            for (int j = i + 1; j < products.size(); j++) {
                String color2 = products.get(j).getColor();
                if (color2 == null)
                    continue;

                // Check if colors are complementary
                List<String> complementaryColors = COMPLEMENTARY_COLORS.getOrDefault(color1.toLowerCase(),
                        Collections.emptyList());

                if (complementaryColors.contains(color2.toLowerCase())) {
                    complementaryPairs++;
                }
            }
        }

        // Consider coordinated if at least 1 complementary pair
        return complementaryPairs > 0;
    }

    /**
     * Finds a product by ID in a list
     */
    private Product findProductById(String productId, List<Product> products) {
        return products.stream()
                .filter(p -> p.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if an outfit already exists in the recommendations
     */
    private boolean outfitExists(List<Outfit> outfits, Outfit newOutfit) {
        // Compare by product IDs
        Set<String> newOutfitProducts = newOutfit.getProductIds();

        for (Outfit existing : outfits) {
            Set<String> existingProducts = existing.getProductIds();

            // If they have the same products (regardless of order), consider them duplicate
            if (existingProducts.containsAll(newOutfitProducts) &&
                    newOutfitProducts.containsAll(existingProducts)) {
                return true;
            }
        }

        return false;
    }
}