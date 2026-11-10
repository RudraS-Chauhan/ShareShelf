<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%-- Friendly error page. Used by BaseServlet (database trouble) and by web.xml for 404/405/500. --%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Something went wrong · ShareShelf</title>
  <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>
<div class="error-box">
  <div class="card">
    <h1>
      <c:choose>
        <c:when test="${not empty errorTitle}"><c:out value="${errorTitle}"/></c:when>
        <c:when test="${pageContext.errorData.statusCode == 404}">Page not found</c:when>
        <c:when test="${pageContext.errorData.statusCode == 405}">That action isn't allowed</c:when>
        <c:otherwise>Something went wrong</c:otherwise>
      </c:choose>
    </h1>
    <p style="color:var(--muted)">
      <c:choose>
        <c:when test="${not empty errorMessage}"><c:out value="${errorMessage}"/></c:when>
        <c:when test="${pageContext.errorData.statusCode == 404}">We couldn't find the page you were looking for.</c:when>
        <c:otherwise>Please go back and try again.</c:otherwise>
      </c:choose>
    </p>
    <a class="btn btn-sm" href="${ctx}/dashboard">Back to the dashboard</a>
  </div>
</div>
</body>
</html>
