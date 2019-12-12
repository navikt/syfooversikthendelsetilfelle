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


const val CALL_PDL = "call_pdl_count"
const val CALL_PDL_SUCCESS = "call_pdl_success_count"
const val CALL_PDL_FAIL = "call_pdl_fail_count"
val COUNT_CALL_PDL: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_PDL)
        .help("Counts the number of calls to persondatalosningen")
        .register()
val COUNT_CALL_PDL_SUCCESS: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_PDL_SUCCESS)
        .help("Counts the number of successful calls to persondatalosningen")
        .register()
val COUNT_CALL_PDL_FAIL: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_PDL_FAIL)
        .help("Counts the number of failed calls to persondatalosningen")
        .register()
