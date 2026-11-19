package com.shareshelf.util;

import static org.junit.jupiter.api.Assertions.*;

import com.shareshelf.model.ListingType;
import org.junit.jupiter.api.Test;

/** Guards the agreed vocabulary (FINAL_SPEC.md) and the small model helpers. */
class AppConstantsTest {

    @Test
    void hostelItemsCategoryWasDroppedOnPurpose() {
        assertEquals(5, AppConstants.CATEGORIES.size());
        assertFalse(AppConstants.CATEGORIES.contains("Hostel Items"));
    }

    @Test
    void defaultBorrowDurationIsOneOfTheOfferedChoices() {
        assertTrue(AppConstants.BORROW_DAYS.contains(AppConstants.DEFAULT_BORROW_DAYS));
        assertTrue(AppConstants.BORROW_DAYS.stream().allMatch(d -> d >= 1 && d <= AppConstants.MAX_BORROW_DAYS));
    }

    @Test
    void listingTypesParseSafely() {
        assertEquals(ListingType.BORROW, ListingType.fromString("borrow"));
        assertNull(ListingType.fromString("SELL"));
        assertNull(ListingType.fromString(null));
    }

    @Test
    void borrowListingsAreRecognised() {
        com.shareshelf.model.ResourceItem item = new com.shareshelf.model.ResourceItem();
        item.setListingType("BORROW");
        assertTrue(item.isBorrow());
        item.setListingType("DONATE");
        assertFalse(item.isBorrow());
    }

    @Test
    void blockSpotsNeedAFloorAndAreAlsoValidLocations() {
        assertTrue(AppConstants.LOCATIONS.containsAll(AppConstants.BLOCK_LOCATIONS));
        assertTrue(AppConstants.LOCATIONS.contains("Cafeteria"));
        assertFalse(AppConstants.BLOCK_LOCATIONS.contains("Library"));
    }

    @Test
    void locationTextCombinesPlaceFloorAndLandmark() {
        assertEquals("C Block, 2nd floor - near the water cooler",
                AppConstants.buildLocation("C Block", "2nd floor", "near the water cooler"));
        assertEquals("Library", AppConstants.buildLocation("Library", "2nd floor", ""));
        assertEquals("Cafeteria - near the juice counter",
                AppConstants.buildLocation("Cafeteria", "", "near the juice counter"));
    }

    @Test
    void longestPossibleLocationFitsTheDatabaseColumn() {
        String longest = AppConstants.buildLocation("Ground in front of D Block", "",
                "x".repeat(AppConstants.MAX_LANDMARK));
        assertTrue(longest.length() <= 100);   // resource_items.location is VARCHAR(100)
    }

    @Test
    void durationsAreOrderedAndLabelledSensibly() {
        assertTrue(AppConstants.DURATION_HOURS.contains(AppConstants.DEFAULT_DURATION_HOURS));
        assertTrue(AppConstants.DURATION_HOURS.stream().allMatch(h -> h >= 1 && h <= AppConstants.MAX_DURATION_HOURS));
        assertEquals("1 hour", AppConstants.durationLabel(1));
        assertEquals("4 hours", AppConstants.durationLabel(4));
        assertEquals("1 day", AppConstants.durationLabel(24));
        assertEquals("7 days", AppConstants.durationLabel(168));
        assertEquals(AppConstants.DURATION_HOURS.size(), AppConstants.durationOptions().size());
    }

    @Test
    void handoverRulesAreSane() {
        assertTrue(AppConstants.HANDOVER_MINUTES >= 5 && AppConstants.HANDOVER_MINUTES <= 30);
        assertTrue(AppConstants.MAX_CODE_ATTEMPTS <= 5, "6-digit codes need a tight attempt limit");
        assertEquals(java.util.List.of("OK", "DAMAGED", "MISSING"), AppConstants.RETURN_CONDITIONS);
    }
}
