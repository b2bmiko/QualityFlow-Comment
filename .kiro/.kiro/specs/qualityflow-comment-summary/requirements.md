# Requirements Document

## Introduction

QualityFlow Comment Summary is an xWiki plugin (Phase 1) that replaces the manual Excel-based comment consolidation step in software document quality reviews (FQP / QD). Reviewers currently annotate Word documents in SharePoint, email comments via Outlook, and a Quality Manager manually compiles them into an Excel tracker before APO approval.

This plugin adds three integrated components directly to any xWiki document page:
1. **Inline text highlighting** — reviewers select text in the document body and anchor a structured comment to it
2. **Right comment sidebar** — a structured panel showing all comments for the document with type badges, reviewer status, and resolve/reply actions
3. **APO Approval Gate bar** — a fixed bottom bar showing blocking conditions and approval actions

The left-side workflow navigation (Drafts, In Review, Approvals, Archive) is explicitly out of scope for Phase 1. OpenProject will be integrated in a later phase to handle workflow state transitions.

All data is stored natively in xWiki using XClass/XObjects. The UI is implemented using Velocity macros, Groovy action pages, plain JavaScript (stored in a wiki page), and CSS — no Java extensions, no JAR deployment, no server restart required.

## Glossary

- **Plugin**: The QualityFlow Comment Summary xWiki application — XClass definitions, Velocity/Groovy pages, JavaScript, and CSS.
- **XClass**: An xWiki class definition (`QualityFlow.CommentClass`) declaring the data schema for a structured comment.
- **XObject**: A single instance of `QualityFlow.CommentClass` stored on a Holder_Page, representing one comment.
- **Comment**: A structured review annotation stored as an XObject with type, status, section, anchor text, author, and date.
- **Blocker**: A comment type indicating a mandatory change required before document approval.
- **Suggestion**: A comment type indicating an optional improvement.
- **Question**: A comment type indicating a clarification request.
- **Open**: A comment status indicating the comment has not been resolved.
- **Resolved**: A comment status indicating the comment has been addressed and closed.
- **Reviewer**: A user who reads a document and submits structured comments.
- **Quality_Manager**: A user responsible for tracking all comments across documents before APO approval.
- **APO**: The approval authority who gives final sign-off after all Blockers are resolved and all Reviewers are Done.
- **APO_Gate**: The fixed bottom bar on a document page showing blocking conditions and Approve/Reject actions.
- **Document_Ref**: The full xWiki page reference (space.page) of the reviewed xWiki document.
- **Comment_Sidebar**: The right-panel UI component injected by the DocumentCommentPanel macro, showing comment cards, reviewer status, and progress.
- **Holder_Page**: The xWiki page on which XObjects for a document's comments are stored, under `QualityFlow.Comments.<DocumentRef>`.
- **Reviewer_Record**: An XObject of type `QualityFlow.ReviewerClass` stored on the Holder_Page, representing one reviewer's assignment and status.
- **Anchor**: The text passage in the document body that a comment is linked to. Stored as the selected text string and character offset.
- **Highlight**: A visual `<mark>` element injected into the document body to indicate where a comment is anchored.
- **Current_User**: The xWiki user currently authenticated and performing an action.
- **Version_History**: The xWiki built-in page versioning mechanism recording each save as a recoverable version.
- **LiveData**: The native xWiki LiveData macro for tabular XObject data with sorting and filtering.
- **SaveComment**: The Groovy action page (`QualityFlow.SaveComment`) handling new comment form submission.
- **ResolveComment**: The Groovy action page (`QualityFlow.ResolveComment`) changing a comment's status to Resolved.
- **UpdateResponse**: The Groovy action page (`QualityFlow.UpdateResponse`) saving a reply to a comment's response field.

---

## Requirements

### Requirement 1: XClass Definition

**User Story:** As a Quality_Manager, I want a well-defined data schema for structured comments and reviewer assignments, so that all review data is consistently structured and queryable across xWiki.

#### Acceptance Criteria

