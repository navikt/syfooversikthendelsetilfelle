package no.nav.syfo.client.pdl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
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
    fun person(ident: String, callId: String): PdlHentPerson? {
        val queryFilePath = "/pdl/hentPerson.graphql"

        val query = this::class.java.getResource(queryFilePath)
                .readText()
                .replace("[\n\r]", "")

        val request = PdlRequest(query, Variables(ident))

        val json = Gson().toJson(request)

        val (_, response, result) = callPdl(json, callId)

        result.fold(success = {
            COUNT_CALL_PDL_SUCCESS.inc()
            return objectMapper.readValue<PdlPersonResponse>(result.get()).data
        }, failure = {
            COUNT_CALL_PDL_FAIL.inc()
            LOG.info("Request with url: $baseUrl failed with reponse code ${response.statusCode}")
            val exception = it.exception
            LOG.error("Error while requesting person from PersonDataLosningen: ${exception.message}", exception)
            return null
        })
    }

    fun identer(ident: String, callId: String): PdlHentIdenter? {
        val queryFilePath = "/pdl/hentIdenter.graphql"

        val query = this::class.java.getResource(queryFilePath)
                .readText()
                .replace("[\n\r]", "")

        val request = PdlHentIdenterRequest(
                query,
                PdlHentIdenterRequestVariables(ident, false, listOf(IdentType.FOLKEREGISTERIDENT.name))
        )

        val json = Gson().toJson(request)

        LOG.info("JTRACE request from henterIdenter PDL $json")

        val (_, response, result) = callPdl(json, callId)

        LOG.info("JTRACE reponse from henterIdenter PDL $response")

        result.fold(success = {
            COUNT_CALL_PDL_SUCCESS.inc()
            return objectMapper.readValue<PdlIdenterResponse>(result.get()).data
        }, failure = {
            COUNT_CALL_PDL_FAIL.inc()
            LOG.info("Request with url: $baseUrl failed with reponse code ${response.statusCode}")
            val exception = it.exception
            LOG.error("Error while requesting identer from PersonDataLosningen: ${exception.message}", exception)
            return null
        })
    }
    private fun callPdl(json: String, callId: String): Triple<Request, Response, Result<String, FuelError>> {
        val bearer = stsRestClient.token()

        COUNT_CALL_PDL.inc()

        return baseUrl
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
