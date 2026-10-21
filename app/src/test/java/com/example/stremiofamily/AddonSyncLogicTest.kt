package com.example.stremiofamily

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class AddonSyncLogicTest {
    private fun addon(url: String) = AddonDescriptor(JSONObject().put("transportUrl", url))

    @Test fun countsAdditionsAndRemovalsByAddonUrl() {
        val difference = addonDifference(
            listOf(addon("https://a/manifest.json"), addon("https://b/manifest.json")),
            listOf(addon("https://b/manifest.json"), addon("https://c/manifest.json"))
        )
        assertEquals(1, difference.additions)
        assertEquals(1, difference.removals)
    }

    @Test fun identicalCollectionsHaveNoChanges() {
        val difference = addonDifference(listOf(addon("https://a/manifest.json")), listOf(addon("https://a/manifest.json")))
        assertEquals(0, difference.additions)
        assertEquals(0, difference.removals)
    }
}
