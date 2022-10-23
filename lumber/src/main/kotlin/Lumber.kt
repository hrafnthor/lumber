package lumber.log

import org.jetbrains.annotations.NonNls
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern

class Lumber private constructor(){

    init {
        throw AssertionError()
    }

    abstract class Tree {

        @get:JvmSynthetic // Hide from public API.
        internal val explicitTag = ThreadLocal<String>()

        @get:JvmSynthetic // Hide from public API.
        protected open val tag: String?
            get() {
                val tag = explicitTag.get()
                if (tag != null) {
                    explicitTag.remove()
                }
                return tag
            }

        /** Log a verbose message with optional format args. */
        open fun v(message: String?, vararg args: Any?) {
            prepareLog(VERBOSE, null, message, *args)
        }

        /** Log a verbose exception and a message with optional format args. */
        open fun v(t: Throwable?, message: String?, vararg args: Any?) {
            prepareLog(VERBOSE, t, message, *args)
        }

        /** Log a verbose exception. */
        open fun v(t: Throwable?) {
            prepareLog(VERBOSE, t, null)
        }

        /** Log a debug message with optional format args. */
        open fun d(message: String?, vararg args: Any?) {
            prepareLog(DEBUG, null, message, *args)
        }

        /** Log a debug exception and a message with optional format args. */
        open fun d(t: Throwable?, message: String?, vararg args: Any?) {
            prepareLog(DEBUG, t, message, *args)
        }

        /** Log a debug exception. */
        open fun d(t: Throwable?) {
            prepareLog(DEBUG, t, null)
        }

        /** Log an info message with optional format args. */
        open fun i(message: String?, vararg args: Any?) {
            prepareLog(INFO, null, message, *args)
        }

        /** Log an info exception and a message with optional format args. */
        open fun i(t: Throwable?, message: String?, vararg args: Any?) {
            prepareLog(INFO, t, message, *args)
        }

        /** Log an info exception. */
        open fun i(t: Throwable?) {
            prepareLog(INFO, t, null)
        }

        /** Log a warning message with optional format args. */
        open fun w(message: String?, vararg args: Any?) {
            prepareLog(WARNING, null, message, *args)
        }

        /** Log a warning exception and a message with optional format args. */
        open fun w(t: Throwable?, message: String?, vararg args: Any?) {
            prepareLog(WARNING, t, message, *args)
        }

        /** Log a warning exception. */
        open fun w(t: Throwable?) {
            prepareLog(WARNING, t, null)
        }

        /** Log an error message with optional format args. */
        open fun e(message: String?, vararg args: Any?) {
            prepareLog(ERROR, null, message, *args)
        }

        /** Log an error exception and a message with optional format args. */
        open fun e(t: Throwable?, message: String?, vararg args: Any?) {
            prepareLog(ERROR, t, message, *args)
        }

        /** Log an error exception. */
        open fun e(t: Throwable?) {
            prepareLog(ERROR, t, null)
        }

        /** Log at `priority` a message with optional format args. */
        open fun log(priority: Int, message: String?, vararg args: Any?) {
            prepareLog(priority, null, message, *args)
        }

        /** Log at `priority` an exception and a message with optional format args. */
        open fun log(priority: Int, t: Throwable?, message: String?, vararg args: Any?) {
            prepareLog(priority, t, message, *args)
        }

        /** Log at `priority` an exception. */
        open fun log(priority: Int, t: Throwable?) {
            prepareLog(priority, t, null)
        }

        /** Return whether a message at `priority` or `tag` should be logged. */
        protected open fun isLoggable(tag: String?, priority: Int) = true

