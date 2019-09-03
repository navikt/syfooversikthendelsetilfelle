package no.nav.syfo.oppfolgingstilfellehendelse.producer.domain

import java.time.LocalDateTime

data class KOppfolgingstilfeller(
        val aktorId: String,
        val orgnummer: String?,
        val tilfeller: List<KOppfolgingstilfelle>,
        val utsendelsestidspunkt: LocalDateTime
)
