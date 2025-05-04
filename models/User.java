package com.fashionstore.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.fashionstore.utils.PasswordUtil;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private Date dateRegistered;
    private Date lastLogin;
    private List<String> wardrobeItemIds; // References to product IDs
    private List<String> outfitIds; // References to outfit IDs
    private List<StylePreference> stylePreferences;
    private boolean isDeactivated = false;
    private Date deactivationDate = null;
    private boolean isDarkModeEnabled = false;
    private boolean isBanned = false;
    private String banReason = null;
    private Date banExpiration = null;

    public User(String username, String email, String passwordHash) {
        this.userId = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;

        // Special case for admin user
        if (username.equals("admin") && passwordHash.equals("admin")) {
            System.out.println("Creating admin user with special password handling");
            this.passwordHash = passwordHash; // Store admin password as is
        }
        // Check if it looks like it's already hashed (base64 string)
        else if (passwordHash.length() > 20 && isBase64(passwordHash)) {
            System.out.println("Using pre-hashed password for user: " + username);
            this.passwordHash = passwordHash;
        }
        // Otherwise hash the password
        else {
            System.out.println("Hashing password for user: " + username);
            try {
                this.passwordHash = PasswordUtil.hashPassword(passwordHash);
            } catch (Exception e) {
                System.err.println("Error hashing password, storing as plaintext: " + e.getMessage());
                this.passwordHash = passwordHash; // Fallback to plaintext if hashing fails
            }
        }

        this.dateRegistered = new Date();
        this.wardrobeItemIds = new ArrayList<>();
        this.outfitIds = new ArrayList<>();
        this.stylePreferences = new ArrayList<>();
    }

    /**
     * Simple check if a string appears to be Base64 encoded
     */
    private boolean isBase64(String str) {
        if (str == null || str.length() % 4 != 0) {
            return false;
        }

        // Base64 should only contain these characters
        String pattern = "^[A-Za-z0-9+/]*={0,2}$";
        return str.matches(pattern);
    }

    /**
     * Verify if the provided password matches this user's password
     */
    public boolean verifyPassword(String password) {
        // Direct comparison for admin or plaintext passwords
        if ((this.username.equals("admin") && password.equals(this.passwordHash)) ||
                password.equals(this.passwordHash)) {
            return true;
        }

        // Try password verification with utility
        try {
            return PasswordUtil.verifyPassword(password, this.passwordHash);
        } catch (Exception e) {
            System.err.println("Error in password verification: " + e.getMessage());
            return false;
        }
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getDateRegistered() {
        return dateRegistered;
    }

    public void setDateRegistered(Date dateRegistered) {
        this.dateRegistered = dateRegistered;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void updateLastLogin() {
        this.lastLogin = new Date();
    }

    public List<String> getWardrobeItemIds() {
        return new ArrayList<>(wardrobeItemIds); // Return a copy to prevent external modification
    }

    public void addToWardrobe(String productId) {
        if (!wardrobeItemIds.contains(productId)) {
            this.wardrobeItemIds.add(productId);
        }
    }

    public boolean removeFromWardrobe(String productId) {
        return this.wardrobeItemIds.remove(productId);
    }

    public List<String> getOutfitIds() {
        return new ArrayList<>(outfitIds); // Return a copy to prevent external modification
    }

    public void addOutfit(String outfitId) {
        if (!outfitIds.contains(outfitId)) {
            this.outfitIds.add(outfitId);
        }
    }

    public boolean removeOutfit(String outfitId) {
        System.out.println("User.removeOutfit: Attempting to remove " + outfitId +
                " from user " + username + " (outfitIds.size=" + outfitIds.size() + ")");
        if (outfitId == null) {
            System.err.println("User.removeOutfit: outfitId is null");
            return false;
        }

        if (outfitIds.contains(outfitId)) {
            System.out.println("User.removeOutfit: Found outfit in user's list");
            boolean result = this.outfitIds.remove(outfitId);
            System.out.println("User.removeOutfit: Removal result: " + result +
                    " (new outfitIds.size=" + outfitIds.size() + ")");
            return result;
        } else {
            System.out.println("User.removeOutfit: Outfit not found in user's list");
            return false;
        }
    }

    public List<StylePreference> getStylePreferences() {
        return new ArrayList<>(stylePreferences); // Return a copy to prevent external modification
    }

    public void addStylePreference(StylePreference preference) {
        if (!stylePreferences.contains(preference)) {
            this.stylePreferences.add(preference);
        }
    }

    public boolean removeStylePreference(StylePreference preference) {
        return this.stylePreferences.remove(preference);
    }

    /**
     * Returns an array of outfit IDs
     * 
     * @return String array containing all outfit IDs
     */
    public String[] getOutfits() {
        return outfitIds.toArray(new String[0]);
    }

    /**
     * Checks if the user has a specific outfit
     * 
     * @param outfitId The outfit ID to check
     * @return true if the user has the outfit, false otherwise
     */
    public boolean hasOutfit(String outfitId) {
        return outfitIds.contains(outfitId);
    }

    /**
     * Checks if the user has a specific wardrobe item
     * 
     * @param productId The product ID to check
     * @return true if the user has the item, false otherwise
     */
    public boolean hasWardrobeItem(String productId) {
        return wardrobeItemIds.contains(productId);
    }

    /**
     * Deactivates the user account temporarily for a week.
     * After a week, the account can be reactivated.
     */
    public void deactivateAccount() {
        this.isDeactivated = true;
        this.deactivationDate = new Date();
    }

    /**
     * Reactivates a deactivated account.
     */
    public void reactivateAccount() {
        this.isDeactivated = false;
        this.deactivationDate = null;
    }

    /**
     * Checks if the user account is currently deactivated.
     *
     * @return true if the account is deactivated, false otherwise
     */
    public boolean isDeactivated() {
        return isDeactivated;
    }

    /**
     * Gets the date when the account was deactivated.
     *
     * @return the deactivation date, or null if not deactivated
     */
    public Date getDeactivationDate() {
        return deactivationDate;
    }

    /**
     * Checks if the deactivation period has expired (one week).
     * 
     * @return true if the account was deactivated more than a week ago
     */
    public boolean isDeactivationPeriodExpired() {
        if (!isDeactivated || deactivationDate == null) {
            return false;
        }

        // Calculate if one week has passed since deactivation
        long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L;
        Date oneWeekAfterDeactivation = new Date(deactivationDate.getTime() + oneWeekInMillis);
        Date now = new Date();

        return now.after(oneWeekAfterDeactivation);
    }

    /**
     * Returns the number of days left before the account is permanently deleted.
     * 
     * @return days left, or 0 if already expired
     */
    public int getDaysLeftUntilPermanentDeletion() {
        if (!isDeactivated || deactivationDate == null) {
            return 0;
        }

        long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L;
        Date expirationDate = new Date(deactivationDate.getTime() + oneWeekInMillis);
        Date now = new Date();

        if (now.after(expirationDate)) {
            return 0;
        }

        long millisLeft = expirationDate.getTime() - now.getTime();
        int daysLeft = (int) (millisLeft / (24 * 60 * 60 * 1000L));
        return daysLeft;
    }

    /**
     * Sets whether dark mode is enabled for this user.
     *
     * @param enabled true to enable dark mode, false otherwise
     */
    public void setDarkModeEnabled(boolean enabled) {
        this.isDarkModeEnabled = enabled;
    }

    /**
     * Checks if dark mode is enabled for this user.
     *
     * @return true if dark mode is enabled, false otherwise
     */
    public boolean isDarkModeEnabled() {
        return isDarkModeEnabled;
    }

    /**
     * Bans the user account.
     *
     * @param reason The reason for the ban
     */
    public void banUser(String reason) {
        this.isBanned = true;
        this.banReason = reason;
        this.banExpiration = null; // Permanent ban by default
    }

    /**
     * Bans the user account temporarily.
     *
     * @param reason The reason for the ban
     * @param days   Number of days for the ban
     */
    public void banUserTemporarily(String reason, int days) {
        this.isBanned = true;
        this.banReason = reason;

        // Calculate expiration date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        this.banExpiration = calendar.getTime();
    }

    /**
     * Checks if the user's ban has expired.
     * 
     * @return true if the ban has expired, false if it's permanent or still active
     */
    public boolean isBanExpired() {
        if (!isBanned) {
            return false; // Not banned
        }

        if (banExpiration == null) {
            return false; // Permanent ban
        }

        Date now = new Date();
        return now.after(banExpiration);
    }

    /**
     * Gets the ban expiration date.
     *
     * @return the ban expiration date, or null if permanent ban or not banned
     */
    public Date getBanExpiration() {
        return banExpiration;
    }

    /**
     * Gets the number of days left on the ban.
     *
     * @return the number of days left, or -1 if permanent ban
     */
    public int getDaysLeftOnBan() {
        if (!isBanned) {
            return 0; // Not banned
        }

        if (banExpiration == null) {
            return -1; // Permanent ban
        }

        Date now = new Date();
        if (now.after(banExpiration)) {
            return 0; // Ban expired
        }

        long millisLeft = banExpiration.getTime() - now.getTime();
        int daysLeft = (int) (millisLeft / (24 * 60 * 60 * 1000L));
        return daysLeft > 0 ? daysLeft : 1; // At least 1 day if there's any time left
    }

    /**
     * Unbans the user account.
     */
    public void unbanUser() {
        this.isBanned = false;
        this.banReason = null;
        this.banExpiration = null;
    }

    /**
     * Checks if the user is banned.
     *
     * @return true if the user is banned, false otherwise
     */
    public boolean isBanned() {
        return isBanned;
    }

    /**
     * Gets the reason for the ban.
     *
     * @return the ban reason, or null if not banned
     */
    public String getBanReason() {
        return banReason;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", wardrobeItems=" + wardrobeItemIds.size() +
                ", outfits=" + outfitIds.size() +
                ", isDeactivated=" + isDeactivated +
                ", isBanned=" + isBanned +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}