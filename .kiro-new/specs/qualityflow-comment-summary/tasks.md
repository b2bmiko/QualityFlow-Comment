 for the cla# Implementation Plan: QualityFlow Comment Summary — Phase 1

## Overview

Implement the QualityFlow Comment Summary xWiki plugin by creating 17 wiki pages and 3 xWiki groups entirely through the xWiki UI. No Java, no JAR, no server restart. All pages are created under the `QualityFlow` space using XClass/XObjects, Velocity macros, Groovy action pages, plain JavaScript (JSX wiki page), and CSS (SSX wiki page). The sequence follows the dependency order: groups → XClasses → CSS/JS → Groovy actions → DocumentCommentPanel → dashboard/admin/webhome.

---

## Tasks

- [x] 1. Create xWiki groups
  - [x] 1.1 Create group XWiki.QFReviewers
    - Navigate to Administration → Users & Groups → Groups → Add group
    - Name: `QFReviewers` (full ref: `XWiki.QFReviewers`)
    - Leave membership empty for now
    - Verify the group page `XWiki.QFReviewers` exists and saves without error
    - _Requirements: 10.1, 10.5, 10.6, 11.2_

  - [x] 1.2 Create group XWiki.QFQualityManagers
    - Navigate to Administration → Users & Groups → Groups → Add group
    - Name: `QFQualityManagers` (full ref: `XWiki.QFQualityManagers`)
    - Verify the group page exists and saves without error
    - _Requirements: 10.6, 11.2_

  - [x] 1.3 Create group XWiki.QFAPO
    - Navigate to Administration → Users & Groups → Groups → Add group
    - Name: `QFAPO` (full ref: `XWiki.QFAPO`)
    - Verify the group page exists and saves without error
    - _Requirements: 8.8, 10.5, 11.2_

- [x] 2. Create QualityFlow.CommentClass XClass
  - [x] 2.1 Create the CommentClass page and add all 12 XClass fields
    - Create page `QualityFlow.CommentClass` via xWiki UI (Edit → Class editor or create with type "Class page")
    - Add field `type`: StaticListClass, values `Blocker|Suggestion|Question`, mandatory, size=20
    - Add field `status`: StaticListClass, values `Open|Resolved`, default `Open`, size=10
    - Add field `section`: StringClass, max 255 chars, optional
    - Add field `anchorText`: StringClass, max 500 chars, optional
    - Add field `anchorOffset`: NumberClass, integer, optional
    - Add field `comment`: TextAreaClass, max 4000 chars, mandatory
    - Add field `author`: UsersClass, single user
    - Add field `response`: TextAreaClass, max 1000 chars, optional
    - Add field `date`: DateClass, include time
    - Add field `document`: PageClass, single reference
    - Add field `resolvedBy`: UsersClass, single user, optional
    - Add field `resolvedDate`: DateClass, include time, optional
    - Save and verify page saves without error; confirm 12 fields listed in class editor
    - Verify `status` default value is `Open`, `type` is marked mandatory, `comment` is marked mandatory
    - _Requirements: 1.1, 1.3, 1.4, 1.5_

  - [ ]* 2.2 Test CommentClass field constraints
    - Open the Class editor on `QualityFlow.CommentClass` and attempt to save an XObject with `type` empty — confirm validation error
    - Attempt to save with `comment` empty — confirm validation error
    - Confirm `status` field defaults to `Open` on a new XObject
    - _Requirements: 1.3, 1.4, 1.5_

- [x] 3. Create QualityFlow.ReviewerClass XClass
  - [x] 3.1 Create the ReviewerClass page and add all 4 XClass fields
    - Create page `QualityFlow.ReviewerClass` via xWiki UI
    - Add field `reviewer`: UsersClass, single user
    - Add field `status`: StaticListClass, values `Pending|Done`, default `Pending`, size=10
    - Add field `document`: PageClass, single reference
    - Add field `assignedDate`: DateClass, include time
    - Save and verify page saves without error; confirm 4 fields in class editor
    - Verify `status` default value is `Pending`
    - _Requirements: 1.2_

  - [ ]* 3.2 Test ReviewerClass field defaults
    - Create a test XObject of ReviewerClass on a scratch page; confirm `status` defaults to `Pending`
    - _Requirements: 1.2_

