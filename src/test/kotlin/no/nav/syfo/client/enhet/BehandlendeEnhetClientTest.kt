package no.nav.syfo.client.enhet

import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.mock.BehandlendeEnhetMock
import no.nav.syfo.testutil.mock.StsRestMock
import no.nav.syfo.testutil.vaultSecrets
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
object BehandlendeEnhetClientTest : Spek({
    val stsOidcClientMock = mockk<StsRestClient>()

    with(TestApplicationEngine()) {
        start()

        val vaultSecrets = vaultSecrets

        val stsRestMock = StsRestMock()
        val stsRestClient = StsRestClient(
            baseUrl = stsRestMock.url,
            username = vaultSecrets.serviceuserUsername,
            password = vaultSecrets.serviceuserPassword
        )

        val behandlendeEnhetMock = BehandlendeEnhetMock()
        val behandlendeEnhetClient = BehandlendeEnhetClient(
            baseUrl = behandlendeEnhetMock.url,
            stsRestClient = stsRestClient
        )

        beforeGroup {
            coEvery { stsOidcClientMock.token() } returns "oidctoken"
            stsRestMock.server.start()
            behandlendeEnhetMock.server.start()
        }

        afterGroup {
            stsRestMock.server.stop(1L, 10L)
            behandlendeEnhetMock.server.stop(1L, 10L)
        }

        describe("BehandlendeEnhetClient successful") {
            it("Get enhet for Fodselsnummer") {
                val result = behandlendeEnhetClient.getEnhet(ARBEIDSTAKER_FNR.value, "callId")

                result shouldEqual behandlendeEnhetMock.behandlendeEnhet
            }
        }
    }
})
