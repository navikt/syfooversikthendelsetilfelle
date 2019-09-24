package no.nav.syfo.client.ereg

import arrow.core.Either
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
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
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket
import java.util.concurrent.*

@InternalAPI
object EregClientTest : Spek({

    with(TestApplicationEngine()) {
        start()

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
                get("/ereg/api/v1/organisasjon/{orgNr}") {
                    call.respond(EregOrganisasjonResponse(navn = EregOrganisasjonNavn("Kristians Test AS")))
                }
            }
        }.start()
        val stsOidcClientMock = mockk<StsRestClient>()
        val eregClient = EregClient("$mockHttpServerUrl/ereg/api/", stsOidcClientMock)

        coEvery { stsOidcClientMock.token() } returns  "oidctoken"

        afterEachTest {
        }

        afterGroup {
            mockServer.stop(1L, 10L, TimeUnit.SECONDS)
        }

        describe("EregClient hentOrgNavn successful") {
            it("Returns valid response when ok") {
                var orgNavn = eregClient.hentOrgByOrgnr("123")
                orgNavn.navn.navn shouldEqual "Kristians Test AS"
            }
        }
    }
})