1. THE Plugin SHALL define the XClass `QualityFlow.CommentClass` with exactly the following fields: `type` (StaticListClass, values: Blocker|Suggestion|Question, mandatory), `status` (StaticListClass, values: Open|Resolved, default: Open), `section` (StringClass, max 255 chars, optional), `anchorText` (StringClass, max 500 chars — the selected text passage the comment is anchored to, optional), `anchorOffset` (NumberClass, integer — character offset of the anchor in the document body, optional), `comment` (TextAreaClass, max 4000 chars, mandatory), `author` (UsersClass, single user, auto-filled), `response` (TextAreaClass, max 1000 chars, optional), `date` (DateClass, include time, auto-filled), `document` (PageClass, single page reference, auto-filled), `resolvedBy` (UsersClass, single user, optional), `resolvedDate` (DateClass, include time, optional).
2. THE Plugin SHALL define the XClass `QualityFlow.ReviewerClass` with the following fields: `reviewer` (UsersClass, single user), `status` (StaticListClass, values: Pending|Done, default: Pending), `document` (PageClass, single page reference), `assignedDate` (DateClass, include time, auto-filled).
3. THE Plugin SHALL set the default value of the `status` field on `QualityFlow.CommentClass` to `Open`.
4. IF the `type` field is empty when a save is attempted, THEN the save SHALL be rejected with a validation error.
5. IF the `comment` field is empty when a save is attempted, THEN the save SHALL be rejected with a validation error.
6. IF `QualityFlow.CommentClass` or `QualityFlow.ReviewerClass` already exist when the Plugin is installed or upgraded, THEN THE Plugin SHALL leave those XClass definitions unchanged and SHALL display a warning on `QualityFlow.Admin`.
7. WHEN the SaveComment or UpdateResponse action processes a form submission, THEN THE Plugin SHALL silently discard any submitted values for `resolvedBy` or `resolvedDate` and SHALL NOT write them to the XObject.

---

### Requirement 2: Comment Storage

**User Story:** As a Reviewer, I want my comments stored inside xWiki using native structures, so that no external database is required and xWiki rights and versioning apply automatically.

#### Acceptance Criteria

1. WHEN a Reviewer submits a valid new comment, THE SaveComment action SHALL create a new XObject of type `QualityFlow.CommentClass` on the Holder_Page. IF the Holder_Page does not exist, THE SaveComment action SHALL create it first.
2. WHEN THE SaveComment action creates a new XObject, it SHALL set `author` to the Current_User's fully-qualified xWiki user reference, `date` to the server-side timestamp, `document` to the Document_Ref, and `status` to `Open`.
3. IF the Reviewer selected a text passage before submitting, THEN THE SaveComment action SHALL store the selected text in `anchorText` and the character offset in `anchorOffset` on the XObject.
4. WHEN a comment XObject is saved to a Holder_Page, THE Plugin SHALL record a new Version_History entry containing the full prior page state.
5. IF the Current_User is not authenticated, THEN THE SaveComment action SHALL redirect to the xWiki login page without creating any XObject.
6. IF the submitted `type` field is empty or invalid, THEN THE SaveComment action SHALL return a validation error and preserve all entered field values.
7. IF the submitted `comment` field is empty, THEN THE SaveComment action SHALL return a validation error and preserve all entered field values.
8. IF the `section` field exceeds 255 characters, THEN THE SaveComment action SHALL return a validation error and SHALL NOT create a new XObject.
9. IF the `anchorText` field exceeds 500 characters, THEN THE SaveComment action SHALL truncate it to 500 characters before saving, without error.
10. IF the XObject write fails, THEN THE SaveComment action SHALL not create a partial XObject, SHALL display an error message, and SHALL redirect to the originating document page.

---

### Requirement 3: Inline Text Highlight and Anchor

**User Story:** As a Reviewer, I want to select a passage of text in the document and anchor my comment to it, so that readers can see exactly which part of the document I am commenting on.

#### Acceptance Criteria

1. WHILE the Current_User is authenticated and the Comment_Sidebar is active on a document page, THE Plugin SHALL load a JavaScript module that listens for mouse-up events on the document body.
2. WHEN the Current_User releases the mouse after selecting a non-empty text passage in the document body, THE JavaScript module SHALL display an "Add Comment" popover near the selected text containing a button to open the comment form.
3. WHEN the Current_User clicks "Add Comment" from the selection popover, THE JavaScript module SHALL capture the selected text string and its character offset, pre-fill the `anchorText` and `anchorOffset` hidden fields in the comment form, and open the comment form in the Comment_Sidebar.
4. WHEN a comment XObject with a non-empty `anchorText` field is rendered on a document page, THE JavaScript module SHALL locate the first occurrence of that text string in the document body and wrap it in a `<mark class="qf-highlight">` element.
5. THE Plugin SHALL assign each highlight a unique `data-comment-id` attribute so that clicking the highlight scrolls the Comment_Sidebar to the corresponding comment card.
6. IF two or more comments share the same `anchorText` value, THEN THE JavaScript module SHALL apply highlights to all matching occurrences and link each to its respective comment card.
7. IF the `anchorText` value of a comment cannot be found in the current document body (e.g., the document was edited after the comment was created), THEN THE JavaScript module SHALL skip the highlight without error and the comment SHALL still be visible in the Comment_Sidebar without a highlight indicator.
8. WHEN the Current_User clicks a `<mark>` highlight in the document body, THE Comment_Sidebar SHALL scroll to and visually focus the corresponding comment card.
9. THE `<mark>` highlight color SHALL differ by comment type: red-tinted for Blocker, blue-tinted for Suggestion, yellow-tinted for Question.

