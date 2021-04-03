package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.Exception
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class User private constructor (
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString ( " " )
            .capitalize(Locale.ROOT)

    private val initials:String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString ( " " )

    private var phone:String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }

    private var _login:String? = null
    var login:String
        set(value) {
            _login = value?.toLowerCase(Locale.ROOT)
        }
        get() = _login!!

    private var salt: String? = null

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    // for email
    constructor(
            firstName: String,
            lastName: String?,
            email: String,
            password: String
    ): this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary email constructor")
        passwordHash = encrypt(password)
    }

    // for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String?
    ): this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println(" Secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(phone, code)
    }

    // for import from csv
    constructor(
            firstName: String,
            lastName: String?,
            email: String?,
            salt: String,
            passwordHash: String?,
            rawPhone: String?
    ) : this(firstName, lastName, email = email, rawPhone = rawPhone, meta = mapOf("src" to "csv")) {
        println("Import from csv file constructor")
        println("Salt: $salt PasswordHash: $passwordHash")
    }

    init {
        println("First init block, pimary constructor was called")

        check(!firstName.isBlank()) {"Firstname must be not blank"}
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) {"Email or phone must be not blank"}

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash.also {
        println("Cecking passwordHash is $passwordHash")
    }

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) {
        passwordHash = encrypt(newPass)
        if (accessCode.isNullOrEmpty()) accessCode = newPass
        println("Password $oldPass has ben canget on new password $newPass")
        } else throw Exception("The entered does not match the current password")
    }

    private fun encrypt(passord: String): String {
        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        }
        println("Salt while encrypt: $salt")
        return salt.plus(passord).md5()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) // 16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    private fun generateAccessCode(): String {
        val possible = "AjoyDnGrMqRuDynKrAbpySmtWjlQviRcG1234567890573"

        return  StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    fun updateRequestCode(): String {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        println("New access code is $accessCode")
        return code
    }

    private fun sendAccessCodeToUser(phone: String?, code: String) {
        println("....sending access code: $accessCode on $phone")
    }

    companion object Factory {
        fun makeUser (
                fullName: String,
                email: String? = null,
                password: String? = null,
                phone: String? = null,
                salt: String? = null,
                passwordHash: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !passwordHash.isNullOrBlank() -> User(firstName, lastName, email, salt!!,passwordHash, phone)
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
                else -> throw Exception("Email or phone must notbe null or blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?> =
                this.split(" ")
                        .filter { it.isNotBlank() }
                        .run {
                            when(size) {
                                1 -> first() to null
                                2 -> first() to last()
                                else -> throw Exception("Fullname must cotain only first name" +
                                        "and last name, current split result ${this@fullNameToPair}")
                            }
                        }
    }
}
