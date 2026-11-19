package com.shareshelf.model;

import com.shareshelf.util.AppConstants;

/**
 * A listed item. The first block of fields mirrors the resource_items table, field for field.
 * ownerName and ownerCompleted are read-only display fields filled by JOINs (not table columns).
 */
public class ResourceItem {

    private int itemId;
    private String ownerId, title, description, category, listingType, itemCondition, location, status;
    private int suggestedDays;          // legacy listings: whole days. 0 = none
    private int suggestedHours;         // newer listings: hours (1 h to 30 days). 0 = use suggestedDays

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
    public int getSuggestedHours() {
        return suggestedHours;
    }
    public void setSuggestedHours(int suggestedHours) {
        this.suggestedHours = suggestedHours;
    }

    /** The suggested duration in hours, whichever way the listing stored it (0 = none). */
    public int getDurationHours() {
        return suggestedHours > 0 ? suggestedHours : suggestedDays * 24;
    }

    /** Text such as "2 hours" or "3 days"; empty when the listing has no suggested duration. */
    public String getDurationLabel() {
        int h = getDurationHours();
        return h > 0 ? AppConstants.durationLabel(h) : "";
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
