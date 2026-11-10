package com.shareshelf.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.shareshelf.model.Request;
import com.shareshelf.model.ResourceItem;
import com.shareshelf.model.Student;
import com.shareshelf.util.DBConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the DAO layer against a real MySQL database (schema.sql already loaded).
 * Skipped automatically when no database is reachable, so `mvn test` still passes on a laptop without MySQL.
 * Every run uses fresh unique admission numbers, so it never collides with existing data.
 */
class DaoIntegrationTest {

    private static final java.util.concurrent.atomic.AtomicInteger COUNTER = new java.util.concurrent.atomic.AtomicInteger();

    private final StudentDAO students = new StudentDAO();
    private final ListingDAO listings = new ListingDAO();
    private final RequestDAO requests = new RequestDAO();

    @BeforeAll
    static void needsDatabase() {
        boolean up;
        try (Connection c = DBConnection.getConnection()) {
            up = c.isValid(2);
        } catch (SQLException e) {
            up = false;
        }
        assumeTrue(up, "No database reachable: skipping DAO integration tests");
    }

    private String uniqueId(String prefix) {
        String t = Long.toString(System.nanoTime(), 36).toUpperCase();
        return prefix + t.substring(Math.max(0, t.length() - 10)) + COUNTER.incrementAndGet();
    }

    private Student newStudent(String id) {
        Student s = new Student();
        s.setStudentId(id);
        s.setName("Test " + id);
        s.setBranch("CSE");
        s.setYear(2);
        s.setEmail(id.toLowerCase() + "@galgotiasuniversity.ac.in");
        s.setHostelOrDay("Hostel");
        s.setWhatsappNumber("919876543210");
        s.setPassword("Passw0rd!x");
        return s;
    }

    private ResourceItem newItem(String ownerId, String type) {
        ResourceItem item = new ResourceItem();
        item.setOwnerId(ownerId);
        item.setTitle("Calculator " + ownerId);
        item.setDescription("test");
        item.setCategory("Stationery");
        item.setListingType(type);
        item.setItemCondition("Good");
        item.setLocation("Library");
        item.setSuggestedDays("BORROW".equals(type) ? 3 : 0);
        return item;
    }

    private Request newRequest(ResourceItem item, String requesterId) {
        Request r = new Request();
        r.setItemId(item.getItemId());
        r.setRequesterId(requesterId);
        r.setOwnerId(item.getOwnerId());
        r.setMessage("please");
        return r;
    }

