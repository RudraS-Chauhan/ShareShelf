# ShareShelf — DAO Contract

Shreya implements these exact method signatures. The Leader's servlets call them exactly like this — matching signatures means both sides can work in parallel without waiting on each other.

## StudentDAO.java (already fully written as the pattern — see repo)

```java
boolean register(Student s);
Student login(String studentId, String password);
boolean isDuplicateId(String studentId);
Student findById(String studentId);
```

## ListingDAO.java

```java
boolean insert(ResourceItem item);
List<ResourceItem> search(String keyword);              // WHERE title LIKE ? AND status='AVAILABLE'
List<ResourceItem> filter(String category, String listingType); // combine with search logic
List<ResourceItem> findByOwner(String ownerId);          // for "My Listings" dashboard section
boolean updateStatus(int itemId, String newStatus);
ResourceItem findById(int itemId);
```

## RequestDAO.java

```java
boolean insert(Request r);
boolean hasDuplicatePending(int itemId, String requesterId);
boolean accept(int requestId, int itemId, java.sql.Date expectedReturnDate); // TRANSACTION — see below
boolean reject(int requestId);
boolean cancel(int requestId);
boolean complete(int requestId, int itemId);             // TRANSACTION — see below
List<Request> findByRequester(String studentId);
List<Request> findByOwner(String studentId);
Request findById(int requestId);
String revealContact(int requestId, String ownerId);     // returns requester's whatsapp_number ONLY if status=='OVERDUE' and ownerId matches; logs the reveal; returns null otherwise
```

### `accept()` — must be a real transaction

```java
Connection conn = DBConnection.getConnection();
conn.setAutoCommit(false);
try {
    // 1. UPDATE requests SET status='ACCEPTED', expected_return_date=? WHERE request_id=?
    // 2. UPDATE resource_items SET status='RESERVED' WHERE item_id=?
    conn.commit();
    return true;
} catch (SQLException e) {
    conn.rollback();
    return false;
} finally {
    conn.setAutoCommit(true);
    conn.close();
}
```

### `complete()` — same transaction pattern

Updates `requests.status='COMPLETED'` and `resource_items.status='CLOSED'` (or back to `AVAILABLE` if re-listing) together.

### `revealContact()` — the safety-net method

```java
// 1. SELECT status, requester_id FROM requests WHERE request_id = ?
// 2. If status != 'OVERDUE' → return null (not allowed yet)
// 3. If the calling ownerId doesn't match this request's owner_id → return null (not their request)
// 4. Otherwise: SELECT whatsapp_number FROM students WHERE student_id = <requester_id>
// 5. UPDATE requests SET contact_revealed=TRUE, revealed_at=NOW() WHERE request_id = ?  (the log)
// 6. Return the whatsapp_number
```

## Ground rule

If either of you needs a method signature that isn't listed here, **add it to this file first**, then implement — don't silently add methods the other side doesn't know exist. Keeps the interface between your two pieces predictable.
