package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.*
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.domain.KOppfolgingstilfellePeker
import no.nav.syfo.util.CallIdArgument
import no.nav.syfo.util.kafkaCallId
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

private val LOG: Logger = LoggerFactory.getLogger("no.nav.syfo.kafka")

suspend fun blockingApplicationLogicOppfolgingstilfelle(
    applicationState: ApplicationState,
    environment: Environment,
    vaultSecrets: VaultSecrets,
    oppfolgingstilfelleService: OppfolgingstilfelleService
) {
    LOG.info("Setting up kafka consumer Oppfolgingstilfelle")

    val consumerOppfolgingstilfelleProperties = kafkaOppfolgingstilfelleConsumerProperties(environment, vaultSecrets)
    val kafkaConsumerOppfolgingstilfelle = KafkaConsumer<String, String>(consumerOppfolgingstilfelleProperties)

    val subscriptionCallback = object : ConsumerRebalanceListener {
        override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
            if (env.oversikthendelseOppfolgingstilfelleTopicSeekToBeginning) {
                log.info("onPartitionsAssigned called for ${partitions?.size ?: 0} partitions. Seeking to beginning.")
                kafkaConsumerOppfolgingstilfelle.seekToBeginning(partitions)
            }
        }

        override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {}
    }

    kafkaConsumerOppfolgingstilfelle.subscribe(
        listOf("aapen-syfo-oppfolgingstilfelle-v1"),
        subscriptionCallback
    )

    while (applicationState.running) {
        pollAndProcessOppfolgingstilfelle(
            kafkaConsumer = kafkaConsumerOppfolgingstilfelle,
            oppfolgingstilfelleService = oppfolgingstilfelleService
        )
        delay(100)
    }
}

suspend fun pollAndProcessOppfolgingstilfelle(
    kafkaConsumer: KafkaConsumer<String, String>,
    oppfolgingstilfelleService: OppfolgingstilfelleService
) {
    var logValues = arrayOf(
        StructuredArguments.keyValue("oppfolgingstilfelleId", "missing"),
        StructuredArguments.keyValue("timestamp", "missing")
    )

    val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
        "{}"
    }
    kafkaConsumer.poll(Duration.ofMillis(0)).forEach {
        if (env.toggleOversikthendelsetilfelle) {
            val callId = kafkaCallId()
            val oppfolgingstilfellePeker: KOppfolgingstilfellePeker =
                objectMapper.readValue(it.value())
            logValues = arrayOf(
                StructuredArguments.keyValue("oppfolgingstilfelleId", it.key()),
                StructuredArguments.keyValue("timestamp", it.timestamp())
            )
            LOG.info("Mottatt oppfolgingstilfellePeker, klar for behandling, $logKeys, {}", *logValues, CallIdArgument(callId))

            oppfolgingstilfelleService.receiveOppfolgingstilfeller(oppfolgingstilfellePeker, callId)
        }
    }
}

fun CoroutineScope.createListenerOppfolgingstilfelle(
    applicationState: ApplicationState,
    action: suspend CoroutineScope.() -> Unit
): Job =
    launch {
        try {
            action()
        } finally {
            applicationState.running = false
        }
    }

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}
