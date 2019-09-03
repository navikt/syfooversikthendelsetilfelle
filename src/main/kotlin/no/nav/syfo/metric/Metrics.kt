package no.nav.syfo.metric

import io.prometheus.client.Counter

const val METRICS_NS = "syfooversikthendelsetilfelle"

const val OPPFOLGINGSTILFELLE_RECEIVED = "oppfolgingstilfelle_received_count"
const val OPPFOLGINGSTILFELLE_GRADERT_RECEIVED = "oppfolgingstilfelle_gradert_received_count"

val COUNT_OPPFOLGINGSTILFELLE_RECEIVED: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OPPFOLGINGSTILFELLE_RECEIVED)
        .help("Counts the number of oppfolgingstilfeller received")
        .register()

val COUNT_OPPFOLGINGSTILFELLE_GRADERT_RECEIVED: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OPPFOLGINGSTILFELLE_GRADERT_RECEIVED)
        .help("Counts the number of  graderte oppfolgingstilfeller received")
        .register()

