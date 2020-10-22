package no.nav.syfo.domain

data class AktorId(val aktor: String) {

    init {
        if (aktor.isEmpty()) {
            throw IllegalArgumentException("$aktor cannot be empty")
        }
    }
}
