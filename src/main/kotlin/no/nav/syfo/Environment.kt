package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

const val localEnvironmentPropertiesPath = "./src/main/resources/localEnv.json"
const val defaultlocalEnvironmentPropertiesPath = "./src/main/resources/localEnvForTests.json"
private val objectMapper: ObjectMapper = ObjectMapper()

fun getEnvironment(): Environment {
    objectMapper.registerKotlinModule()
    return if (appIsRunningLocally) {
        objectMapper.readValue(firstExistingFile(localEnvironmentPropertiesPath, defaultlocalEnvironmentPropertiesPath), Environment::class.java)
    } else {
        Environment(
            getEnvVar("APPLICATION_PORT", "8080").toInt(),
            getEnvVar("APPLICATION_THREADS", "1").toInt(),
            getEnvVar("APPLICATION_NAME", "syfooversikthendelsetilfelle"),
            getEnvVar("OPPFOLGINGSTILFELLE_TOPIC"),
            getEnvVar("OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC_SEEK_TO_START", "false").toBoolean(),
            getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
            getEnvVar("PDL_URL", "http://pdl-api.default/graphql"),
            getEnvVar("SYFOBEHANDLENDEENHET_URL", "http://syfobehandlendeenhet"),
            getEnvVar("SYFOSYKETILFELLE_URL", "http://syfosyketilfelle"),
            getEnvVar("AKTORREGISTER_V1_URL"),
            getEnvVar("SECURITY_TOKEN_SERVICE_REST_URL"),
            getEnvVar("EREG_API_BASE_URL", "https://ereg/")
        )
    }
}

val appIsRunningLocally: Boolean = System.getenv("NAIS_CLUSTER_NAME").isNullOrEmpty()

data class Environment(
    val applicationPort: Int,
    val applicationThreads: Int,
    val applicationName: String,
    val oppfolgingstilfelleTopic: String,
    val oversikthendelseOppfolgingstilfelleTopicSeekToBeginning: Boolean,
    val kafkaBootstrapServers: String,
    val pdlUrl: String,
    val behandlendeenhetUrl: String,
    val syketilfelleUrl: String,
    val aktoerregisterV1Url: String,
    val stsRestUrl: String,
    val eregApiBaseUrl: String
)

data class VaultSecrets(
    val serviceuserUsername: String,
    val serviceuserPassword: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private fun firstExistingFile(vararg paths: String) = paths
    .map(::File)
    .first(File::exists)
