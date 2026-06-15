# QualityFlow Comment Summary — Acceptance Test Case

**Version:** 1.0  
**Author:** Mike Sawaya (Notropia.co)  
**Date:** June 2026  
**Test Type:** Multi-user end-to-end acceptance test  

---

## Overview

This test validates the complete QualityFlow Comment Summary workflow from a fresh install perspective, simulating two users collaborating on a document review.

---

## Prerequisites

### xWiki Instance
- xWiki 14.10 or later
- Admin access with scripting rights enabled
- QualityFlow extension imported (XAR file)

### Test Users

| User | Login | Role |
|------|-------|------|
| Sarah K | XWiki.SarahK | Quality Manager + Reviewer |
| Tom D | XWiki.TomD | Reviewer + APO |

### Group Membership

| Group | Sarah K | Tom D |
|-------|---------|-------|
| QFReviewers | Yes | Yes |
| QFQualityManagers | Yes | No |
| QFAPO | No | Yes |

### Test Environment

- **Browser 1:** Normal window — logged in as Sarah K
- **Browser 2:** Incognito/Private window — logged in as Tom D

---

## Phase 1: Admin Setup

**Actor:** Administrator

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 1.1 | Create user XWiki.SarahK with password | User created, can log in | |
| 1.2 | Create user XWiki.TomD with password | User created, can log in | |
| 1.3 | Add SarahK to QFReviewers group | Member appears in group | |
| 1.4 | Add SarahK to QFQualityManagers group | Member appears in group | |
| 1.5 | Add TomD to QFReviewers group | Member appears in group | |
| 1.6 | Add TomD to QFAPO group | Member appears in group | |
| 1.7 | Verify QualityFlow Admin page loads | Shows XObject counts and green health check | |
| 1.8 | Verify QualityFlow Dashboard loads | Shows empty state or existing documents | |

---

## Phase 2: Create Test Document

**Actor:** Administrator or Sarah K

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 2.1 | Create a new page "Payment Module Spec" | Page created | |
| 2.2 | Edit with wiki editor, add document content (3+ sections of text) | Content saved | |
| 2.3 | Add the QualityFlow embed snippet at the end of the page | Snippet added | |
| 2.4 | Save and view the page | Sidebar appears on the right with empty state: "No comments yet", 0/0 counts, green APO gate at bottom | |

**Embed snippet to add:**

