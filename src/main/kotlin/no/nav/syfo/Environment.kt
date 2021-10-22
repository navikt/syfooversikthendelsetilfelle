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
            applicationPort = getEnvVar("APPLICATION_PORT", "8080").toInt(),
            applicationThreads = getEnvVar("APPLICATION_THREADS", "1").toInt(),
            applicationName = getEnvVar("APPLICATION_NAME", "syfooversikthendelsetilfelle"),
            oppfolgingstilfelleTopic = getEnvVar("OPPFOLGINGSTILFELLE_TOPIC"),
            oversikthendelseOppfolgingstilfelleTopicSeekToBeginning = getEnvVar("OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC_SEEK_TO_START", "false").toBoolean(),
            kafkaBootstrapServers = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
            pdlUrl = getEnvVar("PDL_URL"),
            syfobehandlendeenhetClientId = getEnvVar("SYFOBEHANDLENDEENHET_CLIENT_ID"),
            behandlendeenhetUrl = getEnvVar("SYFOBEHANDLENDEENHET_URL"),
            syketilfelleUrl = getEnvVar("SYFOSYKETILFELLE_URL"),
            aktoerregisterV1Url = getEnvVar("AKTORREGISTER_V1_URL"),
            stsRestUrl = getEnvVar("SECURITY_TOKEN_SERVICE_REST_URL"),
            eregApiBaseUrl = getEnvVar("EREG_API_BASE_URL")
        )
    }
}

val appIsRunningLocally: Boolean = System.getenv("NAIS_CLUSTER_NAME").isNullOrEmpty()

data class Environment(
    val applicationPort: Int,
    val applicationThreads: Int,
    val applicationName: String,

    val azureAppClientId: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppClientSecret: String = getEnvVar("AZURE_APP_CLIENT_SECRET"),
    val azureOpenidConfigTokenEndpoint: String = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),

    val oppfolgingstilfelleTopic: String,
    val oversikthendelseOppfolgingstilfelleTopicSeekToBeginning: Boolean,
    val kafkaBootstrapServers: String,
    val pdlUrl: String,
    val syfobehandlendeenhetClientId: String,
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
