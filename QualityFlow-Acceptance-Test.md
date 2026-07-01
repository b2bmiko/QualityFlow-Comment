# QualityFlow Comment Summary — Acceptance Test Case

**Version:** 1.1  
**Author:** b2bmike  
**Date:** June 2026  
**Test Type:** Multi-user end-to-end acceptance test  

---

## Overview

This test validates the complete QualityFlow Comment Summary workflow from a fresh install perspective, simulating multiple users collaborating on a document review.

---

## Prerequisites

### xWiki Instance
- xWiki 14.10 or later
- Admin access with scripting rights enabled
- QualityFlow extension imported (XAR file) or pages created manually

### Test Users

| User | Login | Password | Role |
|------|-------|----------|------|
| Sarah K | XWiki.SarahK | SarahK2026! | Quality Manager + Reviewer |
| Tom D | XWiki.TomD | TomD2026! | Reviewer + APO |
| kiro2 | XWiki.kiro2 | (admin password) | Admin with Programming rights |

### Group Membership

| Group | Sarah K | Tom D | kiro2 |
|-------|---------|-------|-------|
| QFReviewers | Yes | Yes | No |
| QFQualityManagers | Yes | No | Yes |
| QFAPO | No | Yes | No |

### Test Environment

- **Browser 1:** Normal window — logged in as Sarah K
- **Browser 2:** Incognito/Private window — logged in as Tom D
- **Admin tasks:** logged in as kiro2 (has Programming rights for groovy execution)

---

## Phase 1: Admin Setup

**Actor:** kiro2 (Admin)

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 1.1 | Create user XWiki.SarahK with password SarahK2026! | User created, can log in | |
| 1.2 | Create user XWiki.TomD with password TomD2026! | User created, can log in | |
| 1.3 | Add SarahK to QFReviewers group | Member appears in group | |
| 1.4 | Add SarahK to QFQualityManagers group | Member appears in group | |
| 1.5 | Add TomD to QFReviewers group | Member appears in group | |
| 1.6 | Add TomD to QFAPO group | Member appears in group | |
| 1.7 | Verify QualityFlow Admin page loads at /bin/view/QualityFlow/Admin | Shows XObject counts and green health check | |
| 1.8 | Verify QualityFlow Dashboard loads at /bin/view/QualityFlow/CommentDashboard | Shows empty state or existing documents | |

---

## Phase 2: Create Test Document and Enable QualityFlow

**Actor:** kiro2 (Admin) or Sarah K

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 2.1 | Create a new page "Payment Module Spec" with 3+ sections of text | Page created | |
| 2.2 | Go to /bin/view/QualityFlow/EnableOnPage | Enable page loads | |
| 2.3 | Paste the URL of "Payment Module Spec" and click Enable | Success message, page enabled | |
| 2.4 | Navigate to "Payment Module Spec" | Sidebar appears on the right with empty state: "No comments yet", 0/0 counts, gate bar at bottom | |
| 2.5 | View page source (Ctrl+U) | Page content ends with `{{include reference="QualityFlow.EmbedSnippet"/}}` | |

**Important:** Pages are now enabled using `{{include}}` which automatically loads the latest EmbedSnippet. No manual snippet pasting required.

---

## Phase 3: Sarah K — Quality Manager Actions (Browser 1)

