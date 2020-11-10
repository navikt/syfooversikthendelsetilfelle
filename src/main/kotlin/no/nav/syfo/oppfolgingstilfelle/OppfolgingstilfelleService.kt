package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.enhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.pdl.fullName
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.domain.AktorId
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.metric.*
import no.nav.syfo.oppfolgingstilfelle.domain.*
import no.nav.syfo.oppfolgingstilfelle.retry.OppfolgingstilfelleRetryProducer
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory
import java.util.UUID.randomUUID

enum class MissingValue {
    BEHANDLENDEENHET,
    FODSELSNUMMER
}

const val OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC = "aapen-syfo-oversikthendelse-tilfelle-v1"

class OppfolgingstilfelleService(
    private val aktorService: AktorService,
    private val eregService: EregService,
    private val behandlendeEnhetClient: BehandlendeEnhetClient,
    private val pdlClient: PdlClient,
    private val syketilfelleClient: SyketilfelleClient,
    private val oppfolgingstilfelleRetryProducer: OppfolgingstilfelleRetryProducer,
    private val producer: KafkaProducer<String, KOversikthendelsetilfelle>
) {
    suspend fun receiveOppfolgingstilfelle(
        aktorId: AktorId,
        orgnummer: Virksomhetsnummer,
        callId: String
    ) {
        val isSuccessful = processOppfolgingstilfelle(
            aktorId,
            orgnummer,
            callId
        )
        if (isSuccessful) {
            LOG.info("Sent Oversikthendelsetilfelle on first attempt")
        } else {
            oppfolgingstilfelleRetryProducer.sendFirstOppfolgingstilfelleRetry(
                aktorId,
                orgnummer,
                callId
            )
        }
    }

    suspend fun processOppfolgingstilfelle(
        aktorId: AktorId,
        orgnummer: Virksomhetsnummer,
        callId: String
    ): Boolean {
        val fnr: String = aktorService.fodselsnummerForAktor(aktorId, callId)
            ?: return retryOppfolgingstilfelleWithMissingValue(MissingValue.FODSELSNUMMER)
        val organisasjonNavn = eregService.finnOrganisasjonsNavn(orgnummer.value, callId)

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
            val fnrFullName = pdlClient.person(fnr, callId)?.fullName() ?: ""

            val enhet = behandlendeEnhetClient.getEnhet(fnr, callId)
                ?: return retryOppfolgingstilfelleWithMissingValue(MissingValue.BEHANDLENDEENHET)

            val hendelse = mapKOversikthendelsetilfelle(
                fnr,
                fnrFullName,
                enhet.enhetId,
                oppfolgingstilfelle.orgnummer,
                organisasjonNavn,
                oppfolgingstilfelle.tidslinje.sortedBy { it.dag },
                isGradertToday
            )
            producer.send(producerRecord(hendelse))
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
            MissingValue.BEHANDLENDEENHET -> COUNT_OPPFOLGINGSTILFELLE_SKIPPED_BEHANDLENDEENHET.inc()
            MissingValue.FODSELSNUMMER -> COUNT_OPPFOLGINGSTILFELLE_SKIPPED_FODSELSNUMMER.inc()
        }
        return false
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SyketilfelleClient::class.java)
    }
}

private fun producerRecord(hendelse: KOversikthendelsetilfelle) =
    SyfoProducerRecord(
        topic = OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC,
        key = randomUUID().toString(),
        value = hendelse
    )
