package no.nav.syfo.client.pdl

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.env
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.generator.genereatePdlPersonResponse
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket

@InternalAPI
object PdlClientTest : Spek({

    val stsOidcClientMock = mockk<StsRestClient>()

    with(TestApplicationEngine()) {
        start()

        val pdlResponse = genereatePdlPersonResponse.copy()

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
                post("/${env.pdlUrl}") {
                    call.respond(pdlResponse)
                }
            }
        }.start()

        val pdlClient = PdlClient("$mockHttpServerUrl/${env.pdlUrl}", stsOidcClientMock)

        beforeEachTest {
            coEvery { stsOidcClientMock.token() } returns "oidctoken"
        }

        afterEachTest {
        }

        afterGroup {
            mockServer.stop(1L, 10L)
        }

        describe("Get Person successful") {
            it("Get Person for Fodselsnummer") {
                val result = pdlClient.person(ARBEIDSTAKER_FNR.value, "callId")

                result shouldEqual pdlResponse.data
            }
        }
    }
})
