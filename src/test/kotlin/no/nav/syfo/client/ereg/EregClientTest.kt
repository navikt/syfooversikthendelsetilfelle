package no.nav.syfo.client.ereg

import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.mock.EregMock
import no.nav.syfo.testutil.mock.StsRestMock
import no.nav.syfo.testutil.vaultSecrets
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
object EregClientTest : Spek({

    with(TestApplicationEngine()) {
        start()

        val vaultSecrets = vaultSecrets

        val stsRestMock = StsRestMock()
        val stsRestClient = StsRestClient(
            baseUrl = stsRestMock.url,
            username = vaultSecrets.serviceuserUsername,
            password = vaultSecrets.serviceuserPassword
        )

        val eregMock = EregMock()
        val eregClient = EregClient(
            baseUrl = eregMock.url,
            stsRestClient = stsRestClient
        )

        beforeGroup {
            stsRestMock.server.start()
            eregMock.server.start()
        }

        afterGroup {
            stsRestMock.server.stop(1L, 10L)
            eregMock.server.stop(1L, 10L)
        }

        describe("hentOrgByOrgnr()") {
            it("Returns valid response when ok") {
                val orgNavn = runBlocking {
                    eregClient.hentOrgByOrgnr(
                        VIRKSOMHETSNUMMER,
                        "callId"
                    )
                }
                orgNavn?.navn?.navnelinje1 shouldBeEqualTo eregMock.eregResponse.navn.navnelinje1
                orgNavn?.navn?.redigertnavn shouldBeEqualTo eregMock.eregResponse.navn.redigertnavn
            }
        }
    }
})
