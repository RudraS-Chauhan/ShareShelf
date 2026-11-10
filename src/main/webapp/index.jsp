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
  <c:when test="${param.error == 'invalid_id'}"><c:set var="errText" value="Admission number must be 6 to 20 letters/digits, e.g. 25SCSE1180735."/></c:when>
  <c:when test="${param.error == 'invalid_email'}"><c:set var="errText" value="Use your college email ending in @galgotiasuniversity.ac.in."/></c:when>
  <c:when test="${param.error == 'email_mismatch'}"><c:set var="errText" value="Your college email must contain your own admission number."/></c:when>
  <c:when test="${param.error == 'weak_password'}"><c:set var="errText" value="Password needs at least 8 characters, with a letter and a digit."/></c:when>
  <c:when test="${param.error == 'invalid_whatsapp'}"><c:set var="errText" value="Enter a valid 10-digit Indian mobile number (WhatsApp)."/></c:when>
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
  <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>
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
        <input id="l-id" type="text" name="studentId" placeholder="e.g. 25SCSE1180735" required autocomplete="username">
        <label for="l-pw">Password</label>
        <input id="l-pw" type="password" name="password" required autocomplete="current-password">
        <button type="submit">Login</button>
      </form>

      <%-- REGISTER FORM: field names must match RegisterServlet (server re-checks everything) --%>
      <form id="registerForm" class="card ${showRegister ? '' : 'hidden'}" method="post" action="${ctx}/register">
        <h2>Create your account</h2>
        <label for="r-id">Admission number</label>
        <input id="r-id" type="text" name="studentId" placeholder="e.g. 25SCSE1180735" required>
        <label for="r-name">Full name</label>
        <input id="r-name" type="text" name="name" maxlength="100" required>
        <label for="r-branch">Branch</label>
        <input id="r-branch" type="text" name="branch" maxlength="50" placeholder="e.g. CSE" required>
        <label for="r-year">Year</label>
        <input id="r-year" type="number" name="year" min="1" max="4" required>
        <label for="r-email">College email</label>
        <input id="r-email" type="email" name="email" placeholder="name.25scse1180735@galgotiasuniversity.ac.in" required>
        <label for="r-res">Hostel or day scholar</label>
        <select id="r-res" name="hostelOrDay" required>
          <option value="Hostel">Hostel</option>
          <option value="Day Scholar">Day Scholar</option>
        </select>
        <label for="r-wa">WhatsApp number</label>
        <input id="r-wa" type="tel" name="whatsappNumber" placeholder="10-digit mobile number" required>
        <label class="check">
          <input type="checkbox" name="consent" value="yes" required>
          <span>I agree that my WhatsApp number may be shown <strong>only</strong> to the owner of an item I borrowed, and <strong>only</strong> if that borrow becomes overdue. Every reveal is logged.</span>
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
</script>
</body>
</html>
