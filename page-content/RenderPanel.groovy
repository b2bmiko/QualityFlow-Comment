{{groovy}}
def resp = response
resp.setContentType('text/html; charset=UTF-8')
def writer = resp.getWriter()

def currentDocRef = request.getParameter('docRef') ?: ''
def holderName = currentDocRef.replaceAll('[^a-zA-Z0-9]', '')
def holderRef = "QualityFlow.Comments." + holderName
def holderDoc = xwiki.getDocument(holderRef)
def returnUrl = request.getParameter('returnUrl') ?: '/bin/view/Main/'
def saveUrl = xwiki.getURL('QualityFlow.SaveComment', 'view')
def resolveUrl = xwiki.getURL('QualityFlow.ResolveComment', 'view')
def responseUrl = xwiki.getURL('QualityFlow.UpdateResponse', 'view')
def approvalUrl = xwiki.getURL('QualityFlow.SaveApproval', 'view')
def reviewerUrl = xwiki.getURL('QualityFlow.SaveReviewer', 'view')
def markDoneUrl = xwiki.getURL('QualityFlow.MarkReviewerDone', 'view')
def csrfToken = services.csrf.getToken()

def currentUser = xcontext.userReference?.toString() ?: ''
def isGuest = currentUser.contains('XWikiGuest') || currentUser == ''
def isReviewer = false
def isQM = false
def isAPO = false
if (!isGuest) {
  try { isReviewer = xwiki.getUser(currentUser).isUserInGroup('XWiki.QFReviewers') } catch(e){}
  try { isQM = xwiki.getUser(currentUser).isUserInGroup('XWiki.QFQualityManagers') } catch(e){}
  try { isAPO = xwiki.getUser(currentUser).isUserInGroup('XWiki.QFAPO') } catch(e){}
}
def canAct = !isGuest && (isReviewer || isQM)

def allComments = holderDoc.getObjects('QualityFlow.CommentClass') ?: []
def openComments = allComments.findAll { it.getValue('status') == 'Open' }
def resolvedComments = allComments.findAll { it.getValue('status') == 'Resolved' }
def openBlockers = openComments.count { it.getValue('type') == 'Blocker' }

def allReviewers = holderDoc.getObjects('QualityFlow.ReviewerClass') ?: []
def doneReviewers = allReviewers.findAll { it.getValue('status') == 'Done' }
def pendingReviewers = allReviewers.findAll { it.getValue('status') == 'Pending' }

def allApprovals = holderDoc.getObjects('QualityFlow.ApprovalClass') ?: []
def latestApproval = allApprovals ? allApprovals.last() : null

def totalComments = allComments.size()
def openCount = openComments.size()
def resolvedCount = resolvedComments.size()
def progressPct = totalComments > 0 ? (int)(resolvedCount * 100 / totalComments) : 0
def isGateLocked = (openBlockers > 0 || pendingReviewers.size() > 0)

// Helper: time ago
def timeAgo = { date ->
  if (!date) return ''
  def diffMs = System.currentTimeMillis() - date.getTime()
  def diffMin = (int)(diffMs / 60000)
  if (diffMin < 1) return 'just now'
  if (diffMin < 60) return "${diffMin}m ago"
  if (diffMin < 1440) return "${(int)(diffMin/60)}h ago"
  return "${(int)(diffMin/1440)}d ago"
}

// Helper: XSS escape
def esc = { str -> str?.replace('&','&amp;')?.replace('<','&lt;')?.replace('>','&gt;')?.replace('"','&quot;')?.replace("'",'&#39;') ?: '' }