```
{{html clean="false"}}
<div id="qf-panel-container"></div>
<div id="qf-popover" style="display:none;position:absolute;z-index:9999;background:#212529;color:#fff;border-radius:4px;padding:4px 8px;box-shadow:0 2px 8px rgba(0,0,0,.3);cursor:pointer;font-size:12px;" onclick="qfOpenFormWithAnchor()">&#128172; Add Comment</div>
<script>
(function(){
  var path = window.location.pathname;
  var match = path.match(/\/bin\/view\/(.+)/);
  var docRef = match ? decodeURIComponent(match[1]).replace(/\/$/, '') : '';
  if (!docRef.endsWith('WebHome')) docRef = docRef + '.WebHome';
  docRef = docRef.replace(/\//g, '.');
  var returnUrl = window.location.pathname;
  fetch('/bin/view/QualityFlow/RenderPanel?docRef=' + encodeURIComponent(docRef) + '&returnUrl=' + encodeURIComponent(returnUrl))
    .then(function(r){return r.text();})
    .then(function(html){document.getElementById('qf-panel-container').innerHTML=html;injectHighlights();});
  var popover=document.getElementById('qf-popover');var capturedText='';
  document.addEventListener('mouseup',function(e){if(e.target.closest&&e.target.closest('#qf-panel-container'))return;setTimeout(function(){var sel=window.getSelection();var text=sel?sel.toString().trim():'';if(text.length<3){popover.style.display='none';return;}capturedText=text.substring(0,500);var rect=sel.getRangeAt(0).getBoundingClientRect();popover.style.top=(window.scrollY+rect.bottom+6)+'px';popover.style.left=(window.scrollX+rect.left)+'px';popover.style.display='block';},10);});
  document.addEventListener('mousedown',function(e){if(!popover.contains(e.target))popover.style.display='none';});
  window.qfOpenFormWithAnchor=function(){popover.style.display='none';var form=document.getElementById('qf-add-comment-form');if(form)form.style.display='block';var ai=form?form.querySelector('input[name="anchorText"]'):null;if(!ai&&form){var f=form.querySelector('form');ai=document.createElement('input');ai.type='hidden';ai.name='anchorText';f.appendChild(ai);}if(ai)ai.value=capturedText;var p=document.getElementById('qf-anchor-preview');if(!p&&form){p=document.createElement('div');p.id='qf-anchor-preview';p.style.cssText='background:#fff3cd;border:1px solid #ffda6a;border-radius:4px;padding:6px;font-size:11px;margin-bottom:8px;color:#664d03;';form.querySelector('form').insertBefore(p,form.querySelector('form').firstChild);}if(p){p.innerHTML='Anchored to: <em>"'+capturedText.substring(0,80)+(capturedText.length>80?'...':'')+'"</em>';p.style.display='block';}window.getSelection().removeAllRanges();};
  function injectHighlights(){fetch('/bin/view/QualityFlow/RenderHighlights?docRef='+encodeURIComponent(docRef)).then(function(r){return r.text();}).then(function(json){try{var comments=JSON.parse(json);var area=document.querySelector('.xcontent')||document.querySelector('#xwikicontent')||document.body;comments.forEach(function(c){if(!c.anchorText||c.anchorText.trim()==='')return;var color=c.type==='Blocker'?'rgba(220,53,69,.2)':(c.type==='Suggestion'?'rgba(13,110,253,.15)':'rgba(255,193,7,.25)');if(c.status==='Resolved')color='rgba(108,117,125,.15)';wrapText(area,c.anchorText,color,c.id);});}catch(e){}}).catch(function(){});}
  function wrapText(container,searchText,bgColor,commentId){var walker=document.createTreeWalker(container,NodeFilter.SHOW_TEXT,null,false);var node;while((node=walker.nextNode())){var idx=node.nodeValue.indexOf(searchText);if(idx===-1)continue;var before=node.nodeValue.substring(0,idx);var after=node.nodeValue.substring(idx+searchText.length);var mark=document.createElement('mark');mark.style.cssText='background:'+bgColor+';cursor:pointer;border-radius:2px;padding:0 1px;';mark.setAttribute('data-comment-id',commentId);mark.textContent=searchText;var parent=node.parentNode;if(before)parent.insertBefore(document.createTextNode(before),node);parent.insertBefore(mark,node);if(after)parent.insertBefore(document.createTextNode(after),node);parent.removeChild(node);break;}}
})();
</script>
{{/html}}
```

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
| 3.10 | Reload the page | The text "All payments processed" is highlighted with blue background | |
| 3.11 | Check sidebar header | Shows "Reviewers: 0/1 Done", "Comments: 0/2 Resolved" | |
| 3.12 | Check APO gate bar | Shows 🔒 "1 blocker(s) / 1 review(s) pending" | |
| 3.13 | Check that Approve/Reject buttons are NOT visible | Correct — Sarah is not in QFAPO | |

---

## Phase 4: Tom D — Reviewer Actions (Browser 2)

**Actor:** Tom D (QFReviewers + QFAPO)

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 4.1 | Log in as Tom D (incognito/private window) | Login successful | |
| 4.2 | Navigate to "Payment Module Spec" | Sidebar visible with 2 open comments, Tom listed as Pending reviewer | |
| 4.3 | Verify NO "+ Add Reviewer" button visible | Correct — Tom is not QFQualityManagers | |
| 4.4 | Verify NO "Pending →" button visible on his own reviewer entry | Correct — only QM can mark Done | |
| 4.5 | Click "↩ Reply" on the Blocker comment | Reply textarea appears | |
| 4.6 | Type "Will add PCI DSS reference to Section 4" → click Save | Page reloads, response text shows in green below comment | |
| 4.7 | Click "✓ Resolve" on the Blocker comment | Comment moves to Resolved tab, open count becomes 1 | |
| 4.8 | Click "Resolved" tab | Shows the resolved Blocker with "Resolved by Tom D on [date]" | |
| 4.9 | Click "✓ Resolve" on the Suggestion comment | All comments resolved, open count = 0 | |
| 4.10 | Check APO gate bar | Still 🔒 — Tom's review is still "Pending" (only QM can mark Done) | |

