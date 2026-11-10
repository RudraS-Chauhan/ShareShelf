#!/usr/bin/env python3
"""
ShareShelf end-to-end smoke test (black-box, over HTTP).

Runs the real app (Tomcat + MySQL) through every workflow in docs/TEST_CASES.md and prints PASS/FAIL per check.
Usage:   python3 tests/e2e_smoke.py [base_url]        (default http://localhost:8080/ShareShelf)
Needs:   the app running, an empty `shareshelf` database, Python 3 (standard library only).
Overdue check: set OVERDUE_HOOK to a shell command that backdates a request's due date and restarts the app
(the background thread flags overdue requests on startup + every few minutes); it receives the request id in $1.
"""
import html, http.cookiejar, os, re, subprocess, sys, time, urllib.error, urllib.parse, urllib.request

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080/ShareShelf").rstrip("/")
OVERDUE_HOOK = os.environ.get("OVERDUE_HOOK")
results = []
LAST_BANNER = ""


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k):
        return None


class Client:
    """One browser: keeps its own cookies (session) and never follows redirects automatically."""
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar), NoRedirect)

    def request(self, path, data=None):
        body = urllib.parse.urlencode(data).encode() if data is not None else None
        try:
            r = self.opener.open(BASE + path, body)
            return r.status, r.headers, r.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as e:
            return e.code, e.headers, e.read().decode("utf-8", "replace")

    def get(self, path):
        return self.request(path)

    def post(self, path, data):
        return self.request(path, data)

    def follow(self, resp):
        """Follow one redirect (the Post/Redirect/Get step) and return the final page."""
        status, headers, _ = resp
        assert status in (301, 302, 303), "expected a redirect, got %s" % status
        loc = headers["Location"]
        path = urllib.parse.urlparse(loc).path                    # works for relative and absolute Location headers
        context = urllib.parse.urlparse(BASE).path
        page = self.get(path[len(context):] + ("?" + urllib.parse.urlparse(loc).query if urllib.parse.urlparse(loc).query else ""))
        global LAST_BANNER
        m = re.search(r'class="banner[^"]*"[^>]*>(.*?)</div>', page[2], re.S)
        LAST_BANNER = re.sub(r"<[^>]+>", "", m.group(1)).strip() if m else "(no banner)"
        return (page[0], page[1], html.unescape(page[2]))


def check(name, cond, detail=""):
    results.append((name, bool(cond)))
    print(("PASS  " if cond else "FAIL  ") + name + ("" if cond else "   <-- " + str(detail or LAST_BANNER)[:200]))


def loc(resp):
    return resp[1].get("Location", "")


def register(c, sid, name, email=None, **over):
    data = dict(studentId=sid, name=name, branch="CSE", year="2", email=email or f"{name.lower()}.{sid.lower()}@galgotiasuniversity.ac.in",
                hostelOrDay="Hostel", whatsappNumber="98765432" + sid[-2:], password="Passw0rd!x", consent="yes")
    data.update(over)
    return c.post("/register", data)


def last_id(page, action):
    """Request id of the first form posting to /request/<action> on a page."""
    m = re.search(r'/request/%s".*?name="requestId" value="(\d+)"' % action, page, re.S)
    return m.group(1) if m else None


A, B, C = Client(), Client(), Client()
SA, SB, SC = "25SCSE1000001", "25SCSE1000002", "25SCSE1000003"

print("== Registration & login ==")
check("T1 bad email domain rejected", "error=invalid_email" in loc(register(A, SA, "Asha", email="asha.%s@gmail.com" % SA.lower())))
check("   email without own admission number rejected", "error=email_mismatch" in loc(register(A, SA, "Asha", email="asha.99xyz@galgotiasuniversity.ac.in")))
check("   weak password rejected", "error=weak_password" in loc(register(A, SA, "Asha", password="short")))
check("   password without digit rejected", "error=weak_password" in loc(register(A, SA, "Asha", password="onlyletters")))
check("   invalid WhatsApp number rejected", "error=invalid_whatsapp" in loc(register(A, SA, "Asha", whatsappNumber="12345")))
check("   missing WhatsApp consent rejected", "error=no_consent" in loc(register(A, SA, "Asha", consent="")))
check("   year out of range rejected", "error=invalid_year" in loc(register(A, SA, "Asha", year="9")))
check("   registering Asha works", "registered=true" in loc(register(A, SA, "Asha")))
check("T1 duplicate admission number rejected", "error=duplicate_id" in loc(register(A, SA, "Asha")))
check("   registering Bilal works", "registered=true" in loc(register(B, SB, "Bilal")))
check("   registering Chitra works", "registered=true" in loc(register(C, SC, "Chitra")))
check("   wrong password refused", "error=invalid" in loc(A.post("/login", dict(studentId=SA, password="wrong-pass1"))))
check("   unknown user refused", "error=invalid" in loc(A.post("/login", dict(studentId="25NOBODY99", password="Passw0rd!x"))))
check("   login is case-insensitive on the ID", loc(A.post("/login", dict(studentId=SA.lower(), password="Passw0rd!x"))).endswith("/dashboard"))
check("   Bilal logs in", loc(B.post("/login", dict(studentId=SB, password="Passw0rd!x"))).endswith("/dashboard"))
check("   Chitra logs in", loc(C.post("/login", dict(studentId=SC, password="Passw0rd!x"))).endswith("/dashboard"))

