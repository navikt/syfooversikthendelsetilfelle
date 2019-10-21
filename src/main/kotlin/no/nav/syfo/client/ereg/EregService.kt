package no.nav.syfo.client.ereg

import no.nav.syfo.log


class EregService(private val eregClient: EregClient) {

    fun finnOrganisasjonsNavn(orgNr: String, callId: String): String {
        log.info("Henter organisasjonsnavn for orgNr={}, callId={}", orgNr, callId)
        val regResponse = eregClient.hentOrgByOrgnr(orgNr)
        return regResponse?.navn?.let {
            if (it.redigertnavn?.isNotEmpty() == true) {
                it.redigertnavn
            } else {
                it.navnelinje1
            }
        } ?: ""
    }
}