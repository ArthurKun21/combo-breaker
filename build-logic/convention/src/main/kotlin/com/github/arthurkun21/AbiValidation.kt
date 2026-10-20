package com.github.arthurkun21

import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.work.DisableCachingByDefault

/**
 * Registers binary compatibility validation for an Android library module.
 *
 * AGP 9 compiles Kotlin itself (built-in Kotlin), so neither the Kotlin Gradle plugin's
 * `abiValidation` DSL nor the standalone Binary Compatibility Validator plugin registers any
 * checks for these modules (see Kotlin/binary-compatibility-validator#312 and KT-83410). This
 * wires BCV's own task types to the release variant's compiled classes and exposes the same
 * `checkKotlinAbi` and `updateKotlinAbi` tasks the Kotlin plugin would provide.
 */
internal fun Project.configureAbiValidation() {
    val variant = "release"
    val capitalizedVariant = variant.replaceFirstChar { it.uppercase() }

    val referenceDump = layout.projectDirectory.file("api/$name.api")

    val buildApi = tasks.register<KotlinApiBuildTask>("${variant}ApiBuild") {
        inputClassesDirs.from(
            tasks.named("compile${capitalizedVariant}Kotlin").map { it.outputs.files }
        )
        inputClassesDirs.from(
            tasks.named("compile${capitalizedVariant}JavaWithJavac").map { it.outputs.files }
        )
        outputApiFile.set(layout.buildDirectory.file("abi/$name.api"))
    }

    val checkApi = tasks.register<KotlinApiCompareTask>("checkKotlinAbi") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Checks that the public ABI matches the reference dump in $referenceDump."
        projectApiFile.set(referenceDump)
        generatedApiFile.set(buildApi.flatMap { it.outputApiFile })
    }

    // BCV's comparison failure message tells the user to run an `apiDump` task, so the dump is
    // registered under that name to keep the hint actionable. `updateKotlinAbi` is kept as the
    // project's own name for the same task.
    val dumpApi = tasks.register<AbiDumpTask>("apiDump") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Updates the reference ABI dump in $referenceDump."
        generatedApiFile.set(buildApi.flatMap { it.outputApiFile })
        committedApiFile.set(referenceDump)
    }

    tasks.register("updateKotlinAbi") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Updates the reference ABI dump in $referenceDump."
        dependsOn(dumpApi)
    }

    tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
        dependsOn(checkApi)
    }
}

/** Copies the freshly built ABI dump over the committed reference dump. */
@DisableCachingByDefault(because = "Trivial copy of the generated ABI dump")
public abstract class AbiDumpTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val generatedApiFile: RegularFileProperty

    @get:OutputFile
    public abstract val committedApiFile: RegularFileProperty

    @TaskAction
    public fun dump() {
        val generated = generatedApiFile.get().asFile
        val committed = committedApiFile.get().asFile
        committed.parentFile?.mkdirs()
        generated.copyTo(committed, overwrite = true)
    }
}