print("== Session gate ==")
anon = Client()
for p in ("/dashboard", "/search", "/add-listing", "/my-requests", "/reports", "/reports?format=csv"):
    check("anonymous GET %s redirects to login" % p, "index.jsp?error=login_required" in loc(anon.get(p)))
check("anonymous POST /request/accept redirects to login", "login_required" in loc(anon.post("/request/accept", dict(requestId="1"))))
check("login page is public", anon.get("/")[0] == 200)

print("== Empty database states ==")
s, _, page = B.get("/search")
check("T11 empty DB: search shows a clear empty state", s == 200 and "No listings yet" in page)
check("   dashboard loads with no data", A.get("/dashboard")[0] == 200)
check("   my-requests loads with no data", A.get("/my-requests")[0] == 200)
s, _, page = A.get("/reports")
check("   reports load with zero data", s == 200 and "No requests yet" in page)

print("== Listings ==")
s, _, page = A.post("/add-listing", dict(title="   ", category="Stationery", listingType="BORROW", itemCondition="Good", location="Library"))
check("T3 empty title: form shown again with a message", s == 200 and "Please enter a title" in page)
s, _, page = A.post("/add-listing", dict(title="Calc", category="Nonsense", listingType="BORROW", itemCondition="Good", location="Library"))
check("   invalid category refused", s == 200 and "choose a category" in page.lower())
s, _, page = A.post("/add-listing", dict(title="Calc", category="Stationery", listingType="BORROW", itemCondition="Good", location="Mars"))
check("   invalid location refused", s == 200 and "meeting location" in page.lower())
check("   good BORROW listing created", loc(A.post("/add-listing", dict(title="Casio fx-991ES Calculator", description="Works great", category="Electronics & Accessories",
      listingType="BORROW", itemCondition="Good", location="Library", suggestedDays="3"))).endswith("/dashboard"))
check("   good DONATE listing created", loc(A.post("/add-listing", dict(title="DBMS Textbook 3rd ed", category="Academic Materials",
      listingType="DONATE", itemCondition="Fair", location="Main Gate"))).endswith("/dashboard"))
check("   XSS title stored", loc(A.post("/add-listing", dict(title="<script>alert(1)</script>", category="Stationery", listingType="GIVEAWAY",
      itemCondition="New", location="Library"))).endswith("/dashboard"))
s, _, page = A.get("/dashboard")
check("   dashboard shows the success flash and the listings", "now listed" in page and "Casio fx-991ES Calculator" in page)
check("   flash message shows only once", "now listed" not in A.get("/dashboard")[2])
check("   XSS: script tag is escaped everywhere", "<script>alert(1)</script>" not in page and "&lt;script&gt;alert(1)&lt;/script&gt;" in page)

print("== Search ==")
s, _, page = B.get("/search?q=calculator")
check("T4 keyword search finds the calculator", "Casio fx-991ES Calculator" in page and "DBMS Textbook" not in page)
check("T12 case-insensitive search", "Casio fx-991ES" in B.get("/search?q=CALCULATOR")[2])
check("T13 extra spaces ignored", "Casio fx-991ES" in B.get("/search?q=%20%20calculator%20%20")[2])
check("T10 no match: clear message", "No listings found" in B.get("/search?q=zzzznothing")[2])
check("   category filter", "DBMS Textbook" in B.get("/search?category=Academic+Materials")[2] and "Casio" not in B.get("/search?category=Academic+Materials")[2])
check("   type filter", "DBMS Textbook" in B.get("/search?type=DONATE")[2] and "Casio" not in B.get("/search?type=DONATE")[2])
check("   keyword + category combined", "Casio" in B.get("/search?q=cal&category=Electronics+%26+Accessories")[2])
check("   LIKE wildcards are literal (q=%)", "No listings found" in B.get("/search?q=%25")[2])
check("   SQL injection attempt is harmless", B.get("/search?q=%27+OR+1%3D1+--")[0] == 200 and "Casio" not in B.get("/search?q=%27+OR+1%3D1+--")[2])
check("   own listing shows 'This is your listing'", "This is your listing" in A.get("/search?q=calculator")[2])

