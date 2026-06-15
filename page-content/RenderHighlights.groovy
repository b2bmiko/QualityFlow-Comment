{{groovy}}
def resp = response
resp.setContentType('application/json; charset=UTF-8')
def writer = resp.getWriter()

def currentDocRef = request.getParameter('docRef') ?: ''
def holderName = currentDocRef.replaceAll('[^a-zA-Z0-9]', '')
def holderRef = "QualityFlow.Comments." + holderName
def holderDoc = xwiki.getDocument(holderRef)

def allComments = holderDoc.getObjects('QualityFlow.CommentClass') ?: []

def json = new StringBuilder()
json.append("[")
def first = true
allComments.each { c ->
  def anchorText = c.getValue('anchorText') ?: ''
  if (anchorText) {
    if (!first) json.append(",")
    first = false
    def id = c.getNumber()
    def cType = c.getValue('type') ?: ''
    def cStatus = c.getValue('status') ?: ''
    // Escape JSON special chars
    anchorText = anchorText.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '')
    json.append("{\"id\":\"${id}\",\"anchorText\":\"${anchorText}\",\"type\":\"${cType}\",\"status\":\"${cStatus}\"}")
  }
}
json.append("]")

writer.print(json.toString())
writer.flush()
xcontext.setFinished(true)
{{/groovy}}
