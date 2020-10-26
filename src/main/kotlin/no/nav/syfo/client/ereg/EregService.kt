package no.nav.syfo.client.ereg

class EregService(private val eregClient: EregClient) {

    suspend fun finnOrganisasjonsNavn(orgNr: String, callId: String): String {
        val regResponse = eregClient.hentOrgByOrgnr(orgNr, callId)
        return regResponse?.navn?.let {
            if (it.redigertnavn?.isNotEmpty() == true) {
                it.redigertnavn
            } else {
                it.navnelinje1
            }
        } ?: ""
    }
}
