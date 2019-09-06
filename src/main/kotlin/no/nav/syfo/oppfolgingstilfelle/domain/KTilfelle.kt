package no.nav.syfo.oppfolgingstilfelle.domain

data class KTilfelle(
        val tidslinje: List<KSyketilfelledag>,
        val sisteDagIArbeidsgiverperiode: KSyketilfelledag,
        val antallBrukteDager: Int,
        val oppbruktArbeidsgvierperiode: Boolean
)
