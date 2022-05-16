package no.nav.syfo.testutil.mock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import no.nav.syfo.client.aktor.IdentType
import no.nav.syfo.domain.AktorId
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_AKTORID
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.getRandomPort
import no.nav.syfo.util.NAV_PERSONIDENTER

class AktorregisterMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val server = mockAktorregisterServer(port)

    val aktorIdMissingFnr = AktorId(ARBEIDSTAKER_AKTORID.aktor.replace("0", "9"))

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
                        ARBEIDSTAKER_2_AKTORID.aktor -> {
                            call.respond(
                                mapOf(
                                    ARBEIDSTAKER_2_AKTORID.aktor to RSAktor(
                                        listOf(
                                            RSIdent(
                                                ident = ARBEIDSTAKER_2_FNR.value,
                                                identgruppe = IdentType.NorskIdent.name,
                                                gjeldende = true
                                            ),
                                            RSIdent(
                                                ident = ARBEIDSTAKER_2_AKTORID.aktor,
                                                identgruppe = IdentType.AktoerId.name,
                                                gjeldende = true
                                            )
                                        ),
                                        feilmelding = null
                                    )
                                )
                            )
                        }
                        aktorIdMissingFnr.aktor -> {
                            call.respond(
                                mapOf(
                                    aktorIdMissingFnr.aktor to RSAktor(
                                        identer = null,
                                        feilmelding = "Not found"
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
