# Technical Design Document
# QualityFlow Comment Summary — Phase 1

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│  xWiki Document Page  (any page with DocumentCommentPanel included) │
│                                                                     │
│  ┌──────────────────────────────────┐  ┌───────────────────────┐   │
│  │   qf-doc-body (70%)              │  │  qf-sidebar (30%)     │   │
│  │                                  │  │                       │   │
│  │  Document content rendered       │  │  Reviewer list        │   │
│  │  normally by xWiki               │  │  Progress bar         │   │
│  │                                  │  │  Open/Resolved/All    │   │
│  │  <mark class="qf-highlight       │  │  tabs                 │   │
│  │    qf-blocker" data-comment-id   │◄─┤                       │   │
│  │    ="123">selected text</mark>   │  │  Comment cards        │   │
│  │                                  │  │  (avatar, badge,      │   │
│  │                                  │  │   text, actions)      │   │
│  │                                  │  │                       │   │
│  │                                  │  │  Add Comment form     │   │
│  └──────────────────────────────────┘  └───────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  APO Gate Bar (position:fixed; bottom:0)                    │   │
│  │  🔒 3 blockers must be resolved — Tom Decker review pending │   │
│  │  [Approve - disabled]  [Reject - disabled]  [Resolve]       │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘

        │ form submit                    │ load CSS/JS
        ▼                                ▼
┌───────────────────┐        ┌─────────────────────┐
│  Groovy Actions   │        │  QFStyles / QFScripts│
│  SaveComment      │        │  (wiki pages loaded  │
│  ResolveComment   │        │   via ssfx/jsfx)     │
│  UpdateResponse   │        └─────────────────────┘
│  SaveApproval     │
│  SaveReviewer     │
└────────┬──────────┘
         │ XObject save
         ▼
┌─────────────────────────────────────┐
│  Holder Page                        │
│  QualityFlow.Comments.<DocRef>      │
│                                     │
│  XObject: QualityFlow.CommentClass  │
│  XObject: QualityFlow.CommentClass  │
│  XObject: QualityFlow.ReviewerClass │
│  XObject: QualityFlow.ApprovalClass │
└─────────────────────────────────────┘
         │ XWQL query
         ▼
┌─────────────────────────────────────┐
│  CommentDashboard                   │
│  (Groovy + Velocity table)          │
└─────────────────────────────────────┘
```

---

## 2. XClass Field Definitions

### 2.1 QualityFlow.CommentClass

| Field | xWiki Class Type | Constraints | Notes |
|---|---|---|---|
| type | StaticListClass | Blocker\|Suggestion\|Question, mandatory, size=20 | No default |
| status | StaticListClass | Open\|Resolved, size=10 | Default: Open |
| section | StringClass | max 255 chars, optional | e.g. "Section 2 — Auth" |
| anchorText | StringClass | max 500 chars, optional | Selected text passage |
| anchorOffset | NumberClass | integer, optional | Char offset in doc body |
| comment | TextAreaClass | max 4000 chars, mandatory | Review comment body |
| author | UsersClass | single user | Auto-filled by SaveComment |
| response | TextAreaClass | max 1000 chars, optional | Reply from Quality_Manager |
| date | DateClass | include time | Auto-filled server-side |
| document | PageClass | single reference | Auto-filled by SaveComment |
| resolvedBy | UsersClass | single user, optional | Set by ResolveComment only |
| resolvedDate | DateClass | include time, optional | Set by ResolveComment only |

### 2.2 QualityFlow.ReviewerClass

| Field | xWiki Class Type | Constraints | Notes |
|---|---|---|---|
| reviewer | UsersClass | single user | Assigned reviewer |
| status | StaticListClass | Pending\|Done, size=10 | Default: Pending |
| document | PageClass | single reference | Auto-filled |
| assignedDate | DateClass | include time | Auto-filled |

### 2.3 QualityFlow.ApprovalClass

| Field | xWiki Class Type | Constraints | Notes |
|---|---|---|---|
| decision | StaticListClass | Approved\|Rejected, size=10 | Mandatory |
| author | UsersClass | single user | Auto-filled (APO user) |
| date | DateClass | include time | Auto-filled |
| reason | TextAreaClass | max 2000 chars, optional | Required on Reject |
| document | PageClass | single reference | Auto-filled |

---

## 3. Page Inventory

| Page | Type | Purpose |
|---|---|---|
| QualityFlow.WebHome | Wiki page | Space home, installation guide |
| QualityFlow.CommentClass | Class page | Defines CommentClass XClass |
| QualityFlow.ReviewerClass | Class page | Defines ReviewerClass XClass |
| QualityFlow.ApprovalClass | Class page | Defines ApprovalClass XClass |
| QualityFlow.CommentTemplate | Template | Template for Holder Pages |
| QualityFlow.DocumentCommentPanel | Velocity macro | Main UI — sidebar + APO gate |
| QualityFlow.AddComment | Wiki page | Standalone add comment form (fallback) |
| QualityFlow.SaveComment | Groovy action | Handles new comment form POST |
| QualityFlow.ResolveComment | Groovy action | Handles resolve action POST |
| QualityFlow.UpdateResponse | Groovy action | Handles reply form POST |
| QualityFlow.AddReviewer | Wiki page | Add reviewer form |
| QualityFlow.SaveReviewer | Groovy action | Handles add reviewer POST |
| QualityFlow.SaveApproval | Groovy action | Handles Approve/Reject POST |
| QualityFlow.CommentDashboard | Wiki page | Cross-document dashboard |
| QualityFlow.QFStyles | CSS page (SSX) | All CSS for layout, sidebar, APO bar |
| QualityFlow.QFScripts | JS page (JSX) | All JavaScript for selection, highlights |
| QualityFlow.Admin | Wiki page | Health check, total XObject count |

---

## 4. Holder Page Naming

For a document at `MySpace.MyDoc`, the Holder Page is:

```
QualityFlow.Comments.MySpaceMyDoc
```

Sanitization in Groovy:
```groovy
def docRef = request.getParameter('docRef')  // e.g. "API Gateway Spec v1.0.WebHome"
def holderName = docRef.replaceAll('[^a-zA-Z0-9]', '')  // "APIGatewaySpecv10WebHome"
def holderFullRef = "QualityFlow.Comments." + holderName
```

---

## 5. Data Flow Diagrams

### 5.1 Add Comment with Text Anchor

```
User selects text in document body
        │
        ▼
JS mouseup listener fires
window.getSelection().toString() → "token expiry is too long"
range.startOffset → 1247
        │
        ▼
Floating popover appears near selection
User clicks "Add Comment"
        │
        ▼
JS populates hidden fields:
  #qf-anchorText = "token expiry is too long"
  #qf-anchorOffset = 1247
Sidebar comment form slides open
"Anchored to: token expiry is too long" shown read-only
        │
User fills: type=Blocker, section="Section 2", comment="Too long for our policy"
User clicks Submit
        │
        ▼
POST to QualityFlow.SaveComment:
  type, section, comment, anchorText, anchorOffset, docRef, returnUrl
        │
        ▼ (SaveComment Groovy)
Auth check → OK
Validation → OK
Get/create Holder Page
newObj = holderDoc.newXObject("QualityFlow.CommentClass")
newObj.set("type", "Blocker")
newObj.set("status", "Open")
newObj.set("section", "Section 2")
newObj.set("anchorText", "token expiry is too long")
newObj.set("anchorOffset", 1247)
newObj.set("comment", "Too long for our policy")
newObj.set("author", currentUser)
newObj.set("date", new Date())
newObj.set("document", docRef)
holderDoc.save("New Blocker comment by " + currentUser)
        │
        ▼
Redirect to returnUrl (original document page)
        │
        ▼
Page reloads → DocumentCommentPanel Velocity renders updated sidebar
JS injects <mark class="qf-highlight qf-blocker" data-comment-id="N">
  token expiry is too long
</mark>
```

### 5.2 Resolve Comment

```
User clicks "Resolve" on comment card (objNum=3)
        │
        ▼
