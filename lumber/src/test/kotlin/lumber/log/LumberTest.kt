package lumber.log

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.assertThrows
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.Logger

class LumberTest : FunSpec({

    beforeEach {
        Lumber.uprootAll()
    }

    afterEach {
        Lumber.uprootAll()
        val manager = LogManager.getLogManager()
        manager.loggerNames
            .toList()
            .filter { it.contains("LumberTest") }
            .map { manager.getLogger(it) }
            .filterNotNull()
            .forEach { logger ->
                logger.handlers.forEach { logger.removeHandler(it) }

            }
        manager.reset()
    }

    test("debugTreeCanAlterCreatedTag") {
        var lineNumber = -1
        val logs = InMemoryLogHandler()
        Lumber.plant(object : InMemoryDebugTree(logs) {

            override fun createStackElementTag(element: StackTraceElement): String {
                lineNumber = element.lineNumber
                return super.createStackElementTag(element) + ':'.toString() + element.lineNumber
            }
        })

        Lumber.d("Test")
        logs
            .hasDebugMessage("LumberTest:$lineNumber", "Test")
            .hasNoMoreMessages()
    }

    test("recursion") {
        val lumber = Lumber.asTree()

        assertThrows<IllegalArgumentException> {
            Lumber.plant(lumber)
        }.message shouldBe "Cannot plant Lumber into itself."
    }

    test("treeCount") {
        Lumber.treeCount shouldBe 0
        for (i in 1..50) {
            Lumber.plant(Lumber.DebugTree())
            Lumber.treeCount shouldBe i
        }
        Lumber.uprootAll()
        Lumber.treeCount shouldBe 0
    }

    test("forestReturnsAllPlanted") {
        val tree1 = Lumber.DebugTree()
        val tree2 = Lumber.DebugTree()

        Lumber.plant(tree1)
        Lumber.plant(tree2)

        Lumber.forest().shouldContainExactly(tree1, tree2)
    }

    test("forestReturnsAllTreesPlanted") {
        val tree1 = Lumber.DebugTree()
        val tree2 = Lumber.DebugTree()
        Lumber.plant(tree1, tree2)

        Lumber.forest().shouldContainExactly(tree1, tree2)
    }

    test("uprootThrowsIfMissing") {
        assertThrows<IllegalArgumentException> {
            Lumber.uproot(Lumber.DebugTree())
        }.message shouldStartWith "Cannot uproot tree which is not planted: "
    }

    test("uprootRemovesTree") {
        val log = InMemoryLogHandler()
        val tree1 = InMemoryDebugTree(log)
        val tree2 = InMemoryDebugTree(log)
        Lumber.plant(tree1)
        Lumber.plant(tree2)
        Lumber.d("First")
        Lumber.uproot(tree1)
        Lumber.d("Second")

        log
            .hasDebugMessage("LumberTest", "First")
            .hasDebugMessage("LumberTest", "First")
            .hasDebugMessage("LumberTest", "Second")
            .hasNoMoreMessages()
    }

    test("uprootAllRemovesAll") {
        val log = InMemoryLogHandler()
        val tree1 = InMemoryDebugTree(log)
        val tree2 = InMemoryDebugTree(log)
        Lumber.plant(tree1)
        Lumber.plant(tree2)
        Lumber.d("First")
        Lumber.uprootAll()
        Lumber.d("Second")

        log
            .hasDebugMessage("LumberTest", "First")
            .hasDebugMessage("LumberTest", "First")
            .hasNoMoreMessages()
    }

    test("noArgsDoesNotFormat") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))
        Lumber.d("te%st")

        log
            .hasDebugMessage("LumberTest", "te%st")
            .hasNoMoreMessages()
    }

    test("debugTreeTagGeneration") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))
        Lumber.d("Hello, world!")

        log
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Suppress("ObjectLiteralToLambda")
    test("debugTreeTagGenerationStripsAnonymousClassMarker") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))
        object : Runnable {
            override fun run() {
                Lumber.d("Hello, world!")

                object : Runnable {
                    override fun run() {
                        Lumber.d("Hello, world!")
                    }
                }.run()
            }
        }.run()

        log
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Suppress("ObjectLiteralToLambda")
    test("debugTreeTagGenerationStripsAnonymousClassMarkerWithInnerSAMLambda") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))
        object : Runnable {
            override fun run() {
                Lumber.d("Hello, world!")

                Runnable { Lumber.d("Hello, world!") }.run()
            }
        }.run()

        log
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Suppress("ObjectLiteralToLambda")
    test("debugTreeTagGenerationStripsAnonymousClassMarkerWithOuterSAMLambda") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))

        Runnable {
            Lumber.d("Hello, world!")

            object : Runnable {
                override fun run() {
                    Lumber.d("Hello, world!")
                }
            }.run()
        }.run()

        log
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasNoMoreMessages()
    }

    test("debugTreeTagGenerationStripsAnonymousLambdaClassMarker") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))

        val outer = {
            Lumber.d("Hello, world!")

            val inner = {
                Lumber.d("Hello, world!")
            }

            inner()
        }

        outer()

        log
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasNoMoreMessages()
    }

    test("debugTreeTagGenerationForSAMLambdasUsesClassName") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))

        Runnable {
            Lumber.d("Hello, world!")

            Runnable {
                Lumber.d("Hello, world!")
            }.run()
        }.run()

        log
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasNoMoreMessages()
    }

    test("debugTreeCustomTag") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))
        Lumber.tag("Custom").d("Hello, world!")

        log
            .hasDebugMessage("Custom", "Hello, world!")
            .hasNoMoreMessages()
    }

    test("messageWithException") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))
        val throwable = truncatedThrowable(NullPointerException::class.java)
        Lumber.e(throwable, "Oh, no!")

        log.assertExceptionLogged(
            Level.SEVERE,
            message = "Oh, no!",
            exceptionClassname = "java.lang.NullPointerException"
        )
    }

    test("exceptionOnly") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))

        Lumber.v(truncatedThrowable(IllegalArgumentException::class.java))
        log.assertExceptionLogged(
            Level.ALL,
            message = null,
            exceptionClassname = "java.lang.IllegalArgumentException",
            tag = "LumberTest",
            0
        )

        Lumber.i(truncatedThrowable(NullPointerException::class.java))
        log.assertExceptionLogged(
            Level.INFO,
            message = null,
            exceptionClassname = "java.lang.NullPointerException",
            tag = "LumberTest",
            1
        )

        Lumber.d(truncatedThrowable(UnsupportedOperationException::class.java))
        log.assertExceptionLogged(
            Level.FINE,
            message = null,
            exceptionClassname = "java.lang.UnsupportedOperationException",
            tag = "LumberTest",
            2
        )

        Lumber.w(truncatedThrowable(UnknownHostException::class.java))
        log.assertExceptionLogged(
            Level.WARNING,
            message = null,
            exceptionClassname = "java.net.UnknownHostException",
            tag = "LumberTest",
            3
        )

        Lumber.e(truncatedThrowable(ConnectException::class.java))
        log.assertExceptionLogged(
            Level.SEVERE,
            message = null,
            exceptionClassname = "java.net.ConnectException",
            tag = "LumberTest",
            4
        )
    }

    test("exceptionOnlyCustomTag") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))

        Lumber.tag("Custom").v(truncatedThrowable(IllegalArgumentException::class.java))
        log.assertExceptionLogged(
            Level.ALL,
            message = null,
            exceptionClassname = "java.lang.IllegalArgumentException",
            tag = "Custom",
            0
        )

        Lumber.tag("Custom").i(truncatedThrowable(NullPointerException::class.java))
        log.assertExceptionLogged(
            Level.INFO,
            message = null,
            exceptionClassname = "java.lang.NullPointerException",
            tag = "Custom",
            1
        )

        Lumber.tag("Custom").d(truncatedThrowable(UnsupportedOperationException::class.java))
        log.assertExceptionLogged(
            Level.FINE,
            message = null,
            exceptionClassname = "java.lang.UnsupportedOperationException",
            tag = "Custom",
            2
        )

        Lumber.tag("Custom").w(truncatedThrowable(UnknownHostException::class.java))
        log.assertExceptionLogged(
            Level.WARNING,
            message = null,
            exceptionClassname = "java.net.UnknownHostException",
            tag = "Custom",
            3
        )

        Lumber.tag("Custom").e(truncatedThrowable(ConnectException::class.java))
        log.assertExceptionLogged(
            Level.SEVERE,
            message = null,
            exceptionClassname = "java.net.ConnectException",
            tag = "Custom",
            4
        )
    }

    test("exceptionFromSpawnedThread") {
        val log = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(log))
        val throwable = truncatedThrowable(NullPointerException::class.java)
        val latch = CountDownLatch(1)
        object : Thread() {
            override fun run() {
                Lumber.e(throwable, "Oh, no!")
                latch.countDown()
            }
        }.start()
        latch.await()
        log.assertExceptionLogged(
            Level.SEVERE,
            message = "Oh, no!",
            exceptionClassname = "java.lang.NullPointerException",
            tag = "LumberTest"
        )
    }

    test("nullMessageWithoutThrowable") {
        val logs = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(logs))
        Lumber.d(null as String?)

        logs.hasNoMoreMessages()
    }

    test("logMessageCallback") {
        val logs = mutableListOf<String>()
        Lumber.plant(object : Lumber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                logs.add("$priority $tag $message")
            }
        })

        Lumber.v("Verbose")
        Lumber.tag("Custom").v("Verbose")
        Lumber.d("Debug")
        Lumber.tag("Custom").d("Debug")
        Lumber.i("Info")
        Lumber.tag("Custom").i("Info")
        Lumber.w("Warn")
        Lumber.tag("Custom").w("Warn")
        Lumber.e("Error")
        Lumber.tag("Custom").e("Error")

        logs.shouldContainExactly(
            "1 LumberTest Verbose",
            "1 Custom Verbose",
            "2 LumberTest Debug",
            "2 Custom Debug",
            "3 LumberTest Info",
            "3 Custom Info",
            "4 LumberTest Warn",
            "4 Custom Warn",
            "5 LumberTest Error",
            "5 Custom Error",
        )
    }

    test("logAtSpecificPriority") {
        val logs = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(logs))

        Lumber.log(Lumber.VERBOSE, "Hello, World!")
        Lumber.log(Lumber.DEBUG, "Hello, World!")
        Lumber.log(Lumber.INFO, "Hello, World!")
        Lumber.log(Lumber.WARNING, "Hello, World!")
        Lumber.log(Lumber.ERROR, "Hello, World!")

        logs
            .hasVerboseMessage("LumberTest", "Hello, World!")
            .hasDebugMessage("LumberTest", "Hello, World!")
            .hasInfoMessage("LumberTest", "Hello, World!")
            .hasWarnMessage("LumberTest", "Hello, World!")
            .hasErrorMessage("LumberTest", "Hello, World!")
            .hasNoMoreMessages()
    }

    test("noLogsAtUnknownPriority") {
        val logs = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(logs))

        Lumber.log(Integer.MAX_VALUE, "Hello, World!")
        Lumber.log(Lumber.INFO, "Hello, World!")

        logs
            .hasInfoMessage("LumberTest", "Hello, World!")
            .hasNoMoreMessages()
    }

    test("formatting") {
        val logs = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(logs))

        Lumber.v("Hello, %s!", "World")
        Lumber.d("Hello, %s!", "World")
        Lumber.i("Hello, %s!", "World")
        Lumber.w("Hello, %s!", "World")
        Lumber.e("Hello, %s!", "World")

        logs
            .hasVerboseMessage("LumberTest", "Hello, World!")
            .hasDebugMessage("LumberTest", "Hello, World!")
            .hasInfoMessage("LumberTest", "Hello, World!")
            .hasWarnMessage("LumberTest", "Hello, World!")
            .hasErrorMessage("LumberTest", "Hello, World!")
            .hasNoMoreMessages()
    }

    test("isLoggableControlsLogging") {
        val logs = InMemoryLogHandler()
        Lumber.plant(object : InMemoryDebugTree(logs) {
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return priority == Lumber.INFO
            }
        })

        Lumber.v("Hello, World!")
        Lumber.d("Hello, World!")
        Lumber.i("Hello, World!")
        Lumber.w("Hello, World!")
        Lumber.e("Hello, World!")

        logs
            .hasInfoMessage("LumberTest", "Hello, World!")
            .hasNoMoreMessages()
    }

    test("isLoggableTagControlsLogging") {
        val logs = InMemoryLogHandler()
        Lumber.plant(object : InMemoryDebugTree(logs) {
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return "FILTER" == tag
            }
        })

        Lumber.tag("FILTER").v("Hello, World!")
        Lumber.d("Hello, World!")
        Lumber.i("Hello, World!")
        Lumber.w("Hello, World!")
        Lumber.e("Hello, World!")

        logs
            .hasVerboseMessage("FILTER", "Hello, World!")
            .hasNoMoreMessages()
    }

    test("logsUnknownHostExceptions") {
        val logs = InMemoryLogHandler()
        Lumber.plant(InMemoryDebugTree(logs))
        Lumber.e(truncatedThrowable(UnknownHostException::class.java), null)

        logs
            .assertExceptionLogged(
                Level.SEVERE,
                message = "",
                exceptionClassname = "UnknownHostException"
            )
    }

    test("tagIsClearedWhenNotLoggable") {
        val logs = InMemoryLogHandler()
        Lumber.plant(object : InMemoryDebugTree(logs) {
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return priority >= Lumber.WARNING
            }
        })
        Lumber.tag("NotLogged").i("Message not logged")
        Lumber.w("Message logged")

        logs
            .hasWarnMessage("LumberTest", "Message logged")
            .hasNoMoreMessages()
    }

    test("logsWithCustomFormatter") {
        val logs = InMemoryLogHandler()
        Lumber.plant(object : InMemoryDebugTree(logs) {
            override fun formatMessage(message: String, args: Array<out Any?>): String {
                return String.format("Test formatting: $message", *args)
            }
        })

        Lumber.d("Test message logged. %d", 100)

        logs
            .hasDebugMessage("LumberTest", "Test formatting: Test message logged. 100")
    }
})

