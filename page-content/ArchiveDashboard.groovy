{{groovy}}
def resp = response
def esc = { str -> str?.replace('&','&amp;')?.replace('<','&lt;')?.replace('>','&gt;')?.replace('"','&quot;')?.replace("'",'&#39;') ?: '' }

// ── EXPORT: Word (.doc) format ───────────────────────────────────────
def exportCycle = request.getParameter('exportCycle') ?: ''
def exportFormat = request.getParameter('exportFormat') ?: ''

if (exportCycle && exportFormat) {
  // Find all archive pages for this cycle
  def cycleQuery = services.query.xwql(
    "select distinct doc.fullName from Document doc where doc.fullName like :pattern"
  ).bindValue('pattern', "QualityFlow.Archive.${exportCycle}.%").execute()

  if (exportFormat == 'word') {
    resp.setContentType('application/msword; charset=UTF-8')
    resp.setHeader('Content-Disposition', "attachment; filename=\"QualityFlow-Archive-${exportCycle}.doc\"")
    def w = resp.getWriter()
    w.println('<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/TR/REC-html40">')
    w.println('<head><meta charset="UTF-8"/><style>body{font-family:Calibri,sans-serif;font-size:11pt;}table{border-collapse:collapse;width:100%;}th,td{border:1px solid #999;padding:6px 8px;font-size:10pt;}th{background:#f0f0f0;font-weight:bold;}.blocker{color:#842029;font-weight:bold;}.resolved{color:#0f5132;}</style></head>')
    w.println('<body>')
    w.println("<h1>QualityFlow Archive — Cycle ${esc(exportCycle)}</h1>")
    w.println("<p>Exported: ${new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm').format(new Date())}</p>")

    cycleQuery.each { archiveFullName ->
      def archiveDoc = xwiki.getDocument(archiveFullName)
      def comments = archiveDoc.getObjects('QualityFlow.CommentClass') ?: []
      def reviewers = archiveDoc.getObjects('QualityFlow.ReviewerClass') ?: []
      def approvals = archiveDoc.getObjects('QualityFlow.ApprovalClass') ?: []
      def docName = archiveFullName.replace("QualityFlow.Archive.${exportCycle}.", '')

      w.println("<h2>${esc(docName)}</h2>")

      if (comments) {
        w.println('<h3>Comments</h3>')
        w.println('<table><tr><th>Type</th><th>Status</th><th>Comment</th><th>Author</th><th>Date</th><th>Response</th><th>Resolved By</th></tr>')
        comments.each { c ->
          def typeClass = c.getValue('type') == 'Blocker' ? ' class="blocker"' : ''
          def statusClass = c.getValue('status') == 'Resolved' ? ' class="resolved"' : ''
          def cDate = c.getValue('date') ? new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm').format(c.getValue('date')) : ''
          def cAuthor = xwiki.getUserName(c.getValue('author'), false) ?: ''
          def resolvedBy = xwiki.getUserName(c.getValue('resolvedBy'), false) ?: ''
          w.println("<tr><td${typeClass}>${esc(c.getValue('type') ?: '')}</td><td${statusClass}>${esc(c.getValue('status') ?: '')}</td><td>${esc(c.getValue('comment') ?: '')}</td><td>${esc(cAuthor)}</td><td>${cDate}</td><td>${esc(c.getValue('response') ?: '')}</td><td>${esc(resolvedBy)}</td></tr>")
        }
        w.println('</table>')
      }

      if (reviewers) {
        w.println('<h3>Reviewers</h3>')
        w.println('<table><tr><th>Reviewer</th><th>Status</th><th>Assigned</th></tr>')
        reviewers.each { r ->
          def rDate = r.getValue('assignedDate') ? new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm').format(r.getValue('assignedDate')) : ''
          def rName = xwiki.getUserName(r.getValue('reviewer'), false) ?: ''
          w.println("<tr><td>${esc(rName)}</td><td>${esc(r.getValue('status') ?: '')}</td><td>${rDate}</td></tr>")
        }
        w.println('</table>')
      }

      if (approvals) {
        w.println('<h3>Approvals</h3>')
        w.println('<table><tr><th>Decision</th><th>APO</th><th>Date</th><th>Comment</th></tr>')
        approvals.each { a ->
          def aDate = a.getValue('date') ? new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm').format(a.getValue('date')) : ''
          def aAuthor = xwiki.getUserName(a.getValue('author'), false) ?: ''
          w.println("<tr><td>${esc(a.getValue('decision') ?: '')}</td><td>${esc(aAuthor)}</td><td>${aDate}</td><td>${esc(a.getValue('reason') ?: '')}</td></tr>")
        }
        w.println('</table>')
      }
    }

    w.println('</body></html>')
    w.flush()
    xcontext.setFinished(true)
    return
  }

  if (exportFormat == 'pdf') {
    // xWiki PDF export via redirect to the archive cycle index page
    // We'll generate an HTML page and use xWiki's built-in PDF export
    resp.setContentType('text/html; charset=UTF-8')
    resp.setHeader('Content-Disposition', "attachment; filename=\"QualityFlow-Archive-${exportCycle}.html\"")
    def w = resp.getWriter()
    w.println('<!DOCTYPE html><html><head><meta charset="UTF-8"/><style>body{font-family:Arial,sans-serif;font-size:10pt;margin:20px;}table{border-collapse:collapse;width:100%;margin-bottom:16px;}th,td{border:1px solid #ccc;padding:5px 8px;font-size:9pt;}th{background:#f5f5f5;}.blocker{color:#842029;font-weight:bold;}.resolved{color:#0f5132;}</style></head><body>')
    w.println("<h1>QualityFlow Archive — Cycle ${esc(exportCycle)}</h1>")
    w.println("<p><em>Exported: ${new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm').format(new Date())}</em></p>")

    cycleQuery.each { archiveFullName ->
      def archiveDoc = xwiki.getDocument(archiveFullName)
      def comments = archiveDoc.getObjects('QualityFlow.CommentClass') ?: []
      def reviewers = archiveDoc.getObjects('QualityFlow.ReviewerClass') ?: []
      def approvals = archiveDoc.getObjects('QualityFlow.ApprovalClass') ?: []
      def docName = archiveFullName.replace("QualityFlow.Archive.${exportCycle}.", '')

      w.println("<h2>${esc(docName)}</h2>")

      if (comments) {
        w.println('<h3>Comments</h3><table><tr><th>Type</th><th>Status</th><th>Comment</th><th>Author</th><th>Date</th><th>Response</th><th>Resolved By</th></tr>')
        comments.each { c ->
          def typeClass = c.getValue('type') == 'Blocker' ? ' class="blocker"' : ''
          def statusClass = c.getValue('status') == 'Resolved' ? ' class="resolved"' : ''
          def cDate = c.getValue('date') ? new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm').format(c.getValue('date')) : ''
          def cAuthor = xwiki.getUserName(c.getValue('author'), false) ?: ''
          def resolvedBy = xwiki.getUserName(c.getValue('resolvedBy'), false) ?: ''
          w.println("<tr><td${typeClass}>${esc(c.getValue('type') ?: '')}</td><td${statusClass}>${esc(c.getValue('status') ?: '')}</td><td>${esc(c.getValue('comment') ?: '')}</td><td>${esc(cAuthor)}</td><td>${cDate}</td><td>${esc(c.getValue('response') ?: '')}</td><td>${esc(resolvedBy)}</td></tr>")
        }
        w.println('</table>')
      }

      if (reviewers) {
        w.println('<h3>Reviewers</h3><table><tr><th>Reviewer</th><th>Status</th><th>Assigned</th></tr>')
        reviewers.each { r ->
          def rDate = r.getValue('assignedDate') ? new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm').format(r.getValue('assignedDate')) : ''
          def rName = xwiki.getUserName(r.getValue('reviewer'), false) ?: ''
          w.println("<tr><td>${esc(rName)}</td><td>${esc(r.getValue('status') ?: '')}</td><td>${rDate}</td></tr>")
        }
        w.println('</table>')
      }

      if (approvals) {
        w.println('<h3>Approvals</h3><table><tr><th>Decision</th><th>APO</th><th>Date</th><th>Comment</th></tr>')
        approvals.each { a ->
          def aDate = a.getValue('date') ? new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm').format(a.getValue('date')) : ''
          def aAuthor = xwiki.getUserName(a.getValue('author'), false) ?: ''
          w.println("<tr><td>${esc(a.getValue('decision') ?: '')}</td><td>${esc(aAuthor)}</td><td>${aDate}</td><td>${esc(a.getValue('reason') ?: '')}</td></tr>")
        }
        w.println('</table>')
      }
    }

    w.println('<p style="margin-top:24px;font-size:9pt;color:#666;">Use your browser\'s Print &rarr; Save as PDF for PDF output.</p>')
    w.println('</body></html>')
    w.flush()
    xcontext.setFinished(true)
    return
  }
}