---

### Requirement 4: Right Comment Sidebar

**User Story:** As a Reviewer, I want a structured comment panel alongside the document I am reading, so that I can see all review feedback, reviewer statuses, and take actions without leaving the page.

#### Acceptance Criteria

1. THE Comment_Sidebar SHALL be injected to the right of the document body on any page where `QualityFlow.DocumentCommentPanel` is included, without modifying the document content itself.
2. THE Comment_Sidebar SHALL display at the top: the count of assigned Reviewers, the count with `status` = `Done`, and a progress bar showing the ratio of resolved comments to total comments (e.g., "4 / 7").
3. THE Comment_Sidebar SHALL display a Reviewer list showing each assigned Reviewer's display name and status badge (Done in green, Pending in amber).
4. THE Comment_Sidebar SHALL display an "Add Reviewer" button visible to Quality_Manager role users; clicking it SHALL open a form to assign a new Reviewer to the document.
5. THE Comment_Sidebar SHALL display comment tabs: Open / Resolved / All, each showing the count. The default active tab SHALL be Open.
6. WHEN a tab is selected, THE Comment_Sidebar SHALL display only comments matching that status filter.
7. WHILE rendering each comment card, THE Comment_Sidebar SHALL display: the reviewer's avatar initial and display name, the type badge (BLOCKER / SUGGESTION / QUESTION) with appropriate color, the `section` value if non-empty, the time elapsed since `date` (e.g., "2h ago"), the full `comment` text, and — if `anchorText` is non-empty — a small anchor icon linking back to the highlight in the document body.
8. WHILE the Current_User is authenticated, THE Comment_Sidebar SHALL display a "Resolve" button on each Open comment card and a "Reply" action on every comment card regardless of status.
9. IF a comment card has a non-empty `response` field, THE Comment_Sidebar SHALL display the response text beneath the comment text within the same card.
10. IF the current document has zero comments, THE Comment_Sidebar SHALL display "No comments yet. Select text or click Add Comment to get started."
11. THE Comment_Sidebar SHALL be responsive: on screens narrower than 1024px it SHALL collapse to a toggle button that slides the panel in/out.

---

### Requirement 5: Add Comment Form

**User Story:** As a Reviewer, I want a simple form in the sidebar to submit a structured comment, with the anchor pre-filled when I selected text first.

#### Acceptance Criteria

1. THE Comment_Sidebar SHALL display an "Add Comment" button always visible at the top of the sidebar for authenticated users, in addition to the text-selection popover.
2. WHEN the Reviewer opens the comment form (via popover or sidebar button), THE form SHALL display: `type` (required, static list: Blocker / Suggestion / Question), `section` (optional, short text, max 255 chars), `comment` (required, textarea, max 4000 chars).
3. IF the form was opened from a text selection, THE form SHALL display the captured `anchorText` value in a read-only "Anchored to" field so the Reviewer can confirm which passage is linked.
4. THE form SHALL NOT display `author`, `date`, `document`, `status`, `response`, `resolvedBy`, or `resolvedDate` fields to the Reviewer.
5. IF the Reviewer submits the form with `type` empty, THE form SHALL display a validation error and SHALL NOT submit.
6. IF the Reviewer submits the form with `comment` empty, THE form SHALL display a validation error and SHALL NOT submit.
7. WHEN the SaveComment action completes successfully, THE Comment_Sidebar SHALL reload the comment list and display the new comment card without a full page reload where possible; if a full reload is required, THE Plugin SHALL redirect to the originating document page.
8. IF the SaveComment action returns an error, THE form SHALL display the error and preserve all entered field values.

---

### Requirement 6: Resolve Action

**User Story:** As a Quality_Manager or Reviewer, I want to resolve an open comment, so that the team can track which issues have been addressed.

#### Acceptance Criteria

