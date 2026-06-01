package page.stephens.dailydozen.platform

import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement

actual fun shareJson(fileName: String, content: String) {
    val href = "data:application/json;charset=utf-8," + encodeURIComponent(content)
    val anchor = document.createElement("a") as HTMLAnchorElement
    anchor.href = href
    anchor.setAttribute("download", fileName)
    anchor.click()
}

actual fun pickJson(onResult: (String?) -> Unit) {
    // A file <input> + FileReader round-trip; left to the web build's discretion.
    onResult(null)
}

private fun encodeURIComponent(value: String): String =
    js("encodeURIComponent(value)")
