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
    extensions.getByType<PublishingExtension>().repositories.maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/ArthurKun21/combo-breaker")
        credentials(PasswordCredentials::class) {
            username = providers.environmentVariable(GitHubActorEnv).orNull
            password = providers.environmentVariable(GitHubTokenEnv).orNull
        }
    }

    // The publication coordinates are derived from the project properties set by the
    // convention plugin before the publishing plugin was applied: the group and version
    // from the project, and the artifact id from the POM_ARTIFACT_ID property or the
    // project name

    extensions.configure<MavenPublishBaseExtension> {
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
