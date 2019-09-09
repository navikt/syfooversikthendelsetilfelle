package no.nav.syfo.oppfolgingstilfelle.domain

import java.time.LocalDate

data class KSyketilfelledag(
        val dag: LocalDate,
        val prioritertSyketilfellebit: KSyketilfellebit?
)
