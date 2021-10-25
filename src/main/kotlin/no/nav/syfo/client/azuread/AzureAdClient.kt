package no.nav.syfo.client.azuread

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.syfo.client.httpClientProxy
import org.slf4j.LoggerFactory

class AzureAdClient(
    private val azureAppClientId: String,
    private val azureAppClientSecret: String,
    private val azureOpenidConfigTokenEndpoint: String
) {
    private val httpClient = httpClientProxy()

    private val mutex = Mutex()

    @Volatile
    private var tokenMap = HashMap<String, AzureAdToken>()

    suspend fun getSystemToken(scopeClientId: String): AzureAdToken? {
        return mutex.withLock {
            (
                tokenMap[scopeClientId]
                    ?.takeUnless { cachedToken ->
                        cachedToken.isExpired()
                    }
                    ?: run {
                        getAccessToken(
                            Parameters.build {
                                append("client_id", azureAppClientId)
                                append("client_secret", azureAppClientSecret)
                                append("grant_type", "client_credentials")
                                append("scope", "api://$scopeClientId/.default")
                            }
                        )?.let {
                            val azureadToken = it.toAzureAdToken()
                            tokenMap[scopeClientId] = azureadToken
                            azureadToken
                        }
                    }
                )
        }
    }

    private suspend fun getAccessToken(
        formParameters: Parameters
    ): AzureAdTokenResponse? {
        return try {
            val response: HttpResponse = httpClient.post(azureOpenidConfigTokenEndpoint) {
                accept(ContentType.Application.Json)
                body = FormDataContent(formParameters)
            }
            response.receive<AzureAdTokenResponse>()
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e)
            null
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e)
            null
        }
    }

    private fun handleUnexpectedResponseException(
        responseException: ResponseException
    ) {
        log.error(
            "Error while requesting AzureAdAccessToken with statusCode=${responseException.response.status.value}",
            responseException
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(AzureAdClient::class.java)
    }
}
