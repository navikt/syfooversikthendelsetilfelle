package no.nav.syfo.client.ereg

import com.fasterxml.jackson.module.kotlin.*
import com.github.kittinunf.fuel.*
import com.github.kittinunf.result.*
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.log


data class EregOrganisasjonNavn(
    val redigertnavn: String
)

data class EregOrganisasjonResponse
(
    val navn: EregOrganisasjonNavn
)

class EregClient(private val baseUrl: String, private val stsRestClient: StsRestClient) {

    val hentOrganisasjonPath = "v1/organisasjon/{orgNr}"

    fun hentOrgByOrgnr(orgnr: String): EregOrganisasjonResponse {
        val token = stsRestClient.token()

        val (_, _, result) = "$baseUrl/${hentOrganisasjonPath.replace("orgNr", orgnr)}"
                .httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $token"
                ))
                .responseString()

        return when (result) {
            is Result.Success -> jacksonObjectMapper().registerKotlinModule().readValue(result.value)
            is Result.Failure -> {
                val exception = result.getException()
                log.error("Feil under henting av organisasjon: ${exception.message}", exception)
                throw exception
            }
        }
    }
}