// ── OPEN COMMENT CARDS ───────────────────────────────────────────────
def openCardsHtml = new StringBuilder()
if (openComments.isEmpty()) {
  openCardsHtml.append('<div style="text-align:center;color:#6c757d;font-size:12px;padding:20px 10px;">No comments yet. Select text or click Add Comment to get started.</div>')
} else {
  openComments.each { c ->
    def objNum = c.getNumber()
    def authorName = esc(xwiki.getUserName(c.getValue('author'), false) ?: 'Unknown')
    def initial = authorName.substring(0,1).toUpperCase()
    def cType = esc(c.getValue('type') ?: '')
    def cText = esc(c.getValue('comment') ?: '')
    def cResponse = esc(c.getValue('response') ?: '')
    def cSection = esc(c.getValue('section') ?: '')
    def cTimeAgo = timeAgo(c.getValue('date'))
    def badgeColor = cType == 'Blocker' ? 'background:#f8d7da;color:#842029' : (cType == 'Suggestion' ? 'background:#cfe2ff;color:#084298' : 'background:#fff3cd;color:#664d03')

    openCardsHtml.append("<div data-qf-card=\"${objNum}\" style=\"background:#fff;border:1px solid #dee2e6;border-radius:6px;padding:10px;margin-bottom:8px;\">")
    openCardsHtml.append("<div style=\"display:flex;align-items:center;gap:6px;margin-bottom:4px;\"><span style=\"display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:50%;background:#6c757d;color:#fff;font-size:11px;font-weight:700;\">${initial}</span><span style=\"font-size:12px;font-weight:600;flex:1;\">${authorName}</span><span style=\"display:inline-block;padding:2px 7px;border-radius:3px;font-size:10px;font-weight:700;${badgeColor}\">${cType}</span></div>")

    // Show anchored text indicator (like Word margin comments)
    def anchorText = esc(c.getValue('anchorText') ?: '')
    if (anchorText) {
      def truncAnchor = anchorText.length() > 60 ? anchorText.substring(0, 60) + '...' : anchorText
      openCardsHtml.append("<div style=\"background:#fffde6;border-left:3px solid #ffc107;padding:4px 8px;margin-bottom:6px;border-radius:0 4px 4px 0;font-size:11px;color:#664d03;cursor:pointer;\" onclick=\"qfScrollToMark(${objNum})\" title=\"Click to scroll to highlighted text\">&#128205; <em>&ldquo;${truncAnchor}&rdquo;</em></div>")
    }
    if (cSection) { openCardsHtml.append("<div style=\"font-size:11px;color:#6c757d;margin-bottom:2px;\">&#128205; ${cSection}</div>") }
    if (cTimeAgo) { openCardsHtml.append("<div style=\"font-size:11px;color:#adb5bd;margin-bottom:4px;\">${cTimeAgo}</div>") }
    openCardsHtml.append("<div style=\"font-size:12px;line-height:1.5;\">${cText}</div>")
    if (cResponse) { openCardsHtml.append("<div style=\"margin-top:6px;padding:6px;background:#e8f4e8;border-radius:4px;font-size:12px;color:#155724;\"><strong>Response:</strong> ${cResponse}</div>") }
    if (canAct) {
      openCardsHtml.append("<div style=\"margin-top:8px;display:flex;gap:6px;\">")
      openCardsHtml.append("<form method=\"post\" action=\"${resolveUrl}\" style=\"display:inline;\"><input type=\"hidden\" name=\"form_token\" value=\"${csrfToken}\"/><input type=\"hidden\" name=\"holderRef\" value=\"${holderRef}\"/><input type=\"hidden\" name=\"objNum\" value=\"${objNum}\"/><input type=\"hidden\" name=\"returnUrl\" value=\"${returnUrl}\"/><button type=\"submit\" style=\"padding:4px 10px;border-radius:4px;border:1px solid #a3cfbb;background:#d1e7dd;color:#0f5132;cursor:pointer;font-size:12px;\">&#10003; Resolve</button></form>")
      openCardsHtml.append("<button onclick=\"var el=document.getElementById('qf-reply-${objNum}');el.style.display=el.style.display=='none'?'block':'none'\" style=\"padding:4px 10px;border-radius:4px;border:1px solid #ffda6a;background:#fff3cd;color:#664d03;cursor:pointer;font-size:12px;\">&#8617; Reply</button>")
      openCardsHtml.append("</div>")
      openCardsHtml.append("<div id=\"qf-reply-${objNum}\" style=\"display:none;margin-top:8px;padding-top:8px;border-top:1px solid #e9ecef;\"><form method=\"post\" action=\"${responseUrl}\"><input type=\"hidden\" name=\"form_token\" value=\"${csrfToken}\"/><input type=\"hidden\" name=\"holderRef\" value=\"${holderRef}\"/><input type=\"hidden\" name=\"objNum\" value=\"${objNum}\"/><input type=\"hidden\" name=\"returnUrl\" value=\"${returnUrl}\"/><textarea name=\"response\" rows=\"2\" placeholder=\"Add a response...\" style=\"width:100%;padding:4px 8px;border:1px solid #dee2e6;border-radius:4px;font-size:12px;box-sizing:border-box;margin-bottom:4px;\"></textarea><button type=\"submit\" style=\"background:#0d6efd;color:#fff;border:none;padding:4px 10px;border-radius:4px;cursor:pointer;font-size:12px;\">Save</button></form></div>")
    }
    openCardsHtml.append("</div>")
  }
}

