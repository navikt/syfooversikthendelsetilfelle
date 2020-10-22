package no.nav.syfo.client.aktor

import arrow.core.Either
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.mock.AktorregisterMock
import no.nav.syfo.testutil.mock.StsRestMock
import no.nav.syfo.testutil.vaultSecrets
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertTrue

@InternalAPI
object AktorregisterClientTest : Spek({

    with(TestApplicationEngine()) {
        start()

        val vaultSecrets = vaultSecrets

        val stsRestMock = StsRestMock()
        val stsRestClient = StsRestClient(
            baseUrl = stsRestMock.url,
            username = vaultSecrets.serviceuserUsername,
            password = vaultSecrets.serviceuserPassword
        )

        val aktorregisterMock = AktorregisterMock()
        val aktorregisterClient = AktorregisterClient(
            baseUrl = aktorregisterMock.url,
            stsRestClient = stsRestClient
        )

        beforeGroup {
            stsRestMock.server.start()
            aktorregisterMock.server.start()
        }

        afterGroup {
            stsRestMock.server.stop(1L, 10L)
            aktorregisterMock.server.stop(1L, 10L)
        }

        describe("AktorIdClient successful") {
            it("Get fnr for aktor that exists") {
                var fnr: String? = null
                runBlocking {
                    val lookupResult = aktorregisterClient.getIdenter(ARBEIDSTAKER_FNR.value, "callId")
                    assertTrue(lookupResult is Either.Right)
                    fnr = lookupResult.b.first { it.type == IdentType.NorskIdent }.ident
                }

                fnr shouldEqual ARBEIDSTAKER_FNR.value
            }

            it("Get aktør for fnr that exists") {
                var aktorId: String? = null
                runBlocking {
                    val lookupResult = aktorregisterClient.getIdenter(ARBEIDSTAKER_AKTORID.aktor, "callId")
                    assertTrue(lookupResult is Either.Right)
                    aktorId = lookupResult.b.first { it.type == IdentType.AktoerId }.ident
                }

                aktorId shouldEqual ARBEIDSTAKER_AKTORID.aktor
            }
        }
    }
})
