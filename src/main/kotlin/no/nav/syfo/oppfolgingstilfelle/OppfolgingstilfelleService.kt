package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED
import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_RECEIVED
import no.nav.syfo.oppfolgingstilfellehendelse.producer.domain.KOppfolgingstilfeller

class OppfolgingstilfelleService {

    fun receiveOppfolgingstilfeller(oppfolgingstilfeller: KOppfolgingstilfeller, callId: String = "") {
        COUNT_OPPFOLGINGSTILFELLE_RECEIVED.inc()

        if (oppfolgingstilfeller.tilfeller[0].tidslinje[0].syketilfellebiter[0].tags.indexOf("") > -1) {
            COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED.inc()
        } else {
            COUNT_OPPFOLGINGSTILFELLE_RECEIVED.inc()
        }
    }
}
