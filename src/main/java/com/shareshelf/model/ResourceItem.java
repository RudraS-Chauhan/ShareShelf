package com.shareshelf.model;

/**
 * A listed item. The first block of fields mirrors the resource_items table, field for field.
 * ownerName and ownerCompleted are read-only display fields filled by JOINs (not table columns).
 */
public class ResourceItem {

    private int itemId;
    private String ownerId, title, description, category, listingType, itemCondition, location, status;
    private int suggestedDays;          // 0 = none (only BORROW listings have a suggested duration)

    // display-only, filled by a JOIN with students / requests
    private String ownerName;
    private int ownerCompleted;         // owner's completed exchanges (reliability signal)

    public ResourceItem() {}

    public int getItemId() {
        return itemId;
    }
    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getListingType() {
        return listingType;
    }
    public void setListingType(String listingType) {
        this.listingType = listingType;
    }
    public String getItemCondition() {
        return itemCondition;
    }
    public void setItemCondition(String itemCondition) {
        this.itemCondition = itemCondition;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public int getSuggestedDays() {
        return suggestedDays;
    }
    public void setSuggestedDays(int suggestedDays) {
        this.suggestedDays = suggestedDays;
    }
    public String getOwnerName() {
        return ownerName;
    }
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    public int getOwnerCompleted() {
        return ownerCompleted;
    }
    public void setOwnerCompleted(int ownerCompleted) {
        this.ownerCompleted = ownerCompleted;
    }

    /** True for BORROW listings (the only ones that get a return date). */
    public boolean isBorrow() {
        return ListingType.BORROW.name().equals(listingType);
    }
}
