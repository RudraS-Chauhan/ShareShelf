<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Dashboard (GET /dashboard). Model from DashboardServlet: myListings, pendingRequests, overdueCount, completedCount, borrowDays --%>
<c:set var="pageTitle" value="Dashboard"/>
<c:set var="active" value="dashboard"/>
<c:set var="back" value="dashboard"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<section class="dash-hero">
  <div class="dash-hero-text">
    <p class="dash-eyebrow">${greeting}</p>
    <h1><c:out value="${firstName}"/>, here is your shelf</h1>
    <p class="dash-sub">Your listings, your requests and what needs you next.</p>
    <div class="dash-actions">
      <a class="btn-sm" href="${ctx}/add-listing">+ Add a listing</a>
      <a class="btn-sm btn-outline" href="${ctx}/search">Find an item</a>
      <a class="btn-sm btn-outline" href="${ctx}/wanted">Wanted board</a>
    </div>
  </div>
  <a class="level-card" href="${ctx}/profile?id=${sessionScope.studentId}" aria-label="Your trust level">
    <div class="level-top"><span class="level-icon">${level.icon}</span>
      <div><div class="level-name">${level.name}</div><div class="level-sub">Level ${level.level} of 5</div></div></div>
    <div class="level-bar" role="progressbar" aria-valuenow="${level.percent}" aria-valuemin="0" aria-valuemax="100"><span style="width:${level.percent}%"></span></div>
    <div class="level-note">
      <c:choose>
        <c:when test="${level.max}">Top level reached. Thank you for sharing!</c:when>
        <c:otherwise>${level.toNext} more exchange${level.toNext == 1 ? '' : 's'} to <strong>${level.nextName}</strong></c:otherwise>
      </c:choose>
    </div>
    <c:if test="${not empty myProfile and (myProfile.lenderStarsN > 0 or myProfile.borrowerStarsN > 0)}">
      <div class="level-stars">
        <c:if test="${myProfile.lenderStarsN > 0}">★ ${myProfile.lenderStarsText} as lender</c:if>
        <c:if test="${myProfile.lenderStarsN > 0 and myProfile.borrowerStarsN > 0}"> · </c:if>
        <c:if test="${myProfile.borrowerStarsN > 0}">★ ${myProfile.borrowerStarsText} as borrower</c:if>
      </div>
    </c:if>
  </a>
</section>

<section class="next-steps" aria-label="What needs you next">
  <h2 class="section">Next for you</h2>
  <div class="next-grid">
    <c:if test="${overdueCount > 0}">
      <a class="next-card danger" href="${ctx}/my-requests"><span class="next-ico">⏰</span><span><strong>${overdueCount} overdue</strong><br>Open My requests to follow up.</span></a>
    </c:if>
    <c:if test="${not empty pendingRatings}">
      <a class="next-card warn" href="${ctx}/rate?requestId=${pendingRatings[0].requestId}"><span class="next-ico">⭐</span><span><strong>Rate ${pendingRatings[0].otherName}</strong><br>Needed before your next request.</span></a>
    </c:if>
    <c:if test="${fn:length(pendingRequests) > 0}">
      <a class="next-card" href="#requests"><span class="next-ico">📨</span><span><strong>${fn:length(pendingRequests)} request${fn:length(pendingRequests) == 1 ? '' : 's'} waiting</strong><br>Accept or reject below.</span></a>
    </c:if>
    <c:if test="${handoversReady > 0}">
      <a class="next-card" href="${ctx}/my-requests"><span class="next-ico">🔳</span><span><strong>${handoversReady} handover${handoversReady == 1 ? '' : 's'} ready</strong><br>Show or scan the QR when you meet.</span></a>
    </c:if>
    <c:if test="${toReturn > 0}">
      <a class="next-card" href="${ctx}/my-requests"><span class="next-ico">↩️</span><span><strong>${toReturn} item${toReturn == 1 ? '' : 's'} to return</strong><br>Check the timer in My requests.</span></a>
    </c:if>
    <c:if test="${overdueCount == 0 and empty pendingRatings and fn:length(pendingRequests) == 0 and handoversReady == 0 and toReturn == 0}">
      <div class="next-card calm"><span class="next-ico">✅</span><span><strong>You are all caught up</strong><br>Nothing needs you right now. Maybe list something or check the Wanted board.</span></div>
    </c:if>
  </div>
