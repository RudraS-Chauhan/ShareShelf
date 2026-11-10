<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Browse / search (GET /search). Model from SearchServlet: items, q, selectedCategory, selectedType, filtered, categories, types --%>
<c:set var="pageTitle" value="Search"/>
<c:set var="active" value="search"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Find an item</h1>
    <p>Everything shown here is available right now.</p>
  </div>
</div>

<form class="toolbar" method="get" action="${ctx}/search">
  <div class="grow">
    <label for="q">Keyword</label>
    <input id="q" type="search" name="q" placeholder="e.g. calculator" value="<c:out value='${q}'/>">
  </div>
  <div>
    <label for="category">Category</label>
    <select id="category" name="category">
      <option value="">All categories</option>
      <c:forEach var="c" items="${categories}">
        <option value="<c:out value='${c}'/>" ${c == selectedCategory ? 'selected' : ''}><c:out value="${c}"/></option>
      </c:forEach>
    </select>
  </div>
  <div>
    <label for="type">Type</label>
    <select id="type" name="type">
      <option value="">Any type</option>
      <c:forEach var="t" items="${types}">
        <option value="${t.name()}" ${t.name() == selectedType ? 'selected' : ''}><c:out value="${t.label}"/></option>
      </c:forEach>
    </select>
  </div>
  <button type="submit">Search</button>
  <a class="btn-clear" href="${ctx}/search">Clear</a>
</form>

<c:choose>
  <c:when test="${empty items}">
    <div class="empty">
      <c:choose>
        <c:when test="${filtered}"><strong>No listings found</strong>Try a different keyword or clear the filters.</c:when>
        <c:otherwise><strong>No listings yet</strong>Nothing has been listed. Be the first: <a href="${ctx}/add-listing">add a listing</a>.</c:otherwise>
      </c:choose>
    </div>
  </c:when>
  <c:otherwise>
    <p class="hint" style="margin:-8px 0 14px">${fn:length(items)}${fn:length(items) == 1 ? ' listing' : ' listings'} found</p>
    <div class="cards">
      <c:forEach var="item" items="${items}">
        <article class="item-card">
          <div class="meta">
            <span class="badge badge-type"><c:out value="${item.listingType}"/></span>
            <span class="badge badge-available">Available</span>
          </div>
          <h3><c:out value="${item.title}"/></h3>
          <c:if test="${not empty item.description}">
            <p class="desc"><c:out value="${fn:length(item.description) > 140 ? fn:substring(item.description, 0, 140) : item.description}"/><c:if test="${fn:length(item.description) > 140}">…</c:if></p>
          </c:if>
          <div class="facts">
            <div><strong>Category:</strong> <c:out value="${item.category}"/></div>
            <div><strong>Condition:</strong> <c:out value="${item.itemCondition}"/></div>
            <div><strong>Handover:</strong> <c:out value="${item.location}"/></div>
            <c:if test="${item.borrow and item.suggestedDays > 0}">
              <div><strong>Suggested:</strong> ${item.suggestedDays}${item.suggestedDays == 1 ? ' day' : ' days'}</div>
            </c:if>
            <div>
              <strong>Owner:</strong> <c:out value="${item.ownerName}"/>
              <c:set var="n" value="${item.ownerCompleted}"/><%@ include file="/WEB-INF/views/fragments/trust.jspf" %>
            </div>
          </div>
          <div class="foot">
            <c:choose>
              <c:when test="${item.ownerId == sessionScope.studentId}">
                <span class="hint">This is your listing.</span>
              </c:when>
              <c:otherwise>
                <details class="request-box">
                  <summary><span class="btn btn-sm">Request this item</span></summary>
                  <form method="post" action="${ctx}/request/send">
                    <input type="hidden" name="itemId" value="${item.itemId}">
                    <label for="msg-${item.itemId}">Message to the owner <span class="hint">(when and where suits you)</span></label>
                    <textarea id="msg-${item.itemId}" name="message" maxlength="255" placeholder="Hi! Could I borrow this tomorrow after 2 pm at the library?"></textarea>
                    <button type="submit" class="btn btn-sm">Send request</button>
                  </form>
                </details>
              </c:otherwise>
            </c:choose>
          </div>
        </article>
      </c:forEach>
    </div>
  </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