        @Suppress("NAME_SHADOWING")
        private fun prepareLog(priority: Int, t: Throwable?, message: String?, vararg args: Any?) {
            // Consume tag even when message is not loggable so that next message is correctly tagged.
            val tag = tag
            if (!isLoggable(tag, priority)) {
                return
            }

            var message = message
            if (message.isNullOrEmpty()) {
                if (t == null) {
                    return  // Swallow message if it's null and there's no throwable.
                }
                message = getStackTraceString(t)
            } else {
                if (args.isNotEmpty()) {
                    message = formatMessage(message, args)
                }
                if (t != null) {
                    message += "\n" + getStackTraceString(t)
                }
            }

            log(priority, tag, message, t)
        }

        /** Formats a log message with optional arguments. */
        protected open fun formatMessage(message: String, args: Array<out Any?>) = message.format(*args)

        private fun getStackTraceString(t: Throwable): String {
            val sw = StringWriter(256)
            val pw = PrintWriter(sw, false)
            t.printStackTrace(pw)
            pw.flush()
            return sw.toString()
        }

        /**
         * Write a log message to its destination. Called for all level-specific methods by default.
         *
         * @param priority Log level. See [Level] for constants.
         * @param tag Explicit or inferred tag. May be `null`.
         * @param message Formatted log message.
         * @param t Accompanying exceptions. May be `null`.
         */
        protected abstract fun log(priority: Int, tag: String?, message: String, t: Throwable?)
    }

    /** A [Tree] for debug builds. Automatically infers the tag from the calling class. */
    open class DebugTree: Tree() {
        private val fqcnIgnore: Set<String> = mutableSetOf(
            Lumber::class.java.name,
            Forest::class.java.name,
            Tree::class.java.name,
            DebugTree::class.java.name
        ).apply(::addToIgnore)

        protected open fun addToIgnore(set: MutableSet<String>) {}

        override val tag: String?
            get() = super.tag ?: Throwable().stackTrace
                .first { it.className !in fqcnIgnore }
                .let(::createStackElementTag)

        /**
         * Extract the tag which should be used for the message from the `element`. By default
         * this will use the class name without any anonymous class suffixes (e.g., `Foo$1`
         * becomes `Foo`).
         *
         * Note: This will not be called if a [manual tag][.tag] was specified.
         */
        protected open fun createStackElementTag(element: StackTraceElement): String? {
            var tag = element.className.substringAfterLast('.')
            val m = ANONYMOUS_CLASS.matcher(tag)
            if (m.find()) {
                tag = m.replaceAll("")
            }
            return tag
        }

        /**
         * Retrieves a [Logger] associated with the supplied tag for use in logging.
         */
        internal open fun getLogger(tag: String): Logger {
            return Logger.getLogger(tag).apply {
                level = Level.ALL
            }
        }

        /**
         * Writes a log message to [Logger] associated with the supplied tag.
         *
         * {@inheritDoc}
         */
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val level =  when (priority) {
                VERBOSE -> Level.ALL
                DEBUG -> Level.FINE
                INFO -> Level.INFO
                WARNING -> Level.WARNING
                ERROR -> Level.SEVERE
                else -> null
            }
            if (level != null) {
                getLogger(tag ?: "").log(level, message)
            }
        }

