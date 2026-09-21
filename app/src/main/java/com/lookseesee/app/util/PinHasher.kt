package com.lookseesee.app.util

import java.security.MessageDigest

/**
 * Hashes the parent's PIN before it's persisted. This is a lightweight deterrent
 * against a toddler (or a curious sibling) reading the raw PIN out of app data,
 * not a defense against a determined attacker with root access.
 */
object PinHasher {
    fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun matches(pin: String, storedHash: String): Boolean = hash(pin) == storedHash
}
