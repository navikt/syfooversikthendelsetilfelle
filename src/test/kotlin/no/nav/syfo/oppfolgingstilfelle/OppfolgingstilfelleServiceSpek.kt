package no.nav.syfo.oppfolgingstilfelle

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.common.KafkaEnvironment
import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.aktor.AktorregisterClient
import no.nav.syfo.client.enhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.client.pdl.Gradering
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.kafka.*
import no.nav.syfo.oppfolgingstilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oppfolgingstilfelle.retry.*
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_AKTORID
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.mock.*
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@InternalAPI
object OppfolgingstilfelleServiceSpek : Spek({
    with(TestApplicationEngine()) {
        start()

        val oversikthendelsetilelleTopic = OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC
        val oppfolgingstilfelleRetryTopic = OPPFOLGINGSTILFELLE_RETRY_TOPIC
        val embeddedEnvironment = KafkaEnvironment(
            autoStart = false,
            withSchemaRegistry = false,
            topicNames = listOf(
                oppfolgingstilfelleRetryTopic,
                oversikthendelsetilelleTopic
            )
        )
        val env = testEnvironment(
            getRandomPort(),
            embeddedEnvironment.brokersURL
        )

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
        val aktorService = AktorService(aktorregisterClient)

        val behandlendeEnhetMock = BehandlendeEnhetMock()
        val behandlendeEnhetClient = BehandlendeEnhetClient(
            baseUrl = behandlendeEnhetMock.url,
            stsRestClient = stsRestClient
        )

        val eregMock = EregMock()
        val eregClient = EregClient(
            baseUrl = eregMock.url,
            stsRestClient = stsRestClient
        )
        val eregService = EregService(eregClient)

        val pdlMock = PdlMock()
        val pdlClient = PdlClient(
            baseUrl = pdlMock.url,
            stsRestClient = stsRestClient
        )

        val syketilfelleMock = SyketilfelleMock()
        val syketilfelleClient = SyketilfelleClient(
            baseUrl = syketilfelleMock.url,
            stsRestClient = stsRestClient
        )

        val oversikthendelsetilfelleProducerProperties = kafkaOversikthendelsetilfelleProducerProperties(env, vaultSecrets)
            .overrideForTest()
        val oversikthendelsetilfelleRecordProducer = KafkaProducer<String, KOversikthendelsetilfelle>(oversikthendelsetilfelleProducerProperties)

        val oppfolgingstilfelleRetryProducerProperties = kafkaOppfolgingstilfelleRetryProducerConfig(env, vaultSecrets)
            .overrideForTest()
        val oppfolgingstilfelleRetryRecordProducer = KafkaProducer<String, KOppfolgingstilfelleRetry>(oppfolgingstilfelleRetryProducerProperties)
        val oppfolgingstilfelleRetryProducer = OppfolgingstilfelleRetryProducer(oppfolgingstilfelleRetryRecordProducer)

        val oppfolgingstilfelleService = OppfolgingstilfelleService(
            aktorService = aktorService,
            eregService = eregService,
            behandlendeEnhetClient = behandlendeEnhetClient,
            pdlClient = pdlClient,
            syketilfelleClient = syketilfelleClient,
            oppfolgingstilfelleRetryProducer = oppfolgingstilfelleRetryProducer,
            producer = oversikthendelsetilfelleRecordProducer
        )

        val consumerPropertiesOversikthendelsetilfelle = kafkaOppfolgingstilfelleRetryConsumerProperties(env, vaultSecrets)
            .overrideForTest()
            .apply {
                put("specific.avro.reader", false)
                put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
                put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
            }
        val consumerOversikthendelsetilfelle = KafkaConsumer<String, String>(consumerPropertiesOversikthendelsetilfelle)
        consumerOversikthendelsetilfelle.subscribe(listOf(oversikthendelsetilelleTopic))

        beforeGroup {
            embeddedEnvironment.start()

            aktorregisterMock.server.start()
            behandlendeEnhetMock.server.start()
            eregMock.server.start()
            pdlMock.server.start()
            stsRestMock.server.start()
            syketilfelleMock.server.start()
        }

        afterGroup {
            embeddedEnvironment.tearDown()

            aktorregisterMock.server.stop(1L, 10L)
            behandlendeEnhetMock.server.stop(1L, 10L)
            eregMock.server.stop(1L, 10L)
            pdlMock.server.stop(1L, 10L)
            stsRestMock.server.stop(1L, 10L)
            syketilfelleMock.server.stop(1L, 10L)
        }

        describe("Receive kOppfolgingsplanLPSNAV") {
            it("Should send KOversikthendelsetilfelle ") {
                runBlocking {
                    oppfolgingstilfelleService.receiveOppfolgingstilfelle(
                        oppfolgingstilfelleRecordTimestamp = LocalDateTime.now(),
                        aktorId = ARBEIDSTAKER_AKTORID,
                        orgnummer = Virksomhetsnummer(VIRKSOMHETSNUMMER),
                        callId = ""
                    )
                }

                val messages = getMessagesOversikthendelsetilfelle(consumerOversikthendelsetilfelle)

                messages.size shouldBeEqualTo 1
                messages.first().fnr shouldBeEqualTo ARBEIDSTAKER_FNR.value
                messages.first().enhetId shouldBeEqualTo behandlendeEnhetMock.behandlendeEnhet.enhetId
                messages.first().virksomhetsnummer shouldBeEqualTo VIRKSOMHETSNUMMER
            }
        }

        describe("Receive kOppfolgingsplanLPSNAV") {
            val mockOppfolgingstilfelleRetryProducer = mockk<OppfolgingstilfelleRetryProducer>()
            justRun { mockOppfolgingstilfelleRetryProducer.sendFirstOppfolgingstilfelleRetry(any(), any(), any(), any()) }

            val pdlMockGradering = PdlMock(Gradering.STRENGT_FORTROLIG)
            val pdlClientMockGradering = PdlClient(
                baseUrl = pdlMockGradering.url,
                stsRestClient = stsRestClient
            )
            val oppfolgingstilfelleServiceGradering = OppfolgingstilfelleService(
                aktorService = aktorService,
                eregService = eregService,
                behandlendeEnhetClient = behandlendeEnhetClient,
                pdlClient = pdlClientMockGradering,
                syketilfelleClient = syketilfelleClient,
                oppfolgingstilfelleRetryProducer = mockOppfolgingstilfelleRetryProducer,
                producer = oversikthendelsetilfelleRecordProducer
            )

            beforeEachTest {
                pdlMockGradering.server.start()
            }

            afterEachTest {
                pdlMockGradering.server.stop(1L, 10L)
            }

            it("Should skip and not send KOversikthendelsetilfelle when person is ${Gradering.STRENGT_FORTROLIG}") {
                runBlocking {
                    oppfolgingstilfelleServiceGradering.receiveOppfolgingstilfelle(
                        oppfolgingstilfelleRecordTimestamp = LocalDateTime.now(),
                        aktorId = ARBEIDSTAKER_AKTORID,
                        orgnummer = Virksomhetsnummer(VIRKSOMHETSNUMMER),
                        callId = ""
                    )
                }

                val messages = getMessagesOversikthendelsetilfelle(consumerOversikthendelsetilfelle)
                messages.size shouldBeEqualTo 0

                verify(exactly = 0) {
                    mockOppfolgingstilfelleRetryProducer.sendFirstOppfolgingstilfelleRetry(
                        any(),
                        any(),
                        any(),
                        any()
                    )
                }
            }
        }

        describe("Error handling") {
            val mockOppfolgingstilfelleRetryProducer = mockk<OppfolgingstilfelleRetryProducer>()
            justRun { mockOppfolgingstilfelleRetryProducer.sendFirstOppfolgingstilfelleRetry(any(), any(), any(), any()) }

            val consumerPropertiesOppfolgingstilfelleRetry = kafkaOppfolgingstilfelleRetryConsumerProperties(env, vaultSecrets)
                .overrideForTest()

            val consumerOppfolgingstilfelleRetry = KafkaConsumer<String, String>(consumerPropertiesOppfolgingstilfelleRetry)
            consumerOppfolgingstilfelleRetry.subscribe(listOf(oppfolgingstilfelleRetryTopic))

            val oppfolgingstilfelleServiceWithMockRetry = OppfolgingstilfelleService(
                aktorService = aktorService,
                eregService = eregService,
                behandlendeEnhetClient = behandlendeEnhetClient,
                pdlClient = pdlClient,
                syketilfelleClient = syketilfelleClient,
                oppfolgingstilfelleRetryProducer = mockOppfolgingstilfelleRetryProducer,
                producer = oversikthendelsetilfelleRecordProducer
            )

            it("Should send Oppfolgingstilfelle to retry if processing is not successful") {
                val aktorId = ARBEIDSTAKER_2_AKTORID
                val orgnummer = Virksomhetsnummer(VIRKSOMHETSNUMMER)
                runBlocking {
                    oppfolgingstilfelleServiceWithMockRetry.receiveOppfolgingstilfelle(
                        oppfolgingstilfelleRecordTimestamp = LocalDateTime.now(),
                        aktorId = aktorId,
                        orgnummer = orgnummer,
                        callId = ""
                    )
                }

                val messages = getMessagesOversikthendelsetilfelle(consumerOversikthendelsetilfelle)
                messages.size shouldBeEqualTo 0

                verify(exactly = 1) {
                    mockOppfolgingstilfelleRetryProducer.sendFirstOppfolgingstilfelleRetry(
                        any(),
                        aktorId,
                        orgnummer,
                        ""
                    )
                }
            }
        }
    }
})

private fun getMessagesOversikthendelsetilfelle(
    consumer: KafkaConsumer<String, String>
): ArrayList<KOversikthendelsetilfelle> {
    val messages: ArrayList<KOversikthendelsetilfelle> = arrayListOf()
    consumer.poll(Duration.ofMillis(5000)).forEach {
        val consumedOversikthendelse: KOversikthendelsetilfelle = objectMapper.readValue(it.value())
        messages.add(consumedOversikthendelse)
    }
    return messages
}

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}
