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
import no.nav.syfo.client.ereg.EregOrganisasjonNavn
import no.nav.syfo.client.ereg.EregOrganisasjonResponse
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.getRandomPort

class EregMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port/"
    val eregResponse = generateEregOrganisasjonResponse()
    val server = mockBehandlendeEnhetServer(port, eregResponse)

    private fun mockBehandlendeEnhetServer(
        port: Int,
        eregResponse: EregOrganisasjonResponse
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
                get("v1/organisasjon/$VIRKSOMHETSNUMMER") {
                    call.respond(eregResponse)
                }
            }
        }
    }

    fun generateEregOrganisasjonResponse() =
        EregOrganisasjonResponse(
            navn = EregOrganisasjonNavn(
                navnelinje1 = "Kristians Test AS",
                redigertnavn = "Kristians Test AS, Oslo"
            )
        )
}
