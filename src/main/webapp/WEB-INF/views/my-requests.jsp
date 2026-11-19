<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- My requests (GET /my-requests). Model from MyRequestsServlet: sentRequests, receivedRequests, borrowDays --%>
<c:set var="pageTitle" value="My requests"/>
<c:set var="active" value="requests"/>
<c:set var="back" value="my-requests"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>My requests</h1>
    <p>Track what you asked for, and respond to people asking for your items.</p>
  </div>
</div>

<div id="live" data-mode="reload"></div>
<h2 class="section" style="margin-top:8px">Received on my listings <span class="role-tag">You are the Lender</span> <span class="count-pill">${fn:length(receivedRequests)}</span></h2>
<c:choose>
  <c:when test="${empty receivedRequests}">
    <div class="empty"><strong>No requests received</strong>When someone asks for one of your items it will appear here.</div>
  </c:when>
  <c:otherwise>
    <div class="table-wrap">
      <table class="data">
        <thead><tr><th>Item</th><th>Borrower</th><th>Message</th><th>Status</th><th>Due back</th><th>Actions</th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${receivedRequests}">
          <tr>
            <td data-label="Item"><strong><c:out value="${r.itemTitle}"/></strong><div class="sub"><c:out value="${r.listingType}"/></div></td>
            <td data-label="Borrower">
              <a href="${ctx}/profile?id=${r.requesterId}"><c:out value="${r.requesterName}"/></a><br>
              <c:set var="n" value="${r.requesterCompleted}"/><%@ include file="/WEB-INF/views/fragments/trust.jspf" %>
              <div class="sub"><c:out value="${empty r.requesterBranch ? '' : r.requesterBranch}"/><c:if test="${r.requesterYear > 0}"> · Year ${r.requesterYear}</c:if></div>
              <c:if test="${not empty r.requesterStarsText}"><div class="sub">★ ${r.requesterStarsText}</div></c:if>
              <c:if test="${not empty r.requesterBadge}"><span class="badge-chip">🏅 ${r.requesterBadge}</span></c:if>
              <c:if test="${r.requesterLate > 0}"><div class="sub late">${r.requesterLate} late return${r.requesterLate == 1 ? '' : 's'}</div></c:if>
            </td>
            <td class="msg-text" data-label="Message"><c:out value="${empty r.message ? '(no message)' : r.message}"/></td>
            <td data-label="Status"><span class="badge badge-${fn:toLowerCase(r.status)}"><c:out value="${r.status}"/></span></td>
            <td class="due ${r.status == 'OVERDUE' ? 'late' : ''}" data-label="Due back">
              <c:choose><c:when test="${empty r.dueText}">–</c:when><c:otherwise><c:out value="${r.dueText}"/></c:otherwise></c:choose>
              <c:if test="${not empty r.timerNote}"><div class="sub"><c:out value="${r.timerNote}"/></div></c:if>
              <c:if test="${not empty r.returnConditionText}"><div class="sub"><c:out value="${r.returnConditionText}"/></div></c:if>
              <c:if test="${not empty r.meetTimeText}"><div class="sub">Meet: <c:out value="${r.meetTimeText}"/></div></c:if>
              <c:if test="${not empty r.meetNote}"><div class="sub">Note: <c:out value="${r.meetNote}"/></div></c:if>
              <c:if test="${r.contactRevealed}"><div class="sub">Contact revealed ${fn:substring(r.revealedAt, 0, 10)}</div></c:if>
            </td>
            <td data-label="Actions"><%@ include file="/WEB-INF/views/fragments/owner-actions.jspf" %></td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </div>
  </c:otherwise>
</c:choose>

<h2 class="section">Sent by me <span class="role-tag">You are the Borrower</span> <span class="count-pill">${fn:length(sentRequests)}</span></h2>
<c:choose>
  <c:when test="${empty sentRequests}">
    <div class="empty"><strong>You haven't requested anything</strong><a href="${ctx}/search">Find an item</a> you need.</div>
  </c:when>
  <c:otherwise>
    <div class="table-wrap">
      <table class="data">
        <thead><tr><th>Item</th><th>Lender</th><th>My message</th><th>Status</th><th>Due back</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${sentRequests}">
          <tr>
            <td data-label="Item"><strong><c:out value="${r.itemTitle}"/></strong><div class="sub"><c:out value="${r.listingType}"/></div></td>
            <td data-label="Lender">
              <a href="${ctx}/profile?id=${r.ownerId}"><c:out value="${r.ownerName}"/></a><br>
              <c:set var="n" value="${r.ownerCompleted}"/><%@ include file="/WEB-INF/views/fragments/trust.jspf" %>
              <c:if test="${not empty r.ownerStarsText}"><div class="sub">★ ${r.ownerStarsText}</div></c:if>
              <c:if test="${not empty r.ownerBadge}"><span class="badge-chip">🏅 ${r.ownerBadge}</span></c:if>
            </td>
            <td class="msg-text" data-label="My message"><c:out value="${empty r.message ? '(no message)' : r.message}"/></td>
            <td data-label="Status"><span class="badge badge-${fn:toLowerCase(r.status)}"><c:out value="${r.status}"/></span></td>
            <td class="due ${r.status == 'OVERDUE' ? 'late' : ''}" data-label="Due back">
              <c:choose><c:when test="${empty r.dueText}">–</c:when><c:otherwise><c:out value="${r.dueText}"/></c:otherwise></c:choose>
              <c:if test="${not empty r.timerNote}"><div class="sub"><c:out value="${r.timerNote}"/></div></c:if>
              <c:if test="${not empty r.returnConditionText}"><div class="sub"><c:out value="${r.returnConditionText}"/></div></c:if>
              <c:if test="${not empty r.meetTimeText}"><div class="sub">Meet: <c:out value="${r.meetTimeText}"/></div></c:if>
              <c:if test="${not empty r.meetNote}"><div class="sub">Note: <c:out value="${r.meetNote}"/></div></c:if>
              <c:if test="${r.status == 'OVERDUE'}"><div class="sub">Please return it soon</div></c:if>
            </td>
            <td data-label="">
              <c:if test="${r.status == 'PENDING'}">
                <form class="inline" method="post" action="${ctx}/request/cancel">
                  <input type="hidden" name="requestId" value="${r.requestId}">
                  <input type="hidden" name="back" value="my-requests">
                  <button type="submit" class="btn btn-sm btn-danger">Cancel</button>
                </form>
              </c:if>
              <c:if test="${r.status == 'ACCEPTED' and r.borrow and r.itemStatus == 'RESERVED'}">
                <a class="btn btn-sm" href="${ctx}/scan?requestId=${r.requestId}">Confirm pickup (code)</a>
                <div class="hint">or scan the owner's QR with your camera</div>
              </c:if>
              <c:if test="${(r.status == 'ACCEPTED' or r.status == 'OVERDUE') and r.borrow and r.itemStatus == 'BORROWED'}">
                <a class="btn btn-sm" href="${ctx}/handover/qr?requestId=${r.requestId}">Show return QR</a>
              </c:if>
            </td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </div>
  </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
