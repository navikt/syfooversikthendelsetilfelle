package no.nav.syfo.client.ereg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.metric.COUNT_CALL_EREG_SUCCESS
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonNavn(
    val navnelinje1: String,
    val redigertnavn: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonResponse(
    val navn: EregOrganisasjonNavn
)

class EregClient(
    private val baseUrl: String,
    private val stsRestClient: StsRestClient
) {
    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }

    suspend fun hentOrgByOrgnr(orgnr: String, callId: String): EregOrganisasjonResponse? {
        val token = stsRestClient.token()
        val url = "${baseUrl}v1/organisasjon/$orgnr"

        val response: HttpResponse = client.get(url) {
            header(HttpHeaders.Authorization, bearerHeader(token))
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val eregOrganisasjonResponse = response.receive<EregOrganisasjonResponse>()
                COUNT_CALL_EREG_SUCCESS.inc()
                eregOrganisasjonResponse
            }
            else -> {
                COUNT_CALL_EREG_SUCCESS.inc()
                LOG.error("Error with responseCode=${response.status.value} with callId=$callId while requesting Organisasjon from Ereg")
                null
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(EregClient::class.java)
    }
}
