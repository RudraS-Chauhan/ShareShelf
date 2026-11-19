<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Confirm page (GET/POST /scan). Model from ScanServlet: request, kind, token (may be empty), conditions, error --%>
<c:set var="pageTitle" value="${kind == 'HANDOVER' ? 'Confirm pickup' : 'Confirm return'}"/>
<c:set var="active" value="requests"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>${kind == 'HANDOVER' ? 'Confirm pickup' : 'Confirm return'}</h1>
    <p><c:out value="${request.itemTitle}"/> ·
      <c:choose>
        <c:when test="${kind == 'HANDOVER'}">from <c:out value="${request.ownerName}"/></c:when>
        <c:otherwise>from <c:out value="${request.requesterName}"/></c:otherwise>
      </c:choose>
    </p>
  </div>
</div>

<form class="card form-card" method="post" action="${ctx}/scan">
  <c:if test="${not empty error}"><div class="msg error" role="alert"><c:out value="${error}"/></div></c:if>
  <input type="hidden" name="requestId" value="${request.requestId}">
  <input type="hidden" name="t" value="<c:out value='${token}'/>">
  <div class="form-grid">
    <c:choose>
      <c:when test="${not empty token}">
        <div class="full"><div class="msg success">QR scanned. Check it is the right item, then confirm.</div></div>
      </c:when>
      <c:otherwise>
        <div class="full">
          <label for="code">6-digit code</label>
          <input id="code" type="text" name="code" inputmode="numeric" pattern="[0-9]{6}" maxlength="6" required
                 autocomplete="off" placeholder="123456" class="code-input">
          <div class="hint">Shown on the other person's phone under their QR.</div>
        </div>
      </c:otherwise>
    </c:choose>

    <c:if test="${kind == 'RETURN'}">
      <div class="full">
        <label>Condition of the item</label>
        <div class="choice-row">
          <c:forEach var="c" items="${conditions}">
            <label class="choice"><input type="radio" name="condition" value="${c}" ${param.condition == c || (empty param.condition and c == 'OK') ? 'checked' : ''}>
              <c:out value="${c == 'OK' ? 'Returned fine' : (c == 'DAMAGED' ? 'Damaged' : 'Parts missing')}"/></label>
          </c:forEach>
        </div>
      </div>
      <div class="hint full">After the code is verified you will rate <c:out value="${request.requesterName}"/> as a borrower (next screen).</div>
    </c:if>
  </div>
  <div class="form-actions">
    <button type="submit" class="btn">${kind == 'HANDOVER' ? 'Confirm I picked it up' : 'Confirm return'}</button>
    <a class="btn btn-outline" href="${ctx}/my-requests">Cancel</a>
  </div>
</form>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
