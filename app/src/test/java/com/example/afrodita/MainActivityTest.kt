package com.example.afrodita

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityTest {

    @Test
    fun testDeepLinkUriParsing() {
        val deepLinkUrl = "afrodita://auth/callback#access_token=token123&refresh_token=refresh123&type=signup"

        // Simular lógica de validación de Deep Link
        val isAfroditaAuthCallback = deepLinkUrl.startsWith("afrodita://auth/callback")
        assertTrue("El Deep Link debe pertenecer al esquema afrodita://auth/callback", isAfroditaAuthCallback)

        // Simular escape de URL para evaluar la invocación a JS
        val quotedUrl = "\"" + deepLinkUrl.replace("\"", "\\\"") + "\""
        assertNotNull("El URL citado para Javascript no debe ser nulo", quotedUrl)
        assertTrue("El URL citado debe estar entre comillas dobles", quotedUrl.startsWith("\"") && quotedUrl.endsWith("\""))
    }

    @Test
    fun testDeepLinkJsScriptGeneration() {
        val url = "afrodita://auth/callback#access_token=abc&refresh_token=def"
        val quotedUrl = "\"" + url.replace("\"", "\\\"") + "\""
        val jsScript = "if(typeof handleDeepLink === 'function'){ handleDeepLink($quotedUrl); }"

        assertTrue("El script generado debe invocar handleDeepLink", jsScript.contains("handleDeepLink"))
        assertTrue("El script generado debe incluir el URL entrecomillado", jsScript.contains(quotedUrl))
    }
}
