package no.nav.syfo.domain

data class AktorId(val aktor: String) {
    private val thirteenDigits = Regex("^\\d{13}\$")

    init {
        if (!thirteenDigits.matches(aktor)) {
            throw IllegalArgumentException("$aktor is not a valid aktorId")
        }
    }
}
