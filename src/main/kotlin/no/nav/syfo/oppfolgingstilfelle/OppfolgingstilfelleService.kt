package no.nav.syfo.oppfolgingstilfelle

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.*
import no.nav.syfo.log
import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED
import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_RECEIVED
import no.nav.syfo.oppfolgingstilfelle.domain.KOppfolgingstilfelle

const val GRADERT_AKTIVITET = "GRADERT_AKTIVITET"

class OppfolgingstilfelleService(private val aktorService: AktorService) {

    fun receiveOppfolgingstilfeller(oppfolgingstilfelle: KOppfolgingstilfelle, callId: String = "") {
        var fnr: Either<Nothing, String>? = null
        runBlocking {
            val aktor = AktorId(oppfolgingstilfelle.aktorId)
            fnr = aktorService.getFodselsnummerForAktor(aktor, callId)
        }
        if (fnr != null) {
            log.info("COUNT_OPPFOLGINGSTILFELLE_RECEIVED")
            COUNT_OPPFOLGINGSTILFELLE_RECEIVED.inc()

            if (oppfolgingstilfelle.tidslinje[0].prioritertSyketilfellebit?.let { it.tags.indexOf(GRADERT_AKTIVITET) > -1 }!!) {
                log.info("COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED")

                COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED.inc()
            } else {
                log.info("COUNT_OPPFOLGINGSTILFELLE_RECEIVED")
                COUNT_OPPFOLGINGSTILFELLE_RECEIVED.inc()
            }
        } else log.info("Fant ikke fnr for sykmeldt")
    }
}