1. WHEN the Current_User clicks "Resolve" on a comment card AND the comment's `status` equals `Open`, THE ResolveComment action SHALL change `status` to `Resolved`, set `resolvedBy` to the Current_User's xWiki user reference, and set `resolvedDate` to the server-side timestamp.
2. WHEN THE ResolveComment action saves, THE Plugin SHALL record a new Version_History entry containing the full prior page state.
3. IF the Current_User is not authenticated, THEN THE ResolveComment action SHALL redirect to the xWiki login page without modifying any XObject.
4. IF the comment's `status` is already `Resolved`, THEN THE ResolveComment action SHALL take no action and redirect with a notification that the comment was already resolved.
5. WHEN THE ResolveComment action completes successfully, THE Plugin SHALL redirect to the originating document page displaying the comment with `status` = `Resolved`, `resolvedBy` display name, and `resolvedDate` visible in the card.
6. IF the Current_User is neither a Quality_Manager nor a Reviewer, THEN THE ResolveComment action SHALL reject the request with an authorization error without modifying any XObject.
7. IF the XObject write fails, THEN THE ResolveComment action SHALL leave `status`, `resolvedBy`, and `resolvedDate` unchanged and display an error message.
8. IF the target XObject does not exist, THEN THE ResolveComment action SHALL redirect with a notification that the comment could not be found.
9. WHEN a comment is resolved, THE highlight in the document body for that comment SHALL change to a muted grey color to indicate it is no longer open.

---

### Requirement 7: Reply / Update Response Action

**User Story:** As a Reviewer or Quality_Manager, I want to add or update a textual response to a comment, so that dialogue between reviewer and author is recorded in xWiki.

#### Acceptance Criteria

1. WHEN the Current_User submits a non-empty response text (1–1000 characters) via the Reply action, THE UpdateResponse action SHALL replace the `response` field of the target XObject with the submitted text.
2. WHEN saving a response, THE Plugin SHALL record a new Version_History entry containing the full prior page state.
3. THE UpdateResponse action SHALL accept and save a response regardless of the comment's `status`.
4. IF the Current_User is not authenticated, THEN THE UpdateResponse action SHALL redirect to the xWiki login page without modifying any XObject.
5. IF the Current_User is authenticated but is neither a Reviewer nor a Quality_Manager, THEN THE UpdateResponse action SHALL reject the request with an authorization error.
6. IF the submitted response text is empty or whitespace-only, THEN THE UpdateResponse action SHALL take no action and return to the document page without error.
7. WHEN THE UpdateResponse action completes successfully, THE Plugin SHALL redirect to the originating document page displaying the updated response text in the comment card.
8. IF the XObject write fails, THEN THE UpdateResponse action SHALL leave the `response` field unchanged and display an error message.

---

### Requirement 8: APO Approval Gate Bar

**User Story:** As a Quality_Manager and APO, I want a fixed bar at the bottom of the document page showing the current approval readiness, so that I can see at a glance whether the document is ready for sign-off.

#### Acceptance Criteria

1. THE APO_Gate SHALL be rendered as a fixed bar at the bottom of the document page on any page where `QualityFlow.DocumentCommentPanel` is included.
2. THE APO_Gate SHALL display: the count of unresolved Blockers, the count of Reviewers with `status` = `Pending`, and a locked/unlocked state indicator.
3. THE APO_Gate SHALL be in **locked state** if any of the following conditions are true: (a) one or more comments with `type` = `Blocker` and `status` = `Open` exist, OR (b) one or more Reviewer_Records with `status` = `Pending` exist.
4. WHEN the APO_Gate is in locked state, THE APO_Gate SHALL display a message listing the specific blocking conditions (e.g., "3 blockers must be resolved — Tom Decker review pending") and the Approve and Reject buttons SHALL be disabled.
5. WHEN the APO_Gate is in unlocked state (no open Blockers and no Pending Reviewers), THE APO_Gate SHALL display "All blockers resolved. All reviewers complete. Ready for approval." and enable the Approve and Reject buttons.
6. WHEN the APO clicks "Approve", THE Plugin SHALL record an approval action (author, timestamp, decision = Approved) as an XObject of type `QualityFlow.ApprovalClass` on the Holder_Page and SHALL display a confirmation message.
7. WHEN the APO clicks "Reject", THE Plugin SHALL record a rejection action (author, timestamp, decision = Rejected, reason text) as an XObject of type `QualityFlow.ApprovalClass` on the Holder_Page and SHALL display a confirmation message.
8. IF the Current_User does not have the APO role (as defined by xWiki group membership), THEN the Approve and Reject buttons SHALL be hidden from the APO_Gate, even in unlocked state.
9. THE APO_Gate SHALL display the most recent approval decision (Approved / Rejected / Pending) if one exists for the current document version.

---

### Requirement 9: Comment Dashboard

**User Story:** As a Quality_Manager, I want a single dashboard page showing comment statistics across all reviewed documents, so that I can track overall review progress and replace the manual Excel consolidation.

