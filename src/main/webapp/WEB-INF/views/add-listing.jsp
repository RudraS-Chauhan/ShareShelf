<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Add-listing form. Model from AddListingServlet: formError, categories, types, conditions, locations, borrowDays.
     Field names must match AddListingServlet; typed values come back via ${param.*} so nothing is lost on an error. --%>
<c:set var="pageTitle" value="Add a listing"/>
<c:set var="active" value="add"/>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Add a listing</h1>
    <p>Lend, exchange or give away something you don't need right now.</p>
  </div>
</div>

<form class="card form-card" method="post" action="${ctx}/add-listing">
  <c:if test="${not empty formError}"><div class="msg error" role="alert"><c:out value="${formError}"/></div></c:if>
  <div class="form-grid">
    <div class="full">
      <label for="title">Title</label>
      <input id="title" type="text" name="title" maxlength="100" required placeholder="e.g. Casio fx-991ES scientific calculator"
             value="<c:out value='${param.title}'/>">
    </div>
    <div class="full">
      <label for="description">Description <span class="hint">(optional)</span></label>
      <textarea id="description" name="description" maxlength="1000" placeholder="Anything a borrower should know"><c:out value="${param.description}"/></textarea>
    </div>
    <div>
      <label for="category">Category</label>
      <select id="category" name="category" required>
        <option value="">Choose…</option>
        <c:forEach var="c" items="${categories}">
          <option value="<c:out value='${c}'/>" ${param.category == c ? 'selected' : ''}><c:out value="${c}"/></option>
        </c:forEach>
      </select>
    </div>
    <div>
      <label for="listingType">Listing type</label>
      <select id="listingType" name="listingType" required>
        <option value="">Choose…</option>
        <c:forEach var="t" items="${types}">
          <option value="${t.name()}" ${param.listingType == t.name() ? 'selected' : ''}><c:out value="${t.label}"/></option>
        </c:forEach>
      </select>
    </div>
    <div>
      <label for="itemCondition">Condition</label>
      <select id="itemCondition" name="itemCondition" required>
        <option value="">Choose…</option>
        <c:forEach var="cond" items="${conditions}">
          <option value="<c:out value='${cond}'/>" ${param.itemCondition == cond ? 'selected' : ''}><c:out value="${cond}"/></option>
        </c:forEach>
      </select>
    </div>
    <div>
      <label for="location">Handover spot</label>
      <select id="location" name="location" required>
        <option value="">Choose…</option>
        <c:forEach var="loc" items="${locations}">
          <option value="<c:out value='${loc}'/>" ${param.location == loc ? 'selected' : ''}><c:out value="${loc}"/></option>
        </c:forEach>
      </select>
    </div>
    <div id="durationBox" class="full ${param.listingType == 'BORROW' ? '' : 'hidden'}">
      <label for="suggestedDays">Suggested borrow duration</label>
      <select id="suggestedDays" name="suggestedDays">
        <c:forEach var="d" items="${borrowDays}">
          <option value="${d}" ${(empty param.suggestedDays ? 7 : param.suggestedDays) == d ? 'selected' : ''}>${d}${d == 1 ? ' day' : ' days'}</option>
        </c:forEach>
      </select>
      <div class="hint">You can still choose a different due date when you accept a request.</div>
    </div>
  </div>
  <div class="actions" style="margin-top:20px">
    <button type="submit" class="btn btn-auto">Publish listing</button>
    <a class="btn-sm btn-outline" href="${ctx}/dashboard">Cancel</a>
  </div>
</form>

<script>
  // Show the duration picker only for BORROW listings (the server ignores it for other types).
  var typeSelect = document.getElementById('listingType');
  var durationBox = document.getElementById('durationBox');
  typeSelect.addEventListener('change', function () {
    durationBox.classList.toggle('hidden', typeSelect.value !== 'BORROW');
  });
</script>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
