package com.khuongnd.dexassistant.ui.auth

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.Base64

class ChatGptOidcVerifierTest {
    private val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private fun b64(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    private val key = pair.public as RSAPublicKey
    private fun unsigned(bytes: ByteArray) = bytes.dropWhile { it == 0.toByte() }.toByteArray()
    private fun jwks() = JSONObject().put("keys", JSONArray().put(JSONObject().put("kid", "key").put("kty", "RSA")
        .put("n", b64(unsigned(key.modulus.toByteArray()))).put("e", b64(unsigned(key.publicExponent.toByteArray())))))
    private fun claims() = JSONObject().put("sub", "subject").put("email", "test@example.invalid")
        .put("iss", "https://auth.openai.com").put("aud", "oaiapp_test").put("nonce", "nonce")
        .put("iat", 900).put("exp", 2000)
    private fun jwt(claims: JSONObject, alg: String = "RS256"): String {
        val data = b64(JSONObject().put("alg", alg).put("kid", "key").toString().toByteArray()) + "." + b64(claims.toString().toByteArray())
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(pair.private); update(data.toByteArray(Charsets.US_ASCII))
        }.sign()
        return data + "." + b64(signature)
    }
    private fun rejects(token: String) {
        try { ChatGptOidcVerifier.verify(token, "oaiapp_test", "nonce", jwks(), 1000); fail("invalid token accepted") }
        catch (_: IllegalArgumentException) {}
    }
    @Test fun validatesSignatureIssuerAudienceNonceAndLifetime() {
        assertEquals("subject", ChatGptOidcVerifier.verify(jwt(claims()), "oaiapp_test", "nonce", jwks(), 1000).subject)
        rejects(jwt(claims().put("nonce", "wrong")))
        rejects(jwt(claims().put("iss", "https://other.invalid")))
        rejects(jwt(claims().put("aud", "other")))
        rejects(jwt(claims().put("exp", 800)))
        rejects(jwt(claims().put("iat", 1200)))
        rejects(jwt(claims().put("nbf", 1200)))
        rejects(jwt(claims().put("sub", "")))
        rejects(jwt(claims(), "none"))
        val token = jwt(claims()); rejects(token.substringBeforeLast('.') + "." + b64(ByteArray(256)))
        rejects("malformed")
    }
    @Test fun multipleAudiencesRequireAuthorizedParty() {
        rejects(jwt(claims().put("aud", JSONArray(listOf("oaiapp_test", "other")))))
        val good = claims().put("aud", JSONArray(listOf("oaiapp_test", "other"))).put("azp", "oaiapp_test")
        assertEquals("subject", ChatGptOidcVerifier.verify(jwt(good), "oaiapp_test", "nonce", jwks(), 1000).subject)
    }
}
