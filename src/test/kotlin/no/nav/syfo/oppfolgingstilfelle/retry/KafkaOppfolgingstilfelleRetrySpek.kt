package no.nav.syfo.oppfolgingstilfelle.retry

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
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.kafka.*
import no.nav.syfo.oppfolgingstilfelle.OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_AKTORID
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.generator.generateKOppfolgingstilfelleRetry
import no.nav.syfo.testutil.mock.*
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@InternalAPI
object KafkaOversikthendelseRetrySpek : Spek({

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

        describe("Read and process KOversikthendelseRetry") {
            val oppfolgingstilfelleRetryService = OppfolgingstilfelleRetryService(
                oppfolgingstilfelleService = oppfolgingstilfelleService,
                oppfolgingstilfelleRetryProducer = oppfolgingstilfelleRetryProducer
            )

            val consumerPropertiesOppfolgingstilfelleRetry = kafkaOppfolgingstilfelleRetryConsumerProperties(env, vaultSecrets)
                .overrideForTest()
            val consumerOppfolgingstilfelleRetry = KafkaConsumer<String, String>(consumerPropertiesOppfolgingstilfelleRetry)
            consumerOppfolgingstilfelleRetry.subscribe(listOf(oppfolgingstilfelleRetryTopic))

            it("should send Oversikthendelsetilfelle when KOppfolgingstilfelleRetry is ready to retry and the new retry is successful") {

                val kOppfolgingstilfelleRetry = generateKOppfolgingstilfelleRetry.copy(
                    created = LocalDateTime.now().minusHours(RETRY_OPPFOLGINGSTILFELLE_INTERVAL_MINUTES).minusMinutes(1),
                    retryTime = LocalDateTime.now().minusMinutes(1)
                )
                val mockConsumerOppfolgingstilfelleRetry = createMockConsumerOppfolgingstilfelleRetry(kOppfolgingstilfelleRetry)

                runBlocking {
                    pollAndProcessOppfolgingstilfelleRetryTopic(
                        kafkaConsumer = mockConsumerOppfolgingstilfelleRetry,
                        oppfolgingstilfelleRetryService = oppfolgingstilfelleRetryService
                    )
                }

                val messages = getMessagesOversikthendelsetilfelle(consumerOversikthendelsetilfelle)
                messages.size shouldBeEqualTo 1
                messages.first().fnr shouldBeEqualTo ARBEIDSTAKER_FNR.value
                messages.first().enhetId shouldBeEqualTo behandlendeEnhetMock.behandlendeEnhet.enhetId
                messages.first().virksomhetsnummer shouldBeEqualTo kOppfolgingstilfelleRetry.orgnummer
            }

            it("should skip sending of Oversikthendelsetilfelle when retry has exceeded retry limit") {
                val kOppfolgingstilfelleRetry = generateKOppfolgingstilfelleRetry.copy(
                    created = LocalDateTime.now().minusHours(RETRY_OPPFOLGINGSTILFELLE_INTERVAL_MINUTES).minusMinutes(1),
                    retriedCount = RETRY_OPPFOLGINGSTILFELLE_COUNT_LIMIT
                )
                val mockConsumerOppfolgingstilfelleRetry = createMockConsumerOppfolgingstilfelleRetry(kOppfolgingstilfelleRetry)

                runBlocking {
                    pollAndProcessOppfolgingstilfelleRetryTopic(
                        kafkaConsumer = mockConsumerOppfolgingstilfelleRetry,
                        oppfolgingstilfelleRetryService = oppfolgingstilfelleRetryService
                    )
                }

                val messages = getMessagesOversikthendelsetilfelle(consumerOversikthendelsetilfelle)
                messages.size shouldBeEqualTo 0
            }
        }

        describe("Resend KOversikthendelseRetry") {
            val mockOppfolgingstilfelleRetryProducer = mockk<OppfolgingstilfelleRetryProducer>()
            justRun { mockOppfolgingstilfelleRetryProducer.sendAgainOppfolgingstilfelleRetry(any(), any()) }
            justRun { mockOppfolgingstilfelleRetryProducer.sendRetriedOppfolgingstilfelleRetry(any(), any()) }

            val oppfolgingstilfelleRetryService = OppfolgingstilfelleRetryService(
                oppfolgingstilfelleService = oppfolgingstilfelleService,
                oppfolgingstilfelleRetryProducer = mockOppfolgingstilfelleRetryProducer
            )

            val consumerPropertiesOppfolgingstilfelleRetry = kafkaOppfolgingstilfelleRetryConsumerProperties(env, vaultSecrets)
                .overrideForTest()

            val consumerOppfolgingstilfelleRetry = KafkaConsumer<String, String>(consumerPropertiesOppfolgingstilfelleRetry)
            consumerOppfolgingstilfelleRetry.subscribe(listOf(oppfolgingstilfelleRetryTopic))

            it("should resend KOversikthendelseRetry to topic when it has not exceeded retry limit and is not ready for retry") {
                val kOppfolgingstilfelleRetry = generateKOppfolgingstilfelleRetry.copy(
                    aktorId = ARBEIDSTAKER_AKTORID.aktor,
                    created = LocalDateTime.now(),
                    retryTime = LocalDateTime.now().plusHours(1),
                    retriedCount = 0
                )
                val mockConsumerOppfolgingstilfelleRetry = createMockConsumerOppfolgingstilfelleRetry(kOppfolgingstilfelleRetry)

                runBlocking {
                    pollAndProcessOppfolgingstilfelleRetryTopic(
                        kafkaConsumer = mockConsumerOppfolgingstilfelleRetry,
                        oppfolgingstilfelleRetryService = oppfolgingstilfelleRetryService
                    )
                }

                val messages = getMessagesOversikthendelsetilfelle(consumerOversikthendelsetilfelle)
                messages.size shouldBeEqualTo 0

                verify(exactly = 1) {
                    mockOppfolgingstilfelleRetryProducer.sendAgainOppfolgingstilfelleRetry(kOppfolgingstilfelleRetry, any())
                }
            }
        }

        describe("Read and process KOversikthendelseRetry with unavailable Aktorregister") {
            val mockOppfolgingstilfelleRetryProducer = mockk<OppfolgingstilfelleRetryProducer>()
            justRun { mockOppfolgingstilfelleRetryProducer.sendAgainOppfolgingstilfelleRetry(any(), any()) }
            justRun { mockOppfolgingstilfelleRetryProducer.sendRetriedOppfolgingstilfelleRetry(any(), any()) }

            val oppfolgingstilfelleRetryService = OppfolgingstilfelleRetryService(
                oppfolgingstilfelleService = oppfolgingstilfelleService,
                oppfolgingstilfelleRetryProducer = mockOppfolgingstilfelleRetryProducer
            )

            val consumerPropertiesOppfolgingstilfelleRetry = kafkaOppfolgingstilfelleRetryConsumerProperties(env, vaultSecrets)
                .overrideForTest()

            val consumerOppfolgingstilfelleRetry = KafkaConsumer<String, String>(consumerPropertiesOppfolgingstilfelleRetry)
            consumerOppfolgingstilfelleRetry.subscribe(listOf(oppfolgingstilfelleRetryTopic))

            it("should resend KOversikthendelseRetry when retried and failed due to missing Fodselsnummer") {
                val kOppfolgingstilfelleRetry = generateKOppfolgingstilfelleRetry.copy(
                    aktorId = aktorregisterMock.aktorIdMissingFnr.aktor,
                    created = LocalDateTime.now().minusHours(RETRY_OPPFOLGINGSTILFELLE_INTERVAL_MINUTES).minusMinutes(1),
                    retryTime = LocalDateTime.now().minusMinutes(1),
                    retriedCount = 0
                )
                val mockConsumerOppfolgingstilfelleRetry = createMockConsumerOppfolgingstilfelleRetry(kOppfolgingstilfelleRetry)

                runBlocking {
                    pollAndProcessOppfolgingstilfelleRetryTopic(
                        kafkaConsumer = mockConsumerOppfolgingstilfelleRetry,
                        oppfolgingstilfelleRetryService = oppfolgingstilfelleRetryService
                    )
                }

                val messages = getMessagesOversikthendelsetilfelle(consumerOversikthendelsetilfelle)
                messages.size shouldBeEqualTo 0

                verify(exactly = 1) {
                    mockOppfolgingstilfelleRetryProducer.sendRetriedOppfolgingstilfelleRetry(kOppfolgingstilfelleRetry, any())
                }
            }
        }

        describe("Read and process KOversikthendelseRetry with unavailable BehandlendeEnhet") {
            val mockOppfolgingstilfelleRetryProducer = mockk<OppfolgingstilfelleRetryProducer>()
            justRun { mockOppfolgingstilfelleRetryProducer.sendAgainOppfolgingstilfelleRetry(any(), any()) }
            justRun { mockOppfolgingstilfelleRetryProducer.sendRetriedOppfolgingstilfelleRetry(any(), any()) }

            val oppfolgingstilfelleRetryService = OppfolgingstilfelleRetryService(
                oppfolgingstilfelleService = oppfolgingstilfelleService,
                oppfolgingstilfelleRetryProducer = mockOppfolgingstilfelleRetryProducer
            )

            val consumerPropertiesOppfolgingstilfelleRetry = kafkaOppfolgingstilfelleRetryConsumerProperties(env, vaultSecrets)
                .overrideForTest()

            val consumerOppfolgingstilfelleRetry = KafkaConsumer<String, String>(consumerPropertiesOppfolgingstilfelleRetry)
            consumerOppfolgingstilfelleRetry.subscribe(listOf(oppfolgingstilfelleRetryTopic))

            it("should resend KOversikthendelseRetry when retried and failed due to missing behandlendeEnhet") {
                val kOppfolgingstilfelleRetry = generateKOppfolgingstilfelleRetry.copy(
                    aktorId = ARBEIDSTAKER_2_AKTORID.aktor,
                    created = LocalDateTime.now().minusHours(RETRY_OPPFOLGINGSTILFELLE_INTERVAL_MINUTES).minusMinutes(1),
                    retryTime = LocalDateTime.now().minusMinutes(1),
                    retriedCount = 0
                )
                val mockConsumerOppfolgingstilfelleRetry = createMockConsumerOppfolgingstilfelleRetry(kOppfolgingstilfelleRetry)

                runBlocking {
                    pollAndProcessOppfolgingstilfelleRetryTopic(
                        kafkaConsumer = mockConsumerOppfolgingstilfelleRetry,
                        oppfolgingstilfelleRetryService = oppfolgingstilfelleRetryService
                    )
                }

                val messages = getMessagesOversikthendelsetilfelle(consumerOversikthendelsetilfelle)
                messages.size shouldBeEqualTo 0

                verify(exactly = 1) {
                    mockOppfolgingstilfelleRetryProducer.sendRetriedOppfolgingstilfelleRetry(kOppfolgingstilfelleRetry, any())
                }
            }
        }
    }
})

