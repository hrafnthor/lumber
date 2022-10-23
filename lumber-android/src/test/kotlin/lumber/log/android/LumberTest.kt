package lumber.log.android

import android.os.Build
import android.util.Log
import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth.assertThat
import junit.framework.Assert.assertTrue
import junit.framework.Assert.fail
import lumber.log.Lumber
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLog.LogItem
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
class LumberTest {
    @Before
    @After
    fun setUpAndTearDown() {
        Lumber.uprootAll()
    }

    // NOTE: This class references the line number. Keep it at the top so it does not change.
    @Test fun debugTreeCanAlterCreatedTag() {
        Lumber.plant(object : AndroidDebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String? {
                return super.createStackElementTag(element) + ':'.toString() + element.lineNumber
            }
        })

        Lumber.d("Test")

        assertLog()
            .hasDebugMessage("LumberTest:38", "Test")
            .hasNoMoreMessages()
    }

    @Test fun debugTreeTagGeneration() {
        Lumber.plant(AndroidDebugTree())
        Lumber.d("Hello, world!")

        assertLog()
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasNoMoreMessages()
    }

    internal inner class ThisIsAReallyLongClassName {
        fun run() {
            Lumber.d("Hello, world!")
        }
    }

    @Config(sdk = [25])
    @Test fun debugTreeTagTruncation() {
        Lumber.plant(AndroidDebugTree())

        ThisIsAReallyLongClassName().run()

        assertLog()
            .hasDebugMessage("LumberTest\$ThisIsAReall", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Config(sdk = [26])
    @Test fun debugTreeTagNoTruncation() {
        Lumber.plant(AndroidDebugTree())

        ThisIsAReallyLongClassName().run()

        assertLog()
            .hasDebugMessage("LumberTest\$ThisIsAReallyLongClassName", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Suppress("ObjectLiteralToLambda") // Lambdas != anonymous classes.
    @Test fun debugTreeTagGenerationStripsAnonymousClassMarker() {
        Lumber.plant(AndroidDebugTree())
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

        assertLog()
            .hasDebugMessage("LumberTest\$debugTreeTag", "Hello, world!")
            .hasDebugMessage("LumberTest\$debugTreeTag", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Suppress("ObjectLiteralToLambda") // Lambdas != anonymous classes.
    @Test fun debugTreeTagGenerationStripsAnonymousClassMarkerWithInnerSAMLambda() {
        Lumber.plant(AndroidDebugTree())
        object : Runnable {
            override fun run() {
                Lumber.d("Hello, world!")

                Runnable { Lumber.d("Hello, world!") }.run()
            }
        }.run()

        assertLog()
            .hasDebugMessage("LumberTest\$debugTreeTag", "Hello, world!")
            .hasDebugMessage("LumberTest\$debugTreeTag", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Suppress("ObjectLiteralToLambda") // Lambdas != anonymous classes.
    @Test fun debugTreeTagGenerationStripsAnonymousClassMarkerWithOuterSAMLambda() {
        Lumber.plant(AndroidDebugTree())

        Runnable {
            Lumber.d("Hello, world!")

            object : Runnable {
                override fun run() {
                    Lumber.d("Hello, world!")
                }
            }.run()
        }.run()

        assertLog()
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasDebugMessage("LumberTest\$debugTreeTag", "Hello, world!")
            .hasNoMoreMessages()
    }

    // NOTE: this will fail on some future version of Kotlin when lambdas are compiled using invokedynamic
    // Fix will be to expect the tag to be "LumberTest" as opposed to "LumberTest\$debugTreeTag"
    @Test
    fun debugTreeTagGenerationStripsAnonymousLambdaClassMarker() {
        Lumber.plant(AndroidDebugTree())

        val outer = {
            Lumber.d("Hello, world!")

            val inner = {
                Lumber.d("Hello, world!")
            }

            inner()
        }

        outer()

        assertLog()
            .hasDebugMessage("LumberTest\$debugTreeTag", "Hello, world!")
            .hasDebugMessage("LumberTest\$debugTreeTag", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Test
    fun debugTreeTagGenerationForSAMLambdasUsesClassName() {
        Lumber.plant(AndroidDebugTree())

        Runnable {
            Lumber.d("Hello, world!")

            Runnable {
                Lumber.d("Hello, world!")
            }.run()
        }.run()

        assertLog()
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasDebugMessage("LumberTest", "Hello, world!")
            .hasNoMoreMessages()
    }

    private class ClassNameThatIsReallyReallyReallyLong {
        init {
            Lumber.i("Hello, world!")
        }
    }

    @Test fun debugTreeGeneratedTagIsLoggable() {
        Lumber.plant(object : AndroidDebugTree() {
            private val MAX_TAG_LENGTH = 23

            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                try {
                    assertTrue(Log.isLoggable(tag, priority))
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        assertTrue(tag!!.length <= MAX_TAG_LENGTH)
                    }
                } catch (e: IllegalArgumentException) {
                    fail(e.message)
                }

                super.log(priority, tag, message, t)
            }
        })
        ClassNameThatIsReallyReallyReallyLong()
        assertLog()
            .hasInfoMessage("LumberTest\$ClassNameTha", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Test fun debugTreeCustomTag() {
        Lumber.plant(AndroidDebugTree())
        Lumber.tag("Custom").d("Hello, world!")

        assertLog()
            .hasDebugMessage("Custom", "Hello, world!")
            .hasNoMoreMessages()
    }

    @Test fun messageWithException() {
        Lumber.plant(AndroidDebugTree())
        val datThrowable = truncatedThrowable(NullPointerException::class.java)
        Lumber.e(datThrowable, "OMFG!")

        assertExceptionLogged(Log.ERROR, "OMFG!", "java.lang.NullPointerException")
    }

    @Test fun exceptionOnly() {
        Lumber.plant(AndroidDebugTree())

        Lumber.v(truncatedThrowable(IllegalArgumentException::class.java))
        assertExceptionLogged(Log.VERBOSE, null, "java.lang.IllegalArgumentException", "LumberTest", 0)

        Lumber.i(truncatedThrowable(NullPointerException::class.java))
        assertExceptionLogged(Log.INFO, null, "java.lang.NullPointerException", "LumberTest", 1)

        Lumber.d(truncatedThrowable(UnsupportedOperationException::class.java))
        assertExceptionLogged(Log.DEBUG, null, "java.lang.UnsupportedOperationException", "LumberTest",
            2)

        Lumber.w(truncatedThrowable(UnknownHostException::class.java))
        assertExceptionLogged(Log.WARN, null, "java.net.UnknownHostException", "LumberTest", 3)

        Lumber.e(truncatedThrowable(ConnectException::class.java))
        assertExceptionLogged(Log.ERROR, null, "java.net.ConnectException", "LumberTest", 4)

        Lumber.log(Log.ASSERT, truncatedThrowable(AssertionError::class.java))
        assertExceptionLogged(Log.ASSERT, null, "java.lang.AssertionError", "LumberTest", 5)
    }

    @Test fun exceptionOnlyCustomTag() {
        Lumber.plant(AndroidDebugTree())

        Lumber.tag("Custom").v(truncatedThrowable(IllegalArgumentException::class.java))
        assertExceptionLogged(Log.VERBOSE, null, "java.lang.IllegalArgumentException", "Custom", 0)

        Lumber.tag("Custom").i(truncatedThrowable(NullPointerException::class.java))
        assertExceptionLogged(Log.INFO, null, "java.lang.NullPointerException", "Custom", 1)

        Lumber.tag("Custom").d(truncatedThrowable(UnsupportedOperationException::class.java))
        assertExceptionLogged(Log.DEBUG, null, "java.lang.UnsupportedOperationException", "Custom", 2)

        Lumber.tag("Custom").w(truncatedThrowable(UnknownHostException::class.java))
        assertExceptionLogged(Log.WARN, null, "java.net.UnknownHostException", "Custom", 3)

        Lumber.tag("Custom").e(truncatedThrowable(ConnectException::class.java))
        assertExceptionLogged(Log.ERROR, null, "java.net.ConnectException", "Custom", 4)

        Lumber.tag("Custom").log(Log.ASSERT, truncatedThrowable(AssertionError::class.java))
        assertExceptionLogged(Log.ASSERT, null, "java.lang.AssertionError", "Custom", 5)
    }

    @Test fun exceptionFromSpawnedThread() {
        Lumber.plant(AndroidDebugTree())
        val datThrowable = truncatedThrowable(NullPointerException::class.java)
        val latch = CountDownLatch(1)
        object : Thread() {
            override fun run() {
                Lumber.e(datThrowable, "OMFG!")
                latch.countDown()
            }
        }.start()
        latch.await()
        assertExceptionLogged(Log.ERROR, "OMFG!", "java.lang.NullPointerException", "LumberTest\$exceptionFro")
    }

    @Test fun nullMessageWithThrowable() {
        Lumber.plant(AndroidDebugTree())
        val datThrowable = truncatedThrowable(NullPointerException::class.java)
        Lumber.e(datThrowable, null)

        assertExceptionLogged(Log.ERROR, "", "java.lang.NullPointerException")
    }

    @Test fun chunkAcrossNewlinesAndLimit() {
        Lumber.plant(AndroidDebugTree())
        Lumber.d(
            'a'.repeat(3000) + '\n'.toString() + 'b'.repeat(6000) + '\n'.toString() + 'c'.repeat(3000))

        assertLog()
            .hasDebugMessage("LumberTest", 'a'.repeat(3000))
            .hasDebugMessage("LumberTest", 'b'.repeat(4000))
            .hasDebugMessage("LumberTest", 'b'.repeat(2000))
            .hasDebugMessage("LumberTest", 'c'.repeat(3000))
            .hasNoMoreMessages()
    }

    @Test fun nullMessageWithoutThrowable() {
        Lumber.plant(AndroidDebugTree())
        Lumber.d(null as String?)

        assertLog().hasNoMoreMessages()
    }

    @Test fun logMessageCallback() {
        val logs = ArrayList<String>()
        Lumber.plant(object : AndroidDebugTree() {
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
        Lumber.log(Log.ASSERT, "Assert")
        Lumber.tag("Custom").log(Log.ASSERT, "Assert")

        assertThat(logs).containsExactly( //
            "2 LumberTest Verbose", //
            "2 Custom Verbose", //
            "3 LumberTest Debug", //
            "3 Custom Debug", //
            "4 LumberTest Info", //
            "4 Custom Info", //
            "5 LumberTest Warn", //
            "5 Custom Warn", //
            "6 LumberTest Error", //
            "6 Custom Error", //
            "7 LumberTest Assert", //
            "7 Custom Assert" //
        )
    }

    @Test fun logAtSpecifiedPriority() {
        Lumber.plant(AndroidDebugTree())

        Lumber.log(Log.VERBOSE, "Hello, World!")
        Lumber.log(Log.DEBUG, "Hello, World!")
        Lumber.log(Log.INFO, "Hello, World!")
        Lumber.log(Log.WARN, "Hello, World!")
        Lumber.log(Log.ERROR, "Hello, World!")
        Lumber.log(Log.ASSERT, "Hello, World!")

        assertLog()
            .hasVerboseMessage("LumberTest", "Hello, World!")
            .hasDebugMessage("LumberTest", "Hello, World!")
            .hasInfoMessage("LumberTest", "Hello, World!")
            .hasWarnMessage("LumberTest", "Hello, World!")
            .hasErrorMessage("LumberTest", "Hello, World!")
            .hasAssertMessage("LumberTest", "Hello, World!")
            .hasNoMoreMessages()
    }

    @Test fun formatting() {
        Lumber.plant(AndroidDebugTree())
        Lumber.v("Hello, %s!", "World")
        Lumber.d("Hello, %s!", "World")
        Lumber.i("Hello, %s!", "World")
        Lumber.w("Hello, %s!", "World")
        Lumber.e("Hello, %s!", "World")
        Lumber.log(Log.ASSERT, "Hello, %s!", "World")

        assertLog()
            .hasVerboseMessage("LumberTest", "Hello, World!")
            .hasDebugMessage("LumberTest", "Hello, World!")
            .hasInfoMessage("LumberTest", "Hello, World!")
            .hasWarnMessage("LumberTest", "Hello, World!")
            .hasErrorMessage("LumberTest", "Hello, World!")
            .hasAssertMessage("LumberTest", "Hello, World!")
            .hasNoMoreMessages()
    }

    @Test fun isLoggableControlsLogging() {
        Lumber.plant(object : AndroidDebugTree() {
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return priority == Log.INFO
            }
        })
        Lumber.v("Hello, World!")
        Lumber.d("Hello, World!")
        Lumber.i("Hello, World!")
        Lumber.w("Hello, World!")
        Lumber.e("Hello, World!")
        Lumber.log(Log.ASSERT, "Hello, World!")

        assertLog()
            .hasInfoMessage("LumberTest", "Hello, World!")
            .hasNoMoreMessages()
    }

    @Test fun isLoggableTagControlsLogging() {
        Lumber.plant(object : AndroidDebugTree() {
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return "FILTER" == tag
            }
        })
        Lumber.tag("FILTER").v("Hello, World!")
        Lumber.d("Hello, World!")
        Lumber.i("Hello, World!")
        Lumber.w("Hello, World!")
        Lumber.e("Hello, World!")
        Lumber.log(Log.ASSERT, "Hello, World!")

        assertLog()
            .hasVerboseMessage("FILTER", "Hello, World!")
            .hasNoMoreMessages()
    }

    @Test fun logsUnknownHostExceptions() {
        Lumber.plant(AndroidDebugTree())
        Lumber.e(truncatedThrowable(UnknownHostException::class.java), null)

        assertExceptionLogged(Log.ERROR, "", "UnknownHostException")
    }

    @Test fun tagIsClearedWhenNotLoggable() {
        Lumber.plant(object : AndroidDebugTree() {
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return priority >= Log.WARN
            }
        })
        Lumber.tag("NotLogged").i("Message not logged")
        Lumber.w("Message logged")

        assertLog()
            .hasWarnMessage("LumberTest", "Message logged")
            .hasNoMoreMessages()
    }

    @Test fun logsWithCustomFormatter() {
        Lumber.plant(object : AndroidDebugTree() {
            override fun formatMessage(message: String, vararg args: Any?): String {
                return String.format("Test formatting: $message", *args)
            }
        })
        Lumber.d("Test message logged. %d", 100)

        assertLog()
            .hasDebugMessage("LumberTest", "Test formatting: Test message logged. 100")
    }

    private fun <T : Throwable> truncatedThrowable(throwableClass: Class<T>): T {
        val throwable = throwableClass.newInstance()
        val stackTrace = throwable.stackTrace
        val traceLength = if (stackTrace.size > 5) 5 else stackTrace.size
        throwable.stackTrace = stackTrace.copyOf(traceLength)
        return throwable
    }

    private fun Char.repeat(number: Int) = toString().repeat(number)

    private fun assertExceptionLogged(
        logType: Int,
        message: String?,
        exceptionClassname: String,
        tag: String? = null,
        index: Int = 0
    ) {
        val logs = getLogs()
        assertThat(logs).hasSize(index + 1)
        val log = logs[index]
        assertThat(log.type).isEqualTo(logType)
        assertThat(log.tag).isEqualTo(tag ?: "LumberTest")

        if (message != null) {
            assertThat(log.msg).startsWith(message)
        }

        assertThat(log.msg).contains(exceptionClassname)
        // We use a low-level primitive that Robolectric doesn't populate.
        assertThat(log.throwable).isNull()
    }

    private fun assertLog(): LogAssert {
        return LogAssert(getLogs())
    }

    private fun getLogs() = ShadowLog.getLogs().filter { it.tag != ROBOLECTRIC_INSTRUMENTATION_TAG }

    private inline fun <reified T : Throwable> assertThrows(body: () -> Unit): ThrowableSubject {
        try {
            body()
        } catch (t: Throwable) {
            if (t is T) {
                return assertThat(t)
            }
            throw t
        }
        throw AssertionError("Expected body to throw ${T::class.java.name} but completed successfully")
    }

    private class LogAssert internal constructor(private val items: List<LogItem>) {
        private var index = 0

        fun hasVerboseMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.VERBOSE, tag, message)
        }

        fun hasDebugMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.DEBUG, tag, message)
        }

        fun hasInfoMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.INFO, tag, message)
        }

        fun hasWarnMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.WARN, tag, message)
        }

        fun hasErrorMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.ERROR, tag, message)
        }

        fun hasAssertMessage(tag: String, message: String): LogAssert {
            return hasMessage(Log.ASSERT, tag, message)
        }

        private fun hasMessage(priority: Int, tag: String, message: String): LogAssert {
            val item = items[index++]
            assertThat(item.type).isEqualTo(priority)
            assertThat(item.tag).isEqualTo(tag)
            assertThat(item.msg).isEqualTo(message)
            return this
        }

        fun hasNoMoreMessages() {
            assertThat(items).hasSize(index)
        }
    }

    private companion object {
        private const val ROBOLECTRIC_INSTRUMENTATION_TAG = "MonitoringInstr"
    }
}
