package journal.gratitude.com.gratitudejournal

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.facebook.testing.screenshot.ScreenshotRunner
import journal.gratitude.com.gratitudejournal.di.TestGratitudeApplication

/**
 * A custom [AndroidJUnitRunner] used to replace the application used in tests with a
 * [TestGratitudeApplication].
 */
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, TestGratitudeApplication::class.java.name, context)
    }

    override fun onCreate(args: Bundle) {
        ScreenshotRunner.onCreate(this, args)
        super.onCreate(args)
    }

    override fun finish(resultCode: Int, results: Bundle) {
        ScreenshotRunner.onDestroy()
        super.finish(resultCode, results)
    }
}
