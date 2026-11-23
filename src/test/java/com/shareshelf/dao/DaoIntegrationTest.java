package com.shareshelf.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.shareshelf.model.Request;
import com.shareshelf.model.ResourceItem;
import com.shareshelf.model.Student;
import com.shareshelf.util.AppConstants;
import com.shareshelf.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
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
    private final RequestTransactionService requestTx = new RequestTransactionService();

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
        return newStudent(id, "919876543210");
    }

    private Student newStudent(String id, String whatsapp) {
        Student s = new Student();
        s.setStudentId(id);
        s.setName("Test " + id);
        s.setBranch("CSE");
        s.setYear(2);
        s.setEmail(id.toLowerCase() + "@galgotiasuniversity.ac.in");
        s.setHostelOrDay("Hostel");
        s.setWhatsappNumber(whatsapp);
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

        LocalDateTime due = LocalDateTime.now(AppConstants.CAMPUS_ZONE).plusDays(3);
        assertTrue(requestTx.accept(ra.getRequestId(), item.getItemId(), due, null, null));

        assertEquals("ACCEPTED", requests.findById(ra.getRequestId()).getStatus());
        assertEquals("RESERVED", listings.findById(item.getItemId()).getStatus());
        assertEquals("REJECTED", requests.findById(rb.getRequestId()).getStatus(), "competing request auto-rejected");

        // second accept must fail and change nothing
        assertFalse(requestTx.accept(rb.getRequestId(), item.getItemId(), due, null, null));
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
        requestTx.accept(r1.getRequestId(), borrow.getItemId(), LocalDateTime.now(AppConstants.CAMPUS_ZONE).plusDays(1), null, null);
        assertTrue(requestTx.complete(r1.getRequestId(), borrow.getItemId()));
        assertEquals("AVAILABLE", listings.findById(borrow.getItemId()).getStatus());
        assertEquals(1, requests.countCompleted(owner));
        assertFalse(requestTx.complete(r1.getRequestId(), borrow.getItemId()), "cannot complete twice");

        ResourceItem gift = newItem(owner, "DONATE");
        listings.insert(gift);
        Request r2 = newRequest(gift, taker);
        requests.insert(r2);
        requestTx.accept(r2.getRequestId(), gift.getItemId(), null, null, null);
        assertTrue(requestTx.complete(r2.getRequestId(), gift.getItemId()));
        assertEquals("CLOSED", listings.findById(gift.getItemId()).getStatus());
    }

    @Test
    void overdueFlaggingAndTheWhatsAppSafetyNet() throws SQLException {
        String owner = uniqueId("U8"), taker = uniqueId("U9"), other = uniqueId("UA");
        students.register(newStudent(owner, "919111111111"));
        students.register(newStudent(taker, "919876543210"));
        students.register(newStudent(other, "919222222222"));
        ResourceItem item = newItem(owner, "BORROW");
        listings.insert(item);
        Request r = newRequest(item, taker);
        requests.insert(r);
        requestTx.accept(r.getRequestId(), item.getItemId(), LocalDateTime.now(AppConstants.CAMPUS_ZONE).plusDays(2), null, null);

        assertNull(requestTx.revealContact(r.getRequestId(), owner), "not overdue yet: must stay hidden");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE requests SET expected_return_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY), due_at = DATE_SUB(NOW(), INTERVAL 1 DAY) WHERE request_id = ?")) {
            ps.setInt(1, r.getRequestId());
            ps.executeUpdate();
        }
        assertTrue(requestTx.markOverdue() >= 1);
        assertEquals("OVERDUE", requests.findById(r.getRequestId()).getStatus());

        assertNull(requestTx.revealContact(r.getRequestId(), other), "only the request's owner may reveal");
        assertEquals("919876543210", requestTx.revealContact(r.getRequestId(), owner),
                "the owner gets the BORROWER's number, never their own (919111111111)");
        assertTrue(requests.findById(r.getRequestId()).isContactRevealed(), "reveal is logged on the request");
    }

    @Test
    void sameNumberForOwnerAndBorrowerRevealsNothingAndLogsNothing() throws SQLException {
        String owner = uniqueId("UF"), taker = uniqueId("UG");
        students.register(newStudent(owner, "919333333333"));
        students.register(newStudent(taker, "919333333333"));   // two test accounts on one phone
        ResourceItem item = newItem(owner, "BORROW");
        listings.insert(item);
        Request r = newRequest(item, taker);
        requests.insert(r);
        requestTx.accept(r.getRequestId(), item.getItemId(), LocalDateTime.now(AppConstants.CAMPUS_ZONE).plusDays(2), null, null);
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE requests SET expected_return_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY), due_at = DATE_SUB(NOW(), INTERVAL 1 DAY) WHERE request_id = ?")) {
            ps.setInt(1, r.getRequestId());
            ps.executeUpdate();
        }
        assertTrue(requestTx.markOverdue() >= 1);
        assertEquals(RequestTransactionService.SAME_NUMBER, requestTx.revealContact(r.getRequestId(), owner));
        assertFalse(requests.findById(r.getRequestId()).isContactRevealed(), "nothing was revealed, so nothing is logged");
    }

    @Test
    void meetingDetailsAreStoredAndTimedLoansBecomeOverdueByTheMinute() {
        String owner = uniqueId("UD"), taker = uniqueId("UE");
        students.register(newStudent(owner));
        students.register(newStudent(taker));
        ResourceItem item = newItem(owner, "BORROW");
        item.setSuggestedHours(2);
        listings.insert(item);
        assertEquals(2, listings.findById(item.getItemId()).getDurationHours());
        assertEquals("2 hours", listings.findById(item.getItemId()).getDurationLabel());

        Request r = newRequest(item, taker);
        requests.insert(r);
        LocalDateTime meet = LocalDateTime.now(AppConstants.CAMPUS_ZONE).minusHours(3).withNano(0);
        LocalDateTime due = meet.plusHours(2);          // already one hour in the past
        assertTrue(requestTx.accept(r.getRequestId(), item.getItemId(), due, meet, "table near the left water outlet"));

        Request loaded = requests.findById(r.getRequestId());
        assertEquals(meet, loaded.getMeetTime());
        assertEquals(due, loaded.getDueAt());
        assertEquals("table near the left water outlet", loaded.getMeetNote());
        assertEquals(0, loaded.getRequesterLate());

        assertTrue(requestTx.markOverdue() >= 1, "due_at has passed, so it is flagged even though the date is today");
        loaded = requests.findById(r.getRequestId());
        assertEquals("OVERDUE", loaded.getStatus());
        assertEquals(1, loaded.getRequesterLate(), "the late return is remembered for the requester's record");
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

    @Test
    void handoverCodeWorksOnceExpiresWithNewOnesAndLocksAfterWrongGuesses() {
        String owner = uniqueId("U6"), borrower = uniqueId("U7");
        students.register(newStudent(owner));
        students.register(newStudent(borrower));
        ResourceItem item = newItem(owner, "BORROW");
        listings.insert(item);
        Request r = newRequest(item, borrower);
        requests.insert(r);
        HandoverDAO tokens = new HandoverDAO();

        HandoverDAO.Issued first = tokens.issue(r.getRequestId(), HandoverDAO.HANDOVER);
        assertEquals(6, first.code().length());
        assertEquals(r.getRequestId(), tokens.requestIdOfToken(first.token()));
        assertEquals(-1, tokens.requestIdOfToken("nope"));

        // a newer token cancels the older one
        HandoverDAO.Issued second = tokens.issue(r.getRequestId(), HandoverDAO.HANDOVER);
        assertFalse(tokens.redeem(r.getRequestId(), HandoverDAO.HANDOVER, first.token(), null));
        // a token for a handover cannot be used as a return
        assertFalse(tokens.redeem(r.getRequestId(), HandoverDAO.RETURN, second.token(), null));
        // the real one works exactly once
        assertTrue(tokens.redeem(r.getRequestId(), HandoverDAO.HANDOVER, second.token(), null));
        assertFalse(tokens.redeem(r.getRequestId(), HandoverDAO.HANDOVER, second.token(), null));

        // guessing the code: 5 wrong tries lock it, even for the right code
        HandoverDAO.Issued third = tokens.issue(r.getRequestId(), HandoverDAO.RETURN);
        String wrong = third.code().equals("000000") ? "000001" : "000000";
        for (int i = 0; i < AppConstants.MAX_CODE_ATTEMPTS; i++) {
            assertFalse(tokens.redeem(r.getRequestId(), HandoverDAO.RETURN, null, wrong));
        }
        assertFalse(tokens.redeem(r.getRequestId(), HandoverDAO.RETURN, null, third.code()));

        // expired tokens are refused
        HandoverDAO.Issued fourth = tokens.issue(r.getRequestId(), HandoverDAO.RETURN);
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE handover_tokens SET expires_at = ? WHERE token = ?")) {
            ps.setObject(1, LocalDateTime.now(AppConstants.CAMPUS_ZONE).minusMinutes(1));
            ps.setString(2, fourth.token());
            ps.executeUpdate();
        } catch (SQLException e) {
            fail(e);
        }
        assertFalse(tokens.redeem(r.getRequestId(), HandoverDAO.RETURN, fourth.token(), null));
    }

    @Test
    void ratingsAreOnePerPersonValidatedAndOnlyCountOnceBothSidesRated() {
        String owner = uniqueId("U8"), borrower = uniqueId("U9");
        students.register(newStudent(owner));
        students.register(newStudent(borrower));
        ResourceItem item = newItem(owner, "BORROW");
        listings.insert(item);
        Request r = newRequest(item, borrower);
        requests.insert(r);
        assertTrue(requestTx.accept(r.getRequestId(), item.getItemId(), LocalDateTime.now().plusDays(1), null, ""));
        assertTrue(requestTx.complete(r.getRequestId(), item.getItemId()));
        RatingDAO ratings = new RatingDAO();
        assertTrue(ratings.pendingFor(borrower).isEmpty(), "a return without a scan creates no rating duty");
        requests.setReturnCondition(r.getRequestId(), "OK");   // what the return scan does

        // the borrower owes a rating for the finished borrow
        assertEquals(1, ratings.pendingFor(borrower).size());
        assertEquals(1, ratings.pendingFor(owner).size(), "the lender owes a rating too");
        assertTrue(ratings.pendingFor(owner).get(0).isRateBorrower());

        assertFalse(ratings.add(r.getRequestId(), owner, borrower, 0, "OK", ""), "0 stars refused");
        assertFalse(ratings.add(r.getRequestId(), owner, borrower, 6, "OK", ""), "6 stars refused");
        assertTrue(ratings.add(r.getRequestId(), owner, borrower, 4, "OK", "good"));
        assertFalse(ratings.add(r.getRequestId(), owner, borrower, 5, "OK", "again"), "one rating per person");

        // hidden until the borrower has rated too
        assertEquals(0, requests.findById(r.getRequestId()).getRequesterStarsN());
        assertTrue(ratings.add(r.getRequestId(), borrower, owner, 5, null, "nice"));
        assertTrue(ratings.pendingFor(borrower).isEmpty());
        assertTrue(ratings.pendingFor(owner).isEmpty());
        Request shown = requests.findById(r.getRequestId());
        assertEquals(1, shown.getRequesterStarsN());
        assertEquals(4.0, shown.getRequesterStars(), 0.001);
        assertEquals("4.0 (1)", shown.getRequesterStarsText());
        assertEquals("", shown.getRequesterBadge(), "one exchange is not enough for a badge");

        // the public profile uses the same rule and never exposes contact details
        com.shareshelf.model.Profile profile = new ProfileDAO().load(borrower);
        assertEquals(1, profile.getBorrowerStarsN());
        assertEquals(4.0, profile.getBorrowerStars(), 0.001);
        assertEquals(0, profile.getLenderStarsN());
        assertEquals("good", profile.getReviews().get(0).getComment());
        assertEquals("borrower", profile.getReviews().get(0).getRole());
        assertNull(new ProfileDAO().load("NO_SUCH_STUDENT"));
    }

    @Test
    void borrowClockStartsAtVerifiedPickupNotAtAccept() {
        String owner = uniqueId("U11"), borrower = uniqueId("U12");
        students.register(newStudent(owner));
        students.register(newStudent(borrower));
        ResourceItem item = newItem(owner, "BORROW");
        listings.insert(item);
        Request r = newRequest(item, borrower);
        requests.insert(r);

        String before = requests.stateSignature(owner);
        assertTrue(requestTx.accept(r.getRequestId(), item.getItemId(), null, null, "", 24));
        Request accepted = requests.findById(r.getRequestId());
        assertEquals(24, accepted.getBorrowHours());
        assertEquals("", accepted.getDueText(), "no deadline before pickup");
        assertTrue(accepted.getTimerNote().contains("starts at pickup"));
        assertNotEquals(before, requests.stateSignature(owner), "the live signature changes on accept");

        // a reserved item that was never picked up must not be flagged overdue
        requestTx.markOverdue();
        assertEquals("ACCEPTED", requests.findById(r.getRequestId()).getStatus());

        LocalDateTime t0 = LocalDateTime.now(AppConstants.CAMPUS_ZONE);
        assertTrue(requestTx.startBorrow(r.getRequestId(), item.getItemId()));
        assertFalse(requestTx.startBorrow(r.getRequestId(), item.getItemId()), "pickup counts once");
        Request picked = requests.findById(r.getRequestId());
        assertEquals("BORROWED", picked.getItemStatus());
        assertNotNull(picked.getDueAt());
        long minutes = java.time.Duration.between(t0, picked.getDueAt()).toMinutes();
        assertTrue(minutes >= 24 * 60 - 1 && minutes <= 24 * 60 + 1, "due = pickup time + 24 hours, was " + minutes);

        requests.setReturnCondition(r.getRequestId(), "DAMAGED");
        assertEquals("Returned damaged", requests.findById(r.getRequestId()).getReturnConditionText());
    }

    @Test
    void notificationsAreStoredCountedAndMarkedRead() {
        String id = uniqueId("U10");
        students.register(newStudent(id));
        NotificationDAO n = new NotificationDAO();
        assertEquals(0, n.unreadCount(id));
        n.add(id, "hello", "/dashboard");
        n.add(id, "second", null);
        assertEquals(2, n.unreadCount(id));
        assertEquals("second", n.latest(id).get(0).getMessage(), "newest first");
        n.markAllRead(id);
        assertEquals(0, n.unreadCount(id));
    }

    @Test
    void acceptedRequestCanBeCancelledBeforePickupOnly() {
        String owner = uniqueId("U13"), borrower = uniqueId("U14");
        students.register(newStudent(owner));
        students.register(newStudent(borrower));
        ResourceItem item = newItem(owner, "BORROW");
        listings.insert(item);
        Request r = newRequest(item, borrower);
        requests.insert(r);
        assertTrue(requestTx.accept(r.getRequestId(), item.getItemId(), null, null, "", 24));

        assertTrue(requestTx.cancelAccepted(r.getRequestId(), item.getItemId(), "Changed my mind"));
        Request after = requests.findById(r.getRequestId());
        assertEquals("CANCELLED", after.getStatus());
        assertEquals("Changed my mind", after.getCancelReason());
        assertEquals("AVAILABLE", after.getItemStatus());
        assertFalse(requestTx.cancelAccepted(r.getRequestId(), item.getItemId(), "Other"), "only once");

        // after a verified pickup it is too late to cancel
        Request r2 = newRequest(item, borrower);
        requests.insert(r2);
        assertTrue(requestTx.accept(r2.getRequestId(), item.getItemId(), null, null, "", 24));
        assertTrue(requestTx.startBorrow(r2.getRequestId(), item.getItemId()));
        assertFalse(requestTx.cancelAccepted(r2.getRequestId(), item.getItemId(), "Other"));
        assertEquals("BORROWED", requests.findById(r2.getRequestId()).getItemStatus());
    }

    @Test
    void unclaimedReservationsGoBackOnTheShelfButFutureMeetingsStay() {
        String owner = uniqueId("U15"), borrower = uniqueId("U16");
        students.register(newStudent(owner));
        students.register(newStudent(borrower));
        ResourceItem late = newItem(owner, "BORROW");
        ResourceItem future = newItem(owner, "BORROW");
        listings.insert(late);
        listings.insert(future);
        Request r1 = newRequest(late, borrower);
        Request r2 = newRequest(future, borrower);
        requests.insert(r1);
        requests.insert(r2);
        LocalDateTime now = LocalDateTime.now(AppConstants.CAMPUS_ZONE);
        // meeting was 3 hours ago and nobody came / meeting is tomorrow
        assertTrue(requestTx.accept(r1.getRequestId(), late.getItemId(), null, now.minusHours(3), "", 24));
        assertTrue(requestTx.accept(r2.getRequestId(), future.getItemId(), null, now.plusDays(1), "", 24));

        assertTrue(requestTx.releaseUnclaimed() >= 1);
        Request released = requests.findById(r1.getRequestId());
        assertEquals("CANCELLED", released.getStatus());
        assertEquals("AVAILABLE", released.getItemStatus());
        assertEquals("ACCEPTED", requests.findById(r2.getRequestId()).getStatus(), "a future meeting is left alone");
        assertTrue(new NotificationDAO().unreadCount(borrower) >= 1, "both students are told");
    }

    @Test
    void wantedBoardLimitsPostsPerMonthStoresLocationAndOffersOncePerPerson() {
        String poster = uniqueId("U17"), helper = uniqueId("U18");
        students.register(newStudent(poster));
        students.register(newStudent(helper));
        WantedDAO wanted = new WantedDAO();
        int first = wanted.add(poster, "Lab coat", "Stationery", "two days", "D Block ground floor");
        assertTrue(first > 0);
        assertEquals("D Block ground floor", wanted.findById(first).getLocation());
        for (int i = 1; i < AppConstants.MAX_WANTED_PER_MONTH; i++) {
            assertTrue(wanted.add(poster, "Thing " + i, "Stationery", null, null) > 0);
        }
        assertEquals(-1, wanted.add(poster, "One too many", "Stationery", null, null));
        assertTrue(wanted.listOpen().stream().anyMatch(w -> w.getPostId() == first));

        assertTrue(wanted.offerOnce(first, helper));
        assertFalse(wanted.offerOnce(first, helper), "one offer per person");
        assertFalse(wanted.close(first, helper), "only the poster can close");
        assertTrue(wanted.close(first, poster));
        assertFalse(wanted.listOpen().stream().anyMatch(w -> w.getPostId() == first));
        assertEquals(-1, wanted.add(poster, "Still too many", "Stationery", null, null),
                "closing a post does not give the monthly allowance back");
    }

    @Test
    void unansweredWantedPostsCloseByThemselvesButAnsweredOnesStayUntilThirtyDays() {
        String poster = uniqueId("U19"), helper = uniqueId("U20");
        students.register(newStudent(poster));
        students.register(newStudent(helper));
        WantedDAO wanted = new WantedDAO();
        int quiet = wanted.add(poster, "Nobody answers", "Stationery", null, null);
        int answered = wanted.add(poster, "Someone answers", "Stationery", null, null);
        int fresh = wanted.add(poster, "Brand new", "Stationery", null, null);
        wanted.offerOnce(answered, helper);
        backdate(quiet, 8);
        backdate(answered, 8);

        assertTrue(wanted.expireStale() >= 1);
        assertFalse(wanted.listOpen().stream().anyMatch(w -> w.getPostId() == quiet), "unanswered post closed after 7 days");
        assertTrue(wanted.listOpen().stream().anyMatch(w -> w.getPostId() == answered), "answered post stays");
        assertTrue(wanted.listOpen().stream().anyMatch(w -> w.getPostId() == fresh), "new post stays");
        assertTrue(new NotificationDAO().unreadCount(poster) >= 1, "the poster is told");

        backdate(answered, 31);
        wanted.expireStale();
        assertFalse(wanted.listOpen().stream().anyMatch(w -> w.getPostId() == answered), "everything closes after 30 days");
    }

    private static void backdate(int postId, int days) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE wanted_posts SET created_at = ? WHERE post_id = ?")) {
            ps.setObject(1, LocalDateTime.now(AppConstants.CAMPUS_ZONE).minusDays(days));
            ps.setInt(2, postId);
            ps.executeUpdate();
        } catch (SQLException e) {
            fail(e);
        }
    }

    @Test
    void keepMeSignedInTokenWorksUntilLogoutAndRejectsGuessesAndExpiry() {
        String id = uniqueId("U21");
        students.register(newStudent(id));
        RememberDAO remember = new RememberDAO();
        String cookie = remember.issue(id);
        assertEquals(id, remember.studentFor(cookie));
        assertNull(remember.studentFor(null));
        assertNull(remember.studentFor("short"));
        // right selector, wrong secret
        assertNull(remember.studentFor(cookie.substring(0, 25) + "0".repeat(64)));

        // expired
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE remember_tokens SET expires_at = ? WHERE selector = ?")) {
            ps.setObject(1, LocalDateTime.now(AppConstants.CAMPUS_ZONE).minusMinutes(1));
            ps.setString(2, cookie.substring(0, 24));
            ps.executeUpdate();
        } catch (SQLException e) {
            fail(e);
        }
        assertNull(remember.studentFor(cookie), "expired token refused");

        String second = remember.issue(id);
        assertEquals(id, remember.studentFor(second));
        remember.delete(second);
        assertNull(remember.studentFor(second), "logout kills the token");
    }

    @Test
    void entityDaosShareTheDaoContract() {
        String id = uniqueId("D22");
        Dao<Student, String> studentDao = students;           // the same interface for every entity DAO
        assertFalse(studentDao.exists(id));
        assertTrue(studentDao.insert(newStudent(id)));
        assertTrue(studentDao.exists(id));
        assertEquals(id, studentDao.findById(id).getStudentId());
        Dao<ResourceItem, Integer> listingDao = new ListingDAO();
        assertFalse(listingDao.exists(-1));
        Dao<Request, Integer> requestDao = requests;
        assertNull(requestDao.findById(-1));
    }

    @Test
    void campusImpactCountsRealRows() {
        java.util.Map<String, Object> before = new StatsDAO().campusImpact();
        students.insert(newStudent(uniqueId("D23")));
        java.util.Map<String, Object> after = new StatsDAO().campusImpact();
        assertEquals((Integer) before.get("students") + 1, after.get("students"));
        assertTrue((Integer) after.get("exchanges") >= 0 && (Integer) after.get("items") >= 0);
    }
}
