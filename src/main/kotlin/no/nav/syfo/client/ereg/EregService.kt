package no.nav.syfo.client.ereg

import no.nav.syfo.log


class EregService(private val eregClient: EregClient) {

    fun finnOrganisasjonsNavn(orgNr: String): String? {
        log.info("Henter organisasjonsnavn for orgNr={}", orgNr)
        val regResponse = eregClient.hentOrgByOrgnr(orgNr)
        return regResponse?.navn?.redigertnavn
    }
}