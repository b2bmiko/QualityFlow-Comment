{{groovy}}
// ── AUTH CHECK ───────────────────────────────────────────────────────
def currentUser = xcontext.userReference
if (currentUser == null || currentUser.toString().contains('XWikiGuest')) {
  response.sendRedirect(request.contextPath + '/bin/login/XWiki/XWikiLogin')
  return
}

// ── CSRF CHECK ───────────────────────────────────────────────────────
def formToken = request.getParameter('form_token') ?: ''
if (!formToken || !services.csrf.isTokenValid(formToken)) {
  // If no valid token, show a confirmation page with a fresh token
  def freshToken = services.csrf.getToken()
  def returnUrl = request.getParameter('returnUrl') ?: '/bin/view/QualityFlow/CommentDashboard'
  response.setContentType('text/html; charset=UTF-8')
  def w = response.getWriter()
  w.println('<div style="max-width:500px;margin:60px auto;font-family:-apple-system,sans-serif;text-align:center;">')
  w.println('<h2 style="color:#fd7e14;">&#128230; Archive All Comments</h2>')
  w.println('<p style="font-size:14px;color:#495057;">This will move all active comments to the archive. The dashboard will be cleared.</p>')
  w.println('<form method="post" action="' + xwiki.getURL('QualityFlow.ArchiveAll', 'view') + '">')
  w.println('<input type="hidden" name="form_token" value="' + freshToken + '"/>')
  w.println('<input type="hidden" name="returnUrl" value="' + returnUrl + '"/>')
  w.println('<input type="hidden" name="confirmed" value="1"/>')
  w.println('<button type="submit" style="padding:10px 24px;border-radius:6px;background:#fd7e14;color:#fff;border:none;cursor:pointer;font-size:14px;font-weight:600;margin-right:8px;">Yes, Archive All</button>')
  w.println('<a href="' + returnUrl + '" style="padding:10px 24px;border-radius:6px;background:#6c757d;color:#fff;text-decoration:none;font-size:14px;">Cancel</a>')
  w.println('</form></div>')
  w.flush()
  xcontext.setFinished(true)
  return
}

// ── GROUP CHECK (QFQualityManagers only) ─────────────────────────────
def currentUserStr = currentUser.toString()
def isQM = xwiki.getUser(currentUserStr).isUserInGroup('XWiki.QFQualityManagers')
if (!isQM) {
  def returnUrl = request.getParameter('returnUrl') ?: '/bin/view/QualityFlow/CommentDashboard'
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Not authorized. Only Quality Managers can archive documents.', 'UTF-8'))
  return
}

// ── FIND ALL HOLDER PAGES WITH COMMENTS ──────────────────────────────
def returnUrl = request.getParameter('returnUrl') ?: '/bin/view/QualityFlow/CommentDashboard'

def query = services.query.xwql(
  "select distinct doc.fullName from Document doc, doc.object(QualityFlow.CommentClass) as c"
).execute()

if (!query || query.isEmpty()) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('No active comments to archive.', 'UTF-8'))
  return
}

// ── GENERATE CYCLE LABEL (timestamp-based) ───────────────────────────
def cycleLabel = new java.text.SimpleDateFormat('yyyy-MM-dd_HHmm').format(new Date())

def archivedCount = 0
def errors = []

query.each { holderFullName ->
  try {
    def holderDoc = xwiki.getDocument(holderFullName)
    def comments  = holderDoc.getObjects('QualityFlow.CommentClass') ?: []
    def reviewers = holderDoc.getObjects('QualityFlow.ReviewerClass') ?: []
    def approvals = holderDoc.getObjects('QualityFlow.ApprovalClass') ?: []

    if (comments.isEmpty() && reviewers.isEmpty() && approvals.isEmpty()) return

    // Archive page: QualityFlow.Archive.<cycle>.<originalName>
    def shortName = holderFullName.replace('QualityFlow.Comments.', '')
    def archiveRef = "QualityFlow.Archive.${cycleLabel}.${shortName}"
    def archiveDoc = xwiki.getDocument(archiveRef)

    if (archiveDoc.isNew()) {
      archiveDoc.setTitle("Archive ${cycleLabel} — ${shortName}")
      archiveDoc.setContent("Archived from ${holderFullName} on ${cycleLabel}")
    }

    // Copy CommentClass XObjects
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
    reviewers.each { r ->
      def newObj = archiveDoc.newObject('QualityFlow.ReviewerClass')
      newObj.set('reviewer', r.getValue('reviewer') ?: '')
      newObj.set('status', r.getValue('status') ?: '')
      newObj.set('document', r.getValue('document') ?: '')
      newObj.set('assignedDate', r.getValue('assignedDate'))
    }

    // Copy ApprovalClass XObjects
    approvals.each { a ->
      def newObj = archiveDoc.newObject('QualityFlow.ApprovalClass')
      newObj.set('decision', a.getValue('decision') ?: '')
      newObj.set('author', a.getValue('author') ?: '')
      newObj.set('date', a.getValue('date'))
      newObj.set('reason', a.getValue('reason') ?: '')
      newObj.set('document', a.getValue('document') ?: '')
    }

    // Save archive page
    archiveDoc.save("Bulk archive cycle ${cycleLabel} by ${currentUser.name}")

    // Remove all XObjects from the original holder page
    comments.each { holderDoc.removeObject(it) }
    reviewers.each { holderDoc.removeObject(it) }
    approvals.each { holderDoc.removeObject(it) }
    holderDoc.save("Bulk archived to cycle ${cycleLabel} by ${currentUser.name}")

    archivedCount++
  } catch (Exception e) {
    errors.add("${holderFullName}: ${e.getMessage()}")
  }
}

if (errors.isEmpty()) {
  response.sendRedirect(returnUrl + '?qf_success=1&qf_info=' +
    java.net.URLEncoder.encode("Archived ${archivedCount} document(s) to cycle ${cycleLabel}.", 'UTF-8'))
} else {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode("Archived ${archivedCount} doc(s) but ${errors.size()} failed: ${errors.join('; ')}", 'UTF-8'))
}
{{/groovy}}
