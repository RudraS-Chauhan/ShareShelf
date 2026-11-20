<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Wanted board (GET /wanted). Model from WantedServlet: posts, categories, myMonth, maxMonth, noOfferDays, maxLocation --%>
<c:set var="pageTitle" value="Wanted"/>
<c:set var="active" value="wanted"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Wanted</h1>
    <p>Need something that is not listed yet? Ask here. If you have it, tap "I have this".</p>
  </div>
</div>

<form class="card form-card" method="post" action="${ctx}/wanted">
  <input type="hidden" name="action" value="add">
  <div class="form-grid">
    <div class="full">
      <label for="wtitle">What do you need?</label>
      <input id="wtitle" type="text" name="title" maxlength="100" required placeholder="e.g. Lab coat, size M, for Friday">
    </div>
    <div>
      <label for="wcat">Category</label>
      <select id="wcat" name="category" required>
        <c:forEach var="c" items="${categories}"><option value="<c:out value='${c}'/>"><c:out value="${c}"/></option></c:forEach>
      </select>
    </div>
    <div class="full">
      <label for="wloc">Where should it be brought? <span class="hint">(optional, type any place)</span></label>
      <input id="wloc" type="text" name="location" maxlength="${maxLocation}" placeholder="e.g. D Block ground floor, near the canteen">
    </div>
    <div>
      <label for="wnote">Note <span class="hint">(optional)</span></label>
      <input id="wnote" type="text" name="note" maxlength="200" placeholder="e.g. Only for 2 days">
    </div>
  </div>
  <div class="form-actions">
    <button type="submit" class="btn">Post request</button>
    <span class="hint">${myMonth} of ${maxMonth} posts used in the last 30 days. A post nobody answers closes by itself after ${noOfferDays} days.</span>
  </div>
</form>

<h2 class="section">Open requests <span class="count-pill">${fn:length(posts)}</span></h2>
<c:choose>
  <c:when test="${empty posts}">
    <div class="empty"><strong>Nothing wanted right now</strong>Post what you need above.</div>
  </c:when>
  <c:otherwise>
    <ul class="notif-list">
      <c:forEach var="w" items="${posts}">
        <li>
          <strong><c:out value="${w.title}"/></strong> <span class="role-tag"><c:out value="${w.category}"/></span>
          <c:if test="${not empty w.location}"><div class="sub">📍 <c:out value="${w.location}"/></div></c:if>
          <c:if test="${not empty w.note}"><div class="sub"><c:out value="${w.note}"/></div></c:if>
          <div class="sub">
            <a href="${ctx}/profile?id=${w.studentId}"><c:out value="${w.studentName}"/></a>
            · <c:out value="${w.branch}"/> Year <c:out value="${w.year}"/> · <c:out value="${w.whenText}"/>
          </div>
          <form class="inline" method="post" action="${ctx}/wanted" style="margin-top:8px">
            <input type="hidden" name="postId" value="${w.postId}">
            <c:choose>
              <c:when test="${w.studentId == sessionScope.studentId}">
                <input type="hidden" name="action" value="close">
                <button type="submit" class="btn btn-sm btn-outline">I got it / close</button>
              </c:when>
              <c:otherwise>
                <input type="hidden" name="action" value="offer">
                <button type="submit" class="btn btn-sm">I have this</button>
              </c:otherwise>
            </c:choose>
          </form>
        </li>
      </c:forEach>
    </ul>
  </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
