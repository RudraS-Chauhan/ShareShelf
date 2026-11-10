# ShareShelf — DAO Contract (as implemented)

The servlets call these exact signatures. All SQL lives in the DAOs; every statement is a `PreparedStatement`
inside try-with-resources. SQL failures are logged and rethrown as the unchecked `DataAccessException`
(servlets turn that into a friendly error page), except where a plain `false`/`null` is the documented answer.

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

## RequestDAO
```java
boolean insert(Request r);
boolean hasDuplicatePending(int itemId, String requesterId);
boolean accept(int requestId, int itemId, java.sql.Date expectedReturnDate); // TRANSACTION
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
* **accept()**: (1) `PENDING -> ACCEPTED` with the due date, (2) item `AVAILABLE -> RESERVED`, (3) every other
  `PENDING` request for the same item becomes `REJECTED`. Each step is a guarded `UPDATE ... WHERE status = ...`;
  if a guard matches no row the whole transaction rolls back, so two owners/tabs can never both win.
* **complete()**: request `ACCEPTED/OVERDUE -> COMPLETED` and the item goes back to `AVAILABLE` (BORROW) or
  to `CLOSED` (EXCHANGE/DONATE/GIVEAWAY), together or not at all.
* **revealContact()**: reads the status and owner, fetches the requester's WhatsApp number, sets
  `contact_revealed`/`revealed_at` and inserts a `contact_reveal_logs` row, together or not at all.

## Ground rule
If a servlet needs a method that isn't listed here, add it to this file first, then implement it.
