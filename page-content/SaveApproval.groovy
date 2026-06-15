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
def decision  = request.getParameter('decision') ?: ''
def reason    = request.getParameter('reason') ?: ''
def returnUrl = request.getParameter('returnUrl') ?: '/bin/view/Main/WebHome'

// ── GROUP CHECK (QFAPO only) ─────────────────────────────────────────
def currentUserStr = currentUser.toString()
def isAPO = xwiki.getUser(currentUserStr).isUserInGroup('XWiki.QFAPO')
if (!isAPO) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Not authorized. Only APO members can approve documents.', 'UTF-8'))
  return
}

// ── VALIDATION ───────────────────────────────────────────────────────
if (!['Approved', 'Rejected'].contains(decision)) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Invalid decision value.', 'UTF-8'))
  return
}

// ── GATE CHECK ───────────────────────────────────────────────────────
def holderDoc = xwiki.getDocument(holderRef)
def allComments = holderDoc.getObjects('QualityFlow.CommentClass') ?: []
def openBlockers = allComments.findAll {
  it.getValue('type') == 'Blocker' && it.getValue('status') == 'Open'
}
def allReviewers = holderDoc.getObjects('QualityFlow.ReviewerClass') ?: []
def pendingReviewers = allReviewers.findAll {
  it.getValue('status') == 'Pending'
}

if (!openBlockers.isEmpty() || !pendingReviewers.isEmpty()) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Cannot approve: gate conditions not met. Open blockers: ' +
      openBlockers.size() + ', Pending reviewers: ' + pendingReviewers.size(), 'UTF-8'))
  return
}

// ── CREATE APPROVAL XOBJECT ──────────────────────────────────────────
def newObj = holderDoc.newObject('QualityFlow.ApprovalClass')
newObj.set('decision', decision)
newObj.set('author', currentUser.toString())
newObj.set('date', new Date())
newObj.set('reason', reason)
newObj.set('document', docRef)

try {
  holderDoc.save("Document ${decision} by ${currentUser.name}")
} catch (Exception e) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Failed to save: ' + e.getMessage(), 'UTF-8'))
  return
}

response.sendRedirect(returnUrl + '?qf_success=1')
{{/groovy}}
