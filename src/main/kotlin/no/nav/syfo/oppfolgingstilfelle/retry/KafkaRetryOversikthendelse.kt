package no.nav.syfo.oppfolgingstilfelle.retry

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.*
import no.nav.syfo.kafka.kafkaOppfolgingstilfelleRetryConsumerProperties
import no.nav.syfo.util.callIdArgument
import no.nav.syfo.util.kafkaCallId
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

private val LOG: Logger = LoggerFactory.getLogger("no.nav.syfo.oppfolgingstilfelle.retry")

suspend fun blockingApplicationLogicOppfolgingstilfelleRetry(
    applicationState: ApplicationState,
    environment: Environment,
    vaultSecrets: VaultSecrets,
    oppfolgingstilfelleRetryService: OppfolgingstilfelleRetryService
) {
    LOG.info("Setting up kafka consumer OppfolgingstilfelleRetry")

    val consumerOppfolgingstilfelleRetryProperties = kafkaOppfolgingstilfelleRetryConsumerProperties(environment, vaultSecrets)
    val kafkaConsumerOppfolgingstilfelleRetry = KafkaConsumer<String, String>(consumerOppfolgingstilfelleRetryProperties)

    kafkaConsumerOppfolgingstilfelleRetry.subscribe(
        listOf(OPPFOLGINGSTILFELLE_RETRY_TOPIC)
    )

    while (applicationState.running) {
        pollAndProcessOppfolgingstilfelleRetryTopic(
            kafkaConsumer = kafkaConsumerOppfolgingstilfelleRetry,
            oppfolgingstilfelleRetryService = oppfolgingstilfelleRetryService
        )
        delay(1000L)
    }
}

suspend fun pollAndProcessOppfolgingstilfelleRetryTopic(
    kafkaConsumer: KafkaConsumer<String, String>,
    oppfolgingstilfelleRetryService: OppfolgingstilfelleRetryService
) {
    var logValues = arrayOf(
        StructuredArguments.keyValue("id", "missing"),
        StructuredArguments.keyValue("timestamp", "missing")
    )

    val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
        "{}"
    }

    val messages = kafkaConsumer.poll(Duration.ofMillis(1000))
    messages.forEach {
        val callId = kafkaCallId()
        val kOppfolgingstilfelleRetry: KOppfolgingstilfelleRetry = objectMapper.readValue(it.value())
        logValues = arrayOf(
            StructuredArguments.keyValue("id", it.key()),
            StructuredArguments.keyValue("timestamp", it.timestamp())
        )
        LOG.info("Received KOppfolgingstilfelleRetry, ready to process, $logKeys, {}", *logValues, callIdArgument(callId))
        oppfolgingstilfelleRetryService.receiveOversikthendelseRetry(kOppfolgingstilfelleRetry)
    }
    kafkaConsumer.commitSync()
}

fun CoroutineScope.createListenerOppfolgingstilfelleRetry(
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
