package no.nav.syfo.testutil.mock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import no.nav.syfo.client.syketilfelle.KOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.KSyketilfelledag
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_AKTORID
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
                get("/kafka/oppfolgingstilfelle/beregn/${ARBEIDSTAKER_2_AKTORID.aktor}/$VIRKSOMHETSNUMMER") {
                    call.respond(
                        kOppfolgingstilfelle.copy(
                            aktorId = ARBEIDSTAKER_2_AKTORID.aktor
                        )
                    )
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
            listOf(
                KSyketilfelledag(
                    LocalDate.now().minusDays(10),
                    null
                ),
                KSyketilfelledag(
                    LocalDate.now().plusDays(10),
                    null
                )
            ),
            KSyketilfelledag(
                LocalDate.now().minusDays(1),
                null
            ),
            0,
            false,
            LocalDateTime.now()
        )
}
