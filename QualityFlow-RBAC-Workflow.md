# QualityFlow — Role-Based Activity & Workflow Guide

## Groups & Roles

| Group | Role | Purpose |
|-------|------|---------|
| XWiki.QFReviewers | Reviewer | Subject matter experts who review document content |
| XWiki.QFQualityManagers | Quality Manager (QM) | Orchestrates the review process, manages team |
| XWiki.QFAPO | Approval Process Owner (APO) | Final sign-off authority for document release |

---

## Permissions Matrix

| Action | Reviewer | Quality Manager | APO | Guest |
|--------|:--------:|:---------------:|:---:|:-----:|
| View comments sidebar | ✅ | ✅ | ✅ | ❌ |
| Add comment (Blocker/Suggestion/Question) | ✅ | ✅ | ✅ | ❌ |
| Resolve a comment | ✅ | ✅ | ❌ | ❌ |
| Reply to a comment | ✅ | ✅ | ❌ | ❌ |
| Assign a reviewer | ❌ | ✅ | ❌ | ❌ |
| Mark reviewer as Done | ❌ | ✅ | ❌ | ❌ |
| Approve document | ❌ | ❌ | ✅ | ❌ |
| Reject document | ❌ | ❌ | ✅ | ❌ |
| View Comment Dashboard | ✅ | ✅ | ✅ | ❌ |
| Approve/Reject from Dashboard | ❌ | ❌ | ✅ | ❌ |
| Export CSV | ✅ | ✅ | ✅ | ❌ |
| Enable QualityFlow on a page | ❌ | ✅ | ❌ | ❌ |

---

## Workflow Sequence

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        DOCUMENT REVIEW LIFECYCLE                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. SETUP (Quality Manager)                                             │
│     └─ Enable QualityFlow on target document                            │
│     └─ Assign reviewers from QFReviewers group                          │
│                                                                         │
│  2. REVIEW (Reviewers)                                                  │
│     └─ Read document content                                            │
│     └─ Add comments: Blocker / Suggestion / Question                    │
│     └─ Optionally anchor comments to specific text                      │
│                                                                         │
│  3. RESOLUTION (Reviewers + Quality Manager)                            │
│     └─ Document author addresses feedback                               │
│     └─ Reviewer or QM resolves each comment                             │
│     └─ QM or Reviewer can reply to comments                             │
│     └─ QM marks each reviewer as "Done" when complete                   │
│                                                                         │
│  4. GATE CHECK (automatic)                                              │
│     └─ System checks: any open Blockers? → 🔒 LOCKED                   │
│     └─ System checks: any Pending reviewers? → 🔒 LOCKED               │
│     └─ All clear? → 🔓 UNLOCKED (ready for approval)                   │
│                                                                         │
│  5. APPROVAL (APO only — from Comment Dashboard)                        │
│     └─ APO reviews document status on Comment Dashboard                 │
│     └─ ✓ Approve (with optional comment) → document is signed off      │
│     └─ ✗ Reject (with required reason) → back to step 2                │
│                                                                         │
│  6. RE-REVIEW (if new issues found after approval)                      │
│     └─ New Blocker added → gate automatically re-locks                  │
│     └─ Previous approval is overridden                                  │
│     └─ Cycle repeats from step 3                                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Gate Logic (Document Page — Status Only)

The gate bar at the bottom of each document page is a **status indicator only** (no action buttons). APO approves/rejects from the Comment Dashboard.

| Priority | Condition | Gate State | Display |
|----------|-----------|-----------|---------|
| 1 (highest) | Open Blockers > 0 OR Pending Reviewers > 0 | 🔒 Locked (yellow) | "X blocker(s) / Y review(s) pending" |
| 2 | Prior approval exists (no blockers) | ✅ Approved (green) | "Approved by [name]" |
| 2 | Prior rejection exists (no blockers) | ❌ Rejected (red) | "Rejected by [name] — reason" |
| 3 (lowest) | No blockers, no pending, no prior decision | 🔓 Ready (green) | "All blockers resolved. Ready for approval." |

**Key rule:** A new Blocker comment always overrides any prior approval and locks the gate.

---

## Dashboard Status Logic

| Status | Meaning | Color | APO Action |
|--------|---------|-------|------------|
| Blocked | Open Blocker comments exist | 🔴 Red | — (no action) |
| In Review | Open comments or pending reviewers (no blockers) | 🟡 Yellow | — (no action) |
| Approved | All clear + APO approved | 🟢 Green | ✓/✗ (re-approve) |
| Rejected | APO rejected | 🟢 Green | ✓/✗ (re-approve) |
| Ready | All resolved, no prior approval | 🟢 Green | ✓/✗ (first approval) |

---

## Typical Workflow Example

| Step | Who | Action | Result |
|------|-----|--------|--------|
| 1 | Mike (QM) | Enables QualityFlow on "Project Plan" page | Sidebar appears |
| 2 | Mike (QM) | Assigns Sara as reviewer | Sara appears in reviewer list (Pending) |
| 3 | Sara (Reviewer) | Adds Blocker: "Section 3 is missing budget" | Gate shows 🔒 1 blocker |
| 4 | Sara (Reviewer) | Adds Suggestion: "Add timeline diagram" | Comment appears in Open tab |
| 5 | Document Author | Updates document content | (outside QualityFlow) |
| 6 | Mike (QM) | Resolves both comments | Gate recalculates |
| 7 | Mike (QM) | Marks Sara as "Done" | Reviewer progress: 1/1 Done |
| 8 | System | Gate unlocks | 🔓 "Ready for approval" |
| 9 | Kiro (APO) | Clicks ✓ Approve on Dashboard | Status → Approved ✅ |
| 10 | Sara (Reviewer) | Later adds new Blocker | Gate re-locks 🔒, overrides approval |
| 11 | Mike (QM) | Resolves new Blocker | Gate unlocks again |
| 12 | Kiro (APO) | Re-approves | Status → Approved ✅ |

---

## Comment Types

| Type | Severity | Effect on Gate |
|------|----------|---------------|
| **Blocker** | Critical | 🔒 Locks the approval gate until resolved |
| **Suggestion** | Medium | Does not block approval |
| **Question** | Low | Does not block approval |

---

## Where Each Role Works

### Reviewer (Sara)
- **Primary view:** Document page with sidebar
- **Actions:** Add comments, resolve own/other comments, reply
- **Cannot:** Assign reviewers, mark done, approve/reject

### Quality Manager (Mike)  
- **Primary view:** Document page sidebar + Comment Dashboard
- **Actions:** Everything a reviewer can do + assign reviewers + mark done + enable QualityFlow
- **Cannot:** Approve/reject documents

### APO (Kiro)
- **Primary view:** Comment Dashboard (for batch approval)
- **Actions:** Approve or reject documents (from Dashboard only — with optional comment)
- **Cannot:** Resolve comments, reply, assign reviewers, mark done
- **Note:** Can still add comments on document pages (useful for adding feedback)
- **Approve flow:** Click ✓ on Dashboard → expand form → add optional comment → click "Approve"
- **Reject flow:** Click ✗ on Dashboard → expand form → type required reason → click "Reject"
- **Gate bar on document page:** Status display only (no action buttons) — shows Locked/Approved/Rejected/Ready

---

*QualityFlow v1.0.0 — Mike Sawaya / Notropia.co*
