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
    popover.innerHTML = '<button id="qf-popover-btn">\ud83d\udcac Add Comment</button>';
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
