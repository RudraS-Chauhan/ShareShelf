<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Login / register page (public). Messages come from ?error=code&registered=true&logged_out=true set by the servlets. --%>
<c:if test="${not empty sessionScope.studentId}">
  <c:redirect url="/dashboard"/>
</c:if>
<c:set var="showRegister" value="${param.tab == 'register'}"/>
<c:set var="errText" value=""/>
<c:choose>
  <c:when test="${param.error == 'invalid'}"><c:set var="errText" value="Wrong student ID or password."/></c:when>
  <c:when test="${param.error == 'login_required'}"><c:set var="errText" value="Please log in first."/></c:when>
  <c:when test="${param.error == 'missing_fields'}"><c:set var="errText" value="Please fill in every field."/></c:when>
  <c:when test="${param.error == 'invalid_id'}"><c:set var="errText" value="Admission number must be 6 to 20 letters/digits, e.g. 25SCSE1000000."/></c:when>
  <c:when test="${param.error == 'invalid_email'}"><c:set var="errText" value="Use your college email ending in @galgotiasuniversity.ac.in."/></c:when>
  <c:when test="${param.error == 'email_mismatch'}"><c:set var="errText" value="Your college email must contain your own admission number."/></c:when>
  <c:when test="${param.error == 'weak_password'}"><c:set var="errText" value="Password needs at least 8 characters, with a letter and a digit."/></c:when>
  <c:when test="${param.error == 'invalid_whatsapp'}"><c:set var="errText" value="Enter a valid 10-digit Indian mobile number (WhatsApp)."/></c:when>
  <c:when test="${param.error == 'whatsapp_taken'}"><c:set var="errText" value="That WhatsApp number is already registered to another account. Every student must use their own number."/></c:when>
  <c:when test="${param.error == 'no_consent'}"><c:set var="errText" value="Please tick the WhatsApp consent box to register."/></c:when>
  <c:when test="${param.error == 'invalid_year'}"><c:set var="errText" value="Year must be between 1 and 4."/></c:when>
  <c:when test="${param.error == 'duplicate_id'}"><c:set var="errText" value="That admission number is already registered. Try logging in."/></c:when>
  <c:when test="${not empty param.error}"><c:set var="errText" value="Something went wrong. Please try again."/></c:when>
</c:choose>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ShareShelf · Borrow what you need. Share what you have.</title>
  <link rel="stylesheet" href="${ctx}/css/style.css?v=20261122">
  <%-- Splash shows once per browser tab, and not when a message (error, registered, logged out) is waiting. --%>
  <script>
    try { if (!sessionStorage.getItem('ss_splash') && !location.search) { document.documentElement.classList.add('splash-on'); } } catch (e) {}
  </script>
</head>
<body>
<div id="splash" class="splash" aria-hidden="true">
  <div class="splash-books"><span>📕</span><span>📗</span><span>📘</span><span>🧮</span><span>🥼</span></div>
  <div class="splash-logo">ShareShelf</div>
  <div id="splash-line" class="splash-line">Dusting off the shelves…</div>
  <div class="splash-bar"><i id="splash-fill"></i></div>
