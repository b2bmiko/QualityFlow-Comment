# QualityFlow Comment Summary — High-Level Requirements

Use this document as a prompt to recreate or extend the QualityFlow xWiki plugin from scratch.

---

## What It Is

A structured document review extension for xWiki that replaces manual Excel-based comment tracking. Reviewers annotate wiki pages with typed, text-anchored comments. Quality Managers track progress. An Approval Authority (APO) provides final sign-off.

---

## Core Requirements

### 1. Inline Text-Anchored Comments

- Users select text on any wiki page and anchor a structured comment to it
- Selected text is highlighted (light yellow) with a colored bottom border indicating comment type
- Clicking a highlight scrolls the sidebar to the matching comment card
- Clicking a comment's anchor quote scrolls the page to the highlighted text
- Comments have a type (Blocker, Suggestion, Question), optional section label, and free-text body

### 2. Right-Side Comment Sidebar

- Injected on any enabled page via a single `{{include}}` reference
- Shows reviewer progress, comment counts, and tabbed comment list (Open / Resolved / All)
- Each comment card displays: author avatar, type badge, anchor quote, timestamp, comment text, response, and action buttons
- Authenticated users can add comments, resolve them, and reply

### 3. Role-Based Access Control (3 groups)

| Role | Group | Can Do |
|------|-------|--------|
| Reviewer | XWiki.QFReviewers | Add comments, resolve, reply |
| Quality Manager | XWiki.QFQualityManagers | All reviewer actions + assign reviewers + mark done + archive |
| APO | XWiki.QFAPO | Approve or reject documents (when gate unlocked) |

### 4. APO Approval Gate

- Fixed bar at bottom of page showing approval readiness
- Locked when: any open Blocker exists OR any reviewer is still Pending
- Unlocked when: all blockers resolved AND all reviewers marked Done
- APO can approve (optional comment) or reject (required reason) from the gate bar
- Gate state visible to all users; action buttons only visible to APO

### 5. Reviewer Assignment and Tracking

- Quality Manager assigns reviewers from the sidebar (from QFReviewers group members)
- Each reviewer has a Pending/Done status
- Quality Manager marks reviewers as Done when satisfied
- Reviewer progress shown in sidebar header

### 6. Comment Dashboard

- Central page showing all documents with active comments
- Per-document row: total comments, open count, blockers, reviewer progress, status
- CSV export per document
- "Archive All" button (QM only) to close a review cycle

### 7. Archive System

- Bulk archive moves all active comments/reviewers/approvals to timestamped archive pages
- Archive Dashboard lists past review cycles
- Export archived cycles as Word (.doc) or HTML/PDF
- After archiving, the active dashboard is clean for a new review cycle

### 8. One-Click Enable/Disable

- Admin page where you paste a wiki page URL and click Enable
- Appends `{{include reference="QualityFlow.EmbedSnippet"/}}` to the target page
- All pages share a single EmbedSnippet — updates apply everywhere automatically
- Disable removes the include reference

### 9. Data Storage

- All data stored as xWiki XObjects (CommentClass, ReviewerClass, ApprovalClass)
- Holder pages under `QualityFlow.Comments.<sanitized-doc-ref>`
- No external database, no Java code, no JAR deployment, no server restart
- xWiki's built-in versioning tracks all changes

### 10. No External Dependencies

- Pure wiki-scripting: Groovy, Velocity, HTML, CSS, JavaScript
- Requires xWiki 14.10+ with Programming rights for the installing user
- Works on any standard xWiki instance — on-premise, no external APIs

---

## Technical Architecture

```
Target Document Page
  └── {{include reference="QualityFlow.EmbedSnippet"/}}
        ├── Fetches /QualityFlow/RenderPanel (sidebar HTML)
        ├── Fetches /QualityFlow/RenderHighlights (anchor data JSON)
        └── JavaScript handles: popover, highlights, scroll navigation

Action Pages (Groovy):
  SaveComment, ResolveComment, UpdateResponse,
  SaveReviewer, MarkReviewerDone, SaveApproval,
  ArchiveAll, ArchiveDocument, EnableOnPage

Dashboard Pages:
  CommentDashboard (active review), ArchiveDashboard (past cycles)

Data:
  QualityFlow.Comments.<DocRef> — XObjects per document
  QualityFlow.Archive.<cycle-timestamp>.<DocRef> — archived cycles
```

---

## Page Inventory

| Page | Purpose |
|------|---------|
| WebHome | Application home with navigation tiles |
| EmbedSnippet | JavaScript snippet included on target pages |
| RenderPanel | AJAX endpoint returning sidebar HTML |
| RenderHighlights | AJAX endpoint returning anchor data JSON |
| SaveComment | Comment creation handler |
| ResolveComment | Resolve action handler |
| UpdateResponse | Reply handler |
| SaveReviewer | Reviewer assignment handler |
| MarkReviewerDone | Mark reviewer complete |
| SaveApproval | APO approve/reject handler |
| EnableOnPage | One-click enable/disable tool |
| ArchiveAll | Bulk archive all active comments |
| ArchiveDocument | Archive single document |
| ArchiveDashboard | Browse past cycles + export |
| CommentDashboard | Cross-document active review dashboard |
| Admin | Health check and XObject counts |
| QFStyles | CSS stylesheet |
| QFScripts | Client-side JavaScript (legacy, now handled by EmbedSnippet) |

---

## XClass Schema

### CommentClass
type (Blocker|Suggestion|Question), status (Open|Resolved), section, anchorText, anchorOffset, comment, author, response, date, document, resolvedBy, resolvedDate

### ReviewerClass
reviewer, status (Pending|Done), document, assignedDate

### ApprovalClass
decision (Approved|Rejected), author, date, reason, document

---

## Key Design Decisions

1. **Include over embed** — Target pages use `{{include}}` to reference EmbedSnippet, so updates to the snippet apply globally without touching each page
2. **Groovy with xcontext.setFinished(true)** — Action pages take full control of HTTP output for clean JSON/HTML responses
3. **CSRF protection** — All write actions validate `form_token` from `services.csrf`
4. **Light yellow highlights** — All comment types use the same background color; the bottom border color differentiates type (red/blue/amber/grey)
5. **Bidirectional navigation** — Click highlight → scroll to card; click card anchor → scroll to highlight
6. **Archive by cycle** — Timestamped archive allows multiple review rounds per document

---

## How to Prompt This Extension

To recreate this from scratch, use the following prompt structure:

> Build a structured document review extension for xWiki 14.10+ with these features:
> - Inline text-anchored comments (Blocker/Suggestion/Question types)
> - Right-side comment sidebar with reviewer tracking and progress
> - APO approval gate (locks on open blockers or pending reviewers)
> - Role-based access: Reviewers, Quality Managers, APO (3 xWiki groups)
> - Comment Dashboard with CSV export
> - Archive system for closing review cycles with Word/PDF export
> - One-click enable via {{include}} on any wiki page
> - All data in XObjects, no Java, no JAR, no external dependencies
> - Light yellow text highlights with colored borders by comment type
> - Bidirectional scroll between highlights and sidebar comment cards

---

*QualityFlow Comment Summary v1.1 — b2bmike — LGPL-2.1*
