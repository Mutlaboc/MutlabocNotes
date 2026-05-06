package com.example.mutlabocsnotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ManifestRegressionTest {

    @Test
    fun exactAlarmPermissionStrategyStaysSingleAndExplicit() {
        val permissions = manifest.children("uses-permission")
            .map { it.androidAttribute("name") }

        assertEquals(
            1,
            permissions.count { it == "android.permission.SCHEDULE_EXACT_ALARM" }
        )
        assertEquals(
            0,
            permissions.count { it == "android.permission.USE_EXACT_ALARM" }
        )
    }

    @Test
    fun yandexClientIdMetadataIsDeclaredOnce() {
        val yandexMetadata = application.children("meta-data")
            .filter { it.androidAttribute("name") == YANDEX_CLIENT_ID_METADATA }

        assertEquals(1, yandexMetadata.size)
        assertEquals(YANDEX_CLIENT_ID_PLACEHOLDER, yandexMetadata.single().androidAttribute("value"))
    }

    @Test
    fun yandexAuthCallbackIntentFilterKeepsSdkContract() {
        val mainActivity = application.children("activity")
            .single { it.androidAttribute("name") == ".MainActivity" }
        val callbackFilter = mainActivity.children("intent-filter")
            .single { filter ->
                filter.children("action").any {
                    it.androidAttribute("name") == "android.intent.action.VIEW"
                }
            }

        val categories = callbackFilter.children("category")
            .map { it.androidAttribute("name") }
        assertTrue(categories.contains("android.intent.category.DEFAULT"))
        assertTrue(categories.contains("android.intent.category.BROWSABLE"))

        val callbackData = callbackFilter.children("data")
            .singleOrNull {
                it.androidAttribute("scheme") == "yandexauth" &&
                    it.androidAttribute("host") == YANDEX_CLIENT_ID_PLACEHOLDER
            }
        assertNotNull(callbackData)
    }

    private val manifest: Element by lazy {
        val source = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        ).first { it.isFile }

        DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(source)
            .documentElement
    }

    private val application: Element by lazy {
        manifest.children("application").single()
    }

    private fun Element.children(tagName: String): List<Element> {
        return childNodes.asSequence()
            .filterIsInstance<Element>()
            .filter { it.tagName == tagName }
            .toList()
    }

    private fun Element.androidAttribute(name: String): String {
        return getAttributeNS(ANDROID_NAMESPACE, name)
    }

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<Node> {
        return (0 until length).asSequence().map { item(it) }
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val YANDEX_CLIENT_ID_METADATA = "com.yandex.auth.CLIENT_ID"
        const val YANDEX_CLIENT_ID_PLACEHOLDER = "\${YANDEX_CLIENT_ID}"
    }
}
