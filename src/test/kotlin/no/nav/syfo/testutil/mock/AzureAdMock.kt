package no.nav.syfo.testutil.mock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.client.azuread.AzureAdTokenResponse
import no.nav.syfo.testutil.getRandomPort

class AzureAdMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val azureAdTokenResponse = AzureAdTokenResponse(
        access_token = "token",
        expires_in = 3600,
        token_type = "type"
    )

    val name = "azureadv"
    val server = mockAzureAdV2Server()

    private fun mockAzureAdV2Server(): NettyApplicationEngine {
        return embeddedServer(
            factory = Netty,
            port = port
        ) {
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }
            routing {
                post {
                    call.respond(azureAdTokenResponse)
                }
            }
        }
    }
}
