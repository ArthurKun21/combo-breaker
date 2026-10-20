package dev.romainguy.combobreaker.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Access to the shared `libs` version catalog from convention plugins, where the generated
 * type-safe accessors are not available.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Returns the plugin id declared in the version catalog under [name], so that convention
 * plugins never hard-code plugin ids that are versioned in `gradle/libs.versions.toml`.
 */
internal fun VersionCatalog.pluginId(name: String): String =
    findPlugin(name).get().get().pluginId