private fun <T : Throwable> truncatedThrowable(throwableClass: Class<T>): T {
    val throwable = throwableClass.getDeclaredConstructor().newInstance()
    val stackTrace = throwable.stackTrace
    val traceLength = if (stackTrace.size > 5) 5 else stackTrace.size
    throwable.stackTrace = stackTrace.copyOf(traceLength)
    return throwable
}

private open class InMemoryDebugTree(private val handler: Handler) : Lumber.DebugTree() {

    override fun getLogger(tag: String): Logger {
        return super.getLogger(tag).apply {
            if (!handlers.contains(handler)) { // In case the logger with same tag already exists
                addHandler(handler)
            }
        }
    }
}

private class InMemoryLogHandler : Handler() {

    private var index = 0
    private val records: MutableList<LogRecord> = mutableListOf()

    override fun publish(record: LogRecord?) {
        if (record != null) {
            records.add(record)
        }
    }

    override fun flush() {}

    override fun close() {}

    fun hasVerboseMessage(tag: String, message: String): InMemoryLogHandler {
        return hasMessage(Level.ALL, tag, message)
    }

    fun hasDebugMessage(tag: String, message: String): InMemoryLogHandler {
        return hasMessage(Level.FINE, tag, message)
    }

    fun hasInfoMessage(tag: String, message: String): InMemoryLogHandler {
        return hasMessage(Level.INFO, tag, message)
    }

    fun hasWarnMessage(tag: String, message: String): InMemoryLogHandler {
        return hasMessage(Level.WARNING, tag, message)
    }

    fun hasErrorMessage(tag: String, message: String): InMemoryLogHandler {
        return hasMessage(Level.SEVERE, tag, message)
    }

    fun hasNoMoreMessages() {
        records shouldHaveSize index
    }

    fun assertExceptionLogged(
        level: Level,
        message: String?,
        exceptionClassname: String,
        tag: String? = null,
        index: Int = 0
    ) {
        records shouldHaveSize index + 1
        val record = records[index]
        record.level shouldBe level
        record.loggerName shouldBe (tag ?: "LumberTest")

        if (message != null) {
            record.message shouldStartWith message
        }

        record.message shouldContain exceptionClassname
        record.thrown shouldBe null
    }

    private fun hasMessage(priority: Level, tag: String, message: String): InMemoryLogHandler {
        val item = records[index++]
        item.level shouldBe priority
        item.loggerName shouldBe tag
        item.message shouldBe message
        return this
    }
}
