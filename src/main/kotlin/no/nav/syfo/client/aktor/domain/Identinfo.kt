package no.nav.syfo.client.aktor.domain

data class Identinfo(
    val ident: String,
    val identgruppe: String,
    val gjeldende: Boolean = false
)