POST to QualityFlow.ResolveComment:
  holderRef, objNum, returnUrl
        │
        ▼ (ResolveComment Groovy)
Auth check → OK
Group check → isAMemberOf(QFReviewers or QFQualityManagers) → OK
holderDoc = xwiki.getDocument(holderRef)
obj = holderDoc.getObject("QualityFlow.CommentClass", objNum)
if obj.status == "Resolved" → redirect with "already resolved" message
obj.set("status", "Resolved")
obj.set("resolvedBy", currentUser)
obj.set("resolvedDate", new Date())
holderDoc.save("Comment resolved by " + currentUser)
        │
        ▼
Redirect to returnUrl
JS re-applies highlights → resolved comment mark gets class qf-resolved (grey)
APO Gate re-evaluates → if 0 open blockers + 0 pending reviewers → unlocks
```

### 5.3 APO Approve

```
APO user clicks "Approve" (only visible if in XWiki.QFAPO group)
        │
        ▼
POST to QualityFlow.SaveApproval:
  holderRef, decision=Approved, reason="", returnUrl
        │
        ▼ (SaveApproval Groovy)
Auth check → OK
Group check → isAMemberOf(QFAPO) → OK
Gate check: count open blockers = 0, count pending reviewers = 0 → OK
holderDoc = xwiki.getDocument(holderRef)
newObj = holderDoc.newXObject("QualityFlow.ApprovalClass")
newObj.set("decision", "Approved")
newObj.set("author", currentUser)
newObj.set("date", new Date())
newObj.set("document", docRef)
holderDoc.save("Document approved by " + currentUser)
        │
        ▼
Redirect to returnUrl
APO Gate shows: "✓ Approved by [name] on [date]"
```

---

## 6. DocumentCommentPanel — Velocity Macro

This is the central UI component. It is included on any document page via:
```
{{include reference="QualityFlow.DocumentCommentPanel"/}}
```

### Full Structure

```velocity
## QualityFlow.DocumentCommentPanel
## Load CSS and JS resources
$xwiki.ssfx.use("QualityFlow.QFStyles", true)
$xwiki.jsfx.use("QualityFlow.QFScripts", true)

## Determine current document reference and holder page
#set($currentDocRef = "${doc.space}.${doc.name}")
#set($holderName = $currentDocRef.replaceAll('[^a-zA-Z0-9]', ''))
#set($holderRef = "QualityFlow.Comments.${holderName}")
#set($holderDoc = $xwiki.getDocument($holderRef))
#set($returnUrl = $doc.getURL('view'))
#set($classRef = "QualityFlow.CommentClass")
#set($reviewerClassRef = "QualityFlow.ReviewerClass")
#set($approvalClassRef = "QualityFlow.ApprovalClass")

## Collect all comments for this document
#set($allComments = $holderDoc.getObjects($classRef))
#set($openComments = [])
#set($resolvedComments = [])
#set($openBlockers = 0)
#foreach($c in $allComments)
  #if($c.getValue('status') == 'Open')
    #set($void = $openComments.add($c))
    #if($c.getValue('type') == 'Blocker')
      #set($openBlockers = $openBlockers + 1)
    #end
  #else
    #set($void = $resolvedComments.add($c))
  #end
#end
#set($totalComments = $allComments.size())
#set($openCount = $openComments.size())
#set($resolvedCount = $resolvedComments.size())

## Collect reviewers
#set($allReviewers = $holderDoc.getObjects($reviewerClassRef))
#set($pendingReviewers = [])
#set($doneReviewers = [])
#foreach($r in $allReviewers)
  #if($r.getValue('status') == 'Done')
    #set($void = $doneReviewers.add($r))
  #else
    #set($void = $pendingReviewers.add($r))
  #end
#end

## Check current user auth and group membership
#set($currentUser = $xcontext.user)
#set($isGuest = $currentUser == 'XWiki.XWikiGuest')
#set($isReviewer = $xwiki.isAMemberOf('XWiki.QFReviewers', $currentUser))
#set($isQM = $xwiki.isAMemberOf('XWiki.QFQualityManagers', $currentUser))
#set($isAPO = $xwiki.isAMemberOf('XWiki.QFAPO', $currentUser))
#set($canAct = !$isGuest && ($isReviewer || $isQM))

## Build JSON data block for JS highlight injection
<script id="qf-comments-data" type="application/json">
[
#foreach($c in $allComments)
  #set($objNum = $c.getNumber())
  #set($aText = $c.getValue('anchorText'))
  #set($cType = $c.getValue('type'))
  #set($cStatus = $c.getValue('status'))
  {
    "id": "${objNum}",
    "anchorText": "$escapetool.javascript($aText)",
    "type": "${cType}",
    "status": "${cStatus}"
  }#if($foreach.hasNext),#end
#end
]
</script>

