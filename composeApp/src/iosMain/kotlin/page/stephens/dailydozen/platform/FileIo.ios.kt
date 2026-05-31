package page.stephens.dailydozen.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun shareJson(fileName: String, content: String) {
    val path = (NSTemporaryDirectory() as NSString).stringByAppendingPathComponent(fileName)
    (content as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    val url = NSURL.fileURLWithPath(path)
    val activityController = UIActivityViewController(
        activityItems = listOf(url),
        applicationActivities = null,
    )
    rootViewController()?.presentViewController(activityController, animated = true, completion = null)
}

// Held strongly while the picker is on screen.
private var pickerDelegate: NSObject? = null

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun pickJson(onResult: (String?) -> Unit) {
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON))
    val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>,
        ) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            val text = url?.let {
                val scoped = it.startAccessingSecurityScopedResource()
                val s = NSString.create(contentsOfURL = it, encoding = NSUTF8StringEncoding, error = null) as String?
                if (scoped) it.stopAccessingSecurityScopedResource()
                s
            }
            onResult(text)
            pickerDelegate = null
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onResult(null)
            pickerDelegate = null
        }
    }
    pickerDelegate = delegate
    picker.delegate = delegate
    rootViewController()?.presentViewController(picker, animated = true, completion = null)
}

private fun rootViewController() =
    UIApplication.sharedApplication.keyWindow?.rootViewController