#### Acceptance Criteria

1. THE Dashboard SHALL display one row per document that has at least one `QualityFlow.CommentClass` XObject.
2. WHEN displaying each row, THE Dashboard SHALL show: Document name (linked), total comments, Open count, Blocker count, Suggestion count, Question count, Reviewer count, Reviewers Done count, last activity date (YYYY-MM-DD), and APO Gate status label.
3. THE APO Gate status label per document SHALL be: "Blocked" if any Open Blocker exists; "In Review" if any Open comment or Pending Reviewer exists; "Ready" if no Open Blockers and no Pending Reviewers but no approval recorded; "Approved" or "Rejected" based on the most recent `QualityFlow.ApprovalClass` XObject.
4. THE Dashboard SHALL use the xWiki LiveData macro to render the document table with column sorting and filtering.
5. THE Dashboard SHALL be accessible at `QualityFlow.CommentDashboard`.
6. WHEN a comment is added, resolved, or an approval is recorded, THE Dashboard SHALL reflect updated statistics on the next full page load without a manual refresh.
7. IF no `QualityFlow.CommentClass` XObjects exist, THE Dashboard SHALL display "No comments have been submitted yet."

---

### Requirement 10: Access Control

**User Story:** As an administrator, I want all comment and approval actions to respect xWiki's native rights model, so that only authorized users can submit, resolve, approve, or view comments.

#### Acceptance Criteria

1. WHEN a user who is not authenticated attempts to access SaveComment, ResolveComment, UpdateResponse, or any approval action, THE Plugin SHALL redirect to the xWiki login page without executing any data-modification logic.
2. IF a user does not have View rights on a document page, THE Comment_Sidebar and APO_Gate SHALL NOT be rendered on that page.
3. IF a user does not have View rights on the `QualityFlow` space, THE Dashboard SHALL NOT be rendered.
4. IF an authenticated user does not have Edit rights on a Holder_Page, THE action pages SHALL return an authorization error and SHALL NOT modify any XObject.
5. THE Approve and Reject buttons in the APO_Gate SHALL only be visible to users in the APO xWiki group.
6. THE "Add Reviewer" button in the Comment_Sidebar SHALL only be visible to users in the Quality_Manager xWiki group.
7. THE Plugin SHALL NOT implement any custom authentication or authorization mechanism outside of the xWiki native rights model.

---

### Requirement 11: Page Structure and Installation

**User Story:** As an administrator, I want a documented and predictable page structure, so that I can install, update, and maintain the plugin without breaking existing xWiki content.

#### Acceptance Criteria

1. THE Plugin SHALL organize all pages under the `QualityFlow` space: `WebHome`, `CommentClass`, `ReviewerClass`, `ApprovalClass`, `CommentTemplate`, `AddComment`, `SaveComment`, `ResolveComment`, `UpdateResponse`, `AddReviewer`, `SaveReviewer`, `SaveApproval`, `DocumentCommentPanel`, `CommentDashboard`, `QFStyles`, `QFScripts`, `Admin`.
2. THE Plugin SHALL be installable solely by creating xWiki pages via the UI or importing a XAR file — no Java code, no JAR deployment, no server restart required.
3. WHEN THE Plugin is installed, THE Plugin SHALL NOT create, modify, or delete any pages outside the `QualityFlow` space.
4. THE `QualityFlow.QFStyles` page SHALL contain all CSS for the Comment_Sidebar, comment card badges, highlight colors, and APO_Gate bar.
5. THE `QualityFlow.QFScripts` page SHALL contain all JavaScript for text selection detection, highlight injection, and sidebar scroll-to-comment behavior.
6. THE Plugin SHALL provide step-by-step installation instructions on `QualityFlow.WebHome` covering: prerequisite xWiki version (14.10+), page creation order, group setup (Reviewer, Quality_Manager, APO, administrator), rights configuration, and how to include `QualityFlow.DocumentCommentPanel` on a target document page.
7. WHEN upgrading, THE Plugin SHALL display a notice on `QualityFlow.WebHome` warning before replacing `WebHome`, `AddComment`, `DocumentCommentPanel`, `QFStyles`, `QFScripts`, or `Admin` pages, as these may contain user-customized content.

---

## Out of Scope — Phase 1

The following are explicitly deferred to later phases:

- Left-side workflow navigation (Drafts, In Review, Approvals, Archive states)
- Workflow state machine and document lifecycle management
- OpenProject integration and task/issue synchronization
- Email notifications for new comments or approvals
- Multi-language / localization support
- Mobile-native app experience