## ── OUTER LAYOUT WRAPPER ──────────────────────────────────────────────
<div class="qf-layout">

  ## Document body wrapper (existing content flows here naturally via CSS)
  <div class="qf-doc-body" id="qf-doc-body">
    ## The actual document content is rendered by xWiki BEFORE this macro
    ## This div is positioned by CSS to leave room for the sidebar
  </div>

  ## ── RIGHT SIDEBAR ──────────────────────────────────────────────────
  <div class="qf-sidebar" id="qf-sidebar">

    ## Sidebar header: reviewer progress
    <div class="qf-sidebar-header">
      <div class="qf-progress-label">
        Reviewers: ${doneReviewers.size()} / ${allReviewers.size()} Done
      </div>
      <div class="qf-progress-bar">
        <div class="qf-progress-fill"
             style="width: #if($totalComments > 0)${math.floor($resolvedCount * 100 / $totalComments)}#{else}0#end%;"></div>
      </div>
      <div class="qf-progress-label">
        Comments: ${resolvedCount} / ${totalComments} Resolved
      </div>
    </div>

    ## Reviewer list
    <div class="qf-reviewer-list">
      #foreach($r in $allReviewers)
        #set($rUser = $r.getValue('reviewer'))
        #set($rName = $xwiki.getUserName($rUser, false))
        #set($rStatus = $r.getValue('status'))
        <div class="qf-reviewer-item">
          <span class="qf-avatar">${rName.substring(0,1).toUpperCase()}</span>
          <span class="qf-reviewer-name">$rName</span>
          <span class="qf-reviewer-badge qf-badge-${rStatus.toLowerCase()}">${rStatus}</span>
        </div>
      #end
      #if($isQM)
        <button class="qf-btn qf-btn-sm" onclick="qfShowAddReviewer()">+ Add Reviewer</button>
      #end
    </div>

    ## Comment tabs
    <div class="qf-tabs">
      <button class="qf-tab qf-tab-active" onclick="qfSetTab('open', this)">
        Open <span class="qf-tab-count">${openCount}</span>
      </button>
      <button class="qf-tab" onclick="qfSetTab('resolved', this)">
        Resolved <span class="qf-tab-count">${resolvedCount}</span>
      </button>
      <button class="qf-tab" onclick="qfSetTab('all', this)">
        All <span class="qf-tab-count">${totalComments}</span>
      </button>
    </div>

    ## Add Comment button (authenticated users only)
    #if(!$isGuest)
      <button class="qf-btn qf-btn-primary qf-add-comment-btn"
              onclick="qfShowAddCommentForm()">+ Add Comment</button>
    #end

    ## Add Comment Form (hidden by default)
    #if(!$isGuest)
    <div class="qf-form-panel" id="qf-add-comment-form" style="display:none;">
      <form method="post" action="$xwiki.getURL('QualityFlow.SaveComment', 'view')">
        <input type="hidden" name="docRef" value="$currentDocRef"/>
        <input type="hidden" name="holderRef" value="$holderRef"/>
        <input type="hidden" name="returnUrl" value="$returnUrl"/>
        <input type="hidden" name="anchorText" id="qf-anchorText" value=""/>
        <input type="hidden" name="anchorOffset" id="qf-anchorOffset" value=""/>
        <input type="hidden" name="form_token" value="$!{services.csrf.token}"/>

        <div id="qf-anchor-preview" class="qf-anchor-preview" style="display:none;">
          Anchored to: <em id="qf-anchor-preview-text"></em>
        </div>

        <div class="qf-form-group">
          <label>Type <span class="qf-required">*</span></label>
          <select name="type" required>
            <option value="">— Select —</option>
            <option value="Blocker">Blocker</option>
            <option value="Suggestion">Suggestion</option>
            <option value="Question">Question</option>
          </select>
        </div>

        <div class="qf-form-group">
          <label>Section</label>
          <input type="text" name="section" maxlength="255"
                 placeholder="e.g. Section 2 — Authentication"/>
        </div>

        <div class="qf-form-group">
          <label>Comment <span class="qf-required">*</span></label>
          <textarea name="comment" maxlength="4000" rows="4" required
                    placeholder="Describe your review finding..."></textarea>
        </div>

        <div class="qf-form-actions">
          <button type="submit" class="qf-btn qf-btn-primary">Submit</button>
          <button type="button" class="qf-btn" onclick="qfHideAddCommentForm()">Cancel</button>
        </div>
      </form>
    </div>
    #end

    ## Comment list
    <div class="qf-comment-list" id="qf-comment-list">

      ## Open comments
      <div class="qf-comment-group" data-tab-group="open">
        #if($openComments.isEmpty())
          <div class="qf-empty-state">No open comments.</div>
        #else
          #foreach($c in $openComments)
            #set($objNum = $c.getNumber())
            #set($authorRef = $c.getValue('author'))
            #set($authorName = $xwiki.getUserName($authorRef, false))
            #set($authorInitial = $authorName.substring(0,1).toUpperCase())
            #set($cType = $c.getValue('type'))
            #set($cSection = $c.getValue('section'))
            #set($cText = $c.getValue('comment'))
            #set($cResponse = $c.getValue('response'))
            #set($cDate = $c.getValue('date'))
            #set($cAnchor = $c.getValue('anchorText'))
            <div class="qf-comment-card qf-card-open" id="qf-card-${objNum}">
              <div class="qf-card-header">
                <span class="qf-avatar">$authorInitial</span>
                <span class="qf-card-author">$authorName</span>
                <span class="qf-badge qf-badge-${cType.toLowerCase()}">$cType.toUpperCase()</span>
                #if($cAnchor && $cAnchor != '')
                  <a class="qf-anchor-icon" href="#" onclick="qfScrollToHighlight('${objNum}'); return false;"
                     title="Jump to highlighted text">⚓</a>
                #end
              </div>
              #if($cSection && $cSection != '')
                <div class="qf-card-section">📍 $cSection</div>
              #end
              <div class="qf-card-timestamp">$datetool.format('relative', $cDate)</div>
              <div class="qf-card-body">$cText</div>
              #if($cResponse && $cResponse != '')
                <div class="qf-card-response">
                  <strong>Response:</strong> $cResponse
                </div>
              #end
              #if($canAct)
                <div class="qf-card-actions">
                  <form method="post"
                        action="$xwiki.getURL('QualityFlow.ResolveComment', 'view')"
                        style="display:inline;">
                    <input type="hidden" name="holderRef" value="$holderRef"/>
                    <input type="hidden" name="objNum" value="$objNum"/>
                    <input type="hidden" name="returnUrl" value="$returnUrl"/>
                    <input type="hidden" name="form_token" value="$!{services.csrf.token}"/>
                    <button type="submit" class="qf-btn qf-btn-resolve">✓ Resolve</button>
                  </form>
                  <button class="qf-btn qf-btn-reply"
                          onclick="qfShowReplyForm('${objNum}')">↩ Reply</button>
                </div>
                <div class="qf-reply-form" id="qf-reply-${objNum}" style="display:none;">
                  <form method="post"
                        action="$xwiki.getURL('QualityFlow.UpdateResponse', 'view')">
                    <input type="hidden" name="holderRef" value="$holderRef"/>
                    <input type="hidden" name="objNum" value="$objNum"/>
                    <input type="hidden" name="returnUrl" value="$returnUrl"/>
                    <input type="hidden" name="form_token" value="$!{services.csrf.token}"/>
                    <textarea name="response" maxlength="1000" rows="2"
                              placeholder="Add a response..."></textarea>
                    <button type="submit" class="qf-btn qf-btn-primary">Save</button>
                    <button type="button" class="qf-btn"
                            onclick="qfHideReplyForm('${objNum}')">Cancel</button>
                  </form>
                </div>
              #end
            </div>
          #end
        #end
      </div>

      ## Resolved comments (hidden by default)
      <div class="qf-comment-group" data-tab-group="resolved" style="display:none;">
        #if($resolvedComments.isEmpty())
          <div class="qf-empty-state">No resolved comments yet.</div>
        #else
          #foreach($c in $resolvedComments)
            #set($objNum = $c.getNumber())
            #set($authorRef = $c.getValue('author'))
            #set($authorName = $xwiki.getUserName($authorRef, false))
            #set($authorInitial = $authorName.substring(0,1).toUpperCase())
            #set($cType = $c.getValue('type'))
            #set($cSection = $c.getValue('section'))
            #set($cText = $c.getValue('comment'))
            #set($cResponse = $c.getValue('response'))
            #set($cDate = $c.getValue('date'))
            #set($resolvedByRef = $c.getValue('resolvedBy'))
            #set($resolvedByName = $xwiki.getUserName($resolvedByRef, false))
            #set($resolvedDate = $c.getValue('resolvedDate'))
            <div class="qf-comment-card qf-card-resolved" id="qf-card-${objNum}">
              <div class="qf-card-header">
                <span class="qf-avatar qf-avatar-resolved">$authorInitial</span>
                <span class="qf-card-author">$authorName</span>
                <span class="qf-badge qf-badge-${cType.toLowerCase()} qf-badge-resolved">$cType.toUpperCase()</span>
                <span class="qf-resolved-label">✓ Resolved</span>
              </div>
              #if($cSection && $cSection != '')
                <div class="qf-card-section">📍 $cSection</div>
              #end
              <div class="qf-card-timestamp">$datetool.format('relative', $cDate)</div>
              <div class="qf-card-body">$cText</div>
              #if($cResponse && $cResponse != '')
                <div class="qf-card-response">
                  <strong>Response:</strong> $cResponse
                </div>
              #end
              <div class="qf-resolved-meta">
                Resolved by $resolvedByName
                #if($resolvedDate) on $datetool.format('yyyy-MM-dd', $resolvedDate)#end
              </div>
              #if($canAct)
                <div class="qf-card-actions">
                  <button class="qf-btn qf-btn-reply"
                          onclick="qfShowReplyForm('${objNum}')">↩ Reply</button>
                </div>
                <div class="qf-reply-form" id="qf-reply-${objNum}" style="display:none;">
                  <form method="post"
                        action="$xwiki.getURL('QualityFlow.UpdateResponse', 'view')">
                    <input type="hidden" name="holderRef" value="$holderRef"/>
                    <input type="hidden" name="objNum" value="$objNum"/>
                    <input type="hidden" name="returnUrl" value="$returnUrl"/>
                    <input type="hidden" name="form_token" value="$!{services.csrf.token}"/>
                    <textarea name="response" maxlength="1000" rows="2"></textarea>
                    <button type="submit" class="qf-btn qf-btn-primary">Save</button>
                    <button type="button" class="qf-btn"
                            onclick="qfHideReplyForm('${objNum}')">Cancel</button>
                  </form>
                </div>
              #end
            </div>
          #end
        #end
      </div>

    </div>## end qf-comment-list
  </div>## end qf-sidebar

</div>## end qf-layout

## ── APO GATE BAR ─────────────────────────────────────────────────────
#set($pendingCount = $pendingReviewers.size())
#set($isGateLocked = $openBlockers > 0 || $pendingCount > 0)

