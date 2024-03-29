package no.nav.syfo.testutil.mock

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import no.nav.syfo.testutil.getRandomPort

class StsRestMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val server = mockStsRestServer(port)

    val defaultToken = OidcToken(
        access_token = "default access token",
        expires_in = 3600,
        token_type = "Bearer",
        unknown_type = "uknown"
    )

    private fun mockStsRestServer(
        port: Int
    ): NettyApplicationEngine {
        return embeddedServer(
            factory = Netty,
            port = port
        ) {
            install(ContentNegotiation) {
                jackson {}
            }
            routing {
                get("/rest/v1/sts/token") {
                    val params = call.request.queryParameters
                    if (params["grant_type"].equals("client_credentials") && params["scope"].equals("openid")) {
                        call.respond(defaultToken)
                    }
                }
            }
        }
    }
}

data class OidcToken(
    val access_token: String,
    val expires_in: Long,
    val token_type: String,
    val unknown_type: String
)
