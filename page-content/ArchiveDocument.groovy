{{groovy}}
// ── AUTH CHECK ───────────────────────────────────────────────────────
def currentUser = xcontext.userReference
if (currentUser == null || currentUser.toString().contains('XWikiGuest')) {
  response.sendRedirect(request.contextPath + '/bin/login/XWiki/XWikiLogin')
  return
}

// ── CSRF CHECK ───────────────────────────────────────────────────────
if (!services.csrf.isTokenValid(request.getParameter('form_token'))) {
  response.sendError(403, 'Invalid CSRF token')
  return
}

// ── GROUP CHECK (QFQualityManagers only) ─────────────────────────────
def currentUserStr = currentUser.toString()
def isQM = xwiki.getUser(currentUserStr).isUserInGroup('XWiki.QFQualityManagers')
if (!isQM) {
  def returnUrl = request.getParameter('returnUrl') ?: '/bin/view/QualityFlow/'
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Not authorized. Only Quality Managers can archive documents.', 'UTF-8'))
  return
}

// ── READ PARAMETERS ──────────────────────────────────────────────────
def holderRef = request.getParameter('holderRef') ?: ''
def returnUrl = request.getParameter('returnUrl') ?: '/bin/view/QualityFlow/CommentDashboard'

if (holderRef.trim().isEmpty()) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Holder page reference is required.', 'UTF-8'))
  return
}

// ── LOAD HOLDER PAGE ─────────────────────────────────────────────────
def holderDoc = xwiki.getDocument(holderRef)
if (holderDoc.isNew()) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Holder page not found.', 'UTF-8'))
  return
}

// ── ARCHIVE: Copy XObjects to archive page, then remove from holder ──
def archiveRef = holderRef.replace('QualityFlow.Comments.', 'QualityFlow.Archive.')
def archiveDoc = xwiki.getDocument(archiveRef)
if (archiveDoc.isNew()) {
  archiveDoc.setTitle("Archived: ${holderDoc.getTitle()}")
  archiveDoc.setContent("Archived comments. Original holder: ${holderRef}")
}

// Copy CommentClass XObjects
def comments = holderDoc.getObjects('QualityFlow.CommentClass') ?: []
comments.each { c ->
  def newObj = archiveDoc.newObject('QualityFlow.CommentClass')
  newObj.set('type', c.getValue('type') ?: '')
  newObj.set('status', c.getValue('status') ?: '')
  newObj.set('section', c.getValue('section') ?: '')
  newObj.set('anchorText', c.getValue('anchorText') ?: '')
  newObj.set('anchorOffset', c.getValue('anchorOffset') ?: 0)
  newObj.set('comment', c.getValue('comment') ?: '')
  newObj.set('author', c.getValue('author') ?: '')
  newObj.set('response', c.getValue('response') ?: '')
  newObj.set('date', c.getValue('date'))
  newObj.set('document', c.getValue('document') ?: '')
  newObj.set('resolvedBy', c.getValue('resolvedBy') ?: '')
  newObj.set('resolvedDate', c.getValue('resolvedDate'))
}

// Copy ReviewerClass XObjects
def reviewers = holderDoc.getObjects('QualityFlow.ReviewerClass') ?: []
reviewers.each { r ->
  def newObj = archiveDoc.newObject('QualityFlow.ReviewerClass')
  newObj.set('reviewer', r.getValue('reviewer') ?: '')
  newObj.set('status', r.getValue('status') ?: '')
  newObj.set('document', r.getValue('document') ?: '')
  newObj.set('assignedDate', r.getValue('assignedDate'))
}

// Copy ApprovalClass XObjects
def approvals = holderDoc.getObjects('QualityFlow.ApprovalClass') ?: []
approvals.each { a ->
  def newObj = archiveDoc.newObject('QualityFlow.ApprovalClass')
  newObj.set('decision', a.getValue('decision') ?: '')
  newObj.set('author', a.getValue('author') ?: '')
  newObj.set('date', a.getValue('date'))
  newObj.set('reason', a.getValue('reason') ?: '')
  newObj.set('document', a.getValue('document') ?: '')
}

try {
  // Save the archive page
  archiveDoc.save("Archived from ${holderRef} by ${currentUser.name}")

  // Remove all XObjects from the original holder page
  comments.each { holderDoc.removeObject(it) }
  reviewers.each { holderDoc.removeObject(it) }
  approvals.each { holderDoc.removeObject(it) }
  holderDoc.setContent("This document has been archived. See ${archiveRef} for historical data.")
  holderDoc.save("Document archived by ${currentUser.name}")
} catch (Exception e) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Archive failed: ' + e.getMessage(), 'UTF-8'))
  return
}

response.sendRedirect(returnUrl + '?qf_success=1&qf_info=' +
  java.net.URLEncoder.encode('Document archived successfully.', 'UTF-8'))
{{/groovy}}
