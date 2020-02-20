package no.nav.syfo.client.ereg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.*
import com.github.kittinunf.fuel.*
import com.github.kittinunf.result.*
import io.ktor.http.HttpHeaders
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.log
import no.nav.syfo.metric.COUNT_CALL_EREG_FAIL
import no.nav.syfo.metric.COUNT_CALL_EREG_SUCCESS
import no.nav.syfo.util.bearerHeader

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonNavn(
        val navnelinje1: String,
        val redigertnavn: String?
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

    fun hentOrgByOrgnr(orgnr: String, callId: String): EregOrganisasjonResponse? {
        val token = stsRestClient.token()
        val url = "${baseUrl}v1/organisasjon/$orgnr"
        val (_, response, result) = url
                .httpGet()
                .header(mapOf(
                        HttpHeaders.Authorization to bearerHeader(token)
                ))
                .responseString()

        return when (result) {
            is Result.Success -> {
                COUNT_CALL_EREG_FAIL.inc()
                jacksonObjectMapper().registerKotlinModule().readValue(result.value)
            }
            is Result.Failure -> {
                COUNT_CALL_EREG_SUCCESS.inc()
                val exception = result.getException()
                log.error("Error with responseCode=${response.statusCode} with callId=${callId} while requesting Organisasjon from Ereg: ${exception.message}", exception)
                null
            }
        }
    }
}
