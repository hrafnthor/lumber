package lumber.log.android.sample

import android.app.Activity
import android.os.Bundle
import lumber.log.Lumber
import lumber.log.sample.Sample

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        Sample().performLoggingOperation()
        Lumber.tag("Android").i("This log message originates within a Android codebase")
    }
}