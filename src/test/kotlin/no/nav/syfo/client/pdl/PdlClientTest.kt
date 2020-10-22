package no.nav.syfo.client.pdl

import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.mock.PdlMock
import no.nav.syfo.testutil.mock.StsRestMock
import no.nav.syfo.testutil.vaultSecrets
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
object PdlClientTest : Spek({

    with(TestApplicationEngine()) {
        start()

        val vaultSecrets = vaultSecrets

        val stsRestMock = StsRestMock()
        val stsRestClient = StsRestClient(
            baseUrl = stsRestMock.url,
            username = vaultSecrets.serviceuserUsername,
            password = vaultSecrets.serviceuserPassword
        )

        val pdlMock = PdlMock()
        val pdlClient = PdlClient(
            baseUrl = pdlMock.url,
            stsRestClient = stsRestClient
        )

        beforeGroup {
            stsRestMock.server.start()
            pdlMock.server.start()
        }

        afterGroup {
            stsRestMock.server.stop(1L, 10L)
            pdlMock.server.stop(1L, 10L)
        }

        describe("Get Person successful") {
            it("Get Person for Fodselsnummer") {
                val result = pdlClient.person(ARBEIDSTAKER_FNR.value, "callId")

                result shouldEqual pdlMock.pdlPersonResponse.data
            }
        }
    }
})
