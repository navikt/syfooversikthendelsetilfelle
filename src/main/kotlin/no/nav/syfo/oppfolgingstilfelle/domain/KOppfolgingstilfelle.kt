package no.nav.syfo.oppfolgingstilfellehendelse.producer.domain

data class KOppfolgingstilfelle(
        val tidslinje: List<KSyketilfelledag>,
        val sisteDagIArbeidsgiverperiode: KSyketilfelledag,
        val antallBrukteDager: Int,
        val oppbruktArbeidsgvierperiode: Boolean
)
