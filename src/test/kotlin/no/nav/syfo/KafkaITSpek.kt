package no.nav.syfo

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.KafkaEnvironment
import no.nav.syfo.kafka.*
import no.nav.syfo.oppfolgingstilfelle.domain.*
import no.nav.syfo.testutil.generator.generateOppfolgingstilfellePeker
import no.nav.syfo.testutil.generator.generateOversikthendelsetilfelle
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket
import java.time.Duration
import java.util.*

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

object KafkaITSpek : Spek({

    fun getRandomPort() = ServerSocket(0).use {
        it.localPort
    }

    val oppfolgingstilfelleTopic = "oppfolgingstilfelle-topic"
    val oversikthendelseOppfolgingstilfelleTopic = "oversikthendelse-oppfolgingstilfelle-topic"

    val embeddedEnvironment = KafkaEnvironment(
        autoStart = false,
        topicNames = listOf(
            oversikthendelseOppfolgingstilfelleTopic
        )
    )

    val credentials = VaultSecrets(
        "",
        ""
    )
    val env = Environment(
        applicationPort = getRandomPort(),
        applicationThreads = 1,
        oppfolgingstilfelleTopic = oppfolgingstilfelleTopic,
        oversikthendelseOppfolgingstilfelleTopic = oversikthendelseOppfolgingstilfelleTopic,
        kafkaBootstrapServers = embeddedEnvironment.brokersURL,
        applicationName = "syfooversikthendelsetilfelle",
        behandlendeenhetUrl = "behandlendeenhet",
        pdlUrl = "pdlurl",
        syketilfelleUrl = "syketilfelle",
        toggleOversikthendelsetilfelle = true,
        oversikthendelseOppfolgingstilfelleTopicSeekToBeginning = false,
        aktoerregisterV1Url = "aktorurl",
        stsRestUrl = "stsurl",
        eregApiBaseUrl = ""
    )

    fun Properties.overrideForTest(): Properties = apply {
        remove("security.protocol")
        remove("sasl.mechanism")
    }

    val producerPropertiesTilfelle = kafkaOversikthendelsetilfelleProducerProperties(env, credentials)
        .overrideForTest()
        .apply {
            put("value.serializer", JacksonKafkaSerializer::class.java.canonicalName)
        }
    val producerTilfelle = KafkaProducer<String, KOppfolgingstilfellePeker>(producerPropertiesTilfelle)

    val consumerPropertiesTilfelle = kafkaOppfolgingstilfelleConsumerProperties(env, credentials)
        .overrideForTest()
        .apply {
            put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-oppfolgingstilfelle")
        }
    val consumerTilfelle = KafkaConsumer<String, String>(consumerPropertiesTilfelle)
    consumerTilfelle.subscribe(listOf(env.oppfolgingstilfelleTopic))

    val producerPropertiesOversikthendelse = kafkaOversikthendelsetilfelleProducerProperties(env, credentials)
        .overrideForTest()
    val producerOversikthendelse = KafkaProducer<String, KOversikthendelsetilfelle>(producerPropertiesOversikthendelse)

    val consumerPropertiesOversikthendelse = kafkaOppfolgingstilfelleConsumerProperties(env, credentials)
        .overrideForTest()
        .apply {
            put(ConsumerConfig.GROUP_ID_CONFIG, "-consumer-oversikthendelstilfelle")
        }
    val consumerTilfelleOversikthendelse = KafkaConsumer<String, String>(consumerPropertiesOversikthendelse)
    consumerTilfelleOversikthendelse.subscribe(listOf(env.oversikthendelseOppfolgingstilfelleTopic))

    beforeGroup {
        embeddedEnvironment.start()
    }

    afterGroup {
        embeddedEnvironment.tearDown()
    }

    describe("Topic ${env.oppfolgingstilfelleTopic}") {
        it("Produce and consume messages from topic") {
            val kOppfolgingstilfellePeker = generateOppfolgingstilfellePeker.copy()
            producerTilfelle.send(SyfoProducerRecord(env.oppfolgingstilfelleTopic, UUID.randomUUID().toString(), kOppfolgingstilfellePeker))

            val messages: ArrayList<KOppfolgingstilfellePeker> = arrayListOf()
            consumerTilfelle.poll(Duration.ofMillis(5000)).forEach {
                val tilfellePeker: KOppfolgingstilfellePeker = objectMapper.readValue(it.value())
                messages.add(tilfellePeker)
            }
            messages.size shouldBeEqualTo 1
            messages.first() shouldBeEqualTo kOppfolgingstilfellePeker
        }
    }

    describe("Topic ${env.oversikthendelseOppfolgingstilfelleTopic}") {
        it("Produce and consume messages from topic") {
            val oversikthendelsetilfelle = generateOversikthendelsetilfelle.copy()
            producerOversikthendelse.send(SyfoProducerRecord(env.oversikthendelseOppfolgingstilfelleTopic, UUID.randomUUID().toString(), oversikthendelsetilfelle))

            val messages: ArrayList<KOversikthendelsetilfelle> = arrayListOf()
            consumerTilfelleOversikthendelse.poll(Duration.ofMillis(5000)).forEach {
                val hendelse: KOversikthendelsetilfelle = objectMapper.readValue(it.value())
                messages.add(hendelse)
            }
            messages.size shouldBeEqualTo 1
            messages.first() shouldBeEqualTo oversikthendelsetilfelle
        }
    }
})
