package com.example.stremiofamily

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StremioApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: StremioApi

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        api = StremioApi(server.url("/").toString())
    }

    @After fun tearDown() = server.shutdown()

    @Test fun loginUsesStremioPostContractAndReturnsSession() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"result":{"authKey":"secret-session","user":{"_id":"user-1","email":"one@example.com"}}}"""))
        val session = api.login("one@example.com", "not-logged")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/login", request.path)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("one@example.com", body.getString("email"))
        assertEquals("not-logged", body.getString("password"))
        assertFalse(body.has("authKey"))
        assertEquals("user-1", session.userId)
        assertEquals("secret-session", session.authKey)
    }

    @Test fun readsAddonDescriptorsAndSendsAuthenticatedReadRequest() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"result":{"lastModified":"2026-10-21T00:00:00Z","addons":[{"transportUrl":"https://example.org/manifest.json","manifest":{"id":"x","name":"Example"},"flags":{}}]}}"""))
        val collection = api.getAddonCollection("session-123")
        val body = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals("session-123", body.getString("authKey"))
        assertTrue(body.getBoolean("update"))
        assertEquals(0, body.getJSONArray("addFromURL").length())
        assertEquals("Example", collection.addons.single().name)
        assertEquals("https://example.org/manifest.json", collection.addons.single().transportUrl)
    }

    @Test fun writesWholeDescriptorCollectionWithoutDroppingFields() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"result":true}"""))
        val descriptor = AddonDescriptor(JSONObject("""{"transportUrl":"https://x/manifest.json","manifest":{"id":"id","name":"X"},"flags":{"pinned":true},"extra":"kept"}"""))
        api.setAddonCollection("session-123", listOf(descriptor))
        val request = server.takeRequest()
        assertEquals("/api/addonCollectionSet", request.path)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("session-123", body.getString("authKey"))
        assertEquals("kept", body.getJSONArray("addons").getJSONObject(0).getString("extra"))
        assertTrue(body.getJSONArray("addons").getJSONObject(0).getJSONObject("flags").getBoolean("pinned"))
    }

    @Test fun reportsRejectedLoginWithoutPersistingCredentials() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"error":"Authentication failed"}"""))
        val error = try { api.login("user@example.com", "wrong") ; null } catch (e: StremioApiException) { e }
        assertEquals("Authentication failed", error?.message)
        assertFalse(error?.message.orEmpty().contains("wrong"))
    }
}