- [x] 4. Create QualityFlow.ApprovalClass XClass
  - [x] 4.1 Create the ApprovalClass page and add all 5 XClass fields
    - Create page `QualityFlow.ApprovalClass` via xWiki UI
    - Add field `decision`: StaticListClass, values `Approved|Rejected`, mandatory, size=10
    - Add field `author`: UsersClass, single user
    - Add field `date`: DateClass, include time
    - Add field `reason`: TextAreaClass, max 2000 chars, optional
    - Add field `document`: PageClass, single reference
    - Save and verify page saves without error; confirm 5 fields in class editor
    - _Requirements: 8.6, 8.7_

- [x] 5. Create QualityFlow.QFStyles (SSX — CSS)
  - [x] 5.1 Create the QFStyles page as a Style Sheet Extension
    - Create page `QualityFlow.QFStyles`
    - Set page content type to **Style Sheet Extension (SSX)** via page administration or type selector
    - Paste the full CSS from design section 8 covering: `.qf-layout`, `.qf-doc-body`, `.qf-sidebar`, sidebar header/progress, reviewer list, avatars, status badges, tabs, comment cards, buttons, forms, highlights, selection popover, APO gate bar, empty state, and responsive breakpoint (`@media (max-width: 1024px)`)
    - Save and verify the page saves without error
    - Verify the page is recognized as SSX (check via `$xwiki.ssfx.use("QualityFlow.QFStyles", true)` on a test page)
    - _Requirements: 4.1, 4.9, 8.1, 11.4_

  - [ ]* 5.2 Test CSS loading
    - On a test wiki page, add `$xwiki.ssfx.use("QualityFlow.QFStyles", true)` and render; confirm no 404 on the CSS resource and `.qf-sidebar` style rules are applied
    - _Requirements: 11.4_

- [x] 6. Create QualityFlow.QFScripts (JSX — JavaScript)
  - [x] 6.1 Create the QFScripts page as a JavaScript Extension
    - Create page `QualityFlow.QFScripts`
    - Set page content type to **JavaScript Extension (JSX)**
    - Paste the full IIFE JavaScript module from design section 7 covering: `injectHighlights()`, `wrapTextInBody()` with TreeWalker, text-selection `onMouseUp` listener, `createPopover()`, `openCommentFormWithAnchor()`, `scrollSidebarToCard()`, `qfSetTab()`, `qfShowAddCommentForm()` / `qfHideAddCommentForm()`, `qfShowReplyForm()` / `qfHideReplyForm()`, `qfShowRejectForm()` / `qfHideRejectForm()`, `qfShowAddReviewer()`, `setupResponsiveToggle()`, and the `DOMContentLoaded` init block
    - Save and verify the page saves without error
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.8, 4.11, 11.5_

  - [ ]* 6.2 Test JS loading and highlight injection
    - On a test wiki page that includes `DocumentCommentPanel`, open browser devtools; confirm `QFScripts` loads without JS errors
    - Add a comment with an `anchorText` value and reload the page; confirm a `<mark class="qf-highlight ...">` element appears around the correct text
    - _Requirements: 3.4, 3.5, 3.9_

