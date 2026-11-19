package com.shareshelf.model;

import com.shareshelf.util.AppConstants;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
    private int itemSuggestedDays, itemSuggestedHours;
    private String requesterBranch;
    private int requesterYear, requesterLate;   // late = past returns that were flagged overdue

    // meeting and deadline details, set when the owner accepts
    private LocalDateTime meetTime, dueAt;
    private String meetNote;
    private int borrowHours;           // agreed hours; the clock starts at pickup (0 = old-style request)
    private String returnCondition;    // OK / DAMAGED / MISSING, set at the return scan
    private double requesterStars, ownerStars;   // average of revealed ratings (0 when none)
    private int requesterStarsN, ownerStarsN;
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

    public int getItemSuggestedHours() {
        return itemSuggestedHours;
    }
    public void setItemSuggestedHours(int itemSuggestedHours) {
        this.itemSuggestedHours = itemSuggestedHours;
    }
    public String getRequesterBranch() {
        return requesterBranch;
    }
    public void setRequesterBranch(String requesterBranch) {
        this.requesterBranch = requesterBranch;
    }
    public int getRequesterYear() {
        return requesterYear;
    }
    public void setRequesterYear(int requesterYear) {
        this.requesterYear = requesterYear;
    }
    public int getRequesterLate() {
        return requesterLate;
    }
    public void setRequesterLate(int requesterLate) {
        this.requesterLate = requesterLate;
    }
    public LocalDateTime getMeetTime() {
        return meetTime;
    }
    public void setMeetTime(LocalDateTime meetTime) {
        this.meetTime = meetTime;
    }
    public String getMeetNote() {
        return meetNote;
    }
    public void setMeetNote(String meetNote) {
        this.meetNote = meetNote;
    }
    public LocalDateTime getDueAt() {
        return dueAt;
    }
    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", Locale.ENGLISH);

    /** e.g. "Wed 18 Nov, 3:00 PM"; empty if no meeting time was set. */
    public String getMeetTimeText() {
        return meetTime == null ? "" : meetTime.format(WHEN);
    }

    /** Return deadline as text: a date and time for timed loans, the date for older ones, "" if none. */
    public String getDueText() {
        if (dueAt != null) {
            return dueAt.format(WHEN);
        }
        return expectedReturnDate == null ? "" : expectedReturnDate.toString();
    }

    /** The listing's suggested duration as text ("2 hours", "3 days"), or "". */
    public String getItemDurationLabel() {
        int h = itemSuggestedHours > 0 ? itemSuggestedHours : itemSuggestedDays * 24;
        return h > 0 ? AppConstants.durationLabel(h) : "";
    }

    /** True when the requested listing is a BORROW listing. */
    public boolean isBorrow() {
        return ListingType.BORROW.name().equals(listingType);
    }

    public int getBorrowHours() { return borrowHours; }
    public void setBorrowHours(int v) { borrowHours = v; }
    public String getReturnCondition() { return returnCondition; }
    public void setReturnCondition(String v) { returnCondition = v; }

    /** "Timer starts at pickup (24 hours)" while the pickup has not happened yet; "" otherwise. */
    public String getTimerNote() {
        boolean waiting = "ACCEPTED".equals(status) && "RESERVED".equals(itemStatus) && borrowHours > 0;
        return waiting ? "Timer starts at pickup (" + com.shareshelf.util.AppConstants.durationLabel(borrowHours) + ")" : "";
    }

    /** Short text for the return condition, "" when not returned yet. */
    public String getReturnConditionText() {
        if (returnCondition == null) {
            return "";
        }
        return switch (returnCondition) {
            case "OK" -> "Returned in good condition";
            case "DAMAGED" -> "Returned damaged";
            case "MISSING" -> "Returned with parts missing";
            default -> "";
        };
    }

    public double getRequesterStars() { return requesterStars; }
    public void setRequesterStars(double v) { requesterStars = v; }
    public double getOwnerStars() { return ownerStars; }
    public void setOwnerStars(double v) { ownerStars = v; }
    public int getRequesterStarsN() { return requesterStarsN; }
    public void setRequesterStarsN(int v) { requesterStarsN = v; }
    public int getOwnerStarsN() { return ownerStarsN; }
    public void setOwnerStarsN(int v) { ownerStarsN = v; }

    /** "4.5 (3)" or "" when nobody has rated yet. */
    public String getRequesterStarsText() { return starsText(requesterStars, requesterStarsN); }
    public String getOwnerStarsText() { return starsText(ownerStars, ownerStarsN); }

    /** Badge for the borrower: earned by 3+ finished exchanges, no late returns, and 4+ stars on average. */
    public String getRequesterBadge() {
        return badge("Reliable borrower", requesterCompleted, requesterLate, requesterStars, requesterStarsN);
    }

    /** Badge for the lender: 3+ finished exchanges and 4+ stars on average. */
    public String getOwnerBadge() {
        return badge("Trusted lender", ownerCompleted, 0, ownerStars, ownerStarsN);
    }

    private static String starsText(double avg, int n) {
        return n == 0 ? "" : String.format(Locale.ENGLISH, "%.1f (%d)", avg, n);
    }

    public static String badge(String name, int completed, int late, double avg, int n) {
        boolean earned = completed >= com.shareshelf.util.AppConstants.BADGE_MIN_EXCHANGES && late == 0
                && n > 0 && avg >= com.shareshelf.util.AppConstants.BADGE_MIN_STARS;
        return earned ? name : "";
    }
}
