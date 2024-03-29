package no.nav.syfo.client.aktor

import arrow.core.Either
import arrow.core.flatMap
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.syfo.client.aktor.domain.IdentinfoListe
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class AktorregisterClient(
    val baseUrl: String,
    val stsRestClient: StsRestClient
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                configure()
            }
        }
        expectSuccess = true
    }

    suspend fun getIdenter(ident: String, callId: String): Either<String, List<Ident>> {
        val bearer = stsRestClient.token()

        val url = "$baseUrl/identer?gjeldende=true"
        val response: HttpResponse = client.get(url) {
            header(HttpHeaders.Authorization, bearerHeader(bearer))
            header(NAV_CALL_ID, callId)
            header(NAV_CONSUMER_ID, APP_CONSUMER_ID)
            header(NAV_PERSONIDENTER, ident)
            accept(ContentType.Application.Json)
        }

        val mapResponse = response.body<Map<String, IdentinfoListe>>()
        val identResponse: IdentinfoListe? = mapResponse[ident]

        return when {
            identResponse == null -> {
                val errorMessage = "Lookup gjeldende identer feilet"
                LOG.error(errorMessage)
                Either.Left(errorMessage)
            }
            identResponse.identer.isNullOrEmpty() -> {
                val errorMessage = "Lookup gjeldende identer feilet med feilmelding ${identResponse.feilmelding}"
                LOG.error(errorMessage)
                Either.Left(errorMessage)
            }
            else -> {
                val identer = identResponse.identer
                Either.Right(
                    identer.map {
                        Ident(
                            it.ident,
                            IdentType.valueOf(it.identgruppe)
                        )
                    }
                )
            }
        }
    }

    private suspend fun getIdent(
        ident: String,
        type: IdentType,
        callId: String
    ): Either<String, String> {
        return getIdenter(ident, callId).flatMap { identList ->
            Either.Right(
                identList.first {
                    it.type == type
                }.ident
            )
        }
    }

    suspend fun getNorskIdent(ident: String, callId: String): Either<String, String> {
        return getIdent(ident, IdentType.NorskIdent, callId)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AktorregisterClient::class.java)
    }
}

enum class IdentType {
    AktoerId, NorskIdent
}

data class Ident(
    val ident: String,
    val type: IdentType
)
