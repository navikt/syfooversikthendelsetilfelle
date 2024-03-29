package no.nav.syfo.client.pdl

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.metric.COUNT_CALL_PDL_FAIL
import no.nav.syfo.metric.COUNT_CALL_PDL_SUCCESS
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class PdlClient(
    private val baseUrl: String,
    private val stsRestClient: StsRestClient
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                configure()
            }
        }
        expectSuccess = true
    }

    suspend fun person(fnr: String, callId: String): PdlHentPerson? {
        val bearer = stsRestClient.token()

        val query = this::class.java.getResource("/pdl/hentPerson.graphql")
            .readText()
            .replace("[\n\r]", "")

        val request = PdlRequest(query, Variables(fnr))

        val response: HttpResponse = client.post(baseUrl) {
            setBody(request)
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Authorization, bearerHeader(bearer))
            header(NAV_CONSUMER_TOKEN, bearerHeader(bearer))
            header(TEMA, ALLE_TEMA_HEADERVERDI)
            header(NAV_CALL_ID, callId)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val pdlPersonReponse = response.body<PdlPersonResponse>()
                return if (pdlPersonReponse.errors != null && pdlPersonReponse.errors.isNotEmpty()) {
                    COUNT_CALL_PDL_FAIL.inc()
                    pdlPersonReponse.errors.forEach {
                        LOG.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                    }
                    null
                } else {
                    COUNT_CALL_PDL_SUCCESS.inc()
                    pdlPersonReponse.data
                }
            }
            else -> {
                COUNT_CALL_PDL_FAIL.inc()
                LOG.error("Request with url: $baseUrl failed with reponse code ${response.status.value}")
                return null
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PdlClient::class.java)
    }
}