private fun createMockConsumerOppfolgingstilfelleRetry(
    kOppfolgingstilfelleRetry: KOppfolgingstilfelleRetry
): KafkaConsumer<String, String> {
    val oppfolgingstilfelleRetryRecord = createKOppfolgingstilfelleRetryRecord(kOppfolgingstilfelleRetry)

    val mockConsumerOppfolgingstilfelleRetry = mockk<KafkaConsumer<String, String>>()
    justRun { mockConsumerOppfolgingstilfelleRetry.commitSync() }
    every { mockConsumerOppfolgingstilfelleRetry.assignment() } returns emptySet()
    every { mockConsumerOppfolgingstilfelleRetry.poll(Duration.ofMillis(1000)) } returns ConsumerRecords(
        mapOf(TopicPartition(OPPFOLGINGSTILFELLE_RETRY_TOPIC, 0) to listOf(oppfolgingstilfelleRetryRecord))
    )
    return mockConsumerOppfolgingstilfelleRetry
}

private fun createKOppfolgingstilfelleRetryRecord(
    kOppfolgingstilfelleRetry: KOppfolgingstilfelleRetry
): ConsumerRecord<String, String> {
    val kOversikthendelsetilfelleRetryJson = objectMapper.writeValueAsString(kOppfolgingstilfelleRetry)
    return ConsumerRecord(
        OPPFOLGINGSTILFELLE_RETRY_TOPIC,
        0,
        1,
        "something",
        kOversikthendelsetilfelleRetryJson
    )
}

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
