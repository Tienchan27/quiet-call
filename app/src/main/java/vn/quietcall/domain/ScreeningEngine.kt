package vn.quietcall.domain

import java.text.Normalizer

data class Brand(val id: Long, val name: String, val note: String, val blocked: Boolean, val numbers: List<String>, val updatedAt: Long = 0)
data class Preferences(val protection: Boolean = true, val silence: Boolean = false, val protectContacts: Boolean = true)
data class Snapshot(val brands: List<Brand>, val allowed: Set<String>, val preferences: Preferences)
enum class Action { ALLOW, BLOCK, SILENCE }
data class Decision(val action: Action, val reason: String, val brand: String? = null)
data class CallEvent(val id: Long, val number: String, val action: Action, val reason: String, val brand: String?, val timestamp: Long, val simulated: Boolean)

/** Country-aware equality for Vietnam. International numbers keep their country code. */
object PhoneNumbers {
    private val phoneFormat = Regex("\\+?[0-9() .-]+")
    private val accents = Regex("\\p{M}+")
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (!phoneFormat.matches(trimmed)) return ""
        var digits = trimmed.filter(Char::isDigit)
        val international = trimmed.startsWith('+') || digits.startsWith("00")
        if (!trimmed.startsWith('+') && digits.startsWith("00")) digits = digits.drop(2)
        if (international && digits.startsWith('0')) return ""
        if (digits.startsWith("84") && digits.length in 11..12) digits = "0" + digits.drop(2)
        return digits
    }
    fun valid(raw: String): Boolean = normalize(raw).length in 8..15
    fun key(raw: String): String = Normalizer.normalize(raw.trim().lowercase(java.util.Locale.ROOT), Normalizer.Form.NFD)
        .replace(accents, "").replace('đ', 'd')
}

/** No network or Android dependencies: the service and simulator share this exact decision path. */
object ScreeningEngine {
    fun evaluate(raw: String, snapshot: Snapshot, isContact: Boolean = false): Decision {
        val number = PhoneNumbers.normalize(raw)
        if (!snapshot.preferences.protection) return Decision(Action.ALLOW, "Bảo vệ đang tạm dừng")
        if (number.isBlank()) return Decision(Action.ALLOW, "Không có số để đối chiếu")
        if (number in snapshot.allowed) return Decision(Action.ALLOW, "Số thuộc danh sách cho phép")
        if (isContact && snapshot.preferences.protectContacts) return Decision(Action.ALLOW, "Bảo vệ số trong danh bạ")
        val brand = snapshot.brands.firstOrNull { it.blocked && number in it.numbers }
            ?: return Decision(Action.ALLOW, "Không thuộc thương hiệu bị chặn")
        return Decision(if (snapshot.preferences.silence) Action.SILENCE else Action.BLOCK,
            "Khớp số đã lưu của ${brand.name}", brand.name)
    }
}
