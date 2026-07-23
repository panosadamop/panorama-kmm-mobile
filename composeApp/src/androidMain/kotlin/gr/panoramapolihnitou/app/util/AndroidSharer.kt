package gr.panoramapolihnitou.app.util

import android.content.Context
import android.content.Intent

/** Android share sheet via ACTION_SEND. */
class AndroidSharer(private val context: Context) : Sharer {
    override fun shareText(title: String, text: String, url: String) {
        val body = if (url.isBlank()) text else "$text\n$url"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
