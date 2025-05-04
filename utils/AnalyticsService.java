package com.fashionstore.utils;

import com.fashionstore.models.Product;
import com.fashionstore.models.User;
import com.fashionstore.models.ShoppingCart;
import com.fashionstore.models.Outfit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for generating analytics data for visualizations.
 * This class provides methods to analyze product, user, and sales data
 * for visualization in charts and tables.
 */
public class AnalyticsService {

    /**
     * Generates category distribution data for visualization.
     * 
     * @param products List of products to analyze
     * @return Map of category names to counts
     */
    public static Map<String, Integer> getCategoryDistribution(List<Product> products) {
        Map<String, Integer> distribution = new HashMap<>();

        products.stream()
                .filter(p -> p.getCategory() != null && !p.getCategory().isEmpty())
                .forEach(p -> {
                    distribution.merge(p.getCategory(), 1, Integer::sum);
                });

        return distribution;
    }

    /**
     * Generates price range distribution data for visualization.
     * 
     * @param products List of products to analyze
     * @return Map of price ranges to counts
     */
    public static Map<String, Integer> getPriceRangeDistribution(List<Product> products) {
        Map<String, Integer> distribution = new LinkedHashMap<>();

        // Initialize ranges
        distribution.put("$0-$25", 0);
        distribution.put("$25-$50", 0);
        distribution.put("$50-$100", 0);
        distribution.put("$100-$200", 0);
        distribution.put("$200+", 0);

        products.forEach(p -> {
            double price = p.getPrice().doubleValue();

            if (price < 25) {
                distribution.put("$0-$25", distribution.get("$0-$25") + 1);
            } else if (price < 50) {
                distribution.put("$25-$50", distribution.get("$25-$50") + 1);
            } else if (price < 100) {
                distribution.put("$50-$100", distribution.get("$50-$100") + 1);
            } else if (price < 200) {
                distribution.put("$100-$200", distribution.get("$100-$200") + 1);
            } else {
                distribution.put("$200+", distribution.get("$200+") + 1);
            }
        });

        return distribution;
    }

    /**
     * Generates stock level distribution for visualization.
     * 
     * @param products List of products to analyze
     * @return Map with stock level categories and counts
     */
    public static Map<String, Integer> getStockLevelDistribution(List<Product> products) {
        Map<String, Integer> distribution = new LinkedHashMap<>();

        // Initialize stock categories
        distribution.put("Out of Stock", 0);
        distribution.put("Low Stock (1-5)", 0);
        distribution.put("Medium Stock (6-20)", 0);
        distribution.put("High Stock (21+)", 0);

        products.forEach(p -> {
            int stock = p.getStockQuantity();

            if (stock == 0) {
                distribution.put("Out of Stock", distribution.get("Out of Stock") + 1);
            } else if (stock <= 5) {
                distribution.put("Low Stock (1-5)", distribution.get("Low Stock (1-5)") + 1);
            } else if (stock <= 20) {
                distribution.put("Medium Stock (6-20)", distribution.get("Medium Stock (6-20)") + 1);
            } else {
                distribution.put("High Stock (21+)", distribution.get("High Stock (21+)") + 1);
            }
        });

        return distribution;
    }

    /**
     * Generates user registration trend data over time.
     * 
     * @param users List of users to analyze
     * @param days  Number of days to include in the trend
     * @return Map of dates to registration counts
     */
    public static Map<LocalDate, Integer> getUserRegistrationTrend(List<User> users, int days) {
        Map<LocalDate, Integer> trend = new LinkedHashMap<>();

        // Initialize days
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            trend.put(today.minusDays(i), 0);
        }

        // Count registrations by date
        users.stream()
                .filter(u -> u.getDateRegistered() != null)
                .forEach(u -> {
                    LocalDate registrationDate = u.getDateRegistered()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    // Only count if within our date range
                    if (registrationDate.isAfter(today.minusDays(days)) ||
                            registrationDate.isEqual(today.minusDays(days))) {
                        trend.put(registrationDate, trend.getOrDefault(registrationDate, 0) + 1);
                    }
                });

        return trend;
    }

    /**
     * Identifies the top trending outfits based on user interactions.
     * 
     * @param outfits List of outfits to analyze
     * @param limit   Maximum number of top outfits to return
     * @return List of top outfits with their score
     */
    public static List<Map.Entry<Outfit, Integer>> getTopTrendingOutfits(List<Outfit> outfits, int limit) {
        // For a real application, this would use real metrics like views, saves, shares
        // For now we'll use a simple algorithm based on creation date + likes

        return outfits.stream()
                .map(outfit -> new AbstractMap.SimpleEntry<>(outfit, calculateOutfitScore(outfit)))
                .sorted(Map.Entry.<Outfit, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Calculate a trending score for an outfit.
     * 
     * @param outfit The outfit to score
     * @return An integer score (higher is more trending)
     */
    private static int calculateOutfitScore(Outfit outfit) {
        // This would be expanded in a real application
        int score = 0;

        // Newer outfits get more points
        if (outfit.getCreatedAt() != null) {
            long daysAgo = ChronoUnit.DAYS.between(
                    outfit.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now());

            // Newer items (last 30 days) get a recency boost
            if (daysAgo <= 30) {
                score += Math.max(0, 30 - daysAgo);
            }
        }

        // Add points for likes (simulated)
        // In a real app, you would have actual user engagement metrics
        score += new Random(outfit.getOutfitId().hashCode()).nextInt(20);

        return score;
    }

    /**
     * Generate a monthly revenue forecast based on historical data.
     * 
     * @return Map of months to projected revenue
     */
    public static Map<String, Double> getRevenueProjection() {
        // In a real application, this would use actual historical sales data
        // Here we'll just generate some sample data
        Map<String, Double> projection = new LinkedHashMap<>();

        // Get current month as starting point
        String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        int currentMonthIndex = LocalDate.now().getMonthValue() - 1;

        // Generate projections for next 6 months
        double baseRevenue = 5000 + new Random().nextDouble() * 5000;
        double growth = 1.05; // 5% monthly growth

        for (int i = 0; i < 6; i++) {
            int monthIndex = (currentMonthIndex + i) % 12;
            double projectedRevenue = baseRevenue * Math.pow(growth, i);
            projection.put(months[monthIndex], projectedRevenue);
        }

        return projection;
    }

    /**
     * Calculate statistical metrics about the store inventory.
     * 
     * @param products List of products to analyze
     * @return Map containing various statistical metrics
     */
    public static Map<String, Object> getInventoryMetrics(List<Product> products) {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalProducts", products.size());
        metrics.put("totalStock", products.stream().mapToInt(Product::getStockQuantity).sum());
        metrics.put("averagePrice", products.stream()
                .mapToDouble(p -> p.getPrice().doubleValue())
                .average()
                .orElse(0.0));
        metrics.put("totalValue", products.stream()
                .mapToDouble(p -> p.getPrice().doubleValue() * p.getStockQuantity())
                .sum());
        metrics.put("lowStockCount", products.stream()
                .filter(p -> p.getStockQuantity() < 5)
                .count());

        return metrics;
    }
}