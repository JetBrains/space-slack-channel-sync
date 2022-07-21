package org.jetbrains.spaceSlackSync

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun String.encrypted(): ByteArray {
    val salt = ByteArray(10).run {
        rnd.get().nextBytes(this)
        joinToString(separator = "") { it.toString(16) }
    }
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKeySpec = SecretKeySpec(masterSecret, "AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, IvParameterSpec(ByteArray(16)))
    return cipher.doFinal((this + delimiter + salt).toByteArray())
}

fun ByteArray.decrypted(): String {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKeySpec = SecretKeySpec(masterSecret, "AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(ByteArray(16)))
    return String(cipher.doFinal(this)).substringBeforeLast(delimiter)
}

private val rnd = object : ThreadLocal<java.security.SecureRandom>() {
    override fun initialValue() = java.security.SecureRandom()
}

private const val delimiter = ":"

private val masterSecret =
    config.getString("app.encryptionKey").ifBlank { error("Encryption key should not be empty") }.toByteArray()
