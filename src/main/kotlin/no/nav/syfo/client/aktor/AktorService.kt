package no.nav.syfo.client.aktor

import no.nav.syfo.domain.AktorId

import org.slf4j.LoggerFactory

class AktorService(
    private val aktorregisterClient: AktorregisterClient
) {
    suspend fun fodselsnummerForAktor(
        aktorId: AktorId,
        callId: String
    ): String? {
        var fnr: String? = null
        aktorregisterClient.getNorskIdent(aktorId.aktor, callId).mapLeft {
            LOG.info("Did not find Fodselsnummer for AktorId")
            fnr = null
        }.map {
            fnr = it
        }
        return fnr
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AktorService::class.java)
    }
}
