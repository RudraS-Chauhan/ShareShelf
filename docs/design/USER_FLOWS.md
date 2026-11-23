# ShareShelf — User Flows

Owned by Member 3, with screenshots added once the UI exists.

## Flow 1: Registration & Login
1. Student opens the app, clicks Register
2. Fills in student ID, name, branch, year, email, hostel, password, WhatsApp number
3. Submits → redirected to login
4. Logs in with student ID + password → session created, redirected to dashboard

## Flow 2: Listing a Resource
1. From dashboard, click "Add Listing"
2. Fill title, description, category, listing type, condition, location, (duration if Borrow)
3. Submit → listing appears with status AVAILABLE, visible on "My Listings"

## Flow 3: Searching & Requesting
1. Student goes to Search/Browse
2. Types a keyword and/or picks filters
3. Sees matching AVAILABLE listings
4. Clicks "Send Request" on one, writes a short message, submits
5. Request appears as PENDING on both students' request pages

## Flow 4: Accept → Handover → Complete
1. Owner sees the pending request under "Received Requests"
2. Clicks Accept — sets expected return duration if Borrow
3. Listing becomes RESERVED, request becomes ACCEPTED
4. Students coordinate the actual handover using the listing's location + request message
5. After return, owner marks the request COMPLETED — listing becomes CLOSED or back to AVAILABLE

## Flow 5: Overdue Safety Net
1. Background thread detects expected_return_date has passed and status isn't COMPLETED
2. Request marked OVERDUE — visible to both students
3. Owner can click "Contact via WhatsApp" — opens a wa.me chat with the requester
4. Reveal is logged with timestamp

(Add screenshots here once each flow is actually built and working.)
