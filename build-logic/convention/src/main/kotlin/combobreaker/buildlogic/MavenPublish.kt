package combobreaker.buildlogic

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.credentials
import org.gradle.kotlin.dsl.getByType

private const val GitHubActorEnv = "GITHUB_ACTOR"
private const val GitHubTokenEnv = "GITHUB_TOKEN"

internal fun Project.configureMavenPublish() {
    version = providers.environmentVariable("RELEASE_TAG")
        .map { it.removePrefix("v") }
        .orElse(providers.gradleProperty("VERSION_NAME"))
        .get()

    extensions.getByType<PublishingExtension>().repositories.maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/ArthurKun21/combo-breaker")
        credentials(PasswordCredentials::class) {
            username = providers.environmentVariable(GitHubActorEnv).orNull
            password = providers.environmentVariable(GitHubTokenEnv).orNull
        }
    }

    val publishGroup = group.toString()
    val publishArtifact = name
    val publishVersion = version.toString()

    extensions.configure<MavenPublishBaseExtension> {
        coordinates(
            groupId = publishGroup,
            artifactId = publishArtifact,
            version = publishVersion,
        )

        publishToMavenCentral()
        signAllPublications()

        pom {
            url.set("https://github.com/ArthurKun21/combo-breaker")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("ArthurKun21")
                    name.set("Arthur")
                    email.set("16458204+ArthurKun21@users.noreply.github.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/ArthurKun21/combo-breaker.git")
                developerConnection.set("scm:git:ssh://github.com/ArthurKun21/combo-breaker.git")
                url.set("https://github.com/ArthurKun21/combo-breaker")
            }
        }
    }
}
