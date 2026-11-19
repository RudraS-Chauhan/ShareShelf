<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- QR page (GET /handover/qr). Model from QrIssueServlet: request, kind, code, expires, qrSvg --%>
<c:set var="pageTitle" value="${kind == 'HANDOVER' ? 'Handover QR' : 'Return QR'}"/>
<c:set var="active" value="requests"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>${kind == 'HANDOVER' ? 'Handover QR' : 'Return QR'}</h1>
    <p><c:out value="${request.itemTitle}"/> ·
      <c:choose>
        <c:when test="${kind == 'HANDOVER'}">borrower: <c:out value="${request.requesterName}"/></c:when>
        <c:otherwise>owner: <c:out value="${request.ownerName}"/></c:otherwise>
      </c:choose>
    </p>
  </div>
</div>

<div id="live" data-mode="goto" data-goto="/my-requests"></div>
<div class="card qr-card">
  <p class="qr-step">
    <c:choose>
      <c:when test="${kind == 'HANDOVER'}">Ask the borrower to scan this with their phone camera while you are together.</c:when>
      <c:otherwise>Ask the owner to scan this with their phone camera while you are together.</c:otherwise>
    </c:choose>
  </p>
  <div class="qr-box">${qrSvg}</div>
  <p class="qr-or">Camera not working? Tell them this code:</p>
  <div class="qr-code" aria-label="6 digit code"><c:out value="${code}"/></div>
  <p class="hint">Valid until ${expires}. Reloading this page makes a new code and cancels this one.
    The code locks after 5 wrong tries.</p>
  <a class="btn btn-outline" href="${ctx}/my-requests">Back to my requests</a>
</div>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
