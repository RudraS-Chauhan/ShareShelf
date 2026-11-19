<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Borrower rates the owner (GET/POST /rate). Model from RateServlet: request, error --%>
<c:set var="pageTitle" value="Rate"/>
<c:set var="active" value="requests"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Rate <c:out value="${otherName}"/> as a ${roleText}</h1>
    <p>About "<c:out value="${request.itemTitle}"/>". Ratings appear on their profile once you have both rated.</p>
  </div>
</div>

<form class="card form-card" method="post" action="${ctx}/rate">
  <c:if test="${not empty error}"><div class="msg error" role="alert"><c:out value="${error}"/></div></c:if>
  <input type="hidden" name="requestId" value="${request.requestId}">
  <div class="form-grid">
    <div class="full">
      <label>How was it with <c:out value="${otherName}"/>? <span class="hint">(required)</span></label>
      <div class="choice-row">
        <c:forEach begin="1" end="5" var="s">
          <label class="choice"><input type="radio" name="stars" value="${s}" ${param.stars == s ? 'checked' : ''}> ${s} ★</label>
        </c:forEach>
      </div>
    </div>
    <div class="full">
      <label for="comment">Comment <span class="hint">(optional)</span></label>
      <input id="comment" type="text" name="comment" maxlength="200" value="<c:out value='${param.comment}'/>" placeholder="e.g. Met on time, item was as described">
    </div>
  </div>
  <div class="form-actions"><button type="submit" class="btn">Save rating</button></div>
</form>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