    @Test
    void passwordIsStoredHashedAndLoginChecksTheHash() throws SQLException {
        String id = uniqueId("U1");
        assertTrue(students.register(newStudent(id)));
        assertTrue(students.isDuplicateId(id));
        assertFalse(students.register(newStudent(id)), "duplicate admission number must be refused");

        assertNotNull(students.login(id, "Passw0rd!x"));
        assertNull(students.login(id, "wrong-password1"));
        assertNull(students.login("NOSUCHUSER1", "Passw0rd!x"));

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT password FROM students WHERE student_id = ?")) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertFalse(rs.getString(1).contains("Passw0rd"), "plaintext password must never be stored");
                assertTrue(rs.getString(1).startsWith("pbkdf2$"));
            }
        }
    }

    @Test
    void listingForUnknownOwnerIsRejectedByTheForeignKey() {
        assertThrows(com.shareshelf.util.DataAccessException.class,
                () -> listings.insert(newItem("NOSUCHOWNER1", "BORROW")));
    }

    @Test
    void searchIsCaseInsensitiveTrimmedAndOnlyShowsAvailableItems() {
        String owner = uniqueId("U2");
        students.register(newStudent(owner));
        ResourceItem item = newItem(owner, "BORROW");
        assertTrue(listings.insert(item));

        assertFalse(listings.searchAvailable("  CALCULATOR " + owner + "  ", "", "").isEmpty());
        assertTrue(listings.searchAvailable("calculator " + owner, "Lab Equipment", "").isEmpty());
        assertTrue(listings.searchAvailable("calculator " + owner, "", "DONATE").isEmpty());
        assertTrue(listings.searchAvailable("%", "", "").stream().noneMatch(i -> i.getItemId() == item.getItemId()),
                "% must be treated as a literal, not a wildcard");

        assertTrue(listings.updateStatus(item.getItemId(), "CLOSED"));
        assertTrue(listings.searchAvailable("calculator " + owner, "", "").isEmpty());
    }

    @Test
    void acceptIsATransactionAndOnlyOneRequestCanWin() {
        String owner = uniqueId("U3"), a = uniqueId("U4"), b = uniqueId("U5");
        students.register(newStudent(owner));
        students.register(newStudent(a));
        students.register(newStudent(b));
        ResourceItem item = newItem(owner, "BORROW");
        listings.insert(item);

        Request ra = newRequest(item, a), rb = newRequest(item, b);
        assertTrue(requests.insert(ra));
        assertTrue(requests.insert(rb));
        assertTrue(requests.hasDuplicatePending(item.getItemId(), a));

        Date due = Date.valueOf(LocalDate.now().plusDays(3));
        assertTrue(requests.accept(ra.getRequestId(), item.getItemId(), due));

        assertEquals("ACCEPTED", requests.findById(ra.getRequestId()).getStatus());
        assertEquals("RESERVED", listings.findById(item.getItemId()).getStatus());
        assertEquals("REJECTED", requests.findById(rb.getRequestId()).getStatus(), "competing request auto-rejected");

        // second accept must fail and change nothing
        assertFalse(requests.accept(rb.getRequestId(), item.getItemId(), due));
        assertEquals("ACCEPTED", requests.findById(ra.getRequestId()).getStatus());
    }

    @Test
    void completeReopensBorrowItemsAndClosesGiveaways() {
        String owner = uniqueId("U6"), taker = uniqueId("U7");
        students.register(newStudent(owner));
        students.register(newStudent(taker));

        ResourceItem borrow = newItem(owner, "BORROW");
        listings.insert(borrow);
        Request r1 = newRequest(borrow, taker);
        requests.insert(r1);
        requests.accept(r1.getRequestId(), borrow.getItemId(), Date.valueOf(LocalDate.now().plusDays(1)));
        assertTrue(requests.complete(r1.getRequestId(), borrow.getItemId()));
        assertEquals("AVAILABLE", listings.findById(borrow.getItemId()).getStatus());
        assertEquals(1, requests.countCompleted(owner));
        assertFalse(requests.complete(r1.getRequestId(), borrow.getItemId()), "cannot complete twice");

        ResourceItem gift = newItem(owner, "DONATE");
        listings.insert(gift);
        Request r2 = newRequest(gift, taker);
        requests.insert(r2);
        requests.accept(r2.getRequestId(), gift.getItemId(), null);
        assertTrue(requests.complete(r2.getRequestId(), gift.getItemId()));
        assertEquals("CLOSED", listings.findById(gift.getItemId()).getStatus());
    }

    @Test
    void overdueFlaggingAndTheWhatsAppSafetyNet() throws SQLException {
        String owner = uniqueId("U8"), taker = uniqueId("U9"), other = uniqueId("UA");
        students.register(newStudent(owner));
        students.register(newStudent(taker));
        students.register(newStudent(other));
        ResourceItem item = newItem(owner, "BORROW");
        listings.insert(item);
        Request r = newRequest(item, taker);
        requests.insert(r);
        requests.accept(r.getRequestId(), item.getItemId(), Date.valueOf(LocalDate.now().plusDays(2)));

        assertNull(requests.revealContact(r.getRequestId(), owner), "not overdue yet: must stay hidden");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE requests SET expected_return_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY) WHERE request_id = ?")) {
            ps.setInt(1, r.getRequestId());
            ps.executeUpdate();
        }
        assertTrue(requests.markOverdue() >= 1);
        assertEquals("OVERDUE", requests.findById(r.getRequestId()).getStatus());

        assertNull(requests.revealContact(r.getRequestId(), other), "only the request's owner may reveal");
        assertEquals("919876543210", requests.revealContact(r.getRequestId(), owner));
        assertTrue(requests.findById(r.getRequestId()).isContactRevealed(), "reveal is logged on the request");
    }

    @Test
    void ownerAndRequesterListsContainTheRequest() {
        String owner = uniqueId("UB"), taker = uniqueId("UC");
        students.register(newStudent(owner));
        students.register(newStudent(taker));
        ResourceItem item = newItem(owner, "GIVEAWAY");
        listings.insert(item);
        Request r = newRequest(item, taker);
        requests.insert(r);

        List<Request> received = requests.findByOwner(owner);
        List<Request> sent = requests.findByRequester(taker);
        assertTrue(received.stream().anyMatch(x -> x.getRequestId() == r.getRequestId()));
        assertTrue(sent.stream().anyMatch(x -> x.getRequestId() == r.getRequestId()));
        assertEquals(item.getTitle(), sent.get(0).getItemTitle());
        assertTrue(requests.cancel(r.getRequestId()));
        assertFalse(requests.reject(r.getRequestId()), "a cancelled request cannot be rejected");
    }
}