**Actor:** Sarah K (QFReviewers + QFQualityManagers)

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 3.1 | Log in as Sarah K | Login successful | |
| 3.2 | Navigate to "Payment Module Spec" | Sidebar visible with "+ Add Comment" and "+ Add Reviewer" buttons | |
| 3.3 | Click "+ Add Reviewer" | Dropdown appears with QFReviewers group members | |
| 3.4 | Select Tom D from dropdown, click "Assign" | Page reloads, Tom D appears in reviewer list with orange "Pending" badge | |
| 3.5 | Click "+ Add Comment" | Comment form opens (Type, Section, Comment fields) | |
| 3.6 | Select Type: Blocker, Section: "Section 2", Comment: "Missing PCI compliance reference" → Submit | Page reloads, Blocker comment card appears with red badge | |
| 3.7 | Select text passage in document body (e.g. "All payments processed") | Dark "Add Comment" popover appears near selection | |
| 3.8 | Click the popover | Comment form opens with "Anchored to: All payments processed" preview | |
| 3.9 | Select Type: Suggestion, Comment: "Consider async processing" → Submit | New Suggestion card appears with blue badge | |
| 3.10 | Reload the page | The text "All payments processed" is highlighted with light yellow background and blue border | |
| 3.11 | Click the highlighted text in the document | Sidebar scrolls to the corresponding comment card with blue focus ring | |
| 3.12 | Click the anchor quote (yellow box) in the sidebar comment card | Page scrolls to the highlighted text and flashes it | |
| 3.13 | Check sidebar header | Shows "Reviewers: 0/1 Done", "Comments: 0/2 Resolved" | |
| 3.14 | Check APO gate bar | Shows "1 blocker(s) / 1 review(s) pending" | |
| 3.15 | Check that Approve/Reject buttons are NOT visible | Correct — Sarah is not in QFAPO | |

---

## Phase 4: Tom D — Reviewer Actions (Browser 2)

**Actor:** Tom D (QFReviewers + QFAPO)

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 4.1 | Log in as Tom D (incognito/private window) | Login successful | |
| 4.2 | Navigate to "Payment Module Spec" | Sidebar visible with 2 open comments, Tom listed as Pending reviewer | |
| 4.3 | Verify NO "+ Add Reviewer" button visible | Correct — Tom is not QFQualityManagers | |
| 4.4 | Verify NO "Pending →" button visible on his own reviewer entry | Correct — only QM can mark Done | |
| 4.5 | Click "Reply" on the Blocker comment | Reply textarea appears | |
| 4.6 | Type "Will add PCI DSS reference to Section 4" → click Save | Page reloads, response text shows in green below comment | |
| 4.7 | Click "Resolve" on the Blocker comment | Comment moves to Resolved tab, open count becomes 1 | |
| 4.8 | Click "Resolved" tab | Shows the resolved Blocker with "Resolved by Tom D on [date]" | |
| 4.9 | Click "Resolve" on the Suggestion comment | All comments resolved, open count = 0 | |
| 4.10 | Check APO gate bar | Still locked — Tom's review is still "Pending" (only QM can mark Done) | |

---

## Phase 5: Sarah K — Mark Reviewer Done (Browser 1)

**Actor:** Sarah K

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 5.1 | Refresh "Payment Module Spec" | Sees both comments in Resolved tab with Tom's reply | |
| 5.2 | Click "Pending →" button next to Tom D's name | Tom changes to green "Done" badge | |
| 5.3 | Check sidebar header | Shows "Reviewers: 1/1 Done", "Comments: 2/2 Resolved" | |
| 5.4 | Check APO gate bar | Shows "All blockers resolved. All reviewers complete. Ready for approval." | |
| 5.5 | Verify Approve/Reject buttons NOT visible for Sarah | Correct — she's not QFAPO | |

---

## Phase 6: Tom D — APO Approval (Browser 2)

**Actor:** Tom D (QFAPO)

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 6.1 | Refresh "Payment Module Spec" | Gate shows unlocked with "Approve" and "Reject" buttons visible | |
| 6.2 | Click "Approve" | Gate changes to "Approved by Tom D" | |
| 6.3 | Verify Approve/Reject buttons are now hidden | Correct — document is already approved | |

---

## Phase 7: Dashboard Verification

**Actor:** Any authenticated user

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 7.1 | Navigate to QualityFlow Comment Dashboard | Table shows "Payment Module Spec" row | |
| 7.2 | Verify counts | Total: 2, Open: 0, Blockers: 0, Reviewers: 1/1 | |
| 7.3 | Verify status | Shows "Approved" with green badge | |
| 7.4 | Click document name | Links to the Payment Module Spec page | |
| 7.5 | Click "Export CSV" | Downloads CSV file with comment data | |

