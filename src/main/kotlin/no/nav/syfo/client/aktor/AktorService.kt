package no.nav.syfo.client.aktor

import no.nav.syfo.client.aktor.domain.AktorId
import no.nav.syfo.client.aktor.domain.Fodselsnummer

class AktorService(private val aktorregisterClient: AktorregisterClient) {

    fun getFodselsnummerForAktor(aktorId: AktorId, callId: String) =
            aktorregisterClient.getNorskIdent(aktorId.aktor, callId).mapLeft {
                throw IllegalStateException("Fant ikke aktor")
            }

    fun getAktorForFodselsnummer(fodselsnummer: Fodselsnummer, callId: String) =
            aktorregisterClient.getAktorId(fodselsnummer.value, callId).mapLeft {
                throw IllegalStateException("Fant ikke aktor")
            }
}
