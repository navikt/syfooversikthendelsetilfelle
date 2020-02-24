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

fun List<KSyketilfelledag>.containsSykmeldingAndSykepengesoknad(): Boolean {
    return this.containsSykmelding() && this.containsSykepengesoknad()
}

fun List<KSyketilfelledag>.containsSykmelding(): Boolean {
    return this
            .any { it.prioritertSyketilfellebit?.tags?.contains(SYKMELDING) ?: false }
}

fun List<KSyketilfelledag>.containsSykepengesoknad(): Boolean {
    return this
            .any { it.prioritertSyketilfellebit?.tags?.contains(SYKEPENGESOKNAD) ?: false }
}

fun List<KSyketilfelledag>.isLatestSykmeldingGradert(): Boolean {
    val sykmeldingerDager = this
            .filter { it.prioritertSyketilfellebit?.tags?.contains(SYKMELDING) ?: false }

    return if (sykmeldingerDager.isNullOrEmpty()) {
        false
    } else {
        sykmeldingerDager.minBy { ChronoUnit.DAYS.between(it.dag, LocalDate.now()) }!!
                .prioritertSyketilfellebit!!.tags.contains(GRADERT_AKTIVITET)
                .or(false)
    }
}
