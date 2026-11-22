# ShareShelf — DAO Contract (as implemented)

The servlets call these exact signatures. All SQL lives in the DAOs; every statement is a `PreparedStatement`
inside try-with-resources. SQL failures are logged and rethrown as the unchecked `DataAccessException`
(servlets turn that into a friendly error page), except where a plain `false`/`null` is the documented answer.

## The shared contract: `Dao<T, ID>`
`StudentDAO`, `ListingDAO`, `RequestDAO` and `WantedDAO` implement one generic interface, so every entity is created and read the same way:
```java
public interface Dao<T, ID> {
    boolean insert(T entity);
    T findById(ID id);                       // null if missing
    default boolean exists(ID id) { return findById(id) != null; }
}
// StudentDAO: Dao<Student,String>   ListingDAO: Dao<ResourceItem,Integer>
// RequestDAO: Dao<Request,Integer>  WantedDAO:  Dao<Wanted,Integer>
```
The other DAOs (Handover, Notification, Rating, Profile, Remember) are not "one entity" stores (tokens, feeds, aggregates), so they keep their own small APIs.

## StudentDAO
```java
boolean register(Student s);                       // hashes the password (PBKDF2) before storing; false on duplicate id
Student login(String studentId, String password);  // verifies against the stored hash; null if wrong
boolean isDuplicateId(String studentId);
Student findById(String studentId);                // never returns the password hash
```

## ListingDAO
```java
boolean insert(ResourceItem item);                           // sets item.itemId; FK rejects unknown owners (DataAccessException)
List<ResourceItem> search(String keyword);                   // AVAILABLE items, title/description LIKE
List<ResourceItem> filter(String category, String listingType);
List<ResourceItem> searchAvailable(String keyword, String category, String listingType); // all three combined (used by SearchServlet)
List<ResourceItem> findByOwner(String ownerId);              // "My listings" (all statuses)
boolean updateStatus(int itemId, String newStatus);
ResourceItem findById(int itemId);
Map<String,Integer> countByStatus();                         // reports
Map<String,Integer> countByCategory();                       // reports
```
Search trims the keyword, is case-insensitive, and treats `%` and `_` literally.

## RequestDAO and RequestTransactionService
`RequestDAO` holds reads, inserts, single-row status changes and reports. `RequestTransactionService` holds the multi-table
workflows (accept, startBorrow, cancelAccepted, complete, revealContact, releaseUnclaimed, markOverdue), each in a JDBC
transaction with `SELECT ... FOR UPDATE`. In the lists below, the methods marked TRANSACTION live in `RequestTransactionService`.

```java
boolean insert(Request r);
boolean hasDuplicatePending(int itemId, String requesterId);
// Phase 2 DAOs (see the Javadoc in each file):
//   HandoverDAO      issue(requestId, kind) / redeem(requestId, kind, token, code)   one-time QR token + 6-digit code, 10 min, locks after 5 wrong tries
//   NotificationDAO  add / unreadCount / latest / markAllRead                         in-app bell; add() never throws
//   RatingDAO        add / pendingFor / hasRated                                      1 rating per person per request, hidden until both rated
boolean accept(int requestId, int itemId, LocalDateTime dueAt, LocalDateTime meetTime, String meetNote); // TRANSACTION
boolean reject(int requestId);                  // PENDING -> REJECTED only
boolean cancel(int requestId);                  // PENDING -> CANCELLED only
boolean complete(int requestId, int itemId);    // TRANSACTION
List<Request> findByRequester(String studentId);
List<Request> findByOwner(String studentId);
Request findById(int requestId);
String revealContact(int requestId, String ownerId);  // TRANSACTION; whatsapp number only if OVERDUE and ownerId matches
int markOverdue();                              // ACCEPTED + due date passed -> OVERDUE; returns rows changed (background thread)
int countCompleted(String studentId);           // reliability signal
Map<String,Integer> countByStatus();            // reports
Map<String,Integer> topRequestedCategories(int limit); // reports (innovation feature)
```

### Transactions (`setAutoCommit(false)` ... `commit()` / `rollback()`)
* **accept()**: (1) `PENDING -> ACCEPTED` with the due time and the agreed meeting time and note, (2) item `AVAILABLE -> RESERVED`, (3) every other
  `PENDING` request for the same item becomes `REJECTED`. Each step is a guarded `UPDATE ... WHERE status = ...`;
  if a guard matches no row the whole transaction rolls back, so two owners/tabs can never both win.
* **complete()**: request `ACCEPTED/OVERDUE -> COMPLETED` and the item goes back to `AVAILABLE` (BORROW) or
  to `CLOSED` (EXCHANGE/DONATE/GIVEAWAY), together or not at all.
* **revealContact()**: reads the status and owner, fetches the requester's WhatsApp number, sets
  `contact_revealed`/`revealed_at` and inserts a `contact_reveal_logs` row, together or not at all.

## Ground rule
If a servlet needs a method that isn't listed here, add it to this file first, then implement it.
