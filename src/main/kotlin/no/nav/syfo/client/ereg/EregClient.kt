package no.nav.syfo.client.ereg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.*
import com.github.kittinunf.fuel.*
import com.github.kittinunf.result.*
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.log


@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonNavn(
        val navnelinje1: String,
        val redigertnavn: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonResponse
(
    val navn: EregOrganisasjonNavn
)

class EregClient(
        private val baseUrl: String,
        private val stsRestClient: StsRestClient
) {

    fun hentOrgByOrgnr(orgnr: String): EregOrganisasjonResponse? {
        val token = stsRestClient.token()
        val url = "${baseUrl}v1/organisasjon/$orgnr"
        val (_, response, result) = url
                .httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $token"
                ))
                .responseString()

        return when (result) {
            is Result.Success -> jacksonObjectMapper().registerKotlinModule().readValue(result.value)
            is Result.Failure -> {
                log.info("Request med url: $url feilet med statuskode ${response.statusCode}")
                val exception = result.getException()
                log.error("Feil under henting av organisasjon: ${exception.message}", exception)
                null
            }
        }
    }
}