// ── DASHBOARD VIEW ───────────────────────────────────────────────────
resp.setContentType('text/html; charset=UTF-8')
def writer = resp.getWriter()

// Find all archive cycles (distinct sub-spaces under QualityFlow.Archive)
def allArchives = services.query.xwql(
  "select distinct doc.fullName from Document doc where doc.fullName like 'QualityFlow.Archive.%'"
).execute()

// Group by cycle
def cycles = [:] as LinkedHashMap
allArchives.each { fullName ->
  def parts = fullName.replace('QualityFlow.Archive.', '').split('\\.', 2)
  if (parts.length >= 2) {
    def cycleName = parts[0]
    def docName = parts[1]
    if (!cycles.containsKey(cycleName)) cycles[cycleName] = []
    cycles[cycleName].add([fullName: fullName, docName: docName])
  }
}

def html = new StringBuilder()
html.append('<div style="font-family:-apple-system,sans-serif;font-size:14px;max-width:900px;margin:0 auto;">')
html.append('<a href="/bin/view/QualityFlow/CommentDashboard" style="display:inline-block;margin-bottom:12px;font-size:13px;color:#0d6efd;text-decoration:none;">&larr; Back to Comment Dashboard</a>')
html.append('<h1 style="font-size:22px;margin-bottom:16px;">&#128451; Archive Dashboard</h1>')

