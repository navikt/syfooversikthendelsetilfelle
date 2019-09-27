package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.*
import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.client.enhet.BehandlendeEnhetClient
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.domain.KOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.util.*
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

private val LOG: Logger = LoggerFactory.getLogger("no.nav.syfo.Kafka")


suspend fun CoroutineScope.setupKafka(
        vaultSecrets: KafkaCredentials,
        aktorService: AktorService,
        eregService: EregService,
        behandlendeEnhetClient: BehandlendeEnhetClient)
{
    LOG.info("Setting up kafka consumer")
    val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets)
            .envOverrides()
    val consumerProperties = kafkaBaseConfig.toConsumerConfig(
            "${env.applicationName}-consumer", valueDeserializer = StringDeserializer::class
    )
    val producerProperties = kafkaBaseConfig.toProducerConfig(
            "${env.applicationName}-producer", valueSerializer = JacksonKafkaSerializer::class
    )
    val oversikthendelseTilfelleProducer = KafkaProducer<String, KOversikthendelsetilfelle>(producerProperties)

    val oppfolgingstilfelleService = OppfolgingstilfelleService(
            aktorService,
            eregService,
            behandlendeEnhetClient,
            oversikthendelseTilfelleProducer
    )
    launchListeners(consumerProperties, state, oppfolgingstilfelleService)
}


@KtorExperimentalAPI
suspend fun blockingApplicationLogic(
        applicationState: ApplicationState,
        kafkaConsumer: KafkaConsumer<String, String>,
        oppfolgingstilfelleService: OppfolgingstilfelleService
) {
    while (applicationState.running) {
        var logValues = arrayOf(
                StructuredArguments.keyValue("oppfolgingstilfelleId", "missing")
        )

        val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
            "{}"
        }

        kafkaConsumer.poll(Duration.ofMillis(0)).forEach {
            val callId = kafkaCallId()
            val oppfolgingstilfeller: KOppfolgingstilfelle =
                    objectMapper.readValue(it.value())
            logValues = arrayOf(
                    StructuredArguments.keyValue("oppfolgingstilfelleId", it.key())
            )
            LOG.info("Mottatt oppfolgingstilfelle, klar for behandling, $logKeys, {}", *logValues, CallIdArgument(callId))

            if (isPreProd()) {
                oppfolgingstilfelleService.receiveOppfolgingstilfeller(oppfolgingstilfeller, callId)
            }
        }
        delay(100)
    }
}

@KtorExperimentalAPI
suspend fun CoroutineScope.launchListeners(
        consumerProperties: Properties,
        applicationState: ApplicationState,
        oppfolgingstilfelleService: OppfolgingstilfelleService
) {

    val kafkaconsumerOppgave = KafkaConsumer<String, String>(consumerProperties)

    val subscriptionCallback = object : ConsumerRebalanceListener {
        override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
            if (env.oversikthendelseOppfolgingstilfelleTopicSeekToBeginning) {
                kafkaconsumerOppgave.seekToBeginning(partitions)
            }
        }

        override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {}
    }

    kafkaconsumerOppgave.subscribe(
            listOf("aapen-syfo-oppfolgingstilfelle-v1"),
            subscriptionCallback
    )

    createListener(applicationState) {
        blockingApplicationLogic(applicationState, kafkaconsumerOppgave, oppfolgingstilfelleService)
    }


    applicationState.initialized = true
}
