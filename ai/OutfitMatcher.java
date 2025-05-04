package com.fashionstore.ai;

import com.fashionstore.models.Outfit;
import com.fashionstore.models.Product;
import com.fashionstore.models.StylePreference;
import com.fashionstore.models.User;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered outfit generation and matching algorithm
 */
public class OutfitMatcher {

    // Style categories for matching
    private static final String[] STYLE_CATEGORIES = {
            "Casual", "Formal", "Business", "Athletic", "Evening"
    };

    // Seasonal categories for matching
    private static final String[] SEASONAL_CATEGORIES = {
            "Spring", "Summer", "Fall", "Winter"
    };

    // Category matching for outfits
    private static final Map<String, List<String>> OUTFIT_COMPONENTS = new HashMap<>();

    static {
        // Initialize outfit components by category
        OUTFIT_COMPONENTS.put("Casual", Arrays.asList("T-shirt", "Jeans", "Sneakers", "Hoodie"));
        OUTFIT_COMPONENTS.put("Formal", Arrays.asList("Dress shirt", "Suit", "Dress", "Heels", "Formal shoes"));
        OUTFIT_COMPONENTS.put("Business", Arrays.asList("Blazer", "Slacks", "Blouse", "Dress shoes"));
        OUTFIT_COMPONENTS.put("Athletic", Arrays.asList("Athletic shirt", "Shorts", "Athletic pants", "Running shoes"));
        OUTFIT_COMPONENTS.put("Evening", Arrays.asList("Dress", "Suit", "Formal shoes", "Accessories"));
    }

    // Color compatibility matrix (simplified)
    private static final Map<String, List<String>> COLOR_COMPATIBILITY = new HashMap<>();

    static {
        // Initialize color compatibility
        COLOR_COMPATIBILITY.put("Black", Arrays.asList("White", "Red", "Blue", "Gray", "Pink"));
        COLOR_COMPATIBILITY.put("White", Arrays.asList("Black", "Blue", "Red", "Brown", "Gray"));
        COLOR_COMPATIBILITY.put("Blue", Arrays.asList("White", "Gray", "Brown", "Green", "Black"));
        COLOR_COMPATIBILITY.put("Red", Arrays.asList("Black", "White", "Gray", "Blue", "Yellow"));
        COLOR_COMPATIBILITY.put("Green", Arrays.asList("Blue", "Yellow", "Brown", "Gray", "Black"));
        // Add more color combinations as needed
    }

    /**
     * Default constructor
     */
    public OutfitMatcher() {
        // Initialize any AI models or data here
    }

    /**
     * Generate an outfit recommendation based on user's wardrobe items
     * 
     * @param user          The user to generate outfit for
     * @param wardrobeItems The user's wardrobe items
     * @return A recommended outfit, or null if it couldn't be generated
     */
    public Outfit generateOutfit(User user, List<Product> wardrobeItems) {
        if (wardrobeItems == null || wardrobeItems.isEmpty()) {
            return null;
        }

        try {
            // Step 1: Choose a style theme randomly
            String styleTheme = STYLE_CATEGORIES[new Random().nextInt(STYLE_CATEGORIES.length)];

            // Step 2: Determine season (could be based on current month in real app)
            int month = LocalDateTime.now().getMonthValue();
            String season;
            if (month >= 3 && month <= 5)
                season = "Spring";
            else if (month >= 6 && month <= 8)
                season = "Summer";
            else if (month >= 9 && month <= 11)
                season = "Fall";
            else
                season = "Winter";

            // Step 3: Filter items by category
            Map<String, List<Product>> categorizedItems = categorizeItems(wardrobeItems);

            // Step 4: Select base item - could be pants, skirt, or dress
            Product baseItem = selectBaseItem(categorizedItems);
            if (baseItem == null)
                return null;

            // Step 5: Select matching top if needed
            Product topItem = null;
            if (!baseItem.getCategory().contains("Dress")) {
                topItem = selectTopItem(categorizedItems, baseItem, styleTheme);
            }

            // Step 6: Select footwear
            Product footwear = selectFootwear(categorizedItems, styleTheme);

            // Step 7: Select accessory if available
            Product accessory = selectAccessory(categorizedItems);

            // Create the outfit
            Outfit outfit = new Outfit(user.getUserId(), generateOutfitName(styleTheme, season));
            outfit.setOutfitId(UUID.randomUUID().toString());
            outfit.setAiGenerated(true);
            outfit.addProduct(baseItem.getProductId());

            if (topItem != null) {
                outfit.addProduct(topItem.getProductId());
            }

            if (footwear != null) {
                outfit.addProduct(footwear.getProductId());
            }

            if (accessory != null) {
                outfit.addProduct(accessory.getProductId());
            }

            return outfit;

        } catch (Exception e) {
            System.err.println("Error generating outfit: " + e.getMessage());
            e.printStackTrace();

            // Fallback: create a simple outfit with random items
            return createFallbackOutfit(user, wardrobeItems);
        }
    }