print("== Requests ==")
item = re.search(r'name="itemId" value="(\d+)"', B.get("/search?q=calculator")[2]).group(1)
check("T9 self-request blocked", "can't request your own" in A.follow(A.post("/request/send", dict(itemId=item, message="mine")))[2])
check("   Bilal's request is sent", "Request sent to Asha" in B.follow(B.post("/request/send", dict(itemId=item, message="Tomorrow 2pm at library?")))[2])
check("T5 duplicate pending request blocked", "already have a pending request" in B.follow(B.post("/request/send", dict(itemId=item, message="again")))[2])
check("   unknown item refused", "could not be found" in B.follow(B.post("/request/send", dict(itemId="99999", message="x")))[2])
check("   Chitra also requests the same item", "Request sent" in C.follow(C.post("/request/send", dict(itemId=item, message="Me too")))[2])
_, _, a_page = A.get("/my-requests")
rid_b = re.search(r'Tomorrow 2pm at library\?.*?name="requestId" value="(\d+)"', a_page, re.S).group(1)
rid_c = re.search(r'Me too.*?name="requestId" value="(\d+)"', a_page, re.S).group(1)
check("   owner sees both requests", rid_b != rid_c)
check("   requester sees Cancel for a pending request", 'action="/ShareShelf/request/cancel"' in B.get("/my-requests")[2])
check("   owner cannot cancel (only the requester can)", "isn't yours" in A.follow(A.post("/request/cancel", dict(requestId=rid_b)))[2])

print("== Accept / reject rules ==")
check("T8 non-owner cannot accept", "isn't yours" in B.follow(B.post("/request/accept", dict(requestId=rid_b, days="3")))[2])
check("   non-owner cannot reject", "isn't yours" in C.follow(C.post("/request/reject", dict(requestId=rid_b)))[2])
check("   non-owner cannot complete", "isn't yours" in C.follow(C.post("/request/complete", dict(requestId=rid_b)))[2])
check("   absurd duration refused", "between 1 and 90" in A.follow(A.post("/request/accept", dict(requestId=rid_b, days="500", back="dashboard")))[2])
check("T7 owner accepts (3 days)", "Accepted." in A.follow(A.post("/request/accept", dict(requestId=rid_b, days="3", back="my-requests")))[2])
_, _, a_page = A.get("/my-requests")
check("   accepted request shows a due date", re.search(r"\d{4}-\d{2}-\d{2}", a_page) is not None)
check("   competing request was auto-rejected", "REJECTED" in C.get("/my-requests")[2])
check("T17 accepting an already-handled request is handled", "already been handled" in A.follow(A.post("/request/accept", dict(requestId=rid_b, days="3")))[2])
check("   RESERVED listing disappears from search", "No listings found" in B.get("/search?q=calculator")[2])
check("T6 request on a reserved item rejected", "no longer available" in C.follow(C.post("/request/send", dict(itemId=item, message="late")))[2])
check("T15 reveal before overdue blocked", "only available to the owner of an overdue" in A.follow(A.post("/request/reveal", dict(requestId=rid_b)))[2])
check("   handover by non-owner refused", "isn't yours" in B.follow(B.post("/request/handover", dict(requestId=rid_b)))[2])
check("   owner marks handed over", "marked as borrowed" in A.follow(A.post("/request/handover", dict(requestId=rid_b)))[2])
check("   second handover refused", "Only an accepted borrow request" in A.follow(A.post("/request/handover", dict(requestId=rid_b)))[2])
check("   close is refused while borrowed", "Only an available listing" in A.follow(A.post("/listing/close", dict(itemId=item)))[2])
check("T-complete owner completes the exchange", "available to borrow again" in A.follow(A.post("/request/complete", dict(requestId=rid_b)))[2])
check("   completing twice is handled", "Only an accepted or overdue" in A.follow(A.post("/request/complete", dict(requestId=rid_b)))[2])
check("   BORROW item is available again", "Casio fx-991ES" in B.get("/search?q=calculator")[2])
trust = re.findall(r'class="trust[^"]*">([^<]*)<', B.get("/search?q=calculator")[2])
check("   reliability signal shows completed exchange", trust == ["✓ 1 completed"], "chips on page: %s" % trust)

