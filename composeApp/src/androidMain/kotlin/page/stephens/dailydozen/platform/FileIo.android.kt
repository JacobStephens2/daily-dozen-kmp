package page.stephens.dailydozen.platform

import android.content.Context
import android.content.Intent

/** Set from the Application so the file helpers have a Context. */
object AndroidAppContext {
    var context: Context? = null
}

actual fun shareJson(fileName: String, content: String) {
    val ctx = AndroidAppContext.context ?: return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TITLE, fileName)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    val chooser = Intent.createChooser(send, "Export Daily Dozen")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(chooser)
}

actual fun pickJson(onResult: (String?) -> Unit) {
    // Android import needs an Activity result (Storage Access Framework), which
    // isn't wired from this context-free helper; sync covers data transfer here.
    onResult(null)
}
