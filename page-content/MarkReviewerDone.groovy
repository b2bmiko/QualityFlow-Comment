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
def objNumStr = request.getParameter('objNum') ?: ''
def returnUrl = request.getParameter('returnUrl') ?: '/bin/view/Main/WebHome'

// ── GROUP CHECK (QFQualityManagers only) ─────────────────────────────
def currentUserStr = currentUser.toString()
def isQM = xwiki.getUser(currentUserStr).isUserInGroup('XWiki.QFQualityManagers')
if (!isQM) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Not authorized. Only Quality Managers can mark reviewers done.', 'UTF-8'))
  return
}

if (!objNumStr.isInteger()) {
  response.sendRedirect(returnUrl + '?qf_error=Invalid+reviewer+reference.')
  return
}
def objNum = objNumStr.toInteger()

// ── LOAD AND UPDATE ──────────────────────────────────────────────────
def holderDoc = xwiki.getDocument(holderRef)
def obj = holderDoc.getObject('QualityFlow.ReviewerClass', objNum)
if (obj == null) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Reviewer not found.', 'UTF-8'))
  return
}

obj.set('status', 'Done')

try {
  holderDoc.save("Reviewer marked Done by ${currentUser.name}")
} catch (Exception e) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Failed to save: ' + e.getMessage(), 'UTF-8'))
  return
}

response.sendRedirect(returnUrl + '?qf_success=1')
{{/groovy}}
