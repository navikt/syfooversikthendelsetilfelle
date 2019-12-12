package no.nav.syfo.client.enhet

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.httpGet
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.metric.*
import org.slf4j.LoggerFactory

class BehandlendeEnhetClient(
        private val baseUrl: String,
        private val stsRestClient: StsRestClient
) {

    fun getEnhet(fnr: String, callId: String): BehandlendeEnhet? {
        val bearer = stsRestClient.token()

        COUNT_CALL_BEHANDLENDEENHET.inc()

        val (_, response, result) = getBehandlendeEnhetUrl(fnr).httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to callId,
                        "Nav-Consumer-Id" to "syfooversikthendelsetilfelle"
                ))
                .responseString()

        result.fold(success = {
            COUNT_CALL_BEHANDLENDEENHET_SUCCESS.inc()
            return objectMapper.readValue<BehandlendeEnhet>(result.get())
        }, failure = {
            COUNT_CALL_BEHANDLENDEENHET_FAIL.inc()
            LOG.info("Request with url: $baseUrl failed with reponse code ${response.statusCode}")
            val exception = it.exception
            LOG.error("Error while requesting person from syfobehandlendeenhet: ${exception.message}", exception)
            return null
        })
    }

    private fun getBehandlendeEnhetUrl(bruker: String): String {
        return "$baseUrl/api/$bruker"
    }

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BehandlendeEnhetClient::class.java)
    }
}
