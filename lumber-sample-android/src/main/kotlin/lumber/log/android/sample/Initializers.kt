package lumber.log.android.sample

import android.content.Context
import androidx.startup.Initializer
import lumber.log.Lumber
import lumber.log.android.AndroidDebugTree
import lumber.log.android.BuildConfig

@Suppress("unused")
class LumberInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            Lumber.plant(AndroidDebugTree())
        }
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