</section>

<section class="stats" aria-label="Your numbers">
  <div class="stat"><span class="stat-ico">📚</span><div class="num">${fn:length(myListings)}</div><div class="lbl">Your listings</div></div>
  <div class="stat"><span class="stat-ico">📨</span><div class="num">${fn:length(pendingRequests)}</div><div class="lbl">Requests waiting for you</div></div>
  <div class="stat ${overdueCount > 0 ? 'warn' : ''}"><span class="stat-ico">⏰</span><div class="num">${overdueCount}</div><div class="lbl">Overdue returns</div></div>
  <div class="stat"><span class="stat-ico">🤝</span><div class="num">${completedCount}</div><div class="lbl">Completed exchanges</div></div>
</section>

<div id="live" data-mode="reload"></div>
<h2 class="section" id="requests">Requests on your items <span class="role-tag">You are the Lender</span> <span class="count-pill">${fn:length(pendingRequests)}</span></h2>
<c:choose>
  <c:when test="${empty pendingRequests}">
    <div class="empty"><div class="empty-ico">📭</div><strong>Nothing waiting for you</strong>New requests on your listings will show up here.</div>
  </c:when>
  <c:otherwise>
    <div class="table-wrap">
      <table class="data">
        <thead><tr><th>Item</th><th>Borrower (asking)</th><th>Message</th><th>Received</th><th>Decision</th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${pendingRequests}">
          <tr>
            <td data-label="Item"><strong><c:out value="${r.itemTitle}"/></strong><div class="sub"><c:out value="${r.listingType}"/></div></td>
            <td data-label="Borrower (asking)">
              <a href="${ctx}/profile?id=${r.requesterId}"><c:out value="${r.requesterName}"/></a><br>
              <c:set var="n" value="${r.requesterCompleted}"/><%@ include file="/WEB-INF/views/fragments/trust.jspf" %>
              <div class="sub"><c:out value="${empty r.requesterBranch ? '' : r.requesterBranch}"/><c:if test="${r.requesterYear > 0}"> · Year ${r.requesterYear}</c:if></div>
              <c:if test="${not empty r.requesterStarsText}"><div class="sub">★ ${r.requesterStarsText}</div></c:if>
              <c:if test="${not empty r.requesterBadge}"><span class="badge-chip">🏅 ${r.requesterBadge}</span></c:if>
              <c:if test="${r.requesterLate > 0}"><div class="sub late">${r.requesterLate} late return${r.requesterLate == 1 ? '' : 's'}</div></c:if>
            </td>
            <td class="msg-text" data-label="Message"><c:out value="${empty r.message ? '(no message)' : r.message}"/></td>
            <td class="due" data-label="Received">${fn:substring(r.createdAt, 0, 16)}</td>
            <td data-label="Decision"><%@ include file="/WEB-INF/views/fragments/owner-actions.jspf" %></td>
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
    <div class="empty"><div class="empty-ico">📚</div><strong>You haven't listed anything yet</strong>Got a spare calculator, lab coat or textbook? <a href="${ctx}/add-listing">Add your first listing</a>.</div>
  </c:when>
  <c:otherwise>
    <div class="table-wrap">
      <table class="data">
        <thead><tr><th>Item</th><th>Category</th><th>Type</th><th>Status</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="item" items="${myListings}">
          <tr>
            <td data-label="Item"><strong><c:out value="${item.title}"/></strong><div class="sub"><c:out value="${item.itemCondition}"/> · <c:out value="${item.location}"/></div></td>
            <td data-label="Category"><c:out value="${item.category}"/></td>
            <td data-label="Type"><span class="badge badge-type"><c:out value="${item.listingType}"/></span></td>
            <td data-label="Status"><span class="badge badge-${fn:toLowerCase(item.status)}"><c:out value="${item.status}"/></span></td>
            <td data-label="">
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
