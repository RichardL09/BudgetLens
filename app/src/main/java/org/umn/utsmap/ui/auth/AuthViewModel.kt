package org.umn.utsmap.ui.auth



import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.setValue

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch



data class DemoUser(val name: String, val email: String, val phone: String = "", val id: String = email, val photo: String = "", val verified: Boolean = false)

private data class DemoAccount(val user: DemoUser, val password: String)



object AuthValidation {

    fun name(value: String): String? = when {

        value.trim().isEmpty() -> "Nama lengkap wajib diisi."

        value.trim().length < 2 -> "Nama minimal 2 karakter."

        else -> null

    }

    fun email(value: String): String? = when {

        value.trim().isEmpty() -> "Email wajib diisi."

        !Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$").matches(value.trim()) -> "Format email tidak valid."

        else -> null

    }

    fun password(value: String): String? = when {

        value.isBlank() -> "Password wajib diisi."

        value.length < 8 -> "Password minimal 8 karakter."

        else -> null

    }

    fun confirmation(password: String, value: String): String? = when {

        value.isEmpty() -> "Konfirmasi password wajib diisi."

        value != password -> "Konfirmasi password belum sama."

        else -> null

    }

}



/** Simulasi UTS: akun dan sesi hanya di memori, tanpa server atau penyimpanan password ke disk. */

class AuthViewModel : ViewModel() {

    private val accounts = mutableListOf(

        DemoAccount(DemoUser("Certified", "certified@email.com", "6281230000001", verified = true), "c3rt1f1ed")

    )

    var user by mutableStateOf<DemoUser?>(null)

        private set

    var register by mutableStateOf(false)

        private set

    var phone by mutableStateOf("")

    var name by mutableStateOf("")

    var email by mutableStateOf("")

    var password by mutableStateOf("")

    var confirmation by mutableStateOf("")

    var submitted by mutableStateOf(false)

        private set

    var loading by mutableStateOf(false)

        private set

    var error by mutableStateOf<String?>(null)

        private set



    fun switchMode(isRegister: Boolean) {

        if (loading || register == isRegister) return

        register = isRegister

        password = ""

        confirmation = ""

        submitted = false

        error = null

    }



    fun clearError() { error = null }



    fun submit() {

        if (loading) return

        submitted = true

        error = null

        if (AuthValidation.email(email) != null || AuthValidation.password(password) != null ||

            (register && (AuthValidation.name(name) != null || (phone.isNotBlank() && !Regex("62[0-9]{8,13}").matches(normalizePhone(phone))) ||

                AuthValidation.confirmation(password, confirmation) != null))) return



        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)

        val enteredPassword = password

        val enteredName = name.trim()

        val enteredPhone = if (phone.isBlank()) "" else normalizePhone(phone)

        val creating = register

        loading = true

