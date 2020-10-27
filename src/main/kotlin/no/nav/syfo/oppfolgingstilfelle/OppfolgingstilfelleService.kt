package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.enhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.pdl.fullName
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.domain.AktorId
import no.nav.syfo.env
import no.nav.syfo.metric.*
import no.nav.syfo.oppfolgingstilfelle.domain.*
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
    private val producer: KafkaProducer<String, KOversikthendelsetilfelle>
) {
    suspend fun receiveOppfolgingstilfeller(oppfolgingstilfellePeker: KOppfolgingstilfellePeker, callId: String = "") {
        val aktor = AktorId(oppfolgingstilfellePeker.aktorId)

        val fnr: String = aktorService.fodselsnummerForAktor(aktor, callId)
            ?: return skipOppfolgingstilfelleWithMissingValue(MissingValue.FODSELSNUMMER)
        val orgNummer = oppfolgingstilfellePeker.orgnummer
        val organisasjonNavn = eregService.finnOrganisasjonsNavn(orgNummer, callId)

        produce(oppfolgingstilfellePeker, fnr, organisasjonNavn, callId)
    }

    private fun skipOppfolgingstilfelleWithMissingValue(missingValue: MissingValue) {
        when (missingValue) {
            MissingValue.BEHANDLENDEENHET -> COUNT_OPPFOLGINGSTILFELLE_SKIPPED_BEHANDLENDEENHET.inc()
            MissingValue.FODSELSNUMMER -> COUNT_OPPFOLGINGSTILFELLE_SKIPPED_FODSELSNUMMER.inc()
        }
    }

    private suspend fun produce(
        oppfolgingstilfellePeker: KOppfolgingstilfellePeker,
        fnr: String,
        organisasjonNavn: String,
        callId: String
    ) {
        val oppfolgingstilfelle = syketilfelleClient.getOppfolgingstilfelle(
            oppfolgingstilfellePeker.aktorId,
            oppfolgingstilfellePeker.orgnummer,
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
                ?: return skipOppfolgingstilfelleWithMissingValue(MissingValue.BEHANDLENDEENHET)

            val hendelse = mapKOversikthendelsetilfelle(
                fnr,
                fnrFullName,
                enhet.enhetId,
                oppfolgingstilfelle.orgnummer,
                organisasjonNavn,
                oppfolgingstilfelle.tidslinje.sortedBy { it.dag },
                isGradertToday
            )
            if (env.toggleOversikthendelsetilfelle) {
                COUNT_OVERSIKTHENDELSE_TILFELLE_PRODUCED.inc()
                producer.send(producerRecord(hendelse))
            } else {
                LOG.info("TOGGLE: Oversikthendelse er togglet av, sender ikke hendelse")
            }
        }
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
