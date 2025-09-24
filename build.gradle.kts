allprojects {
  group = "com.tuempresa"
  version = "1.0.0"

  repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
  }
}

subprojects {
  plugins.apply("java")
  extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
  }
  tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
}