// ── RESOLVED COMMENT CARDS ───────────────────────────────────────────
def resolvedCardsHtml = new StringBuilder()
if (resolvedComments.isEmpty()) {
  resolvedCardsHtml.append('<div style="text-align:center;color:#6c757d;font-size:12px;padding:20px 10px;">No resolved comments yet.</div>')
} else {
  resolvedComments.each { c ->
    def objNum = c.getNumber()
    def authorName = esc(xwiki.getUserName(c.getValue('author'), false) ?: 'Unknown')
    def initial = authorName.substring(0,1).toUpperCase()
    def cText = esc(c.getValue('comment') ?: '')
    def cResponse = esc(c.getValue('response') ?: '')
    def resolvedBy = esc(xwiki.getUserName(c.getValue('resolvedBy'), false) ?: '')
    def rDate = c.getValue('resolvedDate')
    def resolvedDateStr = rDate ? new java.text.SimpleDateFormat('yyyy-MM-dd').format(rDate) : ''

    resolvedCardsHtml.append("<div data-qf-card=\"${objNum}\" style=\"background:#f8f9fa;border:1px solid #dee2e6;border-radius:6px;padding:10px;margin-bottom:8px;opacity:0.75;\">")
    resolvedCardsHtml.append("<div style=\"display:flex;align-items:center;gap:6px;margin-bottom:4px;\"><span style=\"display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:50%;background:#adb5bd;color:#fff;font-size:11px;font-weight:700;\">${initial}</span><span style=\"font-size:12px;font-weight:600;flex:1;\">${authorName}</span><span style=\"font-size:11px;color:#28a745;font-weight:600;\">&#10003; Resolved</span></div>")

    // Show anchored text indicator for resolved comments too
    def anchorTextR = esc(c.getValue('anchorText') ?: '')
    if (anchorTextR) {
      def truncAnchorR = anchorTextR.length() > 60 ? anchorTextR.substring(0, 60) + '...' : anchorTextR
      resolvedCardsHtml.append("<div style=\"background:#f0f0f0;border-left:3px solid #adb5bd;padding:4px 8px;margin-bottom:6px;border-radius:0 4px 4px 0;font-size:11px;color:#6c757d;cursor:pointer;\" onclick=\"qfScrollToMark(${objNum})\" title=\"Click to scroll to highlighted text\">&#128205; <em>&ldquo;${truncAnchorR}&rdquo;</em></div>")
    }
    resolvedCardsHtml.append("<div style=\"font-size:12px;line-height:1.5;\">${cText}</div>")
    if (cResponse) { resolvedCardsHtml.append("<div style=\"margin-top:6px;padding:6px;background:#e8f4e8;border-radius:4px;font-size:12px;color:#155724;\"><strong>Response:</strong> ${cResponse}</div>") }
    resolvedCardsHtml.append("<div style=\"font-size:11px;color:#6c757d;margin-top:4px;\">Resolved by ${resolvedBy}${resolvedDateStr ? ' on ' + resolvedDateStr : ''}</div>")
    if (canAct) {
      resolvedCardsHtml.append("<div style=\"margin-top:6px;\"><button onclick=\"var el=document.getElementById('qf-reply-${objNum}');el.style.display=el.style.display=='none'?'block':'none'\" style=\"padding:4px 10px;border-radius:4px;border:1px solid #ffda6a;background:#fff3cd;color:#664d03;cursor:pointer;font-size:12px;\">&#8617; Reply</button></div>")
      resolvedCardsHtml.append("<div id=\"qf-reply-${objNum}\" style=\"display:none;margin-top:8px;padding-top:8px;border-top:1px solid #e9ecef;\"><form method=\"post\" action=\"${responseUrl}\"><input type=\"hidden\" name=\"form_token\" value=\"${csrfToken}\"/><input type=\"hidden\" name=\"holderRef\" value=\"${holderRef}\"/><input type=\"hidden\" name=\"objNum\" value=\"${objNum}\"/><input type=\"hidden\" name=\"returnUrl\" value=\"${returnUrl}\"/><textarea name=\"response\" rows=\"2\" placeholder=\"Add a response...\" style=\"width:100%;padding:4px 8px;border:1px solid #dee2e6;border-radius:4px;font-size:12px;box-sizing:border-box;margin-bottom:4px;\"></textarea><button type=\"submit\" style=\"background:#0d6efd;color:#fff;border:none;padding:4px 10px;border-radius:4px;cursor:pointer;font-size:12px;\">Save</button></form></div>")
    }
    resolvedCardsHtml.append("</div>")
  }
}