    /**
     * Create a fallback outfit with random items if the AI recommendation fails
     */
    private Outfit createFallbackOutfit(User user, List<Product> wardrobeItems) {
        if (wardrobeItems.size() < 2)
            return null;

        // Shuffle the items
        List<Product> shuffled = new ArrayList<>(wardrobeItems);
        Collections.shuffle(shuffled);

        // Create a simple outfit with up to 4 random items
        Outfit outfit = new Outfit(user.getUserId(), "Freestyle Mix");
        outfit.setOutfitId(UUID.randomUUID().toString());
        outfit.setAiGenerated(true);

        // Add up to 4 items
        for (int i = 0; i < Math.min(4, shuffled.size()); i++) {
            outfit.addProduct(shuffled.get(i).getProductId());
        }

        return outfit;
    }

    /**
     * Generate a creative outfit name based on style and season
     */
    private String generateOutfitName(String styleTheme, String season) {
        // Array of interesting adjectives
        String[] adjectives = {
                "Stylish", "Modern", "Classic", "Trendy", "Chic", "Elegant", "Vibrant",
                "Sophisticated", "Bold", "Minimalist", "Fresh", "Urban", "Relaxed"
        };

        // Select a random adjective
        String adjective = adjectives[new Random().nextInt(adjectives.length)];

        // Create creative name
        return adjective + " " + season + " " + styleTheme;
    }

    /**
     * Categorize wardrobe items by product type
     */
    private Map<String, List<Product>> categorizeItems(List<Product> items) {
        Map<String, List<Product>> categorized = new HashMap<>();

        for (Product item : items) {
            String category = item.getCategory().toLowerCase();

            // Determine the high-level category
            String highLevelCategory = null;

            if (category.contains("shirt") || category.contains("top") ||
                    category.contains("blouse") || category.contains("sweater")) {
                highLevelCategory = "Tops";
            } else if (category.contains("pant") || category.contains("jean") ||
                    category.contains("skirt") || category.contains("trouser") ||
                    category.contains("short")) {
                highLevelCategory = "Bottoms";
            } else if (category.contains("dress")) {
                highLevelCategory = "Dresses";
            } else if (category.contains("shoe") || category.contains("sneaker") ||
                    category.contains("boot") || category.contains("sandal")) {
                highLevelCategory = "Footwear";
            } else if (category.contains("jacket") || category.contains("coat") ||
                    category.contains("blazer") || category.contains("hoodie")) {
                highLevelCategory = "Outerwear";
            } else if (category.contains("accessory") || category.contains("jewel") ||
                    category.contains("hat") || category.contains("belt") ||
                    category.contains("scarf")) {
                highLevelCategory = "Accessories";
            } else {
                highLevelCategory = "Other";
            }

            // Add to the appropriate category
            if (!categorized.containsKey(highLevelCategory)) {
                categorized.put(highLevelCategory, new ArrayList<>());
            }

            categorized.get(highLevelCategory).add(item);
        }

        return categorized;
    }

    /**
     * Select a base item (pants, skirt, or dress)
     */
    private Product selectBaseItem(Map<String, List<Product>> categorizedItems) {
        // Try to find a dress first (complete outfit by itself)
        if (categorizedItems.containsKey("Dresses") && !categorizedItems.get("Dresses").isEmpty()) {
            List<Product> dresses = categorizedItems.get("Dresses");
            return dresses.get(new Random().nextInt(dresses.size()));
        }

        // Try to find bottoms (pants/skirts)
        if (categorizedItems.containsKey("Bottoms") && !categorizedItems.get("Bottoms").isEmpty()) {
            List<Product> bottoms = categorizedItems.get("Bottoms");
            return bottoms.get(new Random().nextInt(bottoms.size()));
        }

        // If no suitable base item found, just pick something random
        for (List<Product> categoryItems : categorizedItems.values()) {
            if (!categoryItems.isEmpty()) {
                return categoryItems.get(new Random().nextInt(categoryItems.size()));
            }
        }

        return null;
    }

