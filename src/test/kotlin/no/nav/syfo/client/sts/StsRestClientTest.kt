package no.nav.syfo.client.sts

import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.testutil.mock.StsRestMock
import no.nav.syfo.testutil.vaultSecrets
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
object StsRestClientTest : Spek({
    with(TestApplicationEngine()) {
        start()

        val vaultSecrets = vaultSecrets

        val stsRestMock = StsRestMock()
        val stsRestClient = StsRestClient(
            baseUrl = stsRestMock.url,
            username = vaultSecrets.serviceuserUsername,
            password = vaultSecrets.serviceuserPassword
        )

        beforeGroup {
            stsRestMock.server.start()
        }

        afterGroup {
            stsRestMock.server.stop(1L, 10L)
        }

        describe("OIDC Token") {
            it("should parse a token successfully") {
                val token: String = runBlocking {
                    stsRestClient.token()
                }
                token shouldBeEqualTo stsRestMock.defaultToken.access_token
            }
        }
    }
})
