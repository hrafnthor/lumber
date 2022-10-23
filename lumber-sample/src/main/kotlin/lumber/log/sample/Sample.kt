package lumber.log.sample

import lumber.log.Lumber

class Sample {

    fun performLoggingOperation() {
        Lumber.tag("Kotlin").i("This log message originates inside of a Kotlin only module")
    }
}
