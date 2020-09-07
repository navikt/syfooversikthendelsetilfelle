# syfotilfelleoversikthendelse
Receives Oppfolgingstilfeller as Kafka-events from Syfosyketilfelle
and generate a Oversikthendelse for each change in a persons Oppfolgingstilfelle.
Each Oversikthendelse it published to a Kafka-topic that is consumed by Syfooversiktsrv.

## Technologies used
* Kafka
* Kotlin
* Ktor
* Gradle
* Mockk
* Spek
* Vault

#### Build
Run `./gradlew clean shadowJar`

#### Lint
Run `./gradlew --continue ktlintCheck`

#### Test
Run `./gradlew test -i`
