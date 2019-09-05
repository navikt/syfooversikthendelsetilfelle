package no.nav.syfo.client

class AktorService(private val aktorregisterClient: AktorregisterClient) {

    fun getFodselsnummerForAktor(aktorId: AktorId) =
            aktorregisterClient.getNorskIdent(aktorId.aktor).mapLeft {
                throw IllegalStateException("Fant ikke aktor")
            }

    fun getAktorForFodselsnummer(fodselsnummer: Fodselsnummer) =
            aktorregisterClient.getAktorId(fodselsnummer.value).mapLeft {
                throw IllegalStateException("Fant ikke aktor")
            }
}
