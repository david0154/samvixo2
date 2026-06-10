package samvixo.nexuzy.com.utils

/**
 * CountryHelper — Supported countries with dial codes for Samvixo
 * Default country: India (+91)
 */
object CountryHelper {

    data class Country(
        val name: String,
        val isoCode: String,
        val dialCode: String,
        val flag: String
    )

    val SUPPORTED_COUNTRIES = listOf(
        Country("India", "IN", "+91", "🇮🇳"),
        Country("United States", "US", "+1", "🇺🇸"),
        Country("United Kingdom", "GB", "+44", "🇬🇧"),
        Country("France", "FR", "+33", "🇫🇷"),
        Country("Singapore", "SG", "+65", "🇸🇬"),
        Country("Brazil", "BR", "+55", "🇧🇷"),
        Country("Spain", "ES", "+34", "🇪🇸"),
        Country("Australia", "AU", "+61", "🇦🇺"),
        Country("Russia", "RU", "+7", "🇷🇺"),
        Country("Poland", "PL", "+48", "🇵🇱")
    )

    val DEFAULT_COUNTRY = SUPPORTED_COUNTRIES.first { it.isoCode == "IN" }

    fun getByIso(iso: String): Country? =
        SUPPORTED_COUNTRIES.firstOrNull { it.isoCode.equals(iso, ignoreCase = true) }

    fun getByDialCode(code: String): Country? =
        SUPPORTED_COUNTRIES.firstOrNull { it.dialCode == code }
}
