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

<h2 class="section" style="margin-top:8px">Received on my listings <span class="count-pill">${fn:length(receivedRequests)}</span></h2>
<c:choose>
  <c:when test="${empty receivedRequests}">
    <div class="empty"><strong>No requests received</strong>When someone asks for one of your items it will appear here.</div>
  </c:when>
  <c:otherwise>
    <div class="table-wrap">
      <table class="data">
        <thead><tr><th>Item</th><th>From</th><th>Message</th><th>Status</th><th>Due back</th><th>Actions</th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${receivedRequests}">
          <tr>
            <td><strong><c:out value="${r.itemTitle}"/></strong><div class="sub"><c:out value="${r.listingType}"/></div></td>
            <td>
              <c:out value="${r.requesterName}"/><br>
              <c:set var="n" value="${r.requesterCompleted}"/><%@ include file="/WEB-INF/views/fragments/trust.jspf" %>
            </td>
            <td class="msg-text"><c:out value="${empty r.message ? '(no message)' : r.message}"/></td>
            <td><span class="badge badge-${fn:toLowerCase(r.status)}"><c:out value="${r.status}"/></span></td>
            <td class="due ${r.status == 'OVERDUE' ? 'late' : ''}">
              <c:choose><c:when test="${empty r.expectedReturnDate}">–</c:when><c:otherwise><c:out value="${r.expectedReturnDate}"/></c:otherwise></c:choose>
              <c:if test="${r.contactRevealed}"><div class="sub">Contact revealed ${fn:substring(r.revealedAt, 0, 10)}</div></c:if>
            </td>
            <td><%@ include file="/WEB-INF/views/fragments/owner-actions.jspf" %></td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </div>
  </c:otherwise>
</c:choose>

<h2 class="section">Sent by me <span class="count-pill">${fn:length(sentRequests)}</span></h2>
<c:choose>
  <c:when test="${empty sentRequests}">
    <div class="empty"><strong>You haven't requested anything</strong><a href="${ctx}/search">Find an item</a> you need.</div>
  </c:when>
  <c:otherwise>
    <div class="table-wrap">
      <table class="data">
        <thead><tr><th>Item</th><th>Owner</th><th>My message</th><th>Status</th><th>Due back</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${sentRequests}">
          <tr>
            <td><strong><c:out value="${r.itemTitle}"/></strong><div class="sub"><c:out value="${r.listingType}"/></div></td>
            <td>
              <c:out value="${r.ownerName}"/><br>
              <c:set var="n" value="${r.ownerCompleted}"/><%@ include file="/WEB-INF/views/fragments/trust.jspf" %>
            </td>
            <td class="msg-text"><c:out value="${empty r.message ? '(no message)' : r.message}"/></td>
            <td><span class="badge badge-${fn:toLowerCase(r.status)}"><c:out value="${r.status}"/></span></td>
            <td class="due ${r.status == 'OVERDUE' ? 'late' : ''}">
              <c:choose><c:when test="${empty r.expectedReturnDate}">–</c:when><c:otherwise><c:out value="${r.expectedReturnDate}"/></c:otherwise></c:choose>
              <c:if test="${r.status == 'OVERDUE'}"><div class="sub">Please return it soon</div></c:if>
            </td>
            <td>
              <c:if test="${r.status == 'PENDING'}">
                <form class="inline" method="post" action="${ctx}/request/cancel">
                  <input type="hidden" name="requestId" value="${r.requestId}">
                  <input type="hidden" name="back" value="my-requests">
                  <button type="submit" class="btn btn-sm btn-danger">Cancel</button>
                </form>
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