- [x] 7. Create QualityFlow.SaveComment (Groovy action)
  - [x] 7.1 Create the SaveComment Groovy action page
    - Create page `QualityFlow.SaveComment`
    - Set the page content type to Velocity+Groovy (use `{{groovy}}` wrapper in page content)
    - Implement full Groovy from design section 9.1:
      - Auth check: redirect to login if `xcontext.userReference` is null or contains `XWikiGuest`
      - CSRF check: validate `form_token` via `services.csrf.isTokenValid()`
      - Read parameters: `docRef`, `holderRef`, `returnUrl`, `type`, `section`, `comment`, `anchorText`, `anchorOffset`
      - Validation: reject empty/invalid `type`, empty `comment`, `section` > 255 chars with redirect + `qf_error` param
      - Silently truncate `anchorText` to 500 chars if over limit (requirement 2.9)
      - Get or create Holder Page at `holderRef`; set title and placeholder content if new
      - Create new XObject of `QualityFlow.CommentClass`; set all fields; intentionally do NOT set `resolvedBy` or `resolvedDate`
      - Save with version comment; on exception redirect with `qf_error`; on success redirect with `qf_success=1`
    - Save and verify the page saves without Groovy compile errors
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 1.7_

  - [ ]* 7.2 Test SaveComment action
    - Submit a valid POST (type=Blocker, comment text, docRef, CSRF token) and confirm XObject is created on Holder Page with correct author, date, status=Open
    - Submit with empty `type` and confirm redirect includes `qf_error` parameter; no XObject created
    - Submit with empty `comment` and confirm redirect includes `qf_error`; no XObject created
    - Submit as unauthenticated user and confirm redirect to login page
    - Submit with `section` > 255 chars and confirm validation error
    - Submit with `anchorText` of 600 chars and confirm XObject stores exactly 500 chars
    - Confirm `resolvedBy` and `resolvedDate` are not set on newly created XObject
    - _Requirements: 2.1, 2.2, 2.5, 2.6, 2.7, 2.8, 2.9, 1.7_

- [x] 8. Create QualityFlow.ResolveComment (Groovy action)
  - [x] 8.1 Create the ResolveComment Groovy action page
    - Create page `QualityFlow.ResolveComment`
    - Set content type to Velocity+Groovy with `{{groovy}}` wrapper
    - Implement Groovy from design section 9.2:
      - Auth check (same pattern as SaveComment)
      - CSRF check
      - Group check: `xwiki.isAMemberOf('XWiki.QFReviewers', currentUser)` OR `xwiki.isAMemberOf('XWiki.QFQualityManagers', currentUser)` — reject with error if neither
      - Read `holderRef`, `objNum`, `returnUrl`
      - Load `holderDoc.getObject('QualityFlow.CommentClass', objNum)` — redirect with not-found error if null
      - If already Resolved, redirect with `qf_info=Comment+was+already+resolved`
      - Set `status=Resolved`, `resolvedBy=xcontext.userReference.toString()`, `resolvedDate=new Date()`
      - Save with version comment; on exception redirect with error
    - Save and verify no Groovy compile errors
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_

  - [ ]* 8.2 Test ResolveComment action
    - Resolve an Open comment as a QFReviewer; confirm status=Resolved, resolvedBy, resolvedDate all set
    - Attempt to resolve the same comment again; confirm redirect with "already resolved" notification, XObject unchanged
    - Attempt resolve as unauthenticated user; confirm redirect to login
    - Attempt resolve as a user in neither QFReviewers nor QFQualityManagers; confirm authorization error
    - Attempt resolve with invalid objNum; confirm "comment not found" redirect
    - _Requirements: 6.1, 6.3, 6.4, 6.6, 6.8_

- [x] 9. Create QualityFlow.UpdateResponse (Groovy action)
  - [x] 9.1 Create the UpdateResponse Groovy action page
    - Create page `QualityFlow.UpdateResponse`
    - Set content type to Velocity+Groovy with `{{groovy}}` wrapper
    - Implement Groovy from design section 9.3:
      - Auth check
      - CSRF check
      - Group check: user must be in QFReviewers or QFQualityManagers; reject if neither
      - Read `holderRef`, `objNum`, `response`, `returnUrl`
      - If `response` is empty/whitespace, redirect without error and without saving (requirement 7.6)
      - Truncate response to 1000 chars if over limit
      - Load XObject; if null redirect with not-found error
      - Set `response` field and save with version comment
    - Save and verify no Groovy compile errors
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8_

  - [ ]* 9.2 Test UpdateResponse action
    - Submit a valid response; confirm `response` field updated on XObject and version history entry created
    - Submit empty response; confirm no save occurs and no error shown
    - Submit response on a Resolved comment; confirm response is saved (requirement 7.3)
    - Submit as unauthenticated user; confirm redirect to login
    - Submit as user in neither group; confirm authorization error
    - _Requirements: 7.1, 7.3, 7.4, 7.5, 7.6_