// ── APO GATE ─────────────────────────────────────────────────────────
def gateHtml = new StringBuilder()
if (isGateLocked) {
  gateHtml.append("<div style=\"position:fixed;bottom:0;left:0;right:0;z-index:1000;display:flex;align-items:center;justify-content:space-between;padding:10px 20px;font-size:13px;box-shadow:0 -2px 8px rgba(0,0,0,.15);background:#fff3cd;border-top:2px solid #ffc107;color:#664d03;\"><div><span style=\"font-size:16px;\">&#128274;</span><span style=\"margin-left:8px;\">${openBlockers} blocker(s) / ${pendingReviewers.size()} review(s) pending</span></div></div>")
} else if (latestApproval) {
  def decision = latestApproval.getValue('decision')
  def approver = esc(xwiki.getUserName(latestApproval.getValue('author'), false) ?: '')
  if (decision == 'Approved') {
    gateHtml.append("<div style=\"position:fixed;bottom:0;left:0;right:0;z-index:1000;display:flex;align-items:center;padding:10px 20px;font-size:13px;box-shadow:0 -2px 8px rgba(0,0,0,.15);background:#d1e7dd;border-top:2px solid #28a745;color:#0f5132;\"><span style=\"font-size:16px;\">&#9989;</span><span style=\"margin-left:8px;\">Approved by ${approver}</span></div>")
  } else {
    def reason = esc(latestApproval.getValue('reason') ?: '')
    gateHtml.append("<div style=\"position:fixed;bottom:0;left:0;right:0;z-index:1000;display:flex;align-items:center;padding:10px 20px;font-size:13px;box-shadow:0 -2px 8px rgba(0,0,0,.15);background:#f8d7da;border-top:2px solid #dc3545;color:#842029;\"><span style=\"font-size:16px;\">&#10060;</span><span style=\"margin-left:8px;\">Rejected by ${approver} &mdash; ${reason}</span></div>")
  }
} else {
  gateHtml.append("<div style=\"position:fixed;bottom:0;left:0;right:0;z-index:1000;display:flex;align-items:center;justify-content:space-between;padding:10px 20px;font-size:13px;box-shadow:0 -2px 8px rgba(0,0,0,.15);background:#d1e7dd;border-top:2px solid #28a745;color:#0f5132;\"><div><span style=\"font-size:16px;\">&#128275;</span><span style=\"margin-left:8px;\">All blockers resolved. All reviewers complete. Ready for approval.</span></div></div>")
}

