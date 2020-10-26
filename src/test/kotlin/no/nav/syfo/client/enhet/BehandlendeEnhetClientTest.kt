package no.nav.syfo.client.enhet

import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.mock.BehandlendeEnhetMock
import no.nav.syfo.testutil.mock.StsRestMock
import no.nav.syfo.testutil.vaultSecrets
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
object BehandlendeEnhetClientTest : Spek({

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
            stsRestMock.server.start()
            behandlendeEnhetMock.server.start()
        }

        afterGroup {
            stsRestMock.server.stop(1L, 10L)
            behandlendeEnhetMock.server.stop(1L, 10L)
        }

        describe("BehandlendeEnhetClient successful") {
            it("Get enhet for Fodselsnummer") {
                val result = runBlocking {
                    behandlendeEnhetClient.getEnhet(ARBEIDSTAKER_FNR.value, "callId")
                }
                result shouldBeEqualTo behandlendeEnhetMock.behandlendeEnhet
            }
        }
    }
})