if (cycles.isEmpty()) {
  html.append('<div style="padding:20px;text-align:center;color:#6c757d;">No archived cycles found.</div>')
} else {
  html.append('<p style="font-size:13px;color:#6c757d;margin-bottom:20px;">' + cycles.size() + ' archived cycle(s) found.</p>')

  def dashUrl = xwiki.getURL('QualityFlow.ArchiveDashboard', 'view')

  cycles.each { cycleName, docs ->
    // Count total comments in this cycle
    def totalComments = 0
    docs.each { d ->
      def archDoc = xwiki.getDocument(d.fullName)
      totalComments += (archDoc.getObjects('QualityFlow.CommentClass') ?: []).size()
    }

    // Parse date from cycle name for display
    def displayDate = cycleName.replace('_', ' ')

    html.append('<div style="margin-bottom:20px;border:1px solid #dee2e6;border-radius:6px;overflow:hidden;">')
    html.append('<div style="display:flex;justify-content:space-between;align-items:center;background:#f8f9fa;padding:12px 14px;border-bottom:1px solid #dee2e6;">')
    html.append('<div>')
    html.append('<strong style="font-size:14px;">&#128197; Cycle: ' + esc(displayDate) + '</strong>')
    html.append('<span style="margin-left:12px;font-size:12px;color:#6c757d;">' + docs.size() + ' doc(s) &middot; ' + totalComments + ' comment(s)</span>')
    html.append('</div>')
    html.append('<div style="display:flex;gap:6px;">')
    html.append('<a href="' + dashUrl + '?exportCycle=' + java.net.URLEncoder.encode(cycleName, 'UTF-8') + '&exportFormat=word" style="font-size:11px;padding:4px 10px;border-radius:4px;background:#0d6efd;color:#fff;text-decoration:none;">&#128196; Word</a>')
    html.append('<a href="' + dashUrl + '?exportCycle=' + java.net.URLEncoder.encode(cycleName, 'UTF-8') + '&exportFormat=pdf" style="font-size:11px;padding:4px 10px;border-radius:4px;background:#6c757d;color:#fff;text-decoration:none;">&#128196; PDF/HTML</a>')
    html.append('</div>')
    html.append('</div>')

    // List documents in this cycle
    html.append('<table style="width:100%;border-collapse:collapse;font-size:13px;">')
    html.append('<thead><tr style="border-bottom:1px solid #e9ecef;"><th style="padding:6px 12px;text-align:left;">Document</th><th style="padding:6px 12px;text-align:center;">Comments</th><th style="padding:6px 12px;text-align:center;">Reviewers</th><th style="padding:6px 12px;text-align:center;">Approval</th></tr></thead>')
    html.append('<tbody>')
    docs.each { d ->
      def archDoc = xwiki.getDocument(d.fullName)
      def comments = archDoc.getObjects('QualityFlow.CommentClass') ?: []
      def reviewers = archDoc.getObjects('QualityFlow.ReviewerClass') ?: []
      def approvals = archDoc.getObjects('QualityFlow.ApprovalClass') ?: []
      def lastApproval = approvals ? approvals.last() : null
      def decision = lastApproval ? lastApproval.getValue('decision') : '—'
      def decisionStyle = decision == 'Approved' ? 'color:#0f5132;font-weight:600' : (decision == 'Rejected' ? 'color:#842029;font-weight:600' : 'color:#6c757d')

      html.append('<tr style="border-bottom:1px solid #e9ecef;">')
      html.append('<td style="padding:6px 12px;">' + esc(d.docName) + '</td>')
      html.append('<td style="padding:6px 12px;text-align:center;">' + comments.size() + '</td>')
      html.append('<td style="padding:6px 12px;text-align:center;">' + reviewers.size() + '</td>')
      html.append('<td style="padding:6px 12px;text-align:center;' + decisionStyle + '">' + esc(decision) + '</td>')
      html.append('</tr>')
    }
    html.append('</tbody></table>')
    html.append('</div>')
  }
}

html.append('<div style="margin-top:24px;padding:12px;background:#f8f9fa;border-radius:6px;font-size:11px;color:#6c757d;text-align:center;">')
html.append('QualityFlow Comment Summary v1.0.0 &copy; 2026 b2bmike<br/>')
html.append('Licensed under LGPL-2.1')
html.append('</div>')
html.append('</div>')

writer.print(html.toString())
writer.flush()
xcontext.setFinished(true)
{{/groovy}}