---

## Phase 5: Sarah K — Mark Reviewer Done (Browser 1)

**Actor:** Sarah K

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 5.1 | Refresh "Payment Module Spec" | Sees both comments in Resolved tab with Tom's reply | |
| 5.2 | Click "Pending →" button next to Tom D's name | Tom changes to green "✓ Done" badge | |
| 5.3 | Check sidebar header | Shows "Reviewers: 1/1 Done", "Comments: 2/2 Resolved" | |
| 5.4 | Check APO gate bar | Shows 🔓 "All blockers resolved. All reviewers complete. Ready for approval." | |
| 5.5 | Verify Approve/Reject buttons NOT visible for Sarah | Correct — she's not QFAPO | |

---

## Phase 6: Tom D — APO Approval (Browser 2)

**Actor:** Tom D (QFAPO)

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 6.1 | Refresh "Payment Module Spec" | Gate shows 🔓 with "✓ Approve" and "✗ Reject" buttons visible | |
| 6.2 | Click "✓ Approve" | Gate changes to "✅ Approved by Tom D" | |
| 6.3 | Verify Approve/Reject buttons are now hidden | Correct — document is already approved | |

---

## Phase 7: Dashboard Verification

**Actor:** Any authenticated user

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 7.1 | Navigate to QualityFlow Dashboard | Table shows "Payment Module Spec" row | |
| 7.2 | Verify counts | Total: 2, Open: 0, Blockers: 0, Reviewers: 1/1 | |
| 7.3 | Verify status | Shows "Approved" with green badge | |
| 7.4 | Click document name | Links to the Payment Module Spec page | |

---

## Phase 8: Rejection Flow (Optional)

To test rejection instead of approval:

| # | Action | Expected Result | Pass/Fail |
|---|--------|-----------------|-----------|
| 8.1 | On a new document with gate unlocked, as QFAPO user click "✗ Reject" | Reject form popup appears with "Reason" textarea | |
| 8.2 | Type reason: "Needs security review from InfoSec team" → click "Confirm Reject" | Gate shows "❌ Rejected by [name] — Needs security review from InfoSec team" | |
| 8.3 | Check Dashboard | Document shows "Rejected" status | |

---

## Phase 9: Role Isolation Verification

| # | Test | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 9.1 | Unauthenticated user views page with QualityFlow | No "+ Add Comment" button, no action buttons visible | |
| 9.2 | User NOT in any QF group views page | Can see comments (read-only) but no Resolve/Reply/Add buttons | |
| 9.3 | QFReviewer can Resolve and Reply | Yes | |
| 9.4 | QFReviewer cannot Add Reviewer | Correct — no button visible | |
| 9.5 | QFReviewer cannot Approve/Reject | Correct — buttons hidden | |
| 9.6 | QFQualityManagers can Add Reviewer and Mark Done | Yes | |
| 9.7 | QFAPO can Approve/Reject when gate unlocked | Yes | |
| 9.8 | QFAPO cannot Approve when gate locked | Buttons hidden when gate is locked | |

---

## Test Summary

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 1 | Admin setup | |
| Phase 2 | Create test document | |
| Phase 3 | Sarah: QM actions | |
| Phase 4 | Tom: Reviewer actions | |
| Phase 5 | Sarah: Mark reviewer done | |
| Phase 6 | Tom: APO approval | |
| Phase 7 | Dashboard verification | |
| Phase 8 | Rejection flow | |
| Phase 9 | Role isolation | |

**Overall Result:** ________________

**Tested by:** ________________  
**Date:** ________________  
**xWiki Version:** ________________  

---

*QualityFlow Comment Summary v1.0 — Mike Sawaya / Notropia.co*
