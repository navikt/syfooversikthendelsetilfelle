package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.pdl.*
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.domain.AktorId
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.metric.*
import no.nav.syfo.oppfolgingstilfelle.domain.*
import no.nav.syfo.oppfolgingstilfelle.retry.OppfolgingstilfelleRetryProducer
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

enum class MissingValue {
    FODSELSNUMMER
}

const val OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC = "aapen-syfo-oversikthendelse-tilfelle-v1"

class OppfolgingstilfelleService(
    private val aktorService: AktorService,
    private val pdlClient: PdlClient,
    private val syketilfelleClient: SyketilfelleClient,
    private val oppfolgingstilfelleRetryProducer: OppfolgingstilfelleRetryProducer,
    private val producer: KafkaProducer<String, KOversikthendelsetilfelle>
) {
    suspend fun receiveOppfolgingstilfelle(
        oppfolgingstilfelleRecordTimestamp: LocalDateTime,
        aktorId: AktorId,
        orgnummer: Virksomhetsnummer,
        oppfolgingstilfelleId: String,
        callId: String
    ) {
        val isSuccessful = processOppfolgingstilfelle(
            oppfolgingstilfelleRecordTimestamp,
            aktorId,
            orgnummer,
            callId
        )
        if (isSuccessful) {
            LOG.info("Sent Oversikthendelsetilfelle on first attempt for tilfelle with id: $oppfolgingstilfelleId")
        } else {
            oppfolgingstilfelleRetryProducer.sendFirstOppfolgingstilfelleRetry(
                oppfolgingstilfelleRecordTimestamp,
                aktorId,
                orgnummer,
                callId
            )
        }
    }

    suspend fun processOppfolgingstilfelle(
        oppfolgingstilfelleRecordTimestamp: LocalDateTime,
        aktorId: AktorId,
        orgnummer: Virksomhetsnummer,
        callId: String
    ): Boolean {
        val fnr: String = aktorService.fodselsnummerForAktor(aktorId, callId)
            ?: return retryOppfolgingstilfelleWithMissingValue(MissingValue.FODSELSNUMMER)

        val person = pdlClient.person(fnr, callId)

        if (person.isKode6()) {
            return true
        }

        val oppfolgingstilfelle = syketilfelleClient.getOppfolgingstilfelle(
            aktorId.aktor,
            orgnummer.value,
            callId
        )

        if (oppfolgingstilfelle != null) {
            val isGradertToday: Boolean = oppfolgingstilfelle.tidslinje.isLatestSykmeldingGradert()

            if (isGradertToday) {
                COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED.inc()
            } else {
                COUNT_OPPFOLGINGSTILFELLE_RECEIVED.inc()
            }
            val fnrFullName = person?.fullName() ?: ""

            val hendelse = mapKOversikthendelsetilfelle(
                fnr,
                fnrFullName,
                oppfolgingstilfelle.orgnummer,
                oppfolgingstilfelle.tidslinje.sortedBy { it.dag },
                oppfolgingstilfelleRecordTimestamp,
                isGradertToday
            )
            val recordKey = UUID.nameUUIDFromBytes(oppfolgingstilfelle.orgnummer.toByteArray())
            producer.send(producerRecord(recordKey, hendelse))
            COUNT_OVERSIKTHENDELSE_TILFELLE_PRODUCED.inc()
            return true
        } else {
            return true
        }
    }

    private fun retryOppfolgingstilfelleWithMissingValue(
        missingValue: MissingValue
    ): Boolean {
        when (missingValue) {
            MissingValue.FODSELSNUMMER -> COUNT_OPPFOLGINGSTILFELLE_SKIPPED_FODSELSNUMMER.inc()
        }
        return false
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SyketilfelleClient::class.java)
    }
}

private fun producerRecord(
    key: UUID,
    hendelse: KOversikthendelsetilfelle
) =
    SyfoProducerRecord(
        topic = OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC,
        key = key.toString(),
        value = hendelse
    )
