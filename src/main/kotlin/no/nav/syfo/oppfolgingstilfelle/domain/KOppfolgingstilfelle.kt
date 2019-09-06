package no.nav.syfo.oppfolgingstilfelle.domain

import java.time.LocalDateTime

data class KOppfolgingstilfelle(
        val aktorId: String,
        val orgnummer: String?,
        val tilfelle: KTilfelle,
        val utsendelsestidspunkt: LocalDateTime
)
