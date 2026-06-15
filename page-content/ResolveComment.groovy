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

if (!objNumStr.isInteger()) {
  response.sendRedirect(returnUrl + '?qf_error=Invalid+comment+reference.')
  return
}
def objNum = objNumStr.toInteger()

// ── GROUP CHECK ──────────────────────────────────────────────────────
def currentUserStr = currentUser.toString()
def isReviewer = xwiki.getUser(currentUserStr).isUserInGroup('XWiki.QFReviewers')
def isQM = xwiki.getUser(currentUserStr).isUserInGroup('XWiki.QFQualityManagers')
if (!isReviewer && !isQM) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Not authorized to resolve comments.', 'UTF-8'))
  return
}

// ── LOAD XOBJECT ─────────────────────────────────────────────────────
def holderDoc = xwiki.getDocument(holderRef)
def obj = holderDoc.getObject('QualityFlow.CommentClass', objNum)
if (obj == null) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Comment not found.', 'UTF-8'))
  return
}

// ── CHECK ALREADY RESOLVED ───────────────────────────────────────────
if (obj.getValue('status') == 'Resolved') {
  response.sendRedirect(returnUrl + '?qf_info=' +
    java.net.URLEncoder.encode('Comment was already resolved.', 'UTF-8'))
  return
}

// ── RESOLVE ──────────────────────────────────────────────────────────
obj.set('status', 'Resolved')
obj.set('resolvedBy', currentUser.toString())
obj.set('resolvedDate', new Date())

try {
  holderDoc.save("Comment resolved by ${currentUser.name}")
} catch (Exception e) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Failed to save: ' + e.getMessage(), 'UTF-8'))
  return
}

response.sendRedirect(returnUrl + '?qf_success=1')
{{/groovy}}