- [x] 10. Create QualityFlow.SaveReviewer (Groovy action)
  - [x] 10.1 Create the SaveReviewer Groovy action page
    - Create page `QualityFlow.SaveReviewer`
    - Set content type to Velocity+Groovy with `{{groovy}}` wrapper
    - Implement Groovy following the same action pattern:
      - Auth check
      - CSRF check
      - Group check: user must be in QFQualityManagers; reject otherwise
      - Read `holderRef`, `docRef`, `reviewer` (user reference), `returnUrl`
      - Get or create Holder Page at `holderRef`
      - Create new XObject of `QualityFlow.ReviewerClass`; set `reviewer`, `status=Pending`, `document=docRef`, `assignedDate=new Date()`
      - Save with version comment; redirect with `qf_success=1` or `qf_error` on failure
    - Save and verify no Groovy compile errors
    - _Requirements: 4.4, 10.1, 10.4_

  - [ ]* 10.2 Test SaveReviewer action
    - Submit a valid reviewer assignment as a QFQualityManager; confirm ReviewerClass XObject created with status=Pending and correct assignedDate
    - Attempt as unauthenticated user; confirm redirect to login
    - Attempt as non-QFQualityManager; confirm authorization error
    - _Requirements: 4.4, 10.1_

- [x] 11. Create QualityFlow.SaveApproval (Groovy action)
  - [x] 11.1 Create the SaveApproval Groovy action page
    - Create page `QualityFlow.SaveApproval`
    - Set content type to Velocity+Groovy with `{{groovy}}` wrapper
    - Implement Groovy from design section 9.4:
      - Auth check
      - CSRF check
      - Group check: user must be in XWiki.QFAPO; reject with error if not
      - Read `holderRef`, `docRef`, `decision` (Approved|Rejected), `reason`, `returnUrl`
      - Gate check: count CommentClass XObjects with type=Blocker and status=Open; count ReviewerClass XObjects with status=Pending; if either count > 0, redirect with `qf_error=Cannot+approve:+gate+conditions+not+met`
      - Create new XObject of `QualityFlow.ApprovalClass`; set `decision`, `author=xcontext.userReference`, `date=new Date()`, `reason`, `document`
      - Save with version comment; redirect with `qf_success=1` or error
    - Save and verify no Groovy compile errors
    - _Requirements: 8.6, 8.7, 8.3, 10.1, 10.5_

  - [ ]* 11.2 Test SaveApproval action
    - Attempt approval with open Blockers; confirm gate error redirect, no XObject created
    - Attempt approval with Pending Reviewers; confirm gate error redirect
    - Approve when gate is clear (no open Blockers, no Pending Reviewers) as QFAPO member; confirm ApprovalClass XObject created with decision=Approved
    - Submit Reject with reason; confirm XObject created with decision=Rejected and reason stored
    - Attempt as unauthenticated user; confirm redirect to login
    - Attempt as non-QFAPO user; confirm authorization error
    - _Requirements: 8.3, 8.6, 8.7, 10.1, 10.5_

- [ ] 12. Checkpoint — Verify all action pages and XClasses
  - Ensure all XClass pages (CommentClass, ReviewerClass, ApprovalClass) load in the class editor without errors
  - Ensure SaveComment, ResolveComment, UpdateResponse, SaveReviewer, SaveApproval all render as Groovy pages (no wiki content shown, no compile error on page load)
  - Ask the user if any questions arise before proceeding.

