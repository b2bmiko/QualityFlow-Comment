{{groovy}}
def currentUser = xcontext.userReference
if (currentUser == null || currentUser.toString().contains('XWikiGuest')) {
  response.sendRedirect(request.contextPath + '/bin/login/XWiki/XWikiLogin')
  return
}

def targetPage = request.getParameter('targetPage') ?: ''
def action = request.getParameter('action') ?: ''

// If user pasted a URL, extract the page reference from it
if (targetPage.contains('/bin/view/')) {
  def urlMatch = targetPage.replaceAll('.*?/bin/view/', '').replaceAll('\\?.*', '').replaceAll('#.*', '').replaceAll('/\\s*$', '')
  targetPage = java.net.URLDecoder.decode(urlMatch, 'UTF-8').replace('/', '.').trim()
  if (!targetPage.endsWith('.WebHome') && !targetPage.contains('.WebHome')) {
    targetPage = targetPage + '.WebHome'
  }
}

def resp = response
resp.setContentType('text/html; charset=UTF-8')
def writer = resp.getWriter()

def snippetDoc = xwiki.getDocument('QualityFlow.EmbedSnippet')
def snippet = '{{include reference="QualityFlow.EmbedSnippet"/}}'

def resultMsg = ''
if (action == 'enable' && targetPage) {
  try {
    def targetDoc = xwiki.getDocument(targetPage)
    if (targetDoc.isNew()) {
      resultMsg = '<div style="padding:10px;background:#f8d7da;border-radius:4px;color:#842029;margin-bottom:12px;">Page "' + targetPage + '" does not exist. Please create it first.</div>'
    } else {
      def content = targetDoc.getContent()
      if (content.contains('qf-panel-container')) {
        resultMsg = '<div style="padding:10px;background:#fff3cd;border-radius:4px;color:#664d03;margin-bottom:12px;">QualityFlow is already enabled on "' + targetPage + '".</div>'
      } else {
        targetDoc.setContent(content + '\n\n' + snippet)
        targetDoc.save('QualityFlow Comment Summary enabled')
        def viewUrl = '/bin/view/' + targetPage.replace('.', '/')
        resultMsg = '<div style="padding:10px;background:#d1e7dd;border-radius:4px;color:#0f5132;margin-bottom:12px;">&#10003; Success! QualityFlow is now enabled on <a href="' + viewUrl + '">' + targetPage + '</a></div>'
      }
    }
  } catch (Exception e) {
    resultMsg = '<div style="padding:10px;background:#f8d7da;border-radius:4px;color:#842029;margin-bottom:12px;">Error: ' + e.getMessage() + '</div>'
  }
} else if (action == 'disable' && targetPage) {
  try {
    def targetDoc = xwiki.getDocument(targetPage)
    if (targetDoc.isNew()) {
      resultMsg = '<div style="padding:10px;background:#f8d7da;border-radius:4px;color:#842029;margin-bottom:12px;">Page not found.</div>'
    } else {
      def content = targetDoc.getContent()
      if (content.contains('qf-panel-container')) {
        def idx = content.lastIndexOf('{{html clean="false"}}')
        if (idx >= 0) {
          targetDoc.setContent(content.substring(0, idx).trim())
          targetDoc.save('QualityFlow Comment Summary disabled')
          resultMsg = '<div style="padding:10px;background:#d1e7dd;border-radius:4px;color:#0f5132;margin-bottom:12px;">&#10003; QualityFlow disabled on "' + targetPage + '".</div>'
        }
      } else {
        resultMsg = '<div style="padding:10px;background:#fff3cd;border-radius:4px;color:#664d03;margin-bottom:12px;">QualityFlow is not currently enabled on "' + targetPage + '".</div>'
      }
    }
  } catch (Exception e) {
    resultMsg = '<div style="padding:10px;background:#f8d7da;border-radius:4px;color:#842029;margin-bottom:12px;">Error: ' + e.getMessage() + '</div>'
  }
}

def enableUrl = xwiki.getURL('QualityFlow.EnableOnPage', 'view')

def html = new StringBuilder()
html.append('<div style="max-width:500px;margin:30px auto;font-family:-apple-system,sans-serif;">')
html.append('<a href="/bin/view/QualityFlow/" style="display:inline-block;margin-bottom:12px;font-size:12px;color:#0d6efd;text-decoration:none;">&larr; Back to QualityFlow Home</a>')
html.append('<h1 style="font-size:22px;margin:0 0 4px 0;">Enable Review Script on Page</h1>')
html.append('<p style="font-size:13px;color:#6c757d;margin-bottom:6px;">This injects the QualityFlow comment sidebar onto a wiki page so reviewers can add structured comments.</p>')
html.append('<p style="font-size:12px;color:#6c757d;margin-bottom:20px;background:#f8f9fa;padding:8px;border-radius:4px;"><strong>How to use:</strong> Open the page you want to enable in your browser, copy the URL from the address bar, and paste it below.</p>')
html.append(resultMsg)
html.append('<div style="background:#fff;border:1px solid #dee2e6;border-radius:6px;padding:16px;margin-bottom:16px;">')
html.append('<form method="post" action="' + enableUrl + '">')
html.append('<input type="hidden" name="action" value="enable"/>')
html.append('<div style="margin-bottom:12px;">')
html.append('<label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">Page URL</label>')
html.append('<input type="text" name="targetPage" required placeholder="Paste the page URL here" value="' + targetPage + '" style="width:100%;padding:8px;border:1px solid #dee2e6;border-radius:4px;font-size:13px;box-sizing:border-box;"/>')
html.append('<small style="color:#6c757d;font-size:11px;">Example: https://your-wiki.example.com/bin/view/MySpace/MyDocument/</small>')
html.append('</div>')
html.append('<button type="submit" style="background:#28a745;color:#fff;border:none;padding:10px 20px;border-radius:4px;cursor:pointer;font-size:13px;font-weight:600;width:100%;">&#10003; Enable QualityFlow on this page</button>')
html.append('</form></div>')
html.append('<div style="background:#fff;border:1px solid #dee2e6;border-radius:6px;padding:16px;">')
html.append('<form method="post" action="' + enableUrl + '">')
html.append('<input type="hidden" name="action" value="disable"/>')
html.append('<div style="margin-bottom:12px;">')
html.append('<label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">Remove QualityFlow from a page</label>')
html.append('<input type="text" name="targetPage" required placeholder="Paste the page URL here" style="width:100%;padding:8px;border:1px solid #dee2e6;border-radius:4px;font-size:13px;box-sizing:border-box;"/>')
html.append('</div>')
html.append('<button type="submit" style="background:#dc3545;color:#fff;border:none;padding:10px 20px;border-radius:4px;cursor:pointer;font-size:13px;font-weight:600;width:100%;">&#10007; Disable QualityFlow</button>')
html.append('</form></div>')
html.append('<p style="font-size:11px;color:#adb5bd;margin-top:16px;text-align:center;">QualityFlow Comment Summary v1.0.0 &mdash; LGPL-2.1 &mdash; b2bmike</p>')
html.append('</div>')

writer.print(html.toString())
writer.flush()
xcontext.setFinished(true)
{{/groovy}}
