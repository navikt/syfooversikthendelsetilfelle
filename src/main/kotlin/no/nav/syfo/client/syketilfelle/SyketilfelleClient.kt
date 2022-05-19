package no.nav.syfo.client.syketilfelle

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.metric.*
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class SyketilfelleClient(
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

    suspend fun getOppfolgingstilfelle(
        aktorId: String,
        virksomhetsnummer: String,
        callId: String
    ): KOppfolgingstilfelle? {
        val bearer = stsRestClient.token()

        val response: HttpResponse = client.get(getSyfosyketilfelleUrl(aktorId, virksomhetsnummer)) {
            header(HttpHeaders.Authorization, bearerHeader(bearer))
            header(NAV_CALL_ID, callId)
            header(NAV_CONSUMER_ID, APP_CONSUMER_ID)
            accept(ContentType.Application.Json)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                COUNT_CALL_SYKETILFELLE_OPPFOLGINGSTILFELLE_AKTOR_SUCCESS.inc()
                return response.body<KOppfolgingstilfelle>()
            }
            HttpStatusCode.NoContent -> {
                LOG.error("Syketilfelle returned HTTP-${response.status.value}: No Oppfolgingstilfelle was found for AktorId")
                COUNT_CALL_SYKETILFELLE_OPPFOLGINGSTILFELLE_AKTOR_EMPTY.inc()
                return null
            }
            else -> {
                COUNT_CALL_SYKETILFELLE_OPPFOLGINGSTILFELLE_AKTOR_FAIL.inc()
                val errorMessage =
                    "Error with responseCode=${response.status.value} for callId=$callId when requesting Oppfolgingstilfelle for aktorId from syfosyketilfelle"
                LOG.error(errorMessage)
                return null
            }
        }
    }

    private fun getSyfosyketilfelleUrl(aktorId: String, virksomhetsnummer: String): String {
        return "$baseUrl/kafka/oppfolgingstilfelle/beregn/$aktorId/$virksomhetsnummer"
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SyketilfelleClient::class.java)
    }
}