print("== Reject, cancel, close ==")
check("   Chitra requests again", "Request sent" in C.follow(C.post("/request/send", dict(itemId=item, message="Please?")))[2])
_, _, a_page = A.get("/my-requests")
rid_c2 = re.search(r'Please\?.*?name="requestId" value="(\d+)"', a_page, re.S).group(1)
check("   owner rejects", "rejected" in A.follow(A.post("/request/reject", dict(requestId=rid_c2)))[2])
check("   rejecting twice handled", "already been handled" in A.follow(A.post("/request/reject", dict(requestId=rid_c2)))[2])
check("   Chitra requests then cancels", "Request sent" in C.follow(C.post("/request/send", dict(itemId=item, message="ok")))[2])
rid_c3 = re.search(r'name="requestId" value="(\d+)"[^>]*>\s*<input type="hidden" name="back" value="my-requests">\s*<button[^>]*>Cancel', C.get("/my-requests")[2])
check("   cancel button present", rid_c3 is not None)
check("   requester cancels", "cancelled" in C.follow(C.post("/request/cancel", dict(requestId=rid_c3.group(1))))[2])
book = re.search(r'DBMS Textbook.*?name="itemId" value="(\d+)"', B.get("/search?q=dbms")[2], re.S).group(1)
check("   Bilal cannot close Asha's listing", "only close your own" in B.follow(B.post("/listing/close", dict(itemId=book)))[2])
check("   Asha closes her book listing", "is closed" in A.follow(A.post("/listing/close", dict(itemId=book)))[2])
check("T6 closed listing is not searchable", "DBMS Textbook" not in B.get("/search?q=dbms")[2])
check("T6 request on a closed listing rejected", "no longer available" in B.follow(B.post("/request/send", dict(itemId=book, message="x")))[2])

print("== Overdue + WhatsApp safety net ==")
check("   Bilal requests again for the overdue test", "Request sent" in B.follow(B.post("/request/send", dict(itemId=item, message="Overdue test")))[2])
rid_o = re.search(r'Overdue test.*?name="requestId" value="(\d+)"', A.get("/my-requests")[2], re.S).group(1)
check("   owner accepts for 1 day", "Accepted." in A.follow(A.post("/request/accept", dict(requestId=rid_o, days="1")))[2])
if OVERDUE_HOOK:
    subprocess.run([OVERDUE_HOOK, rid_o], check=True)      # backdates the due date and restarts the app
    A.post("/login", dict(studentId=SA, password="Passw0rd!x"))   # sessions do not survive the restart
    B.post("/login", dict(studentId=SB, password="Passw0rd!x"))
    s, _, page = A.get("/my-requests")
    check("T14 request is flagged OVERDUE by the background thread", "badge-overdue" in page)
    check("   dashboard shows the overdue alert", "overdue" in A.get("/dashboard")[2].lower())
    check("T15 non-owner cannot reveal the number", "only available to the owner" in B.follow(B.post("/request/reveal", dict(requestId=rid_o)))[2])
    r = A.post("/request/reveal", dict(requestId=rid_o))
    check("T16 owner is sent to a wa.me chat with the requester's number", r[0] == 302 and re.match(r"https://wa\.me/91\d{10}\?text=", loc(r)), loc(r))
    check("   reveal is recorded on the request", "Contact revealed" in A.get("/my-requests")[2])
    check("   overdue request can still be completed", "available to borrow again" in A.follow(A.post("/request/complete", dict(requestId=rid_o)))[2])
else:
    print("SKIP  overdue checks (set OVERDUE_HOOK)")

print("== Reports ==")
s, _, page = A.get("/reports")
check("   reports page shows live numbers + innovation stat", s == 200 and "Most requested categories" in page and "Electronics" in page)
s, h, body = A.get("/reports?format=csv")
check("   CSV export works", s == 200 and "text/csv" in h["Content-Type"] and body.startswith("section,name,count") and "Requests by status" in body)
check("   overdue-checker status visible", "Overdue checker" in page and "Running" in page)

print("== Logout ==")
check("   logout redirects to login", "logged_out=true" in loc(A.get("/logout")))
check("   session is dead after logout", "login_required" in loc(A.get("/dashboard")))
s, _, page = B.get("/nope")
check("   unknown URL gives the friendly 404 page", s == 404 and "Page not found" in page, s)

failed = [n for n, ok in results if not ok]
print("\n%d checks, %d passed, %d failed" % (len(results), len(results) - len(failed), len(failed)))
sys.exit(1 if failed else 0)
