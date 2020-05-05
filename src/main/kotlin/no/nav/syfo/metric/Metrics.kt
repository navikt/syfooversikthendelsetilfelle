package no.nav.syfo.metric

import io.prometheus.client.Counter

const val METRICS_NS = "syfooversikthendelsetilfelle"

const val OPPFOLGINGSTILFELLE_EMPTY = "oppfolgingstilfelle_empty_count"
val COUNT_OPPFOLGINGSTILFELLE_EMPTY: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OPPFOLGINGSTILFELLE_EMPTY)
        .help("Counts the number of responses from syfosyketillfelle with status 204 received")
        .register()

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

const val CALL_BEHANDLENDEENHET = "call_behandlendeenhet_count"
const val CALL_BEHANDLENDEENHET_SUCCESS = "call_behandlendeenhet_success_count"
const val CALL_BEHANDLENDEENHET_FAIL = "call_behandlendeenhet_fail_count"
const val CALL_BEHANDLENDEENHET_EMPTY = "call_behandlendeenhet_empty_count"
val COUNT_CALL_BEHANDLENDEENHET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_BEHANDLENDEENHET)
        .help("Counts the number of calls to syfobehandlendeenhet")
        .register()
val COUNT_CALL_BEHANDLENDEENHET_SUCCESS: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_BEHANDLENDEENHET_SUCCESS)
        .help("Counts the number of successful calls to syfobehandlendeenhet")
        .register()
val COUNT_CALL_BEHANDLENDEENHET_FAIL: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_BEHANDLENDEENHET_FAIL)
        .help("Counts the number of failed calls to syfobehandlendeenhet")
        .register()
val COUNT_CALL_BEHANDLENDEENHET_EMPTY: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_BEHANDLENDEENHET_EMPTY)
        .help("Counts the number of responses from syfobehandlendeenhet with status 204 received")
        .register()


const val CALL_EREG_SUCCESS = "call_ereg_success_count"
const val CALL_EREG_FAIL = "call_ereg_fail_count"
val COUNT_CALL_EREG_SUCCESS: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_EREG_SUCCESS)
        .help("Counts the number of successful calls to EREG")
        .register()
val COUNT_CALL_EREG_FAIL: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_EREG_FAIL)
        .help("Counts the number of failed calls to EREG")
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

const val OPPFOLGINGSTILFELLE_SKIPPED_BEHANDLENDEENHET = "oppfolgingstilfelle_skipped_behandlendeenhet_count"
val COUNT_OPPFOLGINGSTILFELLE_SKIPPED_BEHANDLENDEENHET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OPPFOLGINGSTILFELLE_SKIPPED_BEHANDLENDEENHET)
        .help("Counts the number of Oppfolgingstilfeller skipped because BehandlendeEnhet was not found")
        .register()
const val OPPFOLGINGSTILFELLE_SKIPPED_FODSELSNUMMER = "oppfolgingstilfelle_skipped_fodselsnummer_count"
val COUNT_OPPFOLGINGSTILFELLE_SKIPPED_FODSELSNUMMER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OPPFOLGINGSTILFELLE_SKIPPED_FODSELSNUMMER)
        .help("Counts the number of Oppfolgingstilfeller skipped because Fodselsnummer was not found")
        .register()

const val OVERSIKTHENDELSE_TILFELLE_PRODUCED = "oversikthendelse_tilfelle_produced_count"
val COUNT_OVERSIKTHENDELSE_TILFELLE_PRODUCED: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_TILFELLE_PRODUCED)
        .help("Counts the number of oversikthendelse-tilfeller produced")
        .register()
