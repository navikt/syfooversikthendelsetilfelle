package no.nav.syfo.client.enhet

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.metric.*
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class BehandlendeEnhetClient(
    private val azureAdClient: AzureAdClient,
    private val baseUrl: String,
    private val syfobehandlendeenhetClientId: String
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

    suspend fun getEnhet(personIdentNumber: String, callId: String): BehandlendeEnhet? {
        val systemToken = azureAdClient.getSystemToken(
            scopeClientId = syfobehandlendeenhetClientId
        )?.accessToken ?: throw RuntimeException("Failed to request access to Person: Failed to get OBO token")

        COUNT_CALL_BEHANDLENDEENHET.inc()

        val url = "$baseUrl/api/system/v2/personident"
        try {
            val response: HttpResponse = client.get(url) {
                header(HttpHeaders.Authorization, bearerHeader(systemToken))
                header(NAV_CALL_ID, callId)
                header(NAV_CONSUMER_ID, APP_CONSUMER_ID)
                header(NAV_PERSONIDENT_HEADER, personIdentNumber)
                accept(ContentType.Application.Json)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val behandlendeEnhet = response.receive<BehandlendeEnhet>()
                    return if (isValid(behandlendeEnhet)) {
                        COUNT_CALL_BEHANDLENDEENHET_SUCCESS.inc()
                        behandlendeEnhet
                    } else {
                        COUNT_CALL_BEHANDLENDEENHET_FAIL.inc()
                        LOG.error("Error while requesting behandlendeenhet from syfobehandlendeenhet: Received invalid EnhetId with more than 4 chars for EnhetId {}", behandlendeEnhet.enhetId)
                        null
                    }
                }
                HttpStatusCode.NoContent -> {
                    LOG.error("BehandlendeEnhet returned HTTP-${response.status.value}: No BehandlendeEnhet was found for Fodselsnummer")
                    COUNT_CALL_BEHANDLENDEENHET_EMPTY.inc()
                    return null
                }
                else -> {
                    COUNT_CALL_BEHANDLENDEENHET_FAIL.inc()
                    LOG.error("Error with responseCode=${response.status.value} with callId=$callId while requesting behandlendeenhet from syfobehandlendeenhet")
                    return null
                }
            }
        } catch (responseException: ResponseException) {
            COUNT_CALL_BEHANDLENDEENHET_FAIL.inc()
            LOG.error("Error with responseCode=${responseException.response.status.value} with callId=$callId while requesting behandlendeenhet from syfobehandlendeenhet")
            return null
        }
    }

    private fun isValid(behandlendeEnhet: BehandlendeEnhet): Boolean {
        return behandlendeEnhet.enhetId.length <= 4
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BehandlendeEnhetClient::class.java)
    }
}