## Get latest approval decision
#set($allApprovals = $holderDoc.getObjects($approvalClassRef))
#set($latestApproval = false)
#foreach($ap in $allApprovals)
  #set($latestApproval = $ap)  ## last one wins (objects ordered by creation)
#end

<div class="qf-apo-gate #if($isGateLocked)qf-gate-locked#{else}qf-gate-unlocked#end"
     id="qf-apo-gate">
  <div class="qf-gate-status">
    #if($latestApproval)
      #if($latestApproval.getValue('decision') == 'Approved')
        <span class="qf-gate-icon">✅</span>
        <span>Approved by $xwiki.getUserName($latestApproval.getValue('author'), false)
          on $datetool.format('yyyy-MM-dd', $latestApproval.getValue('date'))</span>
      #else
        <span class="qf-gate-icon">❌</span>
        <span>Rejected by $xwiki.getUserName($latestApproval.getValue('author'), false)
          — $latestApproval.getValue('reason')</span>
      #end
    #elseif($isGateLocked)
      <span class="qf-gate-icon">🔒</span>
      <span>
        #if($openBlockers > 0)${openBlockers} blocker(s) must be resolved#end
        #if($openBlockers > 0 && $pendingCount > 0) — #end
        #if($pendingCount > 0)
          #foreach($r in $pendingReviewers)
            $xwiki.getUserName($r.getValue('reviewer'), false)#if($foreach.hasNext), #end
          #end
          review(s) pending
        #end
      </span>
    #else
      <span class="qf-gate-icon">🔓</span>
      <span>All blockers resolved. All reviewers complete. Ready for approval.</span>
    #end
  </div>

  #if($isAPO && !$latestApproval)
    <div class="qf-gate-actions">
      <form method="post"
            action="$xwiki.getURL('QualityFlow.SaveApproval', 'view')"
            style="display:inline;" id="qf-approve-form">
        <input type="hidden" name="holderRef" value="$holderRef"/>
        <input type="hidden" name="docRef" value="$currentDocRef"/>
        <input type="hidden" name="decision" value="Approved"/>
        <input type="hidden" name="returnUrl" value="$returnUrl"/>
        <input type="hidden" name="form_token" value="$!{services.csrf.token}"/>
        <button type="submit" class="qf-btn qf-btn-approve"
                #if($isGateLocked)disabled#end>✓ Approve</button>
      </form>
      <button class="qf-btn qf-btn-reject" onclick="qfShowRejectForm()"
              #if($isGateLocked)disabled#end>✗ Reject</button>
      <div id="qf-reject-form" style="display:none;">
        <form method="post"
              action="$xwiki.getURL('QualityFlow.SaveApproval', 'view')">
          <input type="hidden" name="holderRef" value="$holderRef"/>
          <input type="hidden" name="docRef" value="$currentDocRef"/>
          <input type="hidden" name="decision" value="Rejected"/>
          <input type="hidden" name="returnUrl" value="$returnUrl"/>
          <input type="hidden" name="form_token" value="$!{services.csrf.token}"/>
          <textarea name="reason" maxlength="2000" rows="2"
                    placeholder="Reason for rejection..." required></textarea>
          <button type="submit" class="qf-btn qf-btn-reject">Confirm Reject</button>
          <button type="button" class="qf-btn" onclick="qfHideRejectForm()">Cancel</button>
        </form>
      </div>
    </div>
  #end
</div>
```

---

## 7. QFScripts — JavaScript Module

```javascript
// QualityFlow.QFScripts
// Stored as a wiki page, loaded via $xwiki.jsfx.use("QualityFlow.QFScripts", true)

