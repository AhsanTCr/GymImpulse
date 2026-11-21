import java.util.Base64

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

// Ensure debug.keystore exists on CI runners (where debug.keystore is gitignored)
// without committing keystores or modifying app/build.gradle.kts signingConfigs.
run {
  val debugKeystore = layout.projectDirectory.file("debug.keystore").asFile
  val debugKeystoreBase64 = layout.projectDirectory.file("debug.keystore.base64").asFile
  if (!debugKeystore.exists()) {
    if (debugKeystoreBase64.exists()) {
      val cleanBase64 = debugKeystoreBase64.readText().replace("\\s+".toRegex(), "")
      debugKeystore.writeBytes(Base64.getDecoder().decode(cleanBase64))
    } else {
      ProcessBuilder(
        "keytool", "-genkeypair", "-noprompt",
        "-keystore", debugKeystore.absolutePath,
        "-storepass", "android",
        "-alias", "androiddebugkey",
        "-keypass", "android",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", "CN=Android Debug,O=Android,C=US"
      ).inheritIO().start().waitFor()
    }
  }
}
