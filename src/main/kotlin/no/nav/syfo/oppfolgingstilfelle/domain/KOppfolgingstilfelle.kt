package no.nav.syfo.oppfolgingstilfelle.domain

import java.time.LocalDateTime

data class KOppfolgingstilfelle(
        val aktorId: String,
        val orgnummer: String?,
        val tidslinje: List<KSyketilfelledag>,
        val sisteDagIArbeidsgiverperiode: KSyketilfelledag,
        val antallBrukteDager: Int,
        val oppbruktArbeidsgvierperiode: Boolean,
        val utsendelsestidspunkt: LocalDateTime
)

