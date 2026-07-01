# QualityFlow Comment Summary

**Structured document review for xWiki — inline highlighting, role-based workflows, APO approval gate.**

QualityFlow adds a review sidebar to any xWiki page. Reviewers leave typed comments (Blocker, Suggestion, Question) that can be anchored to specific text passages. Quality Managers assign reviewers, track progress, and resolve comments. An Approval Authority (APO) provides a final approval gate once all blockers are cleared.

All data stays on-premise. No external APIs or third-party services are contacted.

![License](https://img.shields.io/badge/license-LGPL--2.1-blue)
![xWiki](https://img.shields.io/badge/xWiki-14.10%2B-green)
![No Java](https://img.shields.io/badge/Java-not%20required-lightgrey)

## Key Features

- Inline comment sidebar injected onto any wiki page
- Text-anchored comments with in-page highlighting (color-coded by type)
- Role-based access: Reviewers, Quality Managers, APO
- Comment types: Blocker, Suggestion, Question
- Reviewer assignment and "Mark Done" tracking
- APO approval/rejection gate (unlocks when all blockers are resolved)
- Cross-document Comment Dashboard with CSV export
- Archive system with bulk "Archive All" to start fresh review cycles
- Archive Dashboard with Word and PDF/HTML export of past cycles
- Admin health-check page with XObject counts
- One-click enable/disable via URL paste

## Prerequisites

| Requirement | Details |
|---|---|
| xWiki version | 14.10 or later |
| Programming rights | The admin importing the XAR must have Programming rights (Groovy scripts require it) |
| User groups | Three groups will need to be created: `XWiki.QFReviewers`, `XWiki.QFQualityManagers`, `XWiki.QFAPO` |
| No Java / No JAR | Pure wiki-scripting application — no server restart needed |

## Installation

1. **Download** the latest `QualityFlow-Comment.xar` from the [Releases](../../releases) page.

2. **Import the XAR**
   - Go to **Administration → Content → Import**
   - Upload `QualityFlow-Comment.xar`
   - Select all pages, choose **"Add new versions"** as the merge strategy
   - Click **Import**

3. **Create the required groups** (if they don't already exist)
   - `XWiki.QFReviewers` — add your reviewer users
   - `XWiki.QFQualityManagers` — add quality managers
   - `XWiki.QFAPO` — add approval authorities

4. **Verify installation**
   - Navigate to `/bin/view/QualityFlow/Admin`
   - All three XClass health checks should show green (CommentClass, ReviewerClass, ApprovalClass)

5. **Enable on a document**
   - Navigate to `/bin/view/QualityFlow/EnableOnPage`
   - Paste the URL of any wiki page and click **Enable**
   - The review sidebar will now appear on that page

## How It Works

1. Go to **Enable on a Page** and paste any document URL → click Enable
2. Quality Manager assigns reviewers from the sidebar
3. Reviewers add comments (Blocker / Suggestion / Question), optionally anchored to text
4. Quality Manager resolves comments and marks reviewers Done
5. APO gives final approval once all blockers are resolved
6. Quality Manager archives the cycle (Archive All) to start a new review phase

## Roles

| Group | Permissions |
|---|---|
| QFReviewers | Add comments, Resolve, Reply |
| QFQualityManagers | All reviewer actions + Assign reviewers + Mark Done |
| QFAPO | Approve or Reject documents (when gate unlocked) |

## Project Structure

```
page-content/          Wiki page source files (Groovy, Velocity, JS, CSS)
  ├── WebHome.wiki     Application home page
  ├── Admin.vm         Health check & admin dashboard
  ├── EnableOnPage.groovy   One-click enable/disable tool
  ├── EmbedSnippet.wiki     Script injected into target pages
  ├── RenderPanel.groovy    AJAX sidebar renderer
  ├── RenderHighlights.groovy  JSON endpoint for text highlights
  ├── SaveComment.groovy    Comment creation handler
  ├── ResolveComment.groovy Resolve action handler
  ├── UpdateResponse.groovy Reply handler
  ├── SaveReviewer.groovy   Reviewer assignment handler
  ├── MarkReviewerDone.groovy  Mark reviewer complete
  ├── SaveApproval.groovy   APO approve/reject handler
  ├── ArchiveAll.groovy     Bulk archive all active comments
  ├── ArchiveDocument.groovy  Archive single document comments
  ├── ArchiveDashboard.groovy  Browse past cycles + Word/PDF export
  ├── CommentDashboard.vm   Cross-document dashboard
  ├── AddComment.vm         Comment form template
  ├── AddReviewer.vm        Reviewer assignment form
  ├── DocumentCommentPanel.vm  Per-document comment panel
  ├── CommentTemplate.vm    Comment rendering template
  ├── QFScripts.js          Client-side JavaScript
  └── QFStyles.css          Stylesheet
mockup/                UI mockup (HTML prototype)
```

## Uninstallation

- Delete the `QualityFlow` space from your wiki
- Use the **Disable** button on EnableOnPage to remove the sidebar from individual pages
- Optionally delete the three groups (`QFReviewers`, `QFQualityManagers`, `QFAPO`)

## Data & Privacy

- **Storage:** xWiki XObjects on `QualityFlow.Comments.*` holder pages
- **Archives:** Stored in `QualityFlow.Archive.<cycle-timestamp>.*` pages
- **Access control:** View rights on QualityFlow space required to see comments; write actions require group membership
- **External transmission:** None — all data remains on-premise
- **Export:** CSV export from Comment Dashboard; Word (.doc) and HTML/PDF export from Archive Dashboard

## License

[GNU Lesser General Public License v2.1 (LGPL-2.1)](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html)

You may freely use, modify, and distribute this application. Modifications to QualityFlow source pages must be shared under the same license. Integration with proprietary xWiki content does not require licensing that content under LGPL.

---

**v1.0.0** · Mike Sawaya · [Notropia.co](https://notropia.co)
