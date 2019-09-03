package no.nav.syfo.oppfolgingstilfellehendelse.producer.domain

import java.time.LocalDate

data class KSyketilfelledag(
        val dag: LocalDate,
        val prioritertSyketilfellebit: KSyketilfellebit?,
        val syketilfellebiter: List<KSyketilfellebit>
)
