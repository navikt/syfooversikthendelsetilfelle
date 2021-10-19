package no.nav.syfo.oppfolgingstilfelle.retry

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.domain.AktorId
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.metric.*
import no.nav.syfo.oppfolgingstilfelle.domain.SyfoProducerRecord
import no.nav.syfo.util.NAV_CALL_ID
import no.nav.syfo.util.callIdArgument
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

const val OPPFOLGINGSTILFELLE_RETRY_TOPIC = "privat-syfooversikt-tilfelle-retry-v1"

class OppfolgingstilfelleRetryProducer(
    private val producer: KafkaProducer<String, KOppfolgingstilfelleRetry>
) {
    fun sendFirstOppfolgingstilfelleRetry(
        oppfolgingstilfelleRecordTimestamp: LocalDateTime,
        aktorId: AktorId,
        orgnummer: Virksomhetsnummer,
        callId: String
    ) {
        val now = LocalDateTime.now()
        val firstKOppfolgingstilfelleRetry = KOppfolgingstilfelleRetry(
            created = now,
            retryTime = now.plusMinutes(RETRY_OPPFOLGINGSTILFELLE_INTERVAL_MINUTES),
            retriedCount = 0,
            aktorId = aktorId.aktor,
            orgnummer = orgnummer.value,
            oppfolgingstilfelleRecordTimestamp = oppfolgingstilfelleRecordTimestamp
        )
        producer.send(producerRecord(callId, firstKOppfolgingstilfelleRetry))
        log.warn(
            "Sent first KOppfolgingstilfelleRetry: {}, {}, {}",
            StructuredArguments.keyValue("retriedCount", firstKOppfolgingstilfelleRetry.retriedCount)!!,
            StructuredArguments.keyValue("retryTime", firstKOppfolgingstilfelleRetry.retryTime)!!,
            callIdArgument(callId)
        )
        COUNT_OPPFOLGINGSTILFELLE_RETRY_FIRST.inc()
    }

    fun sendRetriedOppfolgingstilfelleRetry(
        kOppfolgingstilfelleRetry: KOppfolgingstilfelleRetry,
        callId: String
    ) {
        val now = LocalDateTime.now()
        val newRetryCounter = kOppfolgingstilfelleRetry.retriedCount.plus(1)
        val newKOppfolgingstilfelleRetry = kOppfolgingstilfelleRetry.copy(
            created = now,
            retryTime = now.plusMinutes(RETRY_OPPFOLGINGSTILFELLE_INTERVAL_MINUTES),
            retriedCount = newRetryCounter
        )
        producer.send(producerRecord(callId, newKOppfolgingstilfelleRetry))
        log.warn(
            "Sent KOppfolgingstilfelleRetry retried: {}, {}, {}",
            StructuredArguments.keyValue("retriedCount", newKOppfolgingstilfelleRetry.retriedCount)!!,
            StructuredArguments.keyValue("retryTime", newKOppfolgingstilfelleRetry.retryTime)!!,
            callIdArgument(callId)
        )
        COUNT_OPPFOLGINGSTILFELLE_RETRY_NEW.inc()
    }

    fun sendAgainOppfolgingstilfelleRetry(
        kOppfolgingstilfelleRetry: KOppfolgingstilfelleRetry,
        callId: String
    ) {
        producer.send(producerRecord(callId, kOppfolgingstilfelleRetry))
        log.info(
            "Sent KOppfolgingstilfelleRetry again: {}, {}, {}",
            StructuredArguments.keyValue("retriedCount", kOppfolgingstilfelleRetry.retriedCount)!!,
            StructuredArguments.keyValue("retryTime", kOppfolgingstilfelleRetry.retryTime)!!,
            callIdArgument(callId)
        )
        COUNT_OPPFOLGINGSTILFELLE_RETRY_AGAIN.inc()
    }

    private fun producerRecord(
        callId: String,
        oppfolgingstilfelleRetry: KOppfolgingstilfelleRetry
    ) = SyfoProducerRecord(
        topic = OPPFOLGINGSTILFELLE_RETRY_TOPIC,
        key = UUID.nameUUIDFromBytes(oppfolgingstilfelleRetry.orgnummer.toByteArray()).toString(),
        value = oppfolgingstilfelleRetry,
        headers = mapOf(NAV_CALL_ID to callId)
    )

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OppfolgingstilfelleRetryProducer::class.java)
    }
}
