package no.nav.syfo.client.syketilfelle

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.httpGet
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.log

class SyketilfelleClient(
        private val baseUrl: String,
        private val stsRestClient: StsRestClient
) {
    fun getOppfolgingstilfelle(
            aktorId: String,
            virksomhetsnummer: String,
            callId: String
    ): KOppfolgingstilfelle? {
        val bearer = stsRestClient.token()

        val (_, response, result) = getSyfosyketilfelleUrl(aktorId, virksomhetsnummer).httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to callId,
                        "Nav-Consumer-Id" to "syfooversikthendelsetilfelle"
                ))
                .responseString()

        return if (response.statusCode == 204) {
            log.info("Received no content when attempting to retrieve oppfolgingstilfelle from syfosyketilfelle")
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