        viewModelScope.launch {

            try {

                delay(600) // Menampilkan loading pada simulasi autentikasi.

                val existing = accounts.find { it.user.email == normalizedEmail }

                if (creating) {

                    if (creating && enteredPhone.isNotEmpty() && accounts.any { it.user.phone == enteredPhone }) {

                        error = "Nomor telepon sudah terdaftar."

                    } else if (existing != null) {

                        error = "Email sudah terdaftar. Silakan masuk."

                    } else {

                        val account = DemoAccount(DemoUser(enteredName, normalizedEmail, enteredPhone), enteredPassword)

                        accounts.add(account)

                        user = account.user

                    }

                } else if (existing == null || existing.password != enteredPassword) {

                    error = "Email atau password salah."

                } else {

                    user = existing.user

                }

                if (user != null) {

                    password = ""

                    confirmation = ""

                    submitted = false

                }

            } finally {

                loading = false

            }

        }

    }



    fun updateName(value: String) {

        if (AuthValidation.name(value) != null || value.trim().length > 80) return

        val current = user ?: return

        val updated = current.copy(name = value.trim())

        val index = accounts.indexOfFirst { it.user.email == current.email }

        if (index >= 0) accounts[index] = accounts[index].copy(user = updated)

        user = updated

    }



    fun findByPhone(value: String): DemoUser? {

        val phone = normalizePhone(value)

        return (accounts.map { it.user } + starterFriends).find { it.phone.isNotEmpty() && normalizePhone(it.phone) == phone && it.email != user?.email }

    }

    fun updatePhone(value: String): String? {

        val normalized = normalizePhone(value)

        if (!Regex("62[0-9]{8,13}").matches(normalized)) return "Nomor telepon tidak valid. Gunakan 08… atau +62…."

        val current = user ?: return "Silakan masuk terlebih dahulu."

        if (accounts.any { it.user.email != current.email && normalizePhone(it.user.phone) == normalized }) return "Nomor sudah digunakan akun lain."

        val updated = current.copy(phone = normalized)

        val index = accounts.indexOfFirst { it.user.email == current.email }

        if (index >= 0) accounts[index] = accounts[index].copy(user = updated)

        user = updated

        return null

    }

    companion object {

        fun normalizePhone(value: String): String {

            val digits = value.filter { it.isDigit() }

            return if (digits.startsWith("0")) "62" + digits.drop(1) else digits

        }

    }

    private val starterFriends = listOf(
        DemoUser("Marcelline", "marcelline@budgetlens.local", "6281230000011", photo = ""),
        DemoUser("Richard", "richard@budgetlens.local", "6281230000012"),
        DemoUser("Dwiky", "dwiky@budgetlens.local", "6281230000013"),
        DemoUser("Michael", "michael@budgetlens.local", "6281230000014")
    )
    private val friendBook = mutableStateMapOf<String, List<DemoUser>>()
    private val outgoing = mutableStateMapOf<String, List<DemoUser>>()
    private val incoming = mutableStateMapOf<String, List<DemoUser>>()
    val friends get() = user?.id?.let { (friendBook[it] ?: starterFriends).filterNot { friend -> friend.id == it } } ?: starterFriends
    val friendRequests get() = user?.id?.let { incoming[it] ?: emptyList() } ?: emptyList()
    val sentRequests get() = user?.id?.let { outgoing[it] ?: emptyList() } ?: emptyList()
    fun searchPerson(phone: String): DemoUser? = (accounts.map { it.user } + starterFriends).firstOrNull {
        it.phone.isNotEmpty() && normalizePhone(it.phone) == normalizePhone(phone) && it.id != user?.id
    }
    fun requestFriend(phone: String): String? {
        val current = user ?: return "Silakan masuk."
        val target = searchPerson(phone) ?: return "Nomor belum terdaftar."
        if (friends.any { it.id == target.id }) return "Sudah ada di daftar teman."
        if (sentRequests.any { it.id == target.id }) return "Permintaan sudah dikirim."
        outgoing[current.id] = sentRequests + target
        incoming[target.id] = (incoming[target.id] ?: emptyList()) + current
        return null
    }
    fun acceptFriend(id: String): Boolean {
        val current = user ?: return false
        val target = friendRequests.firstOrNull { it.id == id } ?: return false
        friendBook[current.id] = (friends + target).distinctBy { it.id }
        friendBook[target.id] = ((friendBook[target.id] ?: starterFriends.filter { it.id != target.id }) + current).distinctBy { it.id }
        incoming[current.id] = friendRequests.filterNot { it.id == id }
        outgoing[target.id] = (outgoing[target.id] ?: emptyList()).filterNot { it.id == current.id }
        return true
    }
    fun rejectFriend(id: String) {
        val current = user ?: return
        incoming[current.id] = friendRequests.filterNot { it.id == id }
        outgoing[id] = (outgoing[id] ?: emptyList()).filterNot { it.id == current.id }
    }
    fun cancelFriendRequest(id: String) { user?.let { current ->
        outgoing[current.id] = sentRequests.filterNot { it.id == id }
        incoming[id] = (incoming[id] ?: emptyList()).filterNot { it.id == current.id }
    } }
    var hideBalances by mutableStateOf(false)
    var confirmDeletion by mutableStateOf(true)
    var defaultIncome by mutableStateOf(false)
    var sortNewest by mutableStateOf(true)
    fun loadPreferences(context: android.content.Context) {
        val prefs = context.getSharedPreferences("appearance", 0)
        hideBalances = prefs.getBoolean("hide", false); confirmDeletion = prefs.getBoolean("confirm", true)
        defaultIncome = prefs.getBoolean("defaultIncome", false); sortNewest = prefs.getBoolean("newest", true)
    }
    fun setPreference(key: String, value: Boolean, context: android.content.Context) {
        when(key) { "hide" -> hideBalances=value; "confirm" -> confirmDeletion=value; "defaultIncome" -> defaultIncome=value; "newest" -> sortNewest=value; else -> return }
        context.getSharedPreferences("appearance", 0).edit().putBoolean(key,value).apply()
    }
    var themeMode by mutableStateOf("system")
        private set
    fun loadSettings(context: android.content.Context) { themeMode = context.getSharedPreferences("appearance", 0).getString("theme", "system") ?: "system" }
    fun changeTheme(value: String, context: android.content.Context) {
        if (value !in listOf("system", "light", "dark")) return
        themeMode = value
        context.getSharedPreferences("appearance", 0).edit().putString("theme", value).apply()
    }
    private fun replaceUser(updated: DemoUser) {
        val index = accounts.indexOfFirst { it.user.id == updated.id }
        if (index >= 0) accounts[index] = accounts[index].copy(user = updated)
        user = updated
    }
    fun editProfile(newName: String, newPhone: String, photo: String): String? {
        AuthValidation.name(newName)?.let { return it }
        if (newName.trim().length > 80) return "Nama maksimal 80 karakter."
        val current = user ?: return "Silakan masuk."
        val normalized = normalizePhone(newPhone)
        if (!Regex("62[0-9]{8,13}").matches(normalized)) return "Nomor telepon tidak valid."
        if (accounts.any { it.user.id != current.id && it.user.phone == normalized }) return "Nomor telepon sudah terdaftar."
        replaceUser(current.copy(name = newName.trim(), phone = normalized, photo = photo))
        return null
    }
    fun changePassword(old: String, next: String, confirm: String): String? {
        val current = user ?: return "Silakan masuk."
        val index = accounts.indexOfFirst { it.user.id == current.id }
        if (index < 0 || accounts[index].password != old) return "Password saat ini salah."
        AuthValidation.password(next)?.let { return it }
        AuthValidation.confirmation(next, confirm)?.let { return it }
        if (next == old) return "Gunakan password yang berbeda."
        accounts[index] = accounts[index].copy(password = next)
        return null
    }
    var pendingEmail by mutableStateOf<String?>(null)
        private set
    private var emailExpires = 0L
    private var emailAttempts = 0
    fun requestEmail(value: String, currentPassword: String): String? {
        val current = user ?: return "Silakan masuk."
        AuthValidation.email(value)?.let { return it }
        val normalized = value.trim().lowercase(java.util.Locale.ROOT)
        if (accounts.firstOrNull { it.user.id == current.id }?.password != currentPassword) return "Password saat ini salah."
        if (accounts.any { it.user.id != current.id && it.user.email == normalized }) return "Email sudah digunakan."
        pendingEmail = normalized; emailExpires = System.currentTimeMillis() + 300000; emailAttempts = 0
        return null
    }
    fun verifyEmail(code: String): String? {
        val current = user ?: return "Silakan masuk."
        val address = pendingEmail ?: return "Minta kode verifikasi terlebih dahulu."
        if (System.currentTimeMillis() > emailExpires || emailAttempts >= 5) return "Kode kedaluwarsa. Minta kode baru."
        emailAttempts++
        if (code != "123456") return "Kode verifikasi salah."
        if (accounts.any { it.user.id != current.id && it.user.email == address }) return "Email sudah digunakan."
        replaceUser(current.copy(email = address, verified = true)); email = address; pendingEmail = null
        return null
    }
    fun logout() {

        pendingEmail = null
        user = null

        password = ""

        confirmation = ""

        submitted = false

        error = null

        register = false

    }

}