(function () {
  'use strict';

  // ── CONSTANTS ──────────────────────────────────────────────────────
  var HIGHLIGHT_CLASSES = {
    'Blocker':    'qf-highlight qf-hl-blocker',
    'Suggestion': 'qf-highlight qf-hl-suggestion',
    'Question':   'qf-highlight qf-hl-question',
    'Resolved':   'qf-highlight qf-hl-resolved'
  };

  // ── HIGHLIGHT INJECTION ────────────────────────────────────────────
  // Reads comment data from the JSON block rendered by Velocity,
  // finds matching text in the document body, wraps in <mark>.

  function injectHighlights() {
    var dataEl = document.getElementById('qf-comments-data');
    if (!dataEl) return;

    var comments;
    try {
      comments = JSON.parse(dataEl.textContent);
    } catch (e) { return; }

    var docBody = document.querySelector('.qf-doc-body .wiki-content')
                  || document.querySelector('.xwikicontent')
                  || document.body;

    comments.forEach(function (c) {
      if (!c.anchorText || c.anchorText.trim() === '') return;
      var hlClass = c.status === 'Resolved'
        ? HIGHLIGHT_CLASSES['Resolved']
        : (HIGHLIGHT_CLASSES[c.type] || 'qf-highlight');
      wrapTextInBody(docBody, c.anchorText, c.id, hlClass);
    });
  }

  // Uses TreeWalker to find and wrap text nodes containing anchorText.
  function wrapTextInBody(container, searchText, commentId, hlClass) {
    var walker = document.createTreeWalker(
      container,
      NodeFilter.SHOW_TEXT,
      null,
      false
    );
    var node;
    while ((node = walker.nextNode())) {
      var idx = node.nodeValue.indexOf(searchText);
      if (idx === -1) continue;

      // Split text node: [before][match][after]
      var before = node.nodeValue.substring(0, idx);
      var after  = node.nodeValue.substring(idx + searchText.length);

      var mark = document.createElement('mark');
      mark.className = hlClass;
      mark.setAttribute('data-comment-id', commentId);
      mark.textContent = searchText;
      mark.addEventListener('click', function () {
        scrollSidebarToCard(this.getAttribute('data-comment-id'));
      }.bind(mark));

      var parent = node.parentNode;
      if (before) parent.insertBefore(document.createTextNode(before), node);
      parent.insertBefore(mark, node);
      if (after) parent.insertBefore(document.createTextNode(after), node);
      parent.removeChild(node);

      break; // Highlight first occurrence only per comment
    }
  }

  // ── TEXT SELECTION POPOVER ─────────────────────────────────────────
  var popover = null;
  var capturedAnchorText = '';
  var capturedAnchorOffset = 0;

  function createPopover() {
    popover = document.createElement('div');
    popover.id = 'qf-selection-popover';
    popover.className = 'qf-selection-popover';
    popover.innerHTML = '<button id="qf-popover-btn">💬 Add Comment</button>';
    popover.style.display = 'none';
    document.body.appendChild(popover);

    document.getElementById('qf-popover-btn').addEventListener('click', function () {
      openCommentFormWithAnchor();
    });
  }

  function onMouseUp(e) {
    // Ignore clicks inside sidebar
    if (e.target.closest && e.target.closest('#qf-sidebar')) return;

    setTimeout(function () {
      var sel = window.getSelection();
      var text = sel ? sel.toString().trim() : '';

      if (text.length === 0) {
        hidePopover();
        return;
      }

      // Capture anchor data
      capturedAnchorText = text.substring(0, 500); // truncate at 500
      try {
        var range = sel.getRangeAt(0);
        capturedAnchorOffset = range.startOffset;
      } catch (ex) {
        capturedAnchorOffset = 0;
      }

      // Position popover near selection
      var rect = sel.getRangeAt(0).getBoundingClientRect();
      popover.style.top  = (window.scrollY + rect.bottom + 8) + 'px';
      popover.style.left = (window.scrollX + rect.left) + 'px';
      popover.style.display = 'block';
    }, 10);
  }

  function hidePopover() {
    if (popover) popover.style.display = 'none';
  }

  function openCommentFormWithAnchor() {
    hidePopover();
    // Pre-fill hidden anchor fields in the form
    var anchorTextEl   = document.getElementById('qf-anchorText');
    var anchorOffsetEl = document.getElementById('qf-anchorOffset');
    var previewEl      = document.getElementById('qf-anchor-preview');
    var previewTextEl  = document.getElementById('qf-anchor-preview-text');

    if (anchorTextEl)   anchorTextEl.value   = capturedAnchorText;
    if (anchorOffsetEl) anchorOffsetEl.value = capturedAnchorOffset;
    if (previewEl && capturedAnchorText) {
      previewEl.style.display = 'block';
      previewTextEl.textContent = '"' + capturedAnchorText.substring(0, 80) +
        (capturedAnchorText.length > 80 ? '...' : '') + '"';
    }
    window.getSelection().removeAllRanges();
    qfShowAddCommentForm();
  }

  // ── SIDEBAR SCROLL ─────────────────────────────────────────────────
  function scrollSidebarToCard(commentId) {
    var card = document.getElementById('qf-card-' + commentId);
    var sidebar = document.getElementById('qf-sidebar');
    if (!card || !sidebar) return;

    // Make sure correct tab is active
    var cardGroup = card.closest('[data-tab-group]');
    if (cardGroup) {
      var tabGroup = cardGroup.getAttribute('data-tab-group');
      var tabBtn = document.querySelector('.qf-tab[data-tab="' + tabGroup + '"]');
      if (tabBtn) tabBtn.click();
    }

    card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    card.classList.add('qf-card-focused');
    setTimeout(function () { card.classList.remove('qf-card-focused'); }, 2000);
  }
  window.qfScrollToHighlight = scrollSidebarToCard; // Expose for Velocity onclick

  // ── TAB SWITCHING ──────────────────────────────────────────────────
  window.qfSetTab = function (tab, btn) {
    document.querySelectorAll('.qf-tab').forEach(function (t) {
      t.classList.remove('qf-tab-active');
    });
    btn.classList.add('qf-tab-active');

    document.querySelectorAll('.qf-comment-group').forEach(function (g) {
      var groupTab = g.getAttribute('data-tab-group');
      g.style.display = (tab === 'all' || groupTab === tab) ? 'block' : 'none';
    });
  };

  // ── FORM SHOW/HIDE ─────────────────────────────────────────────────
  window.qfShowAddCommentForm = function () {
    var form = document.getElementById('qf-add-comment-form');
    if (form) form.style.display = 'block';
  };
  window.qfHideAddCommentForm = function () {
    var form = document.getElementById('qf-add-comment-form');
    if (form) {
      form.style.display = 'none';
      // Reset anchor fields
      var anchorTextEl = document.getElementById('qf-anchorText');
      var anchorOffsetEl = document.getElementById('qf-anchorOffset');
      var previewEl = document.getElementById('qf-anchor-preview');
      if (anchorTextEl)   anchorTextEl.value = '';
      if (anchorOffsetEl) anchorOffsetEl.value = '';
      if (previewEl)      previewEl.style.display = 'none';
      capturedAnchorText = '';
      capturedAnchorOffset = 0;
    }
  };
  window.qfShowReplyForm = function (objNum) {
    var form = document.getElementById('qf-reply-' + objNum);
    if (form) form.style.display = 'block';
  };
  window.qfHideReplyForm = function (objNum) {
    var form = document.getElementById('qf-reply-' + objNum);
    if (form) form.style.display = 'none';
  };
  window.qfShowRejectForm = function () {
    var form = document.getElementById('qf-reject-form');
    if (form) form.style.display = 'block';
  };
  window.qfHideRejectForm = function () {
    var form = document.getElementById('qf-reject-form');
    if (form) form.style.display = 'none';
  };
  window.qfShowAddReviewer = function () {
    // Navigate to AddReviewer page with docRef parameter
    var docRefEl = document.querySelector('input[name="docRef"]');
    var docRef = docRefEl ? docRefEl.value : '';
    window.location.href = '/bin/view/QualityFlow/AddReviewer?docRef=' + encodeURIComponent(docRef);
  };

  // ── RESPONSIVE SIDEBAR TOGGLE ──────────────────────────────────────
  function setupResponsiveToggle() {
    var toggle = document.getElementById('qf-sidebar-toggle');
    if (!toggle) return;
    toggle.addEventListener('click', function () {
      document.body.classList.toggle('qf-sidebar-open');
    });
  }

  // ── INIT ───────────────────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', function () {
    createPopover();
    injectHighlights();
    setupResponsiveToggle();

    // Listen for text selection on the document body area
    var docArea = document.querySelector('.qf-doc-body') || document.body;
    docArea.addEventListener('mouseup', onMouseUp);

    // Close popover on outside click
    document.addEventListener('mousedown', function (e) {
      if (popover && !popover.contains(e.target)) {
        hidePopover();
      }
    });
  });

}());
```

---

## 8. QFStyles — CSS

```css
/* QualityFlow.QFStyles */
/* Loaded via $xwiki.ssfx.use("QualityFlow.QFStyles", true) */

/* ── LAYOUT ────────────────────────────────────────────────────────── */
.qf-layout {
  display: flex;
  align-items: flex-start;
  gap: 0;
  padding-bottom: 60px; /* room for fixed APO gate bar */
}

.qf-doc-body {
  flex: 1 1 70%;
  min-width: 0;
  padding-right: 16px;
}

.qf-sidebar {
  flex: 0 0 320px;
  width: 320px;
  background: #f8f9fa;
  border-left: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 12px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  position: sticky;
  top: 16px;
  font-size: 13px;
}

/* ── SIDEBAR HEADER ───────────────────────────────────────────────── */
.qf-sidebar-header {
  padding-bottom: 10px;
  border-bottom: 1px solid #dee2e6;
  margin-bottom: 10px;
}

.qf-progress-label {
  font-size: 12px;
  color: #6c757d;
  margin-bottom: 4px;
}

.qf-progress-bar {
  background: #e9ecef;
  border-radius: 4px;
  height: 6px;
  margin-bottom: 6px;
}

.qf-progress-fill {
  background: #28a745;
  height: 6px;
  border-radius: 4px;
  transition: width 0.3s ease;
}

/* ── REVIEWER LIST ────────────────────────────────────────────────── */
.qf-reviewer-list {
  margin-bottom: 12px;
}

.qf-reviewer-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.qf-reviewer-name {
  flex: 1;
  font-size: 12px;
}

/* ── AVATAR ────────────────────────────────────────────────────────── */
.qf-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #6c757d;
  color: white;
  font-size: 11px;
  font-weight: bold;
  flex-shrink: 0;
}

.qf-avatar-resolved {
  background: #adb5bd;
}

/* ── STATUS BADGES ────────────────────────────────────────────────── */
.qf-badge {
  display: inline-block;
  padding: 2px 7px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: bold;
  letter-spacing: 0.5px;
}

