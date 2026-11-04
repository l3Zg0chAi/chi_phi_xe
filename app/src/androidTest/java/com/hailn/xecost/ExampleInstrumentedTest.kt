
package com.hailn.xecost

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun checkApplicationPackage() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals("com.hailn.xecost", context.packageName)
    }

    @Test
    fun checkApplicationStorage() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        assertTrue(context.filesDir.exists())
    }
}
