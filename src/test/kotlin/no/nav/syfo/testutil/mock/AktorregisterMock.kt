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
import no.nav.syfo.client.aktor.IdentType
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.getRandomPort
import no.nav.syfo.util.NAV_PERSONIDENTER

class AktorregisterMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val server = mockAktorregisterServer(port)

    private fun mockAktorregisterServer(
        port: Int
    ): NettyApplicationEngine {
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
                get("/identer") {
                    when (call.request.headers[NAV_PERSONIDENTER]) {
                        ARBEIDSTAKER_FNR.value -> {
                            call.respond(
                                mapOf(
                                    ARBEIDSTAKER_FNR.value to RSAktor(
                                        listOf(
                                            RSIdent(
                                                ident = ARBEIDSTAKER_FNR.value,
                                                identgruppe = IdentType.NorskIdent.name,
                                                gjeldende = true
                                            ),
                                            RSIdent(
                                                ident = ARBEIDSTAKER_AKTORID.aktor,
                                                identgruppe = IdentType.AktoerId.name,
                                                gjeldende = true
                                            )
                                        ),
                                        feilmelding = null
                                    )
                                )
                            )
                        }
                        ARBEIDSTAKER_AKTORID.aktor -> {
                            call.respond(
                                mapOf(
                                    ARBEIDSTAKER_AKTORID.aktor to RSAktor(
                                        listOf(
                                            RSIdent(
                                                ident = ARBEIDSTAKER_FNR.value,
                                                identgruppe = IdentType.NorskIdent.name,
                                                gjeldende = true
                                            ),
                                            RSIdent(
                                                ident = ARBEIDSTAKER_AKTORID.aktor,
                                                identgruppe = IdentType.AktoerId.name,
                                                gjeldende = true
                                            )
                                        ),
                                        feilmelding = null
                                    )
                                )
                            )
                        }
                        else -> error("Something went wrong")
                    }
                }
            }
        }
    }
}

data class RSIdent(
    val ident: String,
    val identgruppe: String,
    val gjeldende: Boolean
)

data class RSAktor(
    val identer: List<RSIdent>? = null,
    val feilmelding: String? = null
)
