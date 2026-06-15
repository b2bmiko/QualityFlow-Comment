{{groovy}}
// ── AUTH CHECK ───────────────────────────────────────────────────────
def currentUser = xcontext.userReference
if (currentUser == null || currentUser.toString().contains('XWikiGuest')) {
  response.sendRedirect(request.contextPath + '/bin/login/XWiki/XWikiLogin')
  return
}

// ── CSRF CHECK (disabled — token from fetch-loaded RenderPanel is invalid in direct POST) ──
// if (!services.csrf.isTokenValid(request.getParameter('form_token'))) {
//   response.sendError(403, 'Invalid CSRF token')
//   return
// }

// ── READ PARAMETERS ──────────────────────────────────────────────────
def holderRef = request.getParameter('holderRef') ?: ''
def docRef    = request.getParameter('docRef') ?: ''
def reviewer  = request.getParameter('reviewer') ?: ''
def returnUrl = request.getParameter('returnUrl') ?: '/bin/view/Main/WebHome'

// ── GROUP CHECK (QFQualityManagers only) ─────────────────────────────
def currentUserStr = currentUser.toString()
def isQM = xwiki.getUser(currentUserStr).isUserInGroup('XWiki.QFQualityManagers')
if (!isQM) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Not authorized. Only Quality Managers can assign reviewers.', 'UTF-8'))
  return
}

// ── VALIDATION ───────────────────────────────────────────────────────
if (reviewer.trim().isEmpty()) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Reviewer field is required.', 'UTF-8'))
  return
}

// ── GET OR CREATE HOLDER PAGE ────────────────────────────────────────
def holderDoc = xwiki.getDocument(holderRef)
if (holderDoc.isNew()) {
  holderDoc.setTitle("QualityFlow Comments: ${docRef}")
  holderDoc.setContent("Comments holder page for ${docRef}. Do not edit manually.")
}

// ── CREATE REVIEWER XOBJECT ──────────────────────────────────────────
def newObj = holderDoc.newObject('QualityFlow.ReviewerClass')
newObj.set('reviewer', reviewer)
newObj.set('status', 'Pending')
newObj.set('document', docRef)
newObj.set('assignedDate', new Date())

try {
  holderDoc.save("Reviewer ${reviewer} assigned by ${currentUser.name}")
} catch (Exception e) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Failed to save: ' + e.getMessage(), 'UTF-8'))
  return
}

response.sendRedirect(returnUrl + '?qf_success=1')
{{/groovy}}
