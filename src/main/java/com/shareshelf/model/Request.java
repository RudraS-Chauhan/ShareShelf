package com.shareshelf.model;
import java.sql.Date;
import java.sql.Timestamp;

public class Request{

    int requestId, itemId;
    String requesterId, ownerId, message, status;
    boolean contactRevealed;
    Timestamp revealedAt;
    Timestamp createdAt;
    Date expectedReturnDate;

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

}