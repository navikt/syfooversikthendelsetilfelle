package no.nav.syfo.oppfolgingstilfelle.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class KSyketilfelledag(
    val dag: LocalDate,
    val prioritertSyketilfellebit: KSyketilfellebit?
)

const val SYKEPENGESOKNAD = "SYKEPENGESOKNAD"
const val GRADERT_AKTIVITET = "GRADERT_AKTIVITET"
const val SYKMELDING = "SYKMELDING"

fun List<KSyketilfelledag>.isLatestSykmeldingGradert(): Boolean {
    val sykmeldingerDager = this
        .filter { it.prioritertSyketilfellebit?.tags?.contains(SYKMELDING) ?: false }

    return sykmeldingerDager.minByOrNull { ChronoUnit.DAYS.between(it.dag, LocalDate.now()) }
        ?.prioritertSyketilfellebit?.tags?.contains(GRADERT_AKTIVITET) ?: false
}
