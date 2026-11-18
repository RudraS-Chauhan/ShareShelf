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
}