// ── REVIEWER LIST ────────────────────────────────────────────────────
def reviewerListHtml = new StringBuilder()
reviewerListHtml.append("<div style=\"border-bottom:1px solid #dee2e6;padding-bottom:8px;margin-bottom:8px;\">")
allReviewers.each { r ->
  def rObjNum = r.getNumber()
  def rName = esc(xwiki.getUserName(r.getValue('reviewer'), false) ?: 'Unknown')
  def rInitial = rName.length() > 1 ? rName.substring(0,2).toUpperCase() : rName.substring(0,1).toUpperCase()
  def rStatus = r.getValue('status') ?: 'Pending'
  def statusColor = rStatus == 'Done' ? 'background:#d1e7dd;color:#0f5132' : 'background:#fff3cd;color:#664d03'
  def avatarColor = rStatus == 'Done' ? 'background:#198754' : 'background:#fd7e14'
  reviewerListHtml.append("<div style=\"display:flex;align-items:center;gap:8px;padding:6px 0;\"><span style=\"display:inline-flex;align-items:center;justify-content:center;width:32px;height:32px;border-radius:50%;${avatarColor};color:#fff;font-size:11px;font-weight:700;\">${rInitial}</span><span style=\"flex:1;font-size:12px;font-weight:500;\">${rName}</span>")
  if (rStatus == 'Done') {
    reviewerListHtml.append("<span style=\"font-size:11px;padding:3px 8px;border-radius:10px;${statusColor}\">&#10003; Done</span>")
  } else if (isQM) {
    reviewerListHtml.append("<form method=\"post\" action=\"${markDoneUrl}\" style=\"display:inline;margin:0;\"><input type=\"hidden\" name=\"form_token\" value=\"${csrfToken}\"/><input type=\"hidden\" name=\"holderRef\" value=\"${holderRef}\"/><input type=\"hidden\" name=\"objNum\" value=\"${rObjNum}\"/><input type=\"hidden\" name=\"returnUrl\" value=\"${returnUrl}\"/><button type=\"submit\" style=\"font-size:11px;padding:3px 8px;border-radius:10px;background:#fff3cd;color:#664d03;border:1px solid #ffda6a;cursor:pointer;\">Pending &#8594;</button></form>")
  } else {
    reviewerListHtml.append("<span style=\"font-size:11px;padding:3px 8px;border-radius:10px;${statusColor}\">Pending</span>")
  }
  reviewerListHtml.append("</div>")
}
if (isQM) {
  def reviewerGroupDoc = xwiki.getDocument('XWiki.QFReviewers')
  def groupMembers = reviewerGroupDoc.getObjects('XWiki.XWikiGroups') ?: []
  def memberOptions = new StringBuilder()
  memberOptions.append("<option value=\"\">Select a reviewer</option>")
  groupMembers.each { m ->
    def memberRef = m.getValue('member') ?: ''
    if (memberRef) {
      def memberName = esc(xwiki.getUserName(memberRef, false) ?: memberRef)
      memberOptions.append("<option value=\"${esc(memberRef)}\">${memberName}</option>")
    }
  }
  reviewerListHtml.append("<div style=\"margin-top:6px;\"><button onclick=\"var el=document.getElementById('qf-add-reviewer-form');el.style.display=el.style.display=='none'?'block':'none'\" style=\"width:100%;padding:6px;border:2px dashed #0d6efd;border-radius:4px;background:none;color:#0d6efd;cursor:pointer;font-size:12px;\">+ Add Reviewer</button></div>")
  reviewerListHtml.append("<div id=\"qf-add-reviewer-form\" style=\"display:none;margin-top:8px;padding:8px;background:#fff;border:1px solid #dee2e6;border-radius:4px;\"><form method=\"post\" action=\"${reviewerUrl}\"><input type=\"hidden\" name=\"form_token\" value=\"${csrfToken}\"/><input type=\"hidden\" name=\"holderRef\" value=\"${holderRef}\"/><input type=\"hidden\" name=\"docRef\" value=\"${currentDocRef}\"/><input type=\"hidden\" name=\"returnUrl\" value=\"${returnUrl}\"/><div style=\"margin-bottom:6px;\"><label style=\"display:block;font-size:11px;font-weight:600;margin-bottom:2px;\">Select reviewer</label><select name=\"reviewer\" required style=\"width:100%;padding:4px 8px;border:1px solid #dee2e6;border-radius:4px;font-size:12px;box-sizing:border-box;\">${memberOptions}</select></div><button type=\"submit\" style=\"background:#0d6efd;color:#fff;border:none;padding:4px 10px;border-radius:4px;cursor:pointer;font-size:12px;\">Assign</button></form></div>")
}
reviewerListHtml.append("</div>")

// ── FULL OUTPUT ──────────────────────────────────────────────────────
def html = new StringBuilder()
html.append("<div style=\"float:right;margin-left:16px;width:320px;background:#f8f9fa;border-left:1px solid #dee2e6;border-radius:4px;padding:12px;max-height:calc(100vh - 120px);overflow-y:auto;font-size:13px;\">")
html.append("<div style=\"padding-bottom:10px;border-bottom:1px solid #dee2e6;margin-bottom:10px;\">")
html.append("<div style=\"font-size:12px;color:#6c757d;margin-bottom:4px;\">Reviewers: ${doneReviewers.size()} / ${allReviewers.size()} Done</div>")
html.append("<div style=\"background:#e9ecef;border-radius:4px;height:6px;margin-bottom:6px;\"><div style=\"background:#28a745;height:6px;border-radius:4px;width:${progressPct}%;\"></div></div>")
html.append("<div style=\"font-size:12px;color:#6c757d;margin-bottom:4px;\">Comments: ${resolvedCount} / ${totalComments} Resolved</div>")
html.append("</div>")
html.append(reviewerListHtml)
html.append("<div style=\"display:flex;border-bottom:2px solid #dee2e6;margin-bottom:10px;\">")
html.append("<button onclick=\"document.getElementById('qf-open').style.display='block';document.getElementById('qf-resolved').style.display='none';\" style=\"background:none;border:none;padding:6px 12px;cursor:pointer;font-size:12px;color:#0d6efd;border-bottom:2px solid #0d6efd;margin-bottom:-2px;font-weight:600;\">Open <span style=\"background:#e9ecef;border-radius:10px;padding:1px 6px;font-size:10px;margin-left:4px;\">${openCount}</span></button>")
html.append("<button onclick=\"document.getElementById('qf-open').style.display='none';document.getElementById('qf-resolved').style.display='block';\" style=\"background:none;border:none;padding:6px 12px;cursor:pointer;font-size:12px;color:#6c757d;border-bottom:2px solid transparent;margin-bottom:-2px;\">Resolved <span style=\"background:#e9ecef;border-radius:10px;padding:1px 6px;font-size:10px;margin-left:4px;\">${resolvedCount}</span></button>")
html.append("<button onclick=\"document.getElementById('qf-open').style.display='block';document.getElementById('qf-resolved').style.display='block';\" style=\"background:none;border:none;padding:6px 12px;cursor:pointer;font-size:12px;color:#6c757d;border-bottom:2px solid transparent;margin-bottom:-2px;\">All <span style=\"background:#e9ecef;border-radius:10px;padding:1px 6px;font-size:10px;margin-left:4px;\">${totalComments}</span></button>")
html.append("</div>")

