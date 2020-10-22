package no.nav.syfo.client.syketilfelle

import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_ANNET
import no.nav.syfo.testutil.mock.StsRestMock
import no.nav.syfo.testutil.mock.SyketilfelleMock
import no.nav.syfo.testutil.vaultSecrets
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
object SyketilfelleClientTest : Spek({

    with(TestApplicationEngine()) {
        start()

        val vaultSecrets = vaultSecrets

        val stsRestMock = StsRestMock()
        val stsRestClient = StsRestClient(
            baseUrl = stsRestMock.url,
            username = vaultSecrets.serviceuserUsername,
            password = vaultSecrets.serviceuserPassword
        )

        val syketilfelleMock = SyketilfelleMock()
        val syketilfelleClient = SyketilfelleClient(
            baseUrl = syketilfelleMock.url,
            stsRestClient = stsRestClient
        )

        beforeGroup {
            stsRestMock.server.start()
            syketilfelleMock.server.start()
        }

        afterGroup {
            stsRestMock.server.stop(1L, 10L)
            syketilfelleMock.server.stop(1L, 10L)
        }

        describe("SyketilfelleClient successful") {
            it("Get oppfolgingstilfelle for aktorId with virksomhetsnummer") {
                val result = syketilfelleClient.getOppfolgingstilfelle(
                    ARBEIDSTAKER_AKTORID.aktor,
                    VIRKSOMHETSNUMMER,
                    "callId"
                )

                result shouldEqual syketilfelleMock.kOppfolgingstilfelle
            }

            it("Do not find oppfolgingstilfelle for aktorId with virksomhetsnummer") {
                val result = syketilfelleClient.getOppfolgingstilfelle(
                    ARBEIDSTAKER_AKTORID.aktor,
                    VIRKSOMHETSNUMMER_ANNET,
                    "callId"
                )

                result shouldEqual null
            }
        }
    }
})
