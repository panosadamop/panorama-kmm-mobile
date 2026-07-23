package gr.panoramapolihnitou.app.util

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/** iOS native share sheet via UIActivityViewController. */
class IosSharer : Sharer {
    override fun shareText(title: String, text: String, url: String) {
        val items = mutableListOf<Any>()
        items.add(text)
        if (url.isNotBlank()) {
            NSURL.URLWithString(url)?.let { items.add(it) }
        }
        val controller = UIActivityViewController(
            activityItems = items,
            applicationActivities = null
        )
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        root?.presentViewController(controller, animated = true, completion = null)
    }
}