        companion object {
            private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\w+)+")
        }
    }

    companion object Forest : Tree() {

        const val VERBOSE = 2
        const val DEBUG = 3
        const val INFO = 4
        const val WARNING = 5
        const val ERROR = 6

        /** Log a verbose message with optional format args. */
        @JvmStatic override fun v(@NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.v(message, *args) }
        }

        /** Log a verbose exception and a message with optional format args. */
        @JvmStatic override fun v(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.v(t, message, *args) }
        }

        /** Log a verbose exception. */
        @JvmStatic override fun v(t: Throwable?) {
            treeArray.forEach { it.v(t) }
        }

        /** Log a debug message with optional format args. */
        @JvmStatic override fun d(@NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.d(message, *args) }
        }

        /** Log a debug exception and a message with optional format args. */
        @JvmStatic override fun d(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.d(t, message, *args) }
        }

        /** Log a debug exception. */
        @JvmStatic override fun d(t: Throwable?) {
            treeArray.forEach { it.d(t) }
        }

        /** Log an info message with optional format args. */
        @JvmStatic override fun i(@NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.i(message, *args) }
        }

        /** Log an info exception and a message with optional format args. */
        @JvmStatic override fun i(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.i(t, message, *args) }
        }

        /** Log an info exception. */
        @JvmStatic override fun i(t: Throwable?) {
            treeArray.forEach { it.i(t) }
        }

        /** Log a warning message with optional format args. */
        @JvmStatic override fun w(@NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.w(message, *args) }
        }

        /** Log a warning exception and a message with optional format args. */
        @JvmStatic override fun w(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.w(t, message, *args) }
        }

        /** Log a warning exception. */
        @JvmStatic override fun w(t: Throwable?) {
            treeArray.forEach { it.w(t) }
        }

        /** Log an error message with optional format args. */
        @JvmStatic override fun e(@NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.e(message, *args) }
        }

        /** Log an error exception and a message with optional format args. */
        @JvmStatic override fun e(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.e(t, message, *args) }
        }

        /** Log an error exception. */
        @JvmStatic override fun e(t: Throwable?) {
            treeArray.forEach { it.e(t) }
        }

        /** Log at `priority` a message with optional format args. */
        @JvmStatic override fun log(priority: Int, @NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.log(priority, message, *args) }
        }

        /** Log at `priority` an exception and a message with optional format args. */
        @JvmStatic
        override fun log(priority: Int, t: Throwable?, @NonNls message: String?, vararg args: Any?) {
            treeArray.forEach { it.log(priority, t, message, *args) }
        }

        /** Log at `priority` an exception. */
        @JvmStatic override fun log(priority: Int, t: Throwable?) {
            treeArray.forEach { it.log(priority, t) }
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            throw AssertionError() // Missing override for log method.
        }

        /**
         * A view into Lumber's planted trees as a tree itself. This can be used for injecting a logger
         * instance rather than using static methods or to facilitate testing.
         */
        @Suppress(
            "NOTHING_TO_INLINE", // Kotlin users should reference `Tree.Forest` directly.
        )
        @JvmStatic inline fun asTree(): Tree = this

        /** Set a one-time tag for use on the next logging call. */
        @JvmStatic  fun tag(tag: String): Tree {
            for (tree in treeArray) {
                tree.explicitTag.set(tag)
            }
            return this
        }

        /** Add a new logging tree. */
        @JvmStatic fun plant(tree: Tree) {
            require(tree !== this) { "Cannot plant Lumber into itself." }
            synchronized(trees) {
                trees.add(tree)
                treeArray = trees.toTypedArray()
            }
        }

        /** Adds new logging trees. */
        @JvmStatic fun plant(vararg trees: Tree) {
            for (tree in trees) {
                requireNotNull(tree) { "trees contained null" }
                require(tree !== this) { "Cannot plant Lumber into itself." }
            }
            synchronized(this.trees) {
                Collections.addAll(this.trees, *trees)
                treeArray = this.trees.toTypedArray()
            }
        }

        /** Remove a planted tree. */
        @JvmStatic fun uproot(tree: Tree) {
            synchronized(trees) {
                require(trees.remove(tree)) { "Cannot uproot tree which is not planted: $tree" }
                treeArray = trees.toTypedArray()
            }
        }

        /** Remove all planted trees. */
        @JvmStatic fun uprootAll() {
            synchronized(trees) {
                trees.clear()
                treeArray = emptyArray()
            }
        }

        /** Return a copy of all planted [trees][Tree]. */
        @JvmStatic fun forest(): List<Tree> {
            synchronized(trees) {
                return Collections.unmodifiableList(trees.toList())
            }
        }

        @get:[JvmStatic JvmName("treeCount")]
        val treeCount get() = treeArray.size

        // Both fields guarded by 'trees'.
        private val trees = ArrayList<Tree>()
        @Volatile private var treeArray = emptyArray<Tree>()
    }
}