---

## Phase 8: Archive Cycle

**Actor:** Sarah K (QFQualityManagers)

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 8.1 | On Comment Dashboard, click "Archive All" | Confirmation page appears: "Yes, Archive All" / "Cancel" | |
| 8.2 | Click "Yes, Archive All" | Redirected to empty dashboard with success message | |
| 8.3 | Click "View Archives" (or navigate to /bin/view/QualityFlow/ArchiveDashboard) | Archive Dashboard shows the archived cycle with timestamp | |
| 8.4 | Verify cycle shows document count and comment count | Correct — 1 doc, 2 comments | |
| 8.5 | Click "Word" export button | Downloads .doc file with full archived comment data | |
| 8.6 | Click "PDF/HTML" export button | Downloads HTML file (use browser Print → PDF) | |
| 8.7 | Return to Comment Dashboard | Dashboard is clean — no active comments | |
| 8.8 | Navigate to "Payment Module Spec" | Sidebar shows no comments — fresh start for new cycle | |

---

## Phase 9: Rejection Flow

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 9.1 | On a new document with gate unlocked, as QFAPO user click "Reject" | Reject form popup appears with "Reason" textarea | |
| 9.2 | Type reason: "Needs security review" → click "Reject" | Gate shows "Rejected by [name] — Needs security review" | |
| 9.3 | Check Dashboard | Document shows "Rejected" status | |

---

## Phase 10: Role Isolation Verification

| # | Test | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 10.1 | Unauthenticated user views page with QualityFlow | No "+ Add Comment" button, no action buttons visible | |
| 10.2 | User NOT in any QF group views page | Can see comments (read-only) but no Resolve/Reply/Add buttons | |
| 10.3 | QFReviewer can Resolve and Reply | Yes | |
| 10.4 | QFReviewer cannot Add Reviewer | Correct — no button visible | |
| 10.5 | QFReviewer cannot Approve/Reject | Correct — buttons hidden | |
| 10.6 | QFQualityManagers can Add Reviewer and Mark Done | Yes | |
| 10.7 | QFQualityManagers can Archive All | Yes — button visible on Dashboard | |
| 10.8 | QFAPO can Approve/Reject when gate unlocked | Yes | |
| 10.9 | QFAPO cannot Approve when gate locked | Buttons disabled when gate is locked | |

---

## Phase 11: Highlight and Navigation Verification

| # | Test | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 11.1 | Page with anchored comment shows highlighted text (light yellow) | Yes | |
| 11.2 | Highlight has colored bottom border (red=Blocker, blue=Suggestion, amber=Question) | Yes | |
| 11.3 | Clicking highlight scrolls sidebar to comment card | Card gets blue focus ring | |
| 11.4 | Clicking anchor quote in sidebar scrolls page to highlight | Highlight flashes briefly | |
| 11.5 | Resolved comments have grey/muted highlights | Yes | |
| 11.6 | If page text changed and anchor can't be found | Alert shown: "text not found on page" | |
| 11.7 | Only one "Add Comment" popover appears on text selection | Yes — no duplicate buttons | |

---

## Test Summary

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 1 | Admin setup | |
| Phase 2 | Create test document | |
| Phase 3 | Sarah: QM actions + highlight navigation | |
| Phase 4 | Tom: Reviewer actions | |
| Phase 5 | Sarah: Mark reviewer done | |
| Phase 6 | Tom: APO approval | |
| Phase 7 | Dashboard verification | |
| Phase 8 | Archive cycle | |
| Phase 9 | Rejection flow | |
| Phase 10 | Role isolation | |
| Phase 11 | Highlight and navigation | |

**Overall Result:** ________________

**Tested by:** ________________  
**Date:** ________________  
**xWiki Version:** ________________  

---

*QualityFlow Comment Summary v1.1 — b2bmike — LGPL-2.1*
