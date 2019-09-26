package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.kafka.KafkaConfig
import no.nav.syfo.kafka.KafkaCredentials
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
                getEnvVar("AADDISCOVERY_URL"),
                getEnvVar("JWKKEYS_URL", "https://login.microsoftonline.com/common/discovery/keys"),
                getEnvVar("JWT_ISSUER"),
                getEnvVar("OPPFOLGINGSTILFELLE_TOPIC"),
                getEnvVar("OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC", "aapen-syfo-oversikthendelse-tilfelle-v1"),
                getEnvVar("OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC_SEEK_TO_START", "false").toBoolean(),
                getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
                getEnvVar("CLIENT_ID"),
                getEnvVar("TOOGLE_OVERSIKTHENDELSETILFELLE", "false").toBoolean(),
                getEnvVar("AKTORREGISTER_V1_URL"),
                getEnvVar("SECURITY_TOKEN_SERVICE_REST_URL"),
                getEnvVar("EREG_API_BASE_URL", "https://modapp-q1.adeo.no/")
        )
    }
}

val appIsRunningLocally: Boolean = System.getenv("NAIS_CLUSTER_NAME").isNullOrEmpty()

data class Environment(
        val applicationPort: Int,
        val applicationThreads: Int,
        val applicationName: String,
        val aadDiscoveryUrl: String,
        val jwkKeysUrl: String,
        val jwtIssuer: String,
        val oppfolgingstilfelleTopic: String,
        val oversikthendelseOppfolgingstilfelleTopic: String,
        val oversikthendelseOppfolgingstilfelleTopicSeekToBeginning: Boolean,
        override val kafkaBootstrapServers: String,
        val clientid: String,
        val toggleOversikthendelsetilfelle: Boolean,
        val aktoerregisterV1Url: String,
        val stsRestUrl: String,
        val eregApiBaseUrl: String
) : KafkaConfig

data class VaultSecrets(
        val serviceuserUsername: String,
        val serviceuserPassword: String
) : KafkaCredentials {
    override val kafkaUsername: String = serviceuserUsername
    override val kafkaPassword: String = serviceuserPassword
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private fun firstExistingFile(vararg paths: String) = paths
        .map(::File)
        .first(File::exists)
