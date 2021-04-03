package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.Exception
import java.util.*

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser (
            fullName: String,
            email: String,
            password: String
    ): User {
        val login = email.toLowerCase(Locale.ROOT)

        when {
            fullName.isNotEmpty() -> println("Fullname is $fullName")
            else -> throw Exception("Fullname required")
        }

        val matchResult = Regex("^(\\w+)\\s(\\w+)$").find(fullName)
        if (matchResult == null) throw Exception("Wrong format fullname")

        if (!map.containsKey(login)) println("User $fullName register")
        else throw Exception("A user with this email already exists")

        return User.makeUser(fullName, email = email, password = password)
                .also { user ->  map[user.login] = user}
    }

    fun loginUser(login: String, password: String): String? =
            map[login.trim()]?.let {
                if (it.checkPassword(password)) it.userInfo
                else null
            }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        if(fullName == "") throw Exception("Fullname required")
        if(rawPhone == "") throw Exception("Phone required")

        val ph = rawPhone.replace("[^+\\d]".toRegex(), "")
        var matchResult = Regex("^\\+([-\\d\\s()]+)?$").find(rawPhone)
        if (matchResult != null) {
            matchResult = Regex("^\\+([0-9]{11}+)$").find(ph)
            if (matchResult == null) throw Exception("Enter a valid phone number starting with a + and containing 11 digits")
        } else throw Exception("Enter a valid phone number starting with a + and containing 11 digits")

        if (!map.containsKey(rawPhone)) println("User $rawPhone register")
        else throw Exception("A user with this phone already exists")
        return User.makeUser(fullName, phone = rawPhone)
                .also { user ->  map[rawPhone] = user}
    }

    fun requestAccessCode(login: String) {
        map[login]?.updateRequestCode()
    }

    fun importUsers(list: List<String>) : List<User> {

        val resData = mutableListOf<User>()

        for (line in list) {
            val userData = line.split(";")
            val (salt, passwordHash) = userData[2].split(":")
            val user = User.makeUser(
                    fullName = userData[0],
                    email = userData[1],
                    phone = if (userData[3].isEmpty()) null else userData[3],
                    salt = salt,
                    passwordHash = passwordHash
            ).also { user -> map[user.login] = user }
            resData.add(user)
        }

        return resData
    }

    fun fail(message: String): Nothing {
        throw IllegalArgumentException(message)
    }
}