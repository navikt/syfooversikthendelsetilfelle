package no.nav.syfo.client.syketilfelle

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.HttpHeaders
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_EMPTY
import no.nav.syfo.util.*

class SyketilfelleClient(
    private val baseUrl: String,
    private val stsRestClient: StsRestClient
) {
    suspend fun getOppfolgingstilfelle(
        aktorId: String,
        virksomhetsnummer: String,
        callId: String
    ): KOppfolgingstilfelle? {
        val bearer = stsRestClient.token()

        val (_, response, result) = getSyfosyketilfelleUrl(aktorId, virksomhetsnummer).httpGet()
            .header(mapOf(
                HttpHeaders.Authorization to bearerHeader(bearer),
                HttpHeaders.Accept to "application/json",
                NAV_CALL_ID to callId,
                NAV_CONSUMER_ID to APP_CONSUMER_ID
            ))
            .responseString()

        return if (response.statusCode == 204) {
            COUNT_OPPFOLGINGSTILFELLE_EMPTY.inc()
            null
        } else {
            objectMapper.readValue<KOppfolgingstilfelle>(result.get())
        }
    }

    private fun getSyfosyketilfelleUrl(aktorId: String, virksomhetsnummer: String): String {
        return "$baseUrl/kafka/oppfolgingstilfelle/beregn/$aktorId/$virksomhetsnummer"
    }

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}
