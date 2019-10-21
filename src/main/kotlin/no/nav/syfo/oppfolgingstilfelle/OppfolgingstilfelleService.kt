package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.aktor.domain.AktorId
import no.nav.syfo.client.enhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.env
import no.nav.syfo.log
import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED
import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_RECEIVED
import no.nav.syfo.oppfolgingstilfelle.domain.*
import org.apache.kafka.clients.producer.KafkaProducer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID.randomUUID

const val SYKEPENGESOKNAD = "SYKEPENGESOKNAD"
const val GRADERT_AKTIVITET = "GRADERT_AKTIVITET"
const val SYKMELDING = "SYKMELDING"

class OppfolgingstilfelleService(
        private val aktorService: AktorService,
        private val eregService: EregService,
        private val behandlendeEnhetClient: BehandlendeEnhetClient,
        private val syketilfelleClient: SyketilfelleClient,
        private val producer: KafkaProducer<String, KOversikthendelsetilfelle>
) {
    fun receiveOppfolgingstilfeller(oppfolgingstilfellePeker: KOppfolgingstilfellePeker, callId: String = "") {
        val aktor = AktorId(oppfolgingstilfellePeker.aktorId)

        val fnr: String = aktorService.fodselsnummerForAktor(aktor, callId)
                ?: return hoppOver("fødselsnummer")
        val orgNummer = oppfolgingstilfellePeker.orgnummer
        val organisasjonNavn = eregService.finnOrganisasjonsNavn(orgNummer, callId)

        produce(oppfolgingstilfellePeker, fnr, organisasjonNavn, callId)

    }

    private fun hoppOver(manglendeVerdi: String) {
        log.info("Mottok oppfølgingstilfelle, men sender ikke på kø fordi $manglendeVerdi mangler")
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

        if (oppfolgingstilfelle != null) {
            val isGradertToday: Boolean = isLatestSykmeldingGradert(oppfolgingstilfelle.tidslinje)

            if (isGradertToday) {
                log.info("COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED")
                COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED.inc()
            } else {
                log.info("COUNT_OPPFOLGINGSTILFELLE_RECEIVED")
                COUNT_OPPFOLGINGSTILFELLE_RECEIVED.inc()
            }
            val enhetId = behandlendeEnhetClient.getEnhet(fnr, callId).enhetId

            val hendelse = mapKOversikthendelsetilfelle(
                    fnr,
                    enhetId,
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
        } else {
            log.info("Fant ikke Opppfolgingstilfelle for sykmeldt")
        }
    }
}

fun isLatestSykmeldingGradert(tidslinje: List<KSyketilfelledag>): Boolean {
    val sykeldingerDager = tidslinje
            .filter { it.prioritertSyketilfellebit?.tags?.contains(SYKMELDING) ?: false }

    return if (sykeldingerDager.isNullOrEmpty()) {
        false
    } else {
        sykeldingerDager.minBy { ChronoUnit.DAYS.between(it.dag, LocalDate.now()) }!!
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
