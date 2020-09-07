package no.nav.syfo.client.pdl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.metric.*
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import io.ktor.http.HttpHeaders

class PdlClient(
    private val baseUrl: String,
    private val stsRestClient: StsRestClient
) {
    fun person(fnr: String, callId: String): PdlHentPerson? {
        COUNT_CALL_PDL.inc()

        val bearer = stsRestClient.token()

        val query = this::class.java.getResource("/pdl/hentPerson.graphql")
            .readText()
            .replace("[\n\r]", "")

        val request = PdlRequest(query, Variables(fnr))

        val json = Gson().toJson(request)

        val (_, response, result) = baseUrl
            .httpPost()
            .header(mapOf(
                HttpHeaders.ContentType to "application/json",
                HttpHeaders.Authorization to bearerHeader(bearer),
                NAV_CONSUMER_TOKEN to bearerHeader(bearer),
                TEMA to ALLE_TEMA_HEADERVERDI,
                NAV_CALL_ID to callId
            ))
            .body(json)
            .responseString()

        result.fold(success = {
            val pdlPersonReponse = objectMapper.readValue<PdlPersonResponse>(result.get())
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
        }, failure = {
            COUNT_CALL_PDL_FAIL.inc()
            LOG.info("Request with url: $baseUrl failed with reponse code ${response.statusCode}")
            val exception = it.exception
            LOG.error("Error while requesting person from PersonDataLosningen: ${exception.message}", exception)
            return null
        })
    }

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PdlClient::class.java)
    }
}
