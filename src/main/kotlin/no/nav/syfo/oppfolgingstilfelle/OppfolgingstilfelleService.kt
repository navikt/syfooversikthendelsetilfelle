package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.aktor.domain.AktorId
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.env
import no.nav.syfo.log
import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED
import no.nav.syfo.metric.COUNT_OPPFOLGINGSTILFELLE_RECEIVED
import no.nav.syfo.oppfolgingstilfelle.domain.*
import org.apache.kafka.clients.producer.KafkaProducer
import java.time.LocalDate
import java.util.UUID.randomUUID

const val GRADERT_AKTIVITET = "GRADERT_AKTIVITET"

class OppfolgingstilfelleService(
        private val aktorService: AktorService,
        private val producer: KafkaProducer<String, KOversikthendelsetilfelle>,
        private val eregService: EregService
) {

    fun receiveOppfolgingstilfeller(oppfolgingstilfelle: KOppfolgingstilfelle, callId: String = "") {
        val aktor = AktorId(oppfolgingstilfelle.aktorId)

        val fnr: String = aktorService.fodselsnummerForAktor(aktor, callId) ?: return
        val orgNummer = oppfolgingstilfelle.orgnummer ?: return
        val organisasjonNavn = eregService.finnOrganisasjonsNavn(orgNummer)

        produce(producer, oppfolgingstilfelle, fnr, organisasjonNavn)

    }
}

private fun produce(
        producer: KafkaProducer<String, KOversikthendelsetilfelle>,
        oppfolgingstilfelle: KOppfolgingstilfelle,
        fnr: String,
        organisasjonNavn: String
) {
    if (oppfolgingstilfelle.orgnummer != null) {
        val isGradertToday: Boolean = isGradertToday(oppfolgingstilfelle.tidslinje)

        if (isGradertToday) {
            log.info("COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED")
            COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED.inc()
        } else {
            log.info("COUNT_OPPFOLGINGSTILFELLE_RECEIVED")
            COUNT_OPPFOLGINGSTILFELLE_RECEIVED.inc()
        }
        // TODO: GET enhet from syfobehandlendeenhet
        val enhetId = tilfelleEnhet()

        val hendelse = mapKOversikthendelsetilfelle(
                fnr,
                enhetId,
                oppfolgingstilfelle.orgnummer,
                organisasjonNavn,
                oppfolgingstilfelle.tidslinje.sortedBy { it.dag },
                isGradertToday
        )
        if (env.toggleOversikthendelsetilfelle) {
            log.info("Legger oversikthendelsetilfelle på kø, {}", hendelse)
            producer.send(producerRecord(hendelse))
        } else {
            log.info("TOGGLE: Oversikthendelse er togglet av, sender ikke hendelse")
        }
    } else log.info("Fant ikke virksomhetsnummer for sykmeldt")
}

var isGradertToday = { tidslinje: List<KSyketilfelledag> ->
    tidslinje.any {
        it.dag.isEqual(LocalDate.now()) && it.prioritertSyketilfellebit!!.tags.contains(GRADERT_AKTIVITET)
    }
}

private fun producerRecord(hendelse: KOversikthendelsetilfelle) =
        SyfoProducerRecord(
                topic = env.oversikthendelseOppfolgingstilfelleTopic,
                key = randomUUID().toString(),
                value = hendelse
        )

private fun tilfelleEnhet(): String = "0314"