- [x] 13. Create QualityFlow.DocumentCommentPanel (Velocity macro)
  - [x] 13.1 Create the DocumentCommentPanel page with full Velocity content
    - Create page `QualityFlow.DocumentCommentPanel`
    - Set syntax to **Velocity** (not wiki markup)
    - Paste the full Velocity macro from design section 6 covering:
      - `$xwiki.ssfx.use` and `$xwiki.jsfx.use` resource loading
      - `currentDocRef`, `holderName` sanitization (`.replaceAll('[^a-zA-Z0-9]', '')`), `holderRef` construction
      - Collection of all comments, open/resolved separation, `openBlockers` count
      - Collection of all reviewers, pending/done separation
      - Auth and group membership checks (`isGuest`, `isReviewer`, `isQM`, `isAPO`, `canAct`)
      - JSON data block in `<script id="qf-comments-data" type="application/json">` for JS highlight injection
      - `qf-layout` outer div with `qf-doc-body` and `qf-sidebar`
      - Sidebar header: reviewer progress label and progress bar
      - Reviewer list with avatar initials, display names, status badges, and "+ Add Reviewer" button (QM only)
      - Comment tabs (Open / Resolved / All) with counts
      - "+ Add Comment" button (authenticated users only)
      - Hidden add-comment form with CSRF token, all hidden fields (docRef, holderRef, returnUrl, anchorText, anchorOffset), anchor preview div, type select, section input, comment textarea
      - Open comment cards loop: avatar, author, type badge, anchor icon, section, timestamp, body, response, Resolve form, Reply form — all gated by `$canAct`
      - Resolved comment cards loop: same fields plus resolved-by meta, reply form
      - APO Gate bar: locked/unlocked CSS class, status message (locked message with blocker/reviewer counts, unlocked ready message, or latest approval decision), Approve form and Reject form (QFAPO only, when no prior approval)
    - Save and verify the page saves without Velocity parse errors
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 5.1, 5.2, 5.3, 5.4, 8.1, 8.2, 8.3, 8.4, 8.5, 8.8, 8.9_

  - [ ]* 13.2 Test DocumentCommentPanel rendering on a document page
    - Edit a test document and add `{{include reference="QualityFlow.DocumentCommentPanel"/}}` at the bottom; save and view
    - Confirm the `qf-layout` div is present in page source; confirm sidebar renders to the right of page content
    - Confirm CSS is injected (QFStyles loaded via ssfx)
    - Confirm JavaScript is injected (QFScripts loaded via jsfx)
    - Confirm `<script id="qf-comments-data">` JSON block is present in page source
    - Confirm APO gate bar renders at fixed bottom
    - Confirm empty-state message when no comments exist (requirement 4.10)
    - _Requirements: 4.1, 4.10, 8.1, 11.4, 11.5_

  - [ ]* 13.3 Test add-comment flow end-to-end
    - As an authenticated reviewer, click "+ Add Comment", fill in type=Suggestion and comment text, submit
    - Confirm redirect back to document page and new comment card appears in sidebar
    - Select a text passage in the document body; confirm popover appears; click "Add Comment"; confirm anchor preview shows in form; submit
    - Confirm `<mark class="qf-highlight qf-hl-suggestion">` wraps the selected text after reload
    - _Requirements: 3.2, 3.3, 3.4, 5.1, 5.2, 5.3, 5.7_

  - [ ]* 13.4 Test resolve and reply flows
    - As a QFReviewer, click "✓ Resolve" on an Open comment; confirm page reloads showing status=Resolved with resolvedBy name and date
    - Confirm resolved highlight changes to grey (`qf-hl-resolved` class)
    - Click "↩ Reply" on a comment, submit response text; confirm response appears in card
    - _Requirements: 6.1, 6.5, 6.9, 7.1, 7.7_

  - [ ]* 13.5 Test APO gate states
    - Confirm gate shows locked (🔒) when open Blockers exist; Approve/Reject buttons disabled
    - Resolve all Blockers and mark all Reviewers Done; confirm gate shows unlocked (🔓)
    - As QFAPO member, click "✓ Approve"; confirm ApprovalClass XObject created and gate shows approved state
    - Confirm Approve/Reject buttons hidden for non-QFAPO users
    - _Requirements: 8.3, 8.4, 8.5, 8.6, 8.8, 8.9_

