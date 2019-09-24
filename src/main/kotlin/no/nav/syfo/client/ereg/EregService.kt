package no.nav.syfo.client.ereg



class EregService(private val eregClient: EregClient) {

    fun finnOrganisasjonsNavn(orgNr: String): String {
        val regResponse = eregClient.hentOrgByOrgnr(orgNr)
        return regResponse.navn.redigertnavn
    }
}