</div>
<div class="landing">
  <section class="hero">
    <div class="logo">📚 ShareShelf</div>
    <h1>Borrow what you need.<br>Share what you have.</h1>
    <p class="tagline">A campus-only shelf for Galgotias students: calculators, lab coats, books and more, shared between classmates.</p>
    <ul>
      <li><span class="dot">🔎</span> Search items students already own</li>
      <li><span class="dot">🤝</span> Request, accept and track every handover</li>
      <li><span class="dot">⏰</span> Due dates with automatic overdue flags</li>
      <li><span class="dot">🛡️</span> Contact shared only if something goes overdue</li>
    </ul>
    <%-- Campus impact counter: filled by /stats; stays hidden if the call fails or every number is 0 --%>
    <div id="impact" class="impact hidden" aria-label="Campus impact so far">
      <div class="impact-tile" data-key="exchanges"><b>0</b><span>exchanges completed</span></div>
      <div class="impact-tile" data-key="items"><b>0</b><span>items on the shelf</span></div>
      <div class="impact-tile" data-key="students"><b>0</b><span>students sharing</span></div>
    </div>
  </section>

  <section class="panel">
    <div class="panel-inner">
      <div class="auth-tabs" role="tablist">
        <button type="button" class="tab-btn ${showRegister ? '' : 'active'}" data-tab="login">Login</button>
        <button type="button" class="tab-btn ${showRegister ? 'active' : ''}" data-tab="register">Register</button>
      </div>

      <c:if test="${not empty errText}"><div class="msg error" role="alert"><c:out value="${errText}"/></div></c:if>
      <c:if test="${param.registered == 'true'}"><div class="msg success">Account created. You can log in now.</div></c:if>
      <c:if test="${param.logged_out == 'true'}"><div class="msg success">You have been logged out.</div></c:if>

      <%-- LOGIN FORM: field names must match LoginServlet --%>
      <form id="loginForm" class="card ${showRegister ? 'hidden' : ''}" method="post" action="${ctx}/login">
        <h2>Login</h2>
        <label for="l-id">Admission number</label>
        <input id="l-id" type="text" autofocus name="studentId" placeholder="e.g. 25SCSE1000000" required autocomplete="username">
        <label for="l-pw">Password</label>
        <input id="l-pw" type="password" name="password" required autocomplete="current-password">
        <label class="check-row"><input type="checkbox" name="remember" value="on" checked> Keep me signed in on this device (30 days)</label>
        <button type="submit">Login</button>
      </form>

      <%-- REGISTER FORM: field names must match RegisterServlet (server re-checks everything) --%>
      <form id="registerForm" class="card ${showRegister ? '' : 'hidden'}" method="post" action="${ctx}/register">
        <h2>Create your account</h2>
        <label for="r-id">Admission number</label>
        <input id="r-id" type="text" name="studentId" placeholder="e.g. 25SCSE1000000" required>
        <div id="r-id-hint" class="hint"></div>
        <label for="r-name">Full name</label>
        <input id="r-name" type="text" name="name" maxlength="100" required>
        <label for="r-branch">Branch</label>
        <input id="r-branch" type="text" name="branch" maxlength="50" placeholder="e.g. CSE" required>
        <label for="r-year">Year</label>
        <input id="r-year" type="number" name="year" min="1" max="4" required>
        <label for="r-email">College email</label>
        <input id="r-email" type="email" name="email" placeholder="name.25scse1000000@galgotiasuniversity.ac.in" required>
        <label for="r-res">Hostel or day scholar</label>
        <select id="r-res" name="hostelOrDay" required>
          <option value="Hostel">Hostel</option>
          <option value="Day Scholar">Day Scholar</option>
        </select>
        <label for="r-wa">WhatsApp number</label>
        <input id="r-wa" type="tel" name="whatsappNumber" placeholder="Your own 10-digit mobile number" required>
        <div class="hint">Must be your own number. One number can belong to only one account.</div>
        <label class="check">
          <input type="checkbox" name="consent" value="yes" required>
          <span>I confirm this is <strong>my own</strong> WhatsApp number and I agree it may be shown <strong>only</strong> to the owner of an item I borrowed, and <strong>only</strong> if that borrow becomes overdue. Every reveal is logged.</span>
        </label>
        <label for="r-pw">Password</label>
        <input id="r-pw" type="password" name="password" minlength="8" required autocomplete="new-password">
        <div class="hint">At least 8 characters, with a letter and a digit.</div>
        <button type="submit">Create account</button>
      </form>
      <p class="note">Only @galgotiasuniversity.ac.in addresses can register.</p>
    </div>
  </section>
