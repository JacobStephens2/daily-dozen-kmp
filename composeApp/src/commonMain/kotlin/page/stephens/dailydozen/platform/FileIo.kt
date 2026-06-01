package page.stephens.dailydozen.platform

/** Share/save a JSON document (Export Data). */
expect fun shareJson(fileName: String, content: String)

/** Pick a JSON document and return its text (Import Data); null if cancelled. */
expect fun pickJson(onResult: (String?) -> Unit)
