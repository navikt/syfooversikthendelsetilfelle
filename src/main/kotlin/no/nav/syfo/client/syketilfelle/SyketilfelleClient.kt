package no.nav.syfo.client.syketilfelle

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
import no.nav.syfo.log
import no.nav.syfo.metric.*
import no.nav.syfo.util.*

class SyketilfelleClient(
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
                return response.receive<KOppfolgingstilfelle>()
            }
            HttpStatusCode.NoContent -> {
                log.error("Syketilfelle returned HTTP-${response.status.value}: No Oppfolgingstilfelle was found for AktorId")
                COUNT_CALL_SYKETILFELLE_OPPFOLGINGSTILFELLE_AKTOR_EMPTY.inc()
                return null
            }
            else -> {
                COUNT_CALL_SYKETILFELLE_OPPFOLGINGSTILFELLE_AKTOR_FAIL.inc()
                val errorMessage =
                    "Error with responseCode=${response.status.value} for callId=$callId when requesting Oppfolgingstilfelle for aktorId from syfosyketilfelle"
                log.error(errorMessage)
                return null
            }
        }
    }

    private fun getSyfosyketilfelleUrl(aktorId: String, virksomhetsnummer: String): String {
        return "$baseUrl/kafka/oppfolgingstilfelle/beregn/$aktorId/$virksomhetsnummer"
    }
}
