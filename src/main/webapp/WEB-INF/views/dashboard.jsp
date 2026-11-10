<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Dashboard (GET /dashboard). Model from DashboardServlet: myListings, pendingRequests, overdueCount, completedCount, borrowDays --%>
<c:set var="pageTitle" value="Dashboard"/>
<c:set var="active" value="dashboard"/>
<c:set var="back" value="dashboard"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Welcome, <c:out value="${sessionScope.studentName}"/></h1>
    <p>Your listings and the requests waiting for your decision.</p>
  </div>
  <a class="btn btn-sm" href="${ctx}/add-listing">+ Add a listing</a>
</div>

<c:if test="${overdueCount > 0}">
  <div class="banner alert" role="alert">
    ⏰ <strong>${overdueCount}</strong> of your items ${overdueCount == 1 ? 'is' : 'are'} overdue.
    <a href="${ctx}/my-requests">Open My requests</a> to contact the borrower.
  </div>
</c:if>

<section class="stats" aria-label="Your numbers">
  <div class="stat"><div class="num">${fn:length(myListings)}</div><div class="lbl">Your listings</div></div>
  <div class="stat"><div class="num">${fn:length(pendingRequests)}</div><div class="lbl">Requests waiting for you</div></div>
  <div class="stat ${overdueCount > 0 ? 'warn' : ''}"><div class="num">${overdueCount}</div><div class="lbl">Overdue returns</div></div>
  <div class="stat"><div class="num">${completedCount}</div><div class="lbl">Completed exchanges</div></div>
</section>

<h2 class="section">Requests on your items <span class="count-pill">${fn:length(pendingRequests)}</span></h2>
<c:choose>
  <c:when test="${empty pendingRequests}">
    <div class="empty"><strong>Nothing waiting for you</strong>New requests on your listings will show up here.</div>
  </c:when>
  <c:otherwise>
    <div class="table-wrap">
      <table class="data">
        <thead><tr><th>Item</th><th>Requested by</th><th>Message</th><th>Received</th><th>Decision</th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${pendingRequests}">
          <tr>
            <td><strong><c:out value="${r.itemTitle}"/></strong><div class="sub"><c:out value="${r.listingType}"/></div></td>
            <td>
              <c:out value="${r.requesterName}"/><br>
              <c:set var="n" value="${r.requesterCompleted}"/><%@ include file="/WEB-INF/views/fragments/trust.jspf" %>
            </td>
            <td class="msg-text"><c:out value="${empty r.message ? '(no message)' : r.message}"/></td>
            <td class="due">${fn:substring(r.createdAt, 0, 16)}</td>
            <td><%@ include file="/WEB-INF/views/fragments/owner-actions.jspf" %></td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </div>
  </c:otherwise>
</c:choose>

<h2 class="section">My listings <span class="count-pill">${fn:length(myListings)}</span></h2>
<c:choose>
  <c:when test="${empty myListings}">
    <div class="empty"><strong>You haven't listed anything yet</strong>Got a spare calculator, lab coat or textbook? <a href="${ctx}/add-listing">Add your first listing</a>.</div>
  </c:when>
  <c:otherwise>
    <div class="table-wrap">
      <table class="data">
        <thead><tr><th>Item</th><th>Category</th><th>Type</th><th>Status</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="item" items="${myListings}">
          <tr>
            <td><strong><c:out value="${item.title}"/></strong><div class="sub"><c:out value="${item.itemCondition}"/> · <c:out value="${item.location}"/></div></td>
            <td><c:out value="${item.category}"/></td>
            <td><span class="badge badge-type"><c:out value="${item.listingType}"/></span></td>
            <td><span class="badge badge-${fn:toLowerCase(item.status)}"><c:out value="${item.status}"/></span></td>
            <td>
              <c:if test="${item.status == 'AVAILABLE'}">
                <form class="inline" method="post" action="${ctx}/listing/close"
                      onsubmit="return confirm('Close this listing? It will disappear from search.');">
                  <input type="hidden" name="itemId" value="${item.itemId}">
                  <button type="submit" class="btn btn-sm btn-outline">Close listing</button>
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