if (!isGuest) {
  html.append("<button onclick=\"document.getElementById('qf-add-comment-form').style.display='block'\" style=\"background:#0d6efd;color:#fff;border:none;padding:8px;border-radius:4px;cursor:pointer;width:100%;text-align:center;margin-bottom:10px;font-size:13px;\">+ Add Comment</button>")
  html.append("<div id=\"qf-add-comment-form\" style=\"display:none;background:#fff;border:1px solid #dee2e6;border-radius:6px;padding:12px;margin-bottom:10px;\"><form method=\"post\" action=\"${saveUrl}\"><input type=\"hidden\" name=\"form_token\" value=\"${csrfToken}\"/><input type=\"hidden\" name=\"docRef\" value=\"${currentDocRef}\"/><input type=\"hidden\" name=\"holderRef\" value=\"${holderRef}\"/><input type=\"hidden\" name=\"returnUrl\" value=\"${returnUrl}\"/><input type=\"hidden\" name=\"anchorText\" value=\"\"/><div style=\"margin-bottom:10px;\"><label style=\"display:block;font-size:12px;font-weight:600;margin-bottom:3px;\">Type <span style=\"color:#dc3545;\">*</span></label><select name=\"type\" required style=\"width:100%;padding:5px 8px;border:1px solid #dee2e6;border-radius:4px;font-size:12px;box-sizing:border-box;\"><option value=\"\">Select</option><option value=\"Blocker\">Blocker</option><option value=\"Suggestion\">Suggestion</option><option value=\"Question\">Question</option></select></div><div style=\"margin-bottom:10px;\"><label style=\"display:block;font-size:12px;font-weight:600;margin-bottom:3px;\">Section</label><input type=\"text\" name=\"section\" maxlength=\"255\" style=\"width:100%;padding:5px 8px;border:1px solid #dee2e6;border-radius:4px;font-size:12px;box-sizing:border-box;\"/></div><div style=\"margin-bottom:10px;\"><label style=\"display:block;font-size:12px;font-weight:600;margin-bottom:3px;\">Comment <span style=\"color:#dc3545;\">*</span></label><textarea name=\"comment\" maxlength=\"4000\" rows=\"4\" required style=\"width:100%;padding:5px 8px;border:1px solid #dee2e6;border-radius:4px;font-size:12px;box-sizing:border-box;\"></textarea></div><div style=\"display:flex;gap:8px;margin-top:8px;\"><button type=\"submit\" style=\"background:#0d6efd;color:#fff;border:none;padding:6px 12px;border-radius:4px;cursor:pointer;font-size:12px;\">Submit</button><button type=\"button\" onclick=\"document.getElementById('qf-add-comment-form').style.display='none'\" style=\"padding:6px 12px;border-radius:4px;border:1px solid #dee2e6;background:#fff;cursor:pointer;font-size:12px;\">Cancel</button></div></form></div>")
}

html.append("<div id=\"qf-open\">${openCardsHtml}</div>")
html.append("<div id=\"qf-resolved\" style=\"display:none;\">${resolvedCardsHtml}</div>")
html.append("</div>")
html.append(gateHtml)

writer.print(html.toString())
writer.flush()
xcontext.setFinished(true)
{{/groovy}}
