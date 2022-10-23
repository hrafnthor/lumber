package lumber.log.android

import android.os.Build
import android.util.Log
import lumber.log.Lumber
import java.util.regex.Pattern

/**
 * A [Lumber.DebugTree] that logs to [Log]
 */
open class AndroidDebugTree : Lumber.DebugTree() {

    override fun addToIgnore(set: MutableSet<String>) {
        set.add(AndroidDebugTree::class.java.name)
    }

    override fun createStackElementTag(element: StackTraceElement): String? {
        var tag = element.className.substringAfterLast('.')
        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }
        // Tag length limit was removed in API 26.
        return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
            tag
        } else {
            tag.substring(0, MAX_TAG_LENGTH)
        }
    }

    @Suppress("NAME_SHADOWING")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val priority =  when (priority) {
            Lumber.VERBOSE -> Log.VERBOSE
            Lumber.DEBUG -> Log.DEBUG
            Lumber.INFO -> Log.INFO
            Lumber.WARNING -> Log.WARN
            Lumber.ERROR -> Log.ERROR
            7 -> Log.ASSERT
            else -> throw IllegalArgumentException()
        }

        if (message.length < MAX_LOG_LENGTH) {
            if (priority == Log.ASSERT) {
                Log.wtf(tag, message)
            } else {
                Log.println(priority, tag, message)
            }
            return
        }

        // Split by line, then ensure each line can fit into Log's maximum length.
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            do {
                val end = newline.coerceAtMost(i + MAX_LOG_LENGTH)
                val part = message.substring(i, end)
                if (priority == Log.ASSERT) {
                    Log.wtf(tag, part)
                } else {
                    Log.println(priority, tag, part)
                }
                i = end
            } while (i < newline)
            i++
        }
    }

    companion object {
        private const val MAX_LOG_LENGTH = 4000
        private const val MAX_TAG_LENGTH = 23
        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
    }
}