- [x] 14. Create QualityFlow.AddComment (standalone fallback form)
  - [x] 14.1 Create the AddComment page with standalone form
    - Create page `QualityFlow.AddComment`
    - Set syntax to Velocity or wiki markup with Velocity
    - Implement a standalone HTML form that:
      - Checks authentication; redirects to login if unauthenticated
      - Reads `docRef` from URL parameter and displays it
      - Renders a `<form method="post" action="...SaveComment">` with fields: hidden `docRef`, hidden `returnUrl` (defaults to docRef URL), hidden `form_token` (`$!{services.csrf.token}`), `type` select (Blocker/Suggestion/Question), `section` text input (max 255), `comment` textarea (max 4000), submit button
      - Displays any `qf_error` parameter from URL as an error message
    - Save and verify page saves and renders the form for an authenticated user
    - _Requirements: 5.1, 5.2, 5.4, 5.5, 5.6, 5.8_

  - [ ]* 14.2 Test AddComment standalone page
    - Navigate to the page as an authenticated user; confirm form renders with correct fields
    - Navigate as unauthenticated; confirm redirect to login
    - Submit with empty type; confirm validation error (browser-level `required` attribute) and no XObject created
    - _Requirements: 5.5, 5.6_

- [x] 15. Create QualityFlow.AddReviewer (add reviewer form page)
  - [x] 15.1 Create the AddReviewer page with reviewer assignment form
    - Create page `QualityFlow.AddReviewer`
    - Set syntax to Velocity
    - Implement:
      - Auth check; group check (must be QFQualityManagers); show error if not authorized
      - Read `docRef` from URL parameter; compute `holderRef` using same sanitization as DocumentCommentPanel
      - Render `<form method="post" action="...SaveReviewer">` with: hidden `holderRef`, hidden `docRef`, hidden `returnUrl`, hidden `form_token`, a UsersClass-style user picker or plain text input for `reviewer`, a submit button labeled "Assign Reviewer"
      - On `qf_success=1` in URL, show confirmation message
    - Save and verify page renders without errors
    - _Requirements: 4.4, 10.6_

  - [ ]* 15.2 Test AddReviewer page
    - Navigate as a QFQualityManager with a `docRef` parameter; confirm form renders
    - Submit a reviewer user reference; confirm ReviewerClass XObject created on the Holder Page with status=Pending
    - Navigate as a non-QFQualityManager; confirm error or redirect
    - _Requirements: 4.4, 10.6_

- [x] 16. Create QualityFlow.CommentTemplate (holder page template)
  - [x] 16.1 Create the CommentTemplate page
    - Create page `QualityFlow.CommentTemplate`
    - Mark the page as a template (xWiki template mechanism: set the `xwiki:XWiki.TemplateProviderClass` XObject or use the Template Provider administration)
    - Set page content to a minimal placeholder: `Comments holder page for {{velocity}}$doc.title{{/velocity}}. Do not edit manually.`
    - Set the title template to `QualityFlow Comments: {{velocity}}$request.getParameter('title'){{/velocity}}`
    - Save and verify the page is recognized as a template
    - _Requirements: 11.1, 11.3_

- [ ] 17. Checkpoint — Verify core plugin is functional
  - Include `QualityFlow.DocumentCommentPanel` on a real target document page
  - Confirm full add-comment → view → resolve → reply → APO gate cycle works without errors
  - Confirm xWiki version history records a new entry after each action
  - Confirm all three XClass types (CommentClass, ReviewerClass, ApprovalClass) can be queried via XWQL on the Holder Page
  - Ask the user if any questions arise before proceeding.

