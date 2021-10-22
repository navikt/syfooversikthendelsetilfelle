package no.nav.syfo.client.enhet

import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.mock.AzureAdMock
import no.nav.syfo.testutil.mock.BehandlendeEnhetMock
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
object BehandlendeEnhetClientTest : Spek({

    with(TestApplicationEngine()) {
        start()

        val azureAdClientMock = AzureAdMock()
        val azureAdClient = AzureAdClient(
            azureAppClientId = "azureAppClientId",
            azureAppClientSecret = "azureAppClientSecret",
            azureOpenidConfigTokenEndpoint = azureAdClientMock.url
        )

        val behandlendeEnhetMock = BehandlendeEnhetMock()
        val behandlendeEnhetClient = BehandlendeEnhetClient(
            azureAdClient = azureAdClient,
            baseUrl = behandlendeEnhetMock.url,
            syfobehandlendeenhetClientId = "syfobehandlendeenhetClientId"
        )

        beforeGroup {
            azureAdClientMock.server.start()
            behandlendeEnhetMock.server.start()
        }

        afterGroup {
            azureAdClientMock.server.stop(1L, 10L)
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