</div>
<script>
  // Tab switch only: all validation is done again on the server.
  document.querySelectorAll('.tab-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var showRegister = btn.dataset.tab === 'register';
      document.getElementById('loginForm').classList.toggle('hidden', showRegister);
      document.getElementById('registerForm').classList.toggle('hidden', !showRegister);
      document.querySelectorAll('.tab-btn').forEach(function (b) { b.classList.toggle('active', b === btn); });
    });
  });

  // On the free host the first request after a quiet spell can take a few seconds: show that something is happening
  // and stop a second click from sending the form twice.
  ['loginForm', 'registerForm'].forEach(function (id) {
    var form = document.getElementById(id);
    if (!form) { return; }
    form.addEventListener('submit', function () {
      var btn = form.querySelector('button[type=submit]');
      if (btn) { btn.disabled = true; btn.textContent = id === 'loginForm' ? 'Signing in…' : 'Creating account…'; }
    });
  });
  // Pressing the browser Back button restores the page with the button still disabled: reset it.
  window.addEventListener('pageshow', function (e) { if (e.persisted) { location.reload(); } });

  // Splash: one short, upbeat screen while the server wakes up (the /stats call below doubles as the wake-up
  // ping). Shown at least 1.4 s so it reads as intentional, and never longer than 6 s.
  var splash = document.getElementById('splash'), splashDone = false;
  var splashShownAt = Date.now();
  function endSplash() {
    if (splashDone) { return; }
    splashDone = true;
    var wait = Math.max(0, 1400 - (Date.now() - splashShownAt));
    setTimeout(function () {
      document.getElementById('splash-fill').style.width = '100%';
      setTimeout(function () {
        splash.classList.add('splash-out');
        document.documentElement.classList.remove('splash-on');
        try { sessionStorage.setItem('ss_splash', '1'); } catch (e) {}
      }, 250);
    }, wait);
  }
  if (document.documentElement.classList.contains('splash-on')) {
    var lines = ['Dusting off the shelves…', 'Finding a calculator near you…', 'Lab coats: counted. Almost ready…', 'Waking up the campus…'];
    var li = 0, fill = document.getElementById('splash-fill'), pct = 8;
    fill.style.width = pct + '%';
    var tick = setInterval(function () {
      li = (li + 1) % lines.length;
      document.getElementById('splash-line').textContent = lines[li];
      pct = Math.min(88, pct + 18);
      fill.style.width = pct + '%';
      if (splashDone) { clearInterval(tick); }
    }, 900);
    setTimeout(endSplash, 6000);
  }

  // Campus impact counter. Real totals from /stats; any failure just leaves the strip hidden.
  fetch('${ctx}/stats', { cache: 'no-store' }).then(function (r) { return r.ok ? r.json() : null; }).then(function (d) {
    if (!d) { return; }
    var box = document.getElementById('impact'), any = false;
    var calm = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;
    box.querySelectorAll('.impact-tile').forEach(function (tile) {
      var n = parseInt(d[tile.dataset.key], 10) || 0, el = tile.querySelector('b');
      if (n <= 0) { tile.classList.add('hidden'); return; }
      any = true;
      if (calm || n < 3) { el.textContent = n; return; }
      var start = null;
      (function step(t) {
        start = start || t;
        var p = Math.min((t - start) / 900, 1);
        el.textContent = Math.round(n * (1 - Math.pow(1 - p, 3)));
        if (p < 1) { requestAnimationFrame(step); }
      })(performance.now());
    });
    if (any) { box.classList.remove('hidden'); }
  }).catch(function () {}).then(endSplash);

  // Friendly hint from the admission number (25 = admitted 2025, SCSE = school). Display only: the server
  // accepts any 6-20 letter/digit ID, and keep this list in step with AppConstants.SCHOOLS.
  var SCHOOLS = { SCSE: 'School of Computer Science and Engineering', SOAI: 'School of Artificial Intelligence',
                  GSFC: 'School of Finance and Commerce', GSOL: 'School of Law', GSOB: 'School of Business (BBA)',
                  SODT: 'School of Defence Technology', SMAS: 'B.Optom (Optometry)', SALM: 'BBA (Aviation)', BAS: 'B.Sc (Maths Hons)', GPTC: 'Diploma (Polytechnic)' };
  document.getElementById('r-id').addEventListener('input', function () {
    var hint = document.getElementById('r-id-hint');
    var m = /^(\d{2})([A-Z]{2,8})(\d{3,10})$/.exec(this.value.trim().toUpperCase());
    if (!m) { hint.textContent = ''; return; }
    var admitted = 2000 + parseInt(m[1], 10), now = new Date();
    var start = now.getMonth() >= 6 ? now.getFullYear() : now.getFullYear() - 1;
    var year = Math.min(4, Math.max(1, start - admitted + 1));
    hint.textContent = 'Class of ' + (admitted + 4) + ' · ' + (SCHOOLS[m[2]] || m[2]) + ' · looks like year ' + year;
    var y = document.getElementById('r-year');
    if (y && !y.value) { y.value = year; }
  });
</script>
</body>
</html>
