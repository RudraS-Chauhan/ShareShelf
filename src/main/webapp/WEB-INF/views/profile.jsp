<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Profile (GET /profile?id=). Model from ProfileServlet: profile, isMe --%>
<c:set var="pageTitle" value="Profile"/>
<c:set var="active" value="profile"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1><c:out value="${profile.name}"/><c:if test="${isMe}"> <span class="role-tag">This is you</span></c:if></h1>
    <p><c:out value="${profile.branch}"/> · Year <c:out value="${profile.year}"/> · <c:out value="${profile.hostelOrDay}"/></p>
  </div>
</div>

<c:if test="${not empty profile.borrowerBadge or not empty profile.lenderBadge}">
  <p>
    <c:if test="${not empty profile.borrowerBadge}"><span class="badge-chip">🏅 ${profile.borrowerBadge}</span></c:if>
    <c:if test="${not empty profile.lenderBadge}"><span class="badge-chip">🏅 ${profile.lenderBadge}</span></c:if>
  </p>
</c:if>

<div class="stats">
  <div class="stat"><strong>${profile.completed}</strong><span>Completed exchanges</span></div>
  <div class="stat"><strong>${profile.late}</strong><span>Late returns</span></div>
  <div class="stat"><strong>${empty profile.borrowerStarsText ? '–' : profile.borrowerStarsText}</strong><span>As a borrower (${profile.borrowerStarsN} ratings)</span></div>
  <div class="stat"><strong>${empty profile.lenderStarsText ? '–' : profile.lenderStarsText}</strong><span>As a lender (${profile.lenderStarsN} ratings)</span></div>
</div>

<h2 class="section">Recent reviews</h2>
<c:choose>
  <c:when test="${empty profile.reviews}">
    <div class="empty"><strong>No reviews yet</strong>Reviews appear here once both people have rated an exchange.</div>
  </c:when>
  <c:otherwise>
    <ul class="notif-list">
      <c:forEach var="rv" items="${profile.reviews}">
        <li>
          <strong>${rv.stars} ★</strong> as a ${rv.role} — <c:out value="${rv.comment}"/>
          <div class="sub">by <c:out value="${rv.raterName}"/> · <c:out value="${rv.whenText}"/></div>
        </li>
      </c:forEach>
    </ul>
  </c:otherwise>
</c:choose>
<p class="hint">Badges: "Reliable borrower" needs 3 finished exchanges, no late returns and an average of 4 stars or more as a borrower.
  "Trusted lender" needs 3 finished exchanges and 4 stars or more as a lender.</p>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
