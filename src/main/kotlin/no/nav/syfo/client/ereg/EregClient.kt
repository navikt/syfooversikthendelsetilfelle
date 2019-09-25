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
        val url = "$baseUrl/${hentOrganisasjonPath.replace("orgNr", orgnr)}"
        val (request, response, result) = url
                .httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $token"
                ))
                .responseString()

        return when (result) {
            is Result.Success -> jacksonObjectMapper().registerKotlinModule().readValue(result.value)
            is Result.Failure -> {
                log.info("Request med url: ${request.url} feilet med statuskode ${response.statusCode}")
                val exception = result.getException()
                log.error("Feil under henting av organisasjon: ${exception.message}", exception)
                throw exception
            }
        }
    }
}