- [x] 18. Create QualityFlow.CommentDashboard (Groovy + Velocity dashboard)
  - [x] 18.1 Create the CommentDashboard page with XWQL aggregation
    - Create page `QualityFlow.CommentDashboard`
    - Set syntax to allow both `{{groovy}}` and `{{velocity}}` blocks (xWiki Wiki syntax 2.1 with scripting rights)
    - Implement Groovy block from design section 10:
      - XWQL query: `select distinct doc.fullName from Document doc, doc.object(QualityFlow.CommentClass) as c order by doc.date desc`
      - For each holder page: collect all CommentClass and ReviewerClass and ApprovalClass XObjects
      - Compute: total, openCount, blockers, suggestions, questions, reviewerTotal, reviewersDone, pendingReviewers, lastActivity (max of all dates), statusLabel (Approved|Rejected|Blocked|In Review|Ready)
      - Derive `docRef` from first CommentClass XObject's `document` field
      - Put result list in `xcontext` as `qfRows`
    - Implement Velocity block:
      - If `$qfRows.isEmpty()`, show `{{info}}No comments have been submitted yet.{{/info}}`
      - Otherwise render a Markdown/wiki table with columns: Document (linked), Total, Open, Blockers, Suggestions, Questions, Reviewers (Done/Total), Last Activity (yyyy-MM-dd), Status
    - Save and verify the page renders the table (or empty-state) without errors
    - _Requirements: 9.1, 9.2, 9.3, 9.5, 9.6, 9.7_

  - [ ]* 18.2 Test CommentDashboard data accuracy
    - With at least two documents having comments, load the dashboard; confirm one row per document
    - Confirm counts match actual XObjects (total, open, blockers, suggestions, questions)
    - Confirm reviewer counts and last activity date are correct
    - Confirm status label transitions: Blocked when open Blocker exists; Ready when clear; Approved after approval recorded
    - Confirm empty state message when no CommentClass XObjects exist
    - _Requirements: 9.1, 9.2, 9.3, 9.7_

- [x] 19. Create QualityFlow.Admin (health check page)
  - [x] 19.1 Create the Admin page with XObject count and health checks
    - Create page `QualityFlow.Admin`
    - Set syntax to allow `{{groovy}}` and `{{velocity}}`
    - Implement Groovy block:
      - Count all CommentClass XObjects across all Holder Pages via XWQL: `select count(obj) from Document doc, doc.object(QualityFlow.CommentClass) as obj`
      - Count all ReviewerClass XObjects similarly
      - Count all ApprovalClass XObjects similarly
      - Check that CommentClass, ReviewerClass, and ApprovalClass pages exist; if any are missing, set a warning flag
      - Put counts and flags in `xcontext`
    - Implement Velocity block:
      - Show counts: "Total comments: N", "Total reviewer records: N", "Total approvals: N"
      - If any XClass is missing, display a warning: "WARNING: QualityFlow.{ClassName} not found — please create the XClass before using the plugin."
      - Links: "→ View Dashboard" (to CommentDashboard), "→ View Space Home" (to WebHome)
    - Save and verify the page renders counts without errors
    - _Requirements: 1.6, 11.1_

  - [ ]* 19.2 Test Admin page
    - Load the Admin page; confirm counts display and match known XObject totals
    - Temporarily rename CommentClass and reload Admin; confirm warning message appears (restore after test)
    - Confirm links to CommentDashboard and WebHome work
    - _Requirements: 1.6_

- [x] 20. Create QualityFlow.WebHome (space home and installation guide)
  - [x] 20.1 Create the WebHome page with full installation guide
    - Create page `QualityFlow.WebHome`
    - Set syntax to xWiki Wiki 2.1 markup
    - Write the installation guide covering all steps from design section 12:
      1. Prerequisites: xWiki 14.10+, admin access
      2. Create the three groups (QFReviewers, QFQualityManagers, QFAPO) — link to Administration
      3. Create XClass pages in order (CommentClass → ReviewerClass → ApprovalClass) with field reference table
      4. Create QFStyles as SSX and QFScripts as JSX — link to each page
      5. Create Groovy action pages in order (SaveComment → ResolveComment → UpdateResponse → SaveReviewer → SaveApproval)
      6. Create DocumentCommentPanel — link to page
      7. Create CommentDashboard, Admin pages
      8. Set space rights for all groups (View + Edit for QFReviewers, QFQualityManagers, QFAPO)
      9. Include DocumentCommentPanel on a target document: `{{include reference="QualityFlow.DocumentCommentPanel"/}}`
      10. Optional: add `<button id="qf-sidebar-toggle">💬</button>` for responsive mobile toggle
    - Add upgrade warning section: list pages that may contain user-customized content (WebHome, AddComment, DocumentCommentPanel, QFStyles, QFScripts, Admin) per requirement 11.7
    - Add quick links section: Dashboard | Admin | CommentClass | DocumentCommentPanel
    - Save and verify the page renders and all links resolve
    - _Requirements: 11.1, 11.2, 11.3, 11.6, 11.7_

  - [ ]* 20.2 Test WebHome page
    - Load the page as a non-admin user with View rights; confirm page renders
    - Click through all quick links; confirm no broken links
    - _Requirements: 11.6_

