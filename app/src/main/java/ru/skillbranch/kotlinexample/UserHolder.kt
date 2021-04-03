package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.util.*

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser (
            fullName: String,
            email: String,
            password: String
    ): User {
        val login = email.toLowerCase(Locale.ROOT)
        if(fullName == "") fail("Fullname required")

        val matchResult = Regex("^(\\w+)\\s(\\w+)$").find(fullName)
        if (matchResult == null) fail("Wrong format fullname")

        if (!map.containsKey(login)) println("User $fullName register")
        else fail("A user with this email already exists")

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
        if(fullName == "") fail("Fullname required")
        if(rawPhone == "") fail("Phone required")

        val ph = rawPhone.replace("[^+\\d]".toRegex(), "")
        var matchResult = Regex("^\\+([-\\d\\s()]+)?$").find(rawPhone)
        if (matchResult != null) {
            matchResult = Regex("^\\+([0-9]{11}+)$").find(ph)
            if (matchResult == null) fail("Enter a valid phone number starting with a + and containing 11 digits")
        } else fail("Enter a valid phone number starting with a + and containing 11 digits")

        if (!map.containsKey(rawPhone)) println("User $rawPhone register")
        else fail("A user with this phone already exists")
        return User.makeUser(fullName, phone = rawPhone)
                .also { user ->  map[rawPhone] = user}
    }

    fun requestAccessCode(login: String) {
        val user = map[login]
        user?.let {
            it.changePassword(it.accessCode!!, it.updateRequestCode())
        }
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