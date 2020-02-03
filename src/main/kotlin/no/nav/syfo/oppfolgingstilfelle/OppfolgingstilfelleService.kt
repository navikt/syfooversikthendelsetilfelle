package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.aktor.domain.AktorId
import no.nav.syfo.client.enhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.pdl.fullName
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.env
import no.nav.syfo.log
import no.nav.syfo.metric.*
import no.nav.syfo.oppfolgingstilfelle.domain.*
import org.apache.kafka.clients.producer.KafkaProducer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID.randomUUID

enum class MissingValue {
    BEHANDLENDEENHET,
    FODSELSNUMMER
}

const val SYKEPENGESOKNAD = "SYKEPENGESOKNAD"
const val GRADERT_AKTIVITET = "GRADERT_AKTIVITET"
const val SYKMELDING = "SYKMELDING"

class OppfolgingstilfelleService(
        private val aktorService: AktorService,
        private val eregService: EregService,
        private val behandlendeEnhetClient: BehandlendeEnhetClient,
        private val pdlClient: PdlClient,
        private val syketilfelleClient: SyketilfelleClient,
        private val producer: KafkaProducer<String, KOversikthendelsetilfelle>
) {
    fun receiveOppfolgingstilfeller(oppfolgingstilfellePeker: KOppfolgingstilfellePeker, callId: String = "") {
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
        log.info("Mottok oppfølgingstilfelle, men sender ikke på kø fordi $missingValue mangler")
    }

    private fun produce(
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

        if (oppfolgingstilfelle != null && containsSykmeldingAndSykepengesoknad(oppfolgingstilfelle.tidslinje)) {
            val isGradertToday: Boolean = isLatestSykmeldingGradert(oppfolgingstilfelle.tidslinje)

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
                log.info("Legger oversikthendelsetilfelle på kø")
                producer.send(producerRecord(hendelse))
            } else {
                log.info("TOGGLE: Oversikthendelse er togglet av, sender ikke hendelse")
            }
        }
    }
}

fun containsSykmeldingAndSykepengesoknad(tidslinje: List<KSyketilfelledag>): Boolean {
    return containsSykmelding(tidslinje) && containsSykepengesoknad(tidslinje)
}

fun containsSykmelding(tidslinje: List<KSyketilfelledag>): Boolean {
    return tidslinje
            .any { it.prioritertSyketilfellebit?.tags?.contains(SYKMELDING) ?: false }
}

fun containsSykepengesoknad(tidslinje: List<KSyketilfelledag>): Boolean {
    return tidslinje
            .any { it.prioritertSyketilfellebit?.tags?.contains(SYKEPENGESOKNAD) ?: false }
}

fun isLatestSykmeldingGradert(tidslinje: List<KSyketilfelledag>): Boolean {
    val sykmeldingerDager = tidslinje
            .filter { it.prioritertSyketilfellebit?.tags?.contains(SYKMELDING) ?: false }

    return if (sykmeldingerDager.isNullOrEmpty()) {
        false
    } else {
        sykmeldingerDager.minBy { ChronoUnit.DAYS.between(it.dag, LocalDate.now()) }!!
                .prioritertSyketilfellebit!!.tags.contains(GRADERT_AKTIVITET)
                .or(false)
    }
}

private fun producerRecord(hendelse: KOversikthendelsetilfelle) =
        SyfoProducerRecord(
                topic = env.oversikthendelseOppfolgingstilfelleTopic,
                key = randomUUID().toString(),
                value = hendelse
        )
