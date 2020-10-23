package no.nav.syfo.client.aktor.domain

data class IdentinfoListe(
    val identer: List<Identinfo>,
    val feilmelding: String? = null
)
