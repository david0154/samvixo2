package com.example.data

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val FIXED_IV = "WhatsAppConnect1" // 16-character IV for demo/simplicity

    /**
     * Generates a 256-bit key from any password string using SHA-256.
     */
    private fun getSecretKeySpec(keyString: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = keyString.toByteArray(Charsets.UTF_8)
        val hash = digest.digest(bytes)
        return SecretKeySpec(hash, "AES")
    }

    /**
     * Encrypts plain text using AES-256 with the provided key.
     */
    fun encrypt(plainText: String, secretKey: String): String {
        return try {
            val keySpec = getSecretKeySpec(secretKey)
            val ivSpec = IvParameterSpec(FIXED_IV.toByteArray(Charsets.UTF_8))
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            "[Encryption Error: ${e.localizedMessage}]"
        }
    }

    /**
     * Decrypts AES encoded text back into plain text.
     */
    fun decrypt(encryptedText: String, secretKey: String): String {
        return try {
            val keySpec = getSecretKeySpec(secretKey)
            val ivSpec = IvParameterSpec(FIXED_IV.toByteArray(Charsets.UTF_8))
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "[Decryption Failed: Content corrupted or wrong E2E Key]"
        }
    }

    /**
     * Generates a random alphanumeric chat key for E2EE Session.
     */
    fun generateSessionKey(): String {
        val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..16)
            .map { kotlin.random.Random.nextInt(0, charPool.length) }
            .map(charPool::get)
            .joinToString("")
    }
}
