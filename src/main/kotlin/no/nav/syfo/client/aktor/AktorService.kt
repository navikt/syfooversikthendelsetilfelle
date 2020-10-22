package no.nav.syfo.client.aktor

import no.nav.syfo.domain.AktorId

import no.nav.syfo.log

class AktorService(
    private val aktorregisterClient: AktorregisterClient
) {
    fun getFodselsnummerForAktor(aktorId: AktorId, callId: String) =
            aktorregisterClient.getNorskIdent(aktorId.aktor, callId).mapLeft {
                throw IllegalStateException("Fant ikke aktor")
            }

    fun fodselsnummerForAktor(aktorId: AktorId, callId: String): String? {
        var fnr: String? = null
        getFodselsnummerForAktor(aktorId, callId).mapLeft {
            log.info("Fant ikke fnr for Aktor")
            throw IllegalStateException("Fant ikke aktor")
        }.map {
            fnr = it
        }
        return fnr
    }
}
