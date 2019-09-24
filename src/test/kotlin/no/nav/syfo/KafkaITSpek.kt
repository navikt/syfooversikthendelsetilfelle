package no.nav.syfo

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.KafkaEnvironment
import no.nav.syfo.kafka.*
import no.nav.syfo.oppfolgingstilfelle.domain.*
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import no.nav.syfo.testutil.generator.generateOversikthendelsetilfelle
import org.amshove.kluent.shouldEqual
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer
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
            topics = listOf(
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
            jwkKeysUrl = "",
            jwtIssuer = "",
            aadDiscoveryUrl = "",
            clientid = "",
            toggleOversikthendelsetilfelle = true,
            aktoerregisterV1Url = "aktorurl",
            stsRestUrl = "stsurl",
            eregApiBaseUrl = ""
    )

    fun Properties.overrideForTest(): Properties = apply {
        remove("security.protocol")
        remove("sasl.mechanism")
    }

    val baseConfig = loadBaseConfig(env, credentials).overrideForTest()

    val producerPropertiesTilfelle = baseConfig
            .toProducerConfig("spek.integration-producer1", valueSerializer = JacksonKafkaSerializer::class)
    val producerTilfelle = KafkaProducer<String, KOppfolgingstilfelle>(producerPropertiesTilfelle)
    val consumerPropertiesTilfelle = baseConfig
            .toConsumerConfig("spek.integration-consumer1", valueDeserializer = StringDeserializer::class)
    val consumerTilfelle = KafkaConsumer<String, String>(consumerPropertiesTilfelle)
    consumerTilfelle.subscribe(listOf(env.oppfolgingstilfelleTopic))

    val producerPropertiesOversikthendelse = baseConfig
            .toProducerConfig("spek.integration-producer2", valueSerializer = JacksonKafkaSerializer::class)
    val producerOversikthendelse = KafkaProducer<String, KOversikthendelsetilfelle>(producerPropertiesOversikthendelse)
    val consumerPropertiesOversikthendelse = baseConfig
            .toConsumerConfig("spek.integration-consumer2", valueDeserializer = StringDeserializer::class)
    val consumerTilfelleOversikthendelse = KafkaConsumer<String, String>(consumerPropertiesOversikthendelse)
    consumerTilfelleOversikthendelse.subscribe(listOf(env.oversikthendelseOppfolgingstilfelleTopic))

    beforeGroup {
        embeddedEnvironment.start()
    }

    afterGroup {
        embeddedEnvironment.tearDown()
    }

    describe("Produce and consume messages from topic") {
        it("Topic ${env.oppfolgingstilfelleTopic}") {
            val oppfolgingstilfelle = generateOppfolgingstilfelle.copy()
            producerTilfelle.send(SyfoProducerRecord(env.oppfolgingstilfelleTopic, UUID.randomUUID().toString(), oppfolgingstilfelle))

            val messages: ArrayList<KOppfolgingstilfelle> = arrayListOf()
            consumerTilfelle.poll(Duration.ofMillis(5000)).forEach {
                val tilfelle: KOppfolgingstilfelle = objectMapper.readValue(it.value())
                messages.add(tilfelle)

            }
            messages.size shouldEqual 1
            messages.first() shouldEqual oppfolgingstilfelle
        }

        it("Topic ${env.oversikthendelseOppfolgingstilfelleTopic}") {
            val oversikthendelsetilfelle = generateOversikthendelsetilfelle.copy()
            producerOversikthendelse.send(SyfoProducerRecord(env.oversikthendelseOppfolgingstilfelleTopic, UUID.randomUUID().toString(), oversikthendelsetilfelle))

            val messages: ArrayList<KOversikthendelsetilfelle> = arrayListOf()
            consumerTilfelleOversikthendelse.poll(Duration.ofMillis(5000)).forEach {
                val hendelse: KOversikthendelsetilfelle = objectMapper.readValue(it.value())
                messages.add(hendelse)

            }
            messages.size shouldEqual 1
            messages.first() shouldEqual oversikthendelsetilfelle
        }
    }
})
