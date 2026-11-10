package com.shareshelf.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * A borrow/exchange request. The first block of fields mirrors the requests table.
 * The "display-only" fields are filled by JOINs so pages can show names and titles
 * without extra queries; they are not columns of requests.
 */
public class Request {

    private int requestId, itemId;
    private String requesterId, ownerId, message, status;
    private boolean contactRevealed;
    private Timestamp revealedAt;
    private Timestamp createdAt;
    private Date expectedReturnDate;

    // display-only (from JOINs with resource_items / students / requests)
    private String itemTitle, listingType, itemStatus, requesterName, ownerName;
    private int itemSuggestedDays;
    private int requesterCompleted, ownerCompleted;   // reliability signal: completed exchanges

    public Request() {}

    public int getRequestId() {
        return requestId;
    }
    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }
    public int getItemId() {
        return itemId;
    }
    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
    public String getRequesterId() {
        return requesterId;
    }
    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    public Date getExpectedReturnDate() {
        return expectedReturnDate;
    }
    public void setExpectedReturnDate(Date expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
    }
    public boolean isContactRevealed() {
        return contactRevealed;
    }
    public void setContactRevealed(boolean contactRevealed) {
        this.contactRevealed = contactRevealed;
    }
    public Timestamp getRevealedAt() {
        return revealedAt;
    }
    public void setRevealedAt(Timestamp revealedAt) {
        this.revealedAt = revealedAt;
    }

    // ---- display-only fields ----
    public String getItemTitle() {
        return itemTitle;
    }
    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }
    public String getListingType() {
        return listingType;
    }
    public void setListingType(String listingType) {
        this.listingType = listingType;
    }
    public String getItemStatus() {
        return itemStatus;
    }
    public void setItemStatus(String itemStatus) {
        this.itemStatus = itemStatus;
    }
    public int getItemSuggestedDays() {
        return itemSuggestedDays;
    }
    public void setItemSuggestedDays(int itemSuggestedDays) {
        this.itemSuggestedDays = itemSuggestedDays;
    }
    public String getRequesterName() {
        return requesterName;
    }
    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }
    public String getOwnerName() {
        return ownerName;
    }
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    public int getRequesterCompleted() {
        return requesterCompleted;
    }
    public void setRequesterCompleted(int requesterCompleted) {
        this.requesterCompleted = requesterCompleted;
    }
    public int getOwnerCompleted() {
        return ownerCompleted;
    }
    public void setOwnerCompleted(int ownerCompleted) {
        this.ownerCompleted = ownerCompleted;
    }

    /** True when the requested listing is a BORROW listing. */
    public boolean isBorrow() {
        return ListingType.BORROW.name().equals(listingType);
    }
}