    /**
     * Select a top item that matches the base item
     */
    private Product selectTopItem(Map<String, List<Product>> categorizedItems, Product baseItem, String styleTheme) {
        if (!categorizedItems.containsKey("Tops") || categorizedItems.get("Tops").isEmpty()) {
            return null;
        }

        List<Product> tops = categorizedItems.get("Tops");
        List<Product> compatibleTops = new ArrayList<>();

        // Find color-compatible tops
        String baseColor = extractMainColor(baseItem.getDescription());
        if (baseColor != null && COLOR_COMPATIBILITY.containsKey(baseColor)) {
            List<String> compatibleColors = COLOR_COMPATIBILITY.get(baseColor);

            for (Product top : tops) {
                String topColor = extractMainColor(top.getDescription());
                if (topColor != null && (compatibleColors.contains(topColor) || baseColor.equals(topColor))) {
                    compatibleTops.add(top);
                }
            }
        }

        // If no color-compatible tops found, just return a random top
        if (compatibleTops.isEmpty()) {
            return tops.get(new Random().nextInt(tops.size()));
        }

        return compatibleTops.get(new Random().nextInt(compatibleTops.size()));
    }

    /**
     * Select footwear that matches the style theme
     */
    private Product selectFootwear(Map<String, List<Product>> categorizedItems, String styleTheme) {
        if (!categorizedItems.containsKey("Footwear") || categorizedItems.get("Footwear").isEmpty()) {
            return null;
        }

        List<Product> footwear = categorizedItems.get("Footwear");
        List<Product> styleMatchedFootwear = new ArrayList<>();

        // Try to find footwear matching the style
        for (Product shoe : footwear) {
            String description = shoe.getDescription().toLowerCase();
            String category = shoe.getCategory().toLowerCase();

            boolean matches = false;

            switch (styleTheme) {
                case "Casual":
                    matches = description.contains("casual") || description.contains("sneaker") ||
                            category.contains("casual") || category.contains("sneaker");
                    break;
                case "Formal":
                    matches = description.contains("formal") || description.contains("dress") ||
                            category.contains("formal") || category.contains("dress");
                    break;
                case "Business":
                    matches = description.contains("business") || description.contains("dress") ||
                            category.contains("business") || category.contains("dress");
                    break;
                case "Athletic":
                    matches = description.contains("athletic") || description.contains("sport") ||
                            category.contains("athletic") || category.contains("sport") ||
                            description.contains("running") || category.contains("running");
                    break;
                case "Evening":
                    matches = description.contains("evening") || description.contains("dress") ||
                            category.contains("evening") || category.contains("dress") ||
                            description.contains("heel") || category.contains("heel");
                    break;
            }

            if (matches) {
                styleMatchedFootwear.add(shoe);
            }
        }

        // If no style-compatible footwear found, just return a random one
        if (styleMatchedFootwear.isEmpty()) {
            return footwear.get(new Random().nextInt(footwear.size()));
        }

        return styleMatchedFootwear.get(new Random().nextInt(styleMatchedFootwear.size()));
    }

    /**
     * Select an accessory to complete the outfit
     */
    private Product selectAccessory(Map<String, List<Product>> categorizedItems) {
        if (!categorizedItems.containsKey("Accessories") || categorizedItems.get("Accessories").isEmpty()) {
            return null;
        }

        List<Product> accessories = categorizedItems.get("Accessories");
        return accessories.get(new Random().nextInt(accessories.size()));
    }

    /**
     * Extract the main color from a product description
     */
    private String extractMainColor(String description) {
        if (description == null)
            return null;

        // Common colors to look for
        String[] commonColors = {
                "Black", "White", "Red", "Blue", "Green", "Yellow", "Purple",
                "Pink", "Orange", "Brown", "Gray", "Navy", "Teal", "Beige"
        };

        // Convert to lowercase for case-insensitive search
        String lowerDesc = description.toLowerCase();

        for (String color : commonColors) {
            if (lowerDesc.contains(color.toLowerCase())) {
                return color;
            }
        }

        return null;
    }
}