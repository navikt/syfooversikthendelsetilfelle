package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED
import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_RECEIVED
import no.nav.syfo.oppfolgingstilfelle.domain.KOppfolgingstilfelle

class OppfolgingstilfelleService {

    fun receiveOppfolgingstilfeller(oppfolgingstilfeller: KOppfolgingstilfelle, callId: String = "") {
        COUNT_OPPFOLGINGSTILFELLE_RECEIVED.inc()

        if (oppfolgingstilfeller.tidslinje[0].prioritertSyketilfellebit?.let { it.tags.indexOf("") > -1 }!!) {
            COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED.inc()
        } else {
            COUNT_OPPFOLGINGSTILFELLE_RECEIVED.inc()
        }
    }
}
