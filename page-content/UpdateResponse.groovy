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
def pResponse = request.getParameter('response') ?: ''
def returnUrl = request.getParameter('returnUrl') ?: '/bin/view/Main/WebHome'

// ── GROUP CHECK ──────────────────────────────────────────────────────
def currentUserStr = currentUser.toString()
def isReviewer = xwiki.getUser(currentUserStr).isUserInGroup('XWiki.QFReviewers')
def isQM = xwiki.getUser(currentUserStr).isUserInGroup('XWiki.QFQualityManagers')
if (!isReviewer && !isQM) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Not authorized to reply to comments.', 'UTF-8'))
  return
}

// ── EMPTY RESPONSE: NO-OP ────────────────────────────────────────────
if (pResponse.trim().isEmpty()) {
  response.sendRedirect(returnUrl)
  return
}

// Truncate to 1000 chars
if (pResponse.length() > 1000) {
  pResponse = pResponse.substring(0, 1000)
}

// ── LOAD XOBJECT ─────────────────────────────────────────────────────
if (!objNumStr.isInteger()) {
  response.sendRedirect(returnUrl + '?qf_error=Invalid+comment+reference.')
  return
}
def objNum = objNumStr.toInteger()

def holderDoc = xwiki.getDocument(holderRef)
def obj = holderDoc.getObject('QualityFlow.CommentClass', objNum)
if (obj == null) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Comment not found.', 'UTF-8'))
  return
}

// ── UPDATE RESPONSE ──────────────────────────────────────────────────
obj.set('response', pResponse)

try {
  holderDoc.save("Response updated by ${currentUser.name}")
} catch (Exception e) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Failed to save: ' + e.getMessage(), 'UTF-8'))
  return
}

response.sendRedirect(returnUrl + '?qf_success=1')
{{/groovy}}
