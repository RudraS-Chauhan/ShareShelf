<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Reports (GET /reports). Model from ReportsServlet: totalListings, totalRequests, listingStatus, listingCategory,
     requestStatus, topRequested, checker (the overdue-checker thread's status; may be null) --%>
<c:set var="pageTitle" value="Reports"/>
<c:set var="active" value="reports"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Reports</h1>
    <p>Live counts straight from the database.</p>
  </div>
  <a class="btn btn-sm btn-outline" href="${ctx}/reports?format=csv">⬇ Download CSV</a>
</div>

<section class="stats">
  <div class="stat"><div class="num">${totalListings}</div><div class="lbl">Total listings</div></div>
  <div class="stat"><div class="num">${listingStatus['AVAILABLE']}</div><div class="lbl">Available now</div></div>
  <div class="stat"><div class="num">${totalRequests}</div><div class="lbl">Total requests</div></div>
  <div class="stat"><div class="num">${requestStatus['PENDING']}</div><div class="lbl">Pending requests</div></div>
  <div class="stat ${requestStatus['OVERDUE'] > 0 ? 'warn' : ''}"><div class="num">${requestStatus['OVERDUE']}</div><div class="lbl">Overdue</div></div>
  <div class="stat"><div class="num">${requestStatus['COMPLETED']}</div><div class="lbl">Completed exchanges</div></div>
</section>

<h2 class="section">Most requested categories</h2>
<c:choose>
  <c:when test="${empty topRequested}">
    <div class="empty"><strong>No requests yet</strong>This ranking appears once students start requesting items.</div>
  </c:when>
  <c:otherwise>
    <div class="highlight">
      <c:forEach var="e" items="${topRequested}" varStatus="s">
        <c:if test="${s.first}"><div style="font-size:15px;margin-bottom:8px">🏆 Students want <strong><c:out value="${e.key}"/></strong> the most (${e.value}${e.value == 1 ? ' request' : ' requests'}).</div></c:if>
        <c:if test="${s.first}"><c:set var="topMax" value="${e.value}"/></c:if>
        <div class="bar-row">
          <span><c:out value="${e.key}"/></span>
          <div class="bar-track"><div class="bar-fill" style="width:${e.value * 100 / topMax}%"></div></div>
          <strong>${e.value}</strong>
        </div>
      </c:forEach>
    </div>
  </c:otherwise>
</c:choose>

<div class="report-grid" style="margin-top:28px">
  <section>
    <h2 class="section" style="margin-top:0">Listings by status</h2>
    <div class="table-wrap"><table class="data"><tbody>
      <c:forEach var="e" items="${listingStatus}">
        <tr><td><span class="badge badge-${fn:toLowerCase(e.key)}"><c:out value="${e.key}"/></span></td><td class="num"><strong>${e.value}</strong></td></tr>
      </c:forEach>
    </tbody></table></div>
  </section>
  <section>
    <h2 class="section" style="margin-top:0">Requests by status</h2>
    <div class="table-wrap"><table class="data"><tbody>
      <c:forEach var="e" items="${requestStatus}">
        <tr><td><span class="badge badge-${fn:toLowerCase(e.key)}"><c:out value="${e.key}"/></span></td><td class="num"><strong>${e.value}</strong></td></tr>
      </c:forEach>
    </tbody></table></div>
  </section>
  <section>
    <h2 class="section" style="margin-top:0">Listings by category</h2>
    <div class="table-wrap"><table class="data"><tbody>
      <c:forEach var="e" items="${listingCategory}">
        <tr><td><c:out value="${e.key}"/></td><td class="num"><strong>${e.value}</strong></td></tr>
      </c:forEach>
      <c:if test="${empty listingCategory}"><tr><td class="sub">No listings yet</td><td></td></tr></c:if>
    </tbody></table></div>
  </section>
</div>

<h2 class="section">Overdue checker (background thread)</h2>
<div class="table-wrap"><table class="data"><tbody>
  <tr><td>Status</td><td>
    <c:choose>
      <c:when test="${empty checker}">Not running</c:when>
      <c:when test="${not empty checker.lastError}"><span class="badge badge-rejected">Last run failed</span> <c:out value="${checker.lastError}"/></c:when>
      <c:otherwise><span class="badge badge-available">Running</span></c:otherwise>
    </c:choose>
  </td></tr>
  <c:if test="${not empty checker}">
    <tr><td>Last scan</td><td><c:choose><c:when test="${empty checker.lastRun}">waiting for the first scan…</c:when><c:otherwise>${fn:replace(fn:substring(checker.lastRun, 0, 19), 'T', ' ')}</c:otherwise></c:choose></td></tr>
    <tr><td>Requests flagged since startup</td><td>${checker.totalFlagged}</td></tr>
  </c:if>
</tbody></table></div>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
