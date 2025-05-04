package com.fashionstore.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a user's style preference
 */
public class StylePreference implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String type;       // The type of preference (e.g., "color", "style", "brand")
    private String value;      // The value of the preference (e.g., "blue", "casual", "Nike")
    private double weight;     // The weight of this preference (0.0-1.0)
    
    /**
     * Creates a new style preference
     * 
     * @param type The type of preference
     * @param value The value of the preference
     * @param weight The weight of this preference (0.0-1.0)
     */
    public StylePreference(String type, String value, double weight) {
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.value = Objects.requireNonNull(value, "Value cannot be null");
        setWeight(weight);
    }
    
    /**
     * Creates a new style preference with default weight (1.0)
     * 
     * @param type The type of preference
     * @param value The value of the preference
     */
    public StylePreference(String type, String value) {
        this(type, value, 1.0);
    }
    
    /**
     * Gets the type of preference
     * 
     * @return The preference type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Gets the category of preference (alias for getType for compatibility)
     * 
     * @return The preference category/type
     */
    public String getCategory() {
        return type;
    }
    
    /**
     * Sets the type of preference
     * 
     * @param type The preference type
     */
    public void setType(String type) {
        this.type = Objects.requireNonNull(type, "Type cannot be null");
    }
    
    /**
     * Gets the value of the preference
     * 
     * @return The preference value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Sets the value of the preference
     * 
     * @param value The preference value
     */
    public void setValue(String value) {
        this.value = Objects.requireNonNull(value, "Value cannot be null");
    }
    
    /**
     * Gets the weight of this preference
     * 
     * @return The preference weight
     */
    public double getWeight() {
        return weight;
    }
    
    /**
     * Sets the weight of this preference
     * 
     * @param weight The preference weight (clamped to 0.0-1.0)
     */
    public void setWeight(double weight) {
        this.weight = Math.max(0.0, Math.min(1.0, weight));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StylePreference that = (StylePreference) o;
        return type.equals(that.type) && value.equals(that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }
    
    @Override
    public String toString() {
        return "StylePreference{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                ", weight=" + weight +
                '}';
    }
}