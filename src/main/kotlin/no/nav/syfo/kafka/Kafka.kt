package no.nav.syfo.kafka

import kotlinx.coroutines.CoroutineScope
import no.nav.syfo.*
import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.enhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregService
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.syketilfelle.SyketilfelleClient
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.domain.KOversikthendelsetilfelle
import org.apache.kafka.clients.producer.KafkaProducer

suspend fun CoroutineScope.setupKafka(
    vaultSecrets: VaultSecrets,
    aktorService: AktorService,
    eregService: EregService,
    behandlendeEnhetClient: BehandlendeEnhetClient,
    pdlClient: PdlClient,
    syketilfelleClient: SyketilfelleClient
) {
    val producerProperties = kafkaOversikthendelsetilfelleProducerProperties(env, vaultSecrets)
    val oversikthendelseTilfelleProducer = KafkaProducer<String, KOversikthendelsetilfelle>(producerProperties)

    val oppfolgingstilfelleService = OppfolgingstilfelleService(
        aktorService,
        eregService,
        behandlendeEnhetClient,
        pdlClient,
        syketilfelleClient,
        oversikthendelseTilfelleProducer
    )

    createListenerOppfolgingstilfelle(state) {
        blockingApplicationLogicOppfolgingstilfelle(
            state,
            env,
            vaultSecrets,
            oppfolgingstilfelleService
        )
    }

    state.initialized = true
}
