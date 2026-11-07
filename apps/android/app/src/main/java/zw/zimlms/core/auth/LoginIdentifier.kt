package zw.zimlms.core.auth

data class LoginIdentifier(val key: String, val value: String)

object LoginIdentifiers {
  private val phone = Regex("^\\+2637[1-8]\\d{7}$")
  private val local = Regex("^07[1-8]\\d{7}$")
  private val noPlus = Regex("^2637[1-8]\\d{7}$")
  private val email = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

  fun parse(input: String): LoginIdentifier {
    val value = input.trim()
    if (email.matches(value)) return LoginIdentifier("email", value)
    val compact = value.replace(Regex("[\\s()-]"), "")
    val normalized = when {
      local.matches(compact) -> "+263" + compact.drop(1)
      noPlus.matches(compact) -> "+" + compact
      else -> compact
    }
    require(phone.matches(normalized)) { "Invalid Zimbabwe mobile number or email address" }
    return LoginIdentifier("phone", normalized)
  }
}
