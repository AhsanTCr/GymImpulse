import java.util.Base64
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

abstract class EnsureDebugKeystoreTask : DefaultTask() {
  @get:Inject
  abstract val execOperations: ExecOperations

  @get:InputFile
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val debugKeystoreBase64: RegularFileProperty

  @get:OutputFile
  abstract val debugKeystore: RegularFileProperty

  @TaskAction
  fun ensure() {
    val keystoreFile = debugKeystore.get().asFile
    if (keystoreFile.exists()) return
    keystoreFile.parentFile?.mkdirs()

    val base64File = debugKeystoreBase64.orNull?.asFile
    if (base64File != null && base64File.exists()) {
      val cleanBase64 = base64File.readText().replace("\\s+".toRegex(), "")
      keystoreFile.writeBytes(Base64.getDecoder().decode(cleanBase64))
    } else {
      execOperations.exec {
        commandLine(
          "keytool", "-genkeypair", "-noprompt",
          "-keystore", keystoreFile.absolutePath,
          "-storepass", "android",
          "-alias", "androiddebugkey",
          "-keypass", "android",
          "-keyalg", "RSA",
          "-keysize", "2048",
          "-validity", "10000",
          "-dname", "CN=Android Debug,O=Android,C=US"
        )
      }
    }
  }
}

val ensureDebugKeystore = tasks.register<EnsureDebugKeystoreTask>("ensureDebugKeystore") {
  val base64Candidate = layout.projectDirectory.file("debug.keystore.base64")
  if (base64Candidate.asFile.exists()) {
    debugKeystoreBase64.set(base64Candidate)
  }
  debugKeystore.set(layout.projectDirectory.file("debug.keystore"))
  onlyIf { !debugKeystore.get().asFile.exists() }
}

project(":app") {
  tasks.matching { it.name == "validateSigningDebug" || it.name == "packageDebug" }.configureEach {
    dependsOn(ensureDebugKeystore)
  }
}
