package hs.flensburg.marlin.business.api.auth.control

import org.mindrot.jbcrypt.BCrypt

object Hashing {
    fun hashPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())

    fun verifyPassword(password: String, hashed: String): Boolean = BCrypt.checkpw(password, hashed)
}