- [x] 21. Configure space rights
  - [x] 21.1 Set QualityFlow space rights for all groups
    - Navigate to QualityFlow space → Administer Space → Rights
    - Grant **View + Edit** to: `XWiki.QFReviewers`, `XWiki.QFQualityManagers`, `XWiki.QFAPO`
    - Grant **View + Edit + Admin + Delete** to `XWiki.XWikiAdminGroup`
    - Save and verify rights configuration
    - _Requirements: 10.2, 10.3, 10.4, 10.7_

  - [ ]* 21.2 Test access control
    - Log in as a user in QFReviewers; confirm View access to all QualityFlow pages and ability to submit comments
    - Log in as an unauthenticated user (or a user with no QualityFlow rights); confirm no sidebar rendered, no action pages accessible (requirement 10.2)
    - Confirm that a QFReviewer cannot see Approve/Reject buttons in the APO gate (requirement 10.5)
    - Confirm that only QFQualityManagers see the "+ Add Reviewer" button (requirement 10.6)
    - _Requirements: 10.1, 10.2, 10.5, 10.6_

- [ ] 22. Final checkpoint — Full system verification
  - Ensure all 17 pages exist under the QualityFlow space and all 3 groups exist
  - Ensure the complete end-to-end flow works: include DocumentCommentPanel on a document → add Blocker comment with text anchor → confirm highlight → resolve comment → confirm grey highlight and resolved state → add reviewer → mark reviewer Done → APO approves → gate shows approved
  - Ensure CommentDashboard shows the document row with correct status
  - Ensure Admin page shows correct XObject counts
  - Ask the user if any questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster initial setup; the core plugin functionality is fully testable using the manual testing checklist in the design document (section 13).
- All Groovy action pages require the page to be saved with scripting rights; ensure the xWiki instance grants script rights to the QualityFlow space or use the admin user for page creation.
- The `holderRef` sanitization must be consistent across DocumentCommentPanel (Velocity), SaveComment, and all other action pages — always `docRef.replaceAll('[^a-zA-Z0-9]', '')` prefixed with `QualityFlow.Comments.`.
- CSS is loaded via `$xwiki.ssfx.use("QualityFlow.QFStyles", true)` and JS via `$xwiki.jsfx.use("QualityFlow.QFScripts", true)` — both require the pages to be of SSX and JSX type respectively.
- The APO gate `SaveApproval` action enforces the gate check server-side even though the buttons are disabled client-side — this is a defense-in-depth measure.
- xWiki version history is recorded automatically on every `holderDocument.save(...)` call; no additional implementation is needed.
- CommentDashboard requires script rights to execute the XWQL query; ensure rights are configured accordingly.
- Responsive sidebar toggle button (`<button id="qf-sidebar-toggle">💬</button>`) should be added to target document pages that need mobile support.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "3.1", "4.1"] },
    { "id": 2, "tasks": ["2.2", "3.2", "5.1", "6.1"] },
    { "id": 3, "tasks": ["5.2", "6.2", "7.1", "8.1", "9.1", "10.1", "11.1"] },
    { "id": 4, "tasks": ["7.2", "8.2", "9.2", "10.2", "11.2", "16.1"] },
    { "id": 5, "tasks": ["13.1", "14.1", "15.1"] },
    { "id": 6, "tasks": ["13.2", "13.3", "13.4", "13.5", "14.2", "15.2"] },
    { "id": 7, "tasks": ["18.1", "19.1", "20.1", "21.1"] },
    { "id": 8, "tasks": ["18.2", "19.2", "20.2", "21.2"] }
  ]
}
```
