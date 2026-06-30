package com.example.homenotes

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Инструментальный тест, который выполняется на Android-устройстве.
 *
 * См. [документацию по тестированию](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    // Проверяет, что instrumentation использует ожидаемый пакет приложения.
    @Test
    fun useAppContext() {
        // Контекст тестируемого приложения.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.homenotes", appContext.packageName)
    }
}
