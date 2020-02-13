package no.nav.syfo.client.syketilfelle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.env
import no.nav.syfo.helper.UserConstants.BRUKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_ANNET
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

@InternalAPI
object SyketilfelleClientTest : Spek({
    val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    }

    val stsOidcClientMock = mockk<StsRestClient>()

    with(TestApplicationEngine()) {
        start()

        val oppfolgingstilfelle = generateOppfolgingstilfelle.copy()

        val oppfolgingstilfelleJson = objectMapper.writeValueAsString(oppfolgingstilfelle)

        application.install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }

        val mockHttpServerPort = ServerSocket(0).use { it.localPort }
        val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
        val mockServer = embeddedServer(Netty, mockHttpServerPort) {
            install(ContentNegotiation) {
                jackson {}
            }
            routing {
                get("/${env.syketilfelleUrl}/kafka/oppfolgingstilfelle/beregn/$BRUKER_FNR/$VIRKSOMHETSNUMMER") {
                    call.respond(oppfolgingstilfelleJson)
                }
                get("/${env.syketilfelleUrl}/kafka/oppfolgingstilfelle/beregn/$BRUKER_FNR/$VIRKSOMHETSNUMMER_ANNET") {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }.start()

        val syketilfelleClient = SyketilfelleClient("$mockHttpServerUrl/${env.syketilfelleUrl}", stsOidcClientMock)

        beforeEachTest {
            coEvery { stsOidcClientMock.token() } returns "oidctoken"
        }

        afterEachTest {
        }

        afterGroup {
            mockServer.stop(1L, 10L)
        }

        describe("SyketilfelleClient successful") {
            it("Get oppfolgingstilfelle for aktorId with virksomhetsnummer") {
                val result = syketilfelleClient.getOppfolgingstilfelle(BRUKER_FNR, VIRKSOMHETSNUMMER, "callId")

                result shouldEqual oppfolgingstilfelle
            }

            it("Do not find oppfolgingstilfelle for aktorId with virksomhetsnummer") {
                val result = syketilfelleClient.getOppfolgingstilfelle(BRUKER_FNR, VIRKSOMHETSNUMMER_ANNET, "callId")

                result shouldEqual null
            }
        }
    }
})