.qf-badge-blocker      { background: #f8d7da; color: #842029; border: 1px solid #f1aeb5; }
.qf-badge-suggestion   { background: #cfe2ff; color: #084298; border: 1px solid #9ec5fe; }
.qf-badge-question     { background: #fff3cd; color: #664d03; border: 1px solid #ffda6a; }
.qf-badge-resolved     { opacity: 0.6; }

.qf-reviewer-badge { font-size: 10px; padding: 2px 6px; border-radius: 10px; }
.qf-badge-done     { background: #d1e7dd; color: #0f5132; }
.qf-badge-pending  { background: #fff3cd; color: #664d03; }

/* ── TABS ──────────────────────────────────────────────────────────── */
.qf-tabs {
  display: flex;
  border-bottom: 2px solid #dee2e6;
  margin-bottom: 10px;
}

.qf-tab {
  background: none;
  border: none;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 12px;
  color: #6c757d;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
}

.qf-tab-active {
  color: #0d6efd;
  border-bottom-color: #0d6efd;
  font-weight: 600;
}

.qf-tab-count {
  background: #e9ecef;
  border-radius: 10px;
  padding: 1px 6px;
  font-size: 10px;
  margin-left: 4px;
}

/* ── COMMENT CARDS ────────────────────────────────────────────────── */
.qf-comment-card {
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 8px;
  transition: box-shadow 0.2s;
}

.qf-comment-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.qf-card-resolved {
  opacity: 0.75;
  background: #f8f9fa;
}

.qf-card-focused {
  box-shadow: 0 0 0 3px rgba(13, 110, 253, 0.35) !important;
}

.qf-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.qf-card-author   { font-size: 12px; font-weight: 600; flex: 1; }
.qf-card-section  { font-size: 11px; color: #6c757d; margin-bottom: 2px; }
.qf-card-timestamp{ font-size: 11px; color: #adb5bd; margin-bottom: 4px; }
.qf-card-body     { font-size: 12px; line-height: 1.5; }

.qf-card-response {
  margin-top: 6px;
  padding: 6px;
  background: #e8f4e8;
  border-radius: 4px;
  font-size: 12px;
  color: #155724;
}

.qf-resolved-meta {
  font-size: 11px;
  color: #6c757d;
  margin-top: 4px;
}

.qf-resolved-label {
  font-size: 11px;
  color: #28a745;
  font-weight: 600;
}

.qf-card-actions {
  margin-top: 8px;
  display: flex;
  gap: 6px;
}

.qf-anchor-icon {
  font-size: 12px;
  text-decoration: none;
  color: #6c757d;
}

/* ── BUTTONS ──────────────────────────────────────────────────────── */
.qf-btn {
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid #dee2e6;
  background: white;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.15s;
}

.qf-btn:hover       { background: #f8f9fa; }
.qf-btn:disabled    { opacity: 0.5; cursor: not-allowed; }
.qf-btn-sm          { padding: 2px 8px; font-size: 11px; }

.qf-btn-primary     { background: #0d6efd; color: white; border-color: #0d6efd; }
.qf-btn-primary:hover { background: #0b5ed7; }

.qf-btn-resolve     { background: #d1e7dd; color: #0f5132; border-color: #a3cfbb; }
.qf-btn-resolve:hover { background: #badbcc; }

.qf-btn-reply       { background: #fff3cd; color: #664d03; border-color: #ffda6a; }
.qf-btn-reply:hover { background: #ffeeba; }

.qf-btn-approve     { background: #28a745; color: white; border-color: #28a745; }
.qf-btn-approve:hover { background: #218838; }

.qf-btn-reject      { background: #dc3545; color: white; border-color: #dc3545; }
.qf-btn-reject:hover { background: #c82333; }

.qf-add-comment-btn {
  width: 100%;
  margin-bottom: 10px;
  text-align: center;
  padding: 8px;
}

/* ── FORMS ────────────────────────────────────────────────────────── */
.qf-form-panel {
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 10px;
}

.qf-form-group {
  margin-bottom: 10px;
}

.qf-form-group label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 3px;
}

.qf-form-group input,
.qf-form-group select,
.qf-form-group textarea {
  width: 100%;
  padding: 5px 8px;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  font-size: 12px;
  box-sizing: border-box;
}

.qf-form-actions { display: flex; gap: 8px; margin-top: 8px; }
.qf-required     { color: #dc3545; }

.qf-anchor-preview {
  background: #fff3cd;
  border: 1px solid #ffda6a;
  border-radius: 4px;
  padding: 6px;
  font-size: 11px;
  margin-bottom: 8px;
  color: #664d03;
}

.qf-reply-form {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e9ecef;
}

.qf-reply-form textarea {
  width: 100%;
  padding: 4px 8px;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  font-size: 12px;
  box-sizing: border-box;
  margin-bottom: 4px;
}

/* ── HIGHLIGHTS ───────────────────────────────────────────────────── */
mark.qf-highlight {
  cursor: pointer;
  border-radius: 2px;
  padding: 0 1px;
}

mark.qf-hl-blocker    { background: rgba(220, 53,  69,  0.25); border-bottom: 2px solid #dc3545; }
mark.qf-hl-suggestion { background: rgba(13,  110, 253, 0.15); border-bottom: 2px solid #0d6efd; }
mark.qf-hl-question   { background: rgba(255, 193, 7,   0.30); border-bottom: 2px solid #ffc107; }
mark.qf-hl-resolved   { background: rgba(108, 117, 125, 0.15); border-bottom: 2px solid #adb5bd; }

mark.qf-highlight:hover { opacity: 0.75; }

/* ── SELECTION POPOVER ────────────────────────────────────────────── */
.qf-selection-popover {
  position: absolute;
  z-index: 9999;
  background: #212529;
  color: white;
  border-radius: 4px;
  padding: 4px 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.qf-selection-popover button {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 12px;
  padding: 2px 4px;
}

/* ── APO GATE BAR ─────────────────────────────────────────────────── */
.qf-apo-gate {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  font-size: 13px;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.15);
}

.qf-gate-locked   { background: #fff3cd; border-top: 2px solid #ffc107; color: #664d03; }
.qf-gate-unlocked { background: #d1e7dd; border-top: 2px solid #28a745; color: #0f5132; }

.qf-gate-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.qf-gate-icon  { font-size: 16px; }
.qf-gate-actions { display: flex; gap: 8px; align-items: center; }

/* ── EMPTY STATE ──────────────────────────────────────────────────── */
.qf-empty-state {
  text-align: center;
  color: #6c757d;
  font-size: 12px;
  padding: 20px 10px;
}

/* ── RESPONSIVE ───────────────────────────────────────────────────── */
@media (max-width: 1024px) {
  .qf-layout { display: block; }

  .qf-sidebar {
    position: fixed;
    top: 0;
    right: -340px;
    width: 320px;
    height: 100vh;
    max-height: 100vh;
    border-left: 1px solid #dee2e6;
    border-radius: 0;
    z-index: 999;
    transition: right 0.3s ease;
    padding-top: 50px;
  }

  body.qf-sidebar-open .qf-sidebar { right: 0; }

  #qf-sidebar-toggle {
    display: block;
    position: fixed;
    bottom: 70px;
    right: 16px;
    z-index: 1001;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: #0d6efd;
    color: white;
    border: none;
    font-size: 18px;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0,0,0,0.3);
  }
}

@media (min-width: 1025px) {
  #qf-sidebar-toggle { display: none; }
}
```

---

## 9. Groovy Action Pages

### 9.1 SaveComment (full implementation)

```groovy
// QualityFlow.SaveComment
// Content type: Velocity+Groovy (set page content type to "Groovy")
// Use: {{groovy}} wrapper in page content

{{groovy}}
import org.xwiki.model.reference.DocumentReference
import org.xwiki.model.reference.SpaceReference
import org.xwiki.model.reference.WikiReference

// ── AUTH CHECK ───────────────────────────────────────────────────────
def currentUser = xcontext.userReference
if (currentUser == null || currentUser.toString().contains('XWikiGuest')) {
  response.sendRedirect(request.contextPath + '/bin/login/XWiki/XWikiLogin')
  return
}

// ── CSRF CHECK ───────────────────────────────────────────────────────
def csrfService = services.csrf
if (!csrfService.isTokenValid(request.getParameter('form_token'))) {
  response.sendError(403, 'Invalid CSRF token')
  return
}

// ── READ PARAMETERS ──────────────────────────────────────────────────
def docRef     = request.getParameter('docRef')     ?: ''
def holderRef  = request.getParameter('holderRef')  ?: ''
def returnUrl  = request.getParameter('returnUrl')  ?: '/bin/view/Main/WebHome'
def pType      = request.getParameter('type')       ?: ''
def pSection   = request.getParameter('section')    ?: ''
def pComment   = request.getParameter('comment')    ?: ''
def pAnchorText   = request.getParameter('anchorText')   ?: ''
def pAnchorOffset = request.getParameter('anchorOffset') ?: '0'

// ── VALIDATION ───────────────────────────────────────────────────────
def validTypes = ['Blocker', 'Suggestion', 'Question']
if (!validTypes.contains(pType)) {
  // Redirect back with error (simple approach: pass as URL param)
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Type is required and must be Blocker, Suggestion, or Question.', 'UTF-8'))
  return
}
if (pComment.trim().isEmpty()) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Comment text is required.', 'UTF-8'))
  return
}
if (pSection.length() > 255) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Section must be 255 characters or less.', 'UTF-8'))
  return
}
// Truncate anchorText silently
if (pAnchorText.length() > 500) {
  pAnchorText = pAnchorText.substring(0, 500)
}

// ── GET OR CREATE HOLDER PAGE ────────────────────────────────────────
def holderDocument = xwiki.getDocument(holderRef)
if (holderDocument.isNew()) {
  holderDocument.setTitle("QualityFlow Comments: ${docRef}")
  holderDocument.setContent("Comments holder page for ${docRef}. Do not edit manually.")
}

// ── CREATE XOBJECT ───────────────────────────────────────────────────
def classRef = "QualityFlow.CommentClass"
def newObj   = holderDocument.newXObject(classRef)

newObj.set('type',         pType)
newObj.set('status',       'Open')
newObj.set('section',      pSection)
newObj.set('anchorText',   pAnchorText)
newObj.set('anchorOffset', pAnchorOffset.isInteger() ? pAnchorOffset.toInteger() : 0)
newObj.set('comment',      pComment)
newObj.set('author',       currentUser.toString())
newObj.set('date',         new Date())
newObj.set('document',     docRef)
// resolvedBy and resolvedDate intentionally NOT set here

// ── SAVE ─────────────────────────────────────────────────────────────
try {
  holderDocument.save("New ${pType} comment by ${currentUser.name}")
} catch (Exception e) {
  response.sendRedirect(returnUrl + '?qf_error=' +
    java.net.URLEncoder.encode('Failed to save comment: ' + e.getMessage(), 'UTF-8'))
  return
}

response.sendRedirect(returnUrl + '?qf_success=1')
{{/groovy}}
```

### 9.2 ResolveComment (pattern)

```groovy
{{groovy}}
// Auth, CSRF checks (same as SaveComment)...
def holderRef  = request.getParameter('holderRef')
def objNum     = request.getParameter('objNum').toInteger()
def returnUrl  = request.getParameter('returnUrl') ?: '/bin/view/Main/WebHome'

// Group check
def currentUser = xcontext.user
def isReviewer  = xwiki.isAMemberOf('XWiki.QFReviewers', currentUser)
def isQM        = xwiki.isAMemberOf('XWiki.QFQualityManagers', currentUser)
if (!isReviewer && !isQM) {
  response.sendRedirect(returnUrl + '?qf_error=Not+authorized+to+resolve+comments.')
  return
}

def holderDoc = xwiki.getDocument(holderRef)
def obj = holderDoc.getObject('QualityFlow.CommentClass', objNum)
if (obj == null) {
  response.sendRedirect(returnUrl + '?qf_error=Comment+not+found.')
  return
}
if (obj.getValue('status') == 'Resolved') {
  response.sendRedirect(returnUrl + '?qf_info=Comment+was+already+resolved.')
  return
}

obj.set('status',      'Resolved')
obj.set('resolvedBy',  xcontext.userReference.toString())
obj.set('resolvedDate', new Date())

try {
  holderDoc.save("Comment resolved by ${currentUser}")
} catch (Exception e) {
  // rollback not needed — save failed before persisting
  response.sendRedirect(returnUrl + '?qf_error=Save+failed:+' + e.getMessage())
  return
}
response.sendRedirect(returnUrl + '?qf_success=1')
{{/groovy}}
```

### 9.3 UpdateResponse (pattern)

```groovy
{{groovy}}
// Auth, CSRF, group checks...
def holderRef = request.getParameter('holderRef')
def objNum    = request.getParameter('objNum').toInteger()
def pResponse = request.getParameter('response') ?: ''
def returnUrl = request.getParameter('returnUrl') ?: '/'

if (pResponse.trim().isEmpty()) {
  response.sendRedirect(returnUrl)
  return
}
if (pResponse.length() > 1000) {
  pResponse = pResponse.substring(0, 1000)
}

def holderDoc = xwiki.getDocument(holderRef)
def obj = holderDoc.getObject('QualityFlow.CommentClass', objNum)
if (obj == null) {
  response.sendRedirect(returnUrl + '?qf_error=Comment+not+found.')
  return
}

obj.set('response', pResponse)
holderDoc.save("Response updated by ${xcontext.user}")
response.sendRedirect(returnUrl + '?qf_success=1')
{{/groovy}}
```

### 9.4 SaveApproval (pattern)

```groovy
{{groovy}}
// Auth, CSRF checks...
def currentUser = xcontext.user
if (!xwiki.isAMemberOf('XWiki.QFAPO', currentUser)) {
  response.sendRedirect(returnUrl + '?qf_error=Not+authorized+to+approve+documents.')
  return
}

def holderRef = request.getParameter('holderRef')
def docRef    = request.getParameter('docRef')
def decision  = request.getParameter('decision')  // Approved or Rejected
def reason    = request.getParameter('reason') ?: ''
def returnUrl = request.getParameter('returnUrl') ?: '/'

// Gate check: verify no open blockers and no pending reviewers
def holderDoc = xwiki.getDocument(holderRef)
def openBlockers = holderDoc.getObjects('QualityFlow.CommentClass').findAll {
  it.getValue('type') == 'Blocker' && it.getValue('status') == 'Open'
}
def pendingReviewers = holderDoc.getObjects('QualityFlow.ReviewerClass').findAll {
  it.getValue('status') == 'Pending'
}
if (!openBlockers.isEmpty() || !pendingReviewers.isEmpty()) {
  response.sendRedirect(returnUrl + '?qf_error=Cannot+approve:+gate+conditions+not+met.')
  return
}

def newObj = holderDoc.newXObject('QualityFlow.ApprovalClass')
newObj.set('decision', decision)
newObj.set('author',   xcontext.userReference.toString())
newObj.set('date',     new Date())
newObj.set('reason',   reason)
newObj.set('document', docRef)

holderDoc.save("Document ${decision} by ${currentUser}")
response.sendRedirect(returnUrl + '?qf_success=1')
{{/groovy}}
```

---

## 10. CommentDashboard — XWQL + Groovy

```groovy
// QualityFlow.CommentDashboard page content:
{{groovy}}
// Find all Holder Pages that have at least one CommentClass XObject
def query = services.query.xwql(
  "select distinct doc.fullName from Document doc, doc.object(QualityFlow.CommentClass) as c " +
  "order by doc.date desc"
).execute()

def rows = []
query.each { holderFullName ->
  def holderDoc = xwiki.getDocument(holderFullName)
  def comments  = holderDoc.getObjects('QualityFlow.CommentClass')
  def reviewers = holderDoc.getObjects('QualityFlow.ReviewerClass')
  def approvals = holderDoc.getObjects('QualityFlow.ApprovalClass')

  def total      = comments.size()
  def openCount  = comments.count { it.getValue('status') == 'Open' }
  def blockers   = comments.count { it.getValue('type') == 'Blocker' && it.getValue('status') == 'Open' }
  def suggestions= comments.count { it.getValue('type') == 'Suggestion' }
  def questions  = comments.count { it.getValue('type') == 'Question' }
  def reviewerTotal  = reviewers.size()
  def reviewersDone  = reviewers.count { it.getValue('status') == 'Done' }
  def pendingReviewers = reviewers.count { it.getValue('status') == 'Pending' }

  // Last activity: max of all dates and resolvedDates
  def allDates = comments.collect { c ->
    [c.getValue('date'), c.getValue('resolvedDate')].findAll { it != null }
  }.flatten()
  def lastActivity = allDates ? allDates.max() : holderDoc.date

  // Status label
  def latestApproval = approvals ? approvals.last() : null
  def statusLabel
  if (latestApproval) {
    statusLabel = latestApproval.getValue('decision')  // Approved or Rejected
  } else if (blockers > 0) {
    statusLabel = 'Blocked'
  } else if (openCount > 0 || pendingReviewers > 0) {
    statusLabel = 'In Review'
  } else {
    statusLabel = 'Ready'
  }

  // Derive the reviewed document reference from the holder page title
  def docRef = holderDoc.getObject('QualityFlow.CommentClass', 0)?.getValue('document') ?: holderFullName

  rows.add([
    docRef: docRef,
    holderRef: holderFullName,
    total: total,
    open: openCount,
    blockers: blockers,
    suggestions: suggestions,
    questions: questions,
    reviewerTotal: reviewerTotal,
    reviewersDone: reviewersDone,
    lastActivity: lastActivity,
    statusLabel: statusLabel
  ])
}

xcontext.put('qfRows', rows)
{{/groovy}}

{{velocity}}
#if($qfRows.isEmpty())
  {{info}}No comments have been submitted yet.{{/info}}
#else
## Render table
| Document | Total | Open | Blockers | Suggestions | Questions | Reviewers | Last Activity | Status |
|---|---|---|---|---|---|---|---|---|
#foreach($row in $qfRows)
| [[$xwiki.getDocument($row.docRef).title>>$row.docRef]] | $row.total | $row.open | $row.blockers | $row.suggestions | $row.questions | $row.reviewersDone / $row.reviewerTotal | $datetool.format('yyyy-MM-dd', $row.lastActivity) | **$row.statusLabel** |
#end
#end
{{/velocity}}
```

---

## 11. Groups and Rights Configuration

| Group | xWiki Group Page | Permissions on QualityFlow space |
|---|---|---|
| Administrator | XWiki.XWikiAdminGroup | View, Edit, Admin, Delete |
| Quality_Manager | XWiki.QFQualityManagers | View, Edit |
| Reviewer | XWiki.QFReviewers | View, Edit |
| APO | XWiki.QFAPO | View, Edit |

**Holder Pages** (`QualityFlow.Comments.*`): Inherit from QualityFlow space. All groups above need Edit to create/modify XObjects.

**CommentDashboard**: View access for QFQualityManagers and QFAPO minimum.

---

## 12. Installation Sequence

1. **Prerequisites**: xWiki 14.10+, admin access, SSH/browser access to your Lightsail instance.

2. **Create groups** in xWiki Administration → Groups:
   - `XWiki.QFReviewers`
   - `XWiki.QFQualityManagers`
   - `XWiki.QFAPO`

3. **Create XClass pages** (in order):
   - `QualityFlow.CommentClass` — add class with fields per Section 2.1
   - `QualityFlow.ReviewerClass` — add class with fields per Section 2.2
   - `QualityFlow.ApprovalClass` — add class with fields per Section 2.3

4. **Create CSS and JS pages**:
   - `QualityFlow.QFStyles` — set content type to "Style Sheet Extension (SSX)", paste CSS from Section 8
   - `QualityFlow.QFScripts` — set content type to "JavaScript Extension (JSX)", paste JS from Section 7

5. **Create Groovy action pages** (set page content to Groovy, mark as "Technical" page):
   - `QualityFlow.SaveComment`
   - `QualityFlow.ResolveComment`
   - `QualityFlow.UpdateResponse`
   - `QualityFlow.SaveReviewer`
   - `QualityFlow.SaveApproval`

6. **Create the DocumentCommentPanel** macro page:
   - `QualityFlow.DocumentCommentPanel` — paste Velocity content from Section 6

7. **Create CommentDashboard**:
   - `QualityFlow.CommentDashboard` — paste Groovy + Velocity from Section 10

8. **Create Admin page**:
   - `QualityFlow.Admin` — Groovy query counting all CommentClass XObjects + link to dashboard

9. **Create WebHome**:
   - `QualityFlow.WebHome` — installation guide, link to dashboard and admin

10. **Set space rights**:
    - Navigate to QualityFlow space → Administer Space → Rights
    - Grant View + Edit to QFReviewers, QFQualityManagers, QFAPO
    - Grant Admin to XWikiAdminGroup

11. **Include panel on a document page**:
    - Edit the target document
    - Add `{{include reference="QualityFlow.DocumentCommentPanel"/}}` at the bottom of the page content
    - Save

12. **Add toggle button for responsive** (optional):
    - Add `<button id="qf-sidebar-toggle">💬</button>` before the include macro

---

## 13. Testing Checklist

### XClass Setup
- [ ] CommentClass has all 12 fields with correct types
- [ ] ReviewerClass has 4 fields
- [ ] ApprovalClass has 5 fields
- [ ] Mandatory fields (type, comment, decision) block save when empty
- [ ] Default values: status=Open, reviewer status=Pending

### Inline Highlight
- [ ] Text selection shows popover
- [ ] Clicking "Add Comment" popover pre-fills anchor preview
- [ ] Submitting creates XObject with anchorText + anchorOffset
- [ ] Page reload shows `<mark>` on selected text
- [ ] Blocker highlight = red-tinted
- [ ] Suggestion highlight = blue-tinted
- [ ] Question highlight = yellow-tinted
- [ ] Resolved comment highlight = grey
- [ ] Clicking highlight scrolls sidebar to correct card
- [ ] Missing anchor text (doc edited) = no highlight, comment still visible

### Comment Sidebar
- [ ] Sidebar renders to the right of document body
- [ ] Reviewer list shows with Done/Pending badges
- [ ] Progress bar shows correct resolved/total ratio
- [ ] Open tab shows only Open comments by default
- [ ] Resolved tab shows only Resolved comments
- [ ] All tab shows all comments
- [ ] Comment card shows: avatar initial, type badge, section, timestamp, text
- [ ] Response field shows below comment if non-empty
- [ ] Add Comment button visible to authenticated users
- [ ] Add Reviewer button visible only to QFQualityManagers

### Add Comment Form
- [ ] Form opens in sidebar
- [ ] Anchor preview shown if came from text selection
- [ ] Type field required — validation error if empty
- [ ] Comment field required — validation error if empty
- [ ] Section max 255 chars enforced
- [ ] Unauthenticated user cannot see Add Comment button
- [ ] Successful save creates XObject and refreshes sidebar

### Resolve Action
- [ ] Resolve button visible on Open comments for authenticated Reviewers/QMs
- [ ] Clicking Resolve sets status=Resolved, resolvedBy, resolvedDate
- [ ] Resolved comment moves to Resolved tab
- [ ] Highlight changes to grey after resolve
- [ ] Double-resolve shows "already resolved" notification
- [ ] Unauthorized user gets error message

### Reply / Response
- [ ] Reply form opens inline in card
- [ ] Submitting updates response field on XObject
- [ ] Response text visible in card
- [ ] Empty response is a no-op (no error, no save)

### APO Gate Bar
- [ ] Fixed bar appears at bottom of page
- [ ] Locked state when open Blockers exist
- [ ] Locked state when Pending Reviewers exist
- [ ] Unlocked state when no open Blockers and no Pending Reviewers
- [ ] Approve/Reject buttons disabled in locked state
- [ ] Approve/Reject buttons hidden for non-APO users
- [ ] Clicking Approve creates ApprovalClass XObject (decision=Approved)
- [ ] Clicking Reject requires reason text, creates XObject (decision=Rejected)
- [ ] Gate shows latest approval decision after approval

### Dashboard
- [ ] One row per document with at least one comment
- [ ] Correct counts for total, open, blockers, suggestions, questions
- [ ] Correct reviewer counts
- [ ] Last activity date correct
- [ ] Status label: Blocked / In Review / Ready / Approved / Rejected
- [ ] "No comments" message when empty
- [ ] Document name links to document page

### Access Control
- [ ] Unauthenticated user redirected to login on any action page
- [ ] User without View rights cannot see Comment_Sidebar
- [ ] User without Edit rights on Holder_Page gets authorization error
- [ ] APO buttons only visible to QFAPO group members
- [ ] Add Reviewer only visible to QFQualityManagers

### Responsive
- [ ] Sidebar collapses below 1024px width
- [ ] Toggle button appears on mobile
- [ ] Sidebar slides in/out on toggle click
- [ ] APO gate bar readable on mobile
