<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Notifications (GET /notifications). Model from NotificationsServlet: notifications --%>
<c:set var="pageTitle" value="Notifications"/>
<c:set var="active" value="notifications"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head"><div><h1>Notifications</h1><p>What happened on your requests and listings.</p></div></div>
<c:choose>
  <c:when test="${empty notifications}">
    <div class="empty"><strong>Nothing yet</strong>You will see requests, pickups and returns here.</div>
  </c:when>
  <c:otherwise>
    <ul class="notif-list">
      <c:forEach var="n" items="${notifications}">
        <li class="${n.read ? '' : 'unread'}">
          <c:choose>
            <c:when test="${not empty n.link}"><a href="${ctx}${n.link}"><c:out value="${n.message}"/></a></c:when>
            <c:otherwise><c:out value="${n.message}"/></c:otherwise>
          </c:choose>
          <div class="sub"><c:out value="${n.whenText}"/></div>
        </li>
      </c:forEach>
    </ul>
  </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
