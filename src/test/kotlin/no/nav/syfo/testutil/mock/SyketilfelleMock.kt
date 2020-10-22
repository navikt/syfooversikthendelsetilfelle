package no.nav.syfo.testutil.mock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.client.syketilfelle.KOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.KSyketilfelledag
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_ANNET
import no.nav.syfo.testutil.getRandomPort
import java.time.LocalDate
import java.time.LocalDateTime

class SyketilfelleMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val kOppfolgingstilfelle = generateOppfolgingstilfelle()
    val server = mockSyketilfelleServer(port, kOppfolgingstilfelle)

    private fun mockSyketilfelleServer(
        port: Int,
        kOppfolgingstilfelle: KOppfolgingstilfelle
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
                get("/kafka/oppfolgingstilfelle/beregn/${ARBEIDSTAKER_AKTORID.aktor}/$VIRKSOMHETSNUMMER") {
                    call.respond(kOppfolgingstilfelle)
                }
                get("/kafka/oppfolgingstilfelle/beregn/${ARBEIDSTAKER_AKTORID.aktor}/$VIRKSOMHETSNUMMER_ANNET") {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }

    fun generateOppfolgingstilfelle() =
        KOppfolgingstilfelle(
            ARBEIDSTAKER_AKTORID.aktor,
            VIRKSOMHETSNUMMER,
            emptyList(),
            KSyketilfelledag(
                LocalDate.now().minusDays(1),
                null
            ),
            0,
            false,
            LocalDateTime.now()
        )
}
