import sbt._

object MicroServiceBuild extends Build with MicroService {
  val appName = "tax-summaries"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val microserviceBootstrapVersion = "5.15.0"
  private val playAuthVersion = "4.3.0"
  private val playHealthVersion = "2.1.0"
  private val playUrlBindersVersion = "2.1.0"
  private val playConfigVersion = "4.2.0"
  private val logbackJsonLoggerVersion = "3.1.0"
  private val domainVersion = "4.1.0"
  private val playGraphiteVersion = "3.2.0"
  private val json4sJacksonVersion = "3.2.10"
  private val jsonSchemaValidatorVersion = "2.2.6"
  private val json4sNativeVersion = "3.2.10"
  private val jsonEncryptionVersion = "3.2.0"
  private val playUIVersion = "7.2.1"

  private val hmrcTestVersion = "2.3.0"
  private val scalatestVersion = "2.2.6"
  private val pegdownVersion = "1.6.0"
  private val scalaTestplusPlayVersion = "1.5.1"
  private val mockitoAllVersion = "1.9.5"

  val compile = Seq(
    filters,
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "uk.gov.hmrc" %% "play-authorisation" % playAuthVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUIVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "play-graphite" % playGraphiteVersion,
    "org.json4s" %% "json4s-jackson" % json4sJacksonVersion,
    "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion,
    "org.json4s" %% "json4s-native" % json4sNativeVersion,
    "com.codahale.metrics" % "metrics-graphite" % "3.0.2",
    "uk.gov.hmrc" %% "time" % "1.1.0",
    "uk.gov.hmrc" %% "json-encryption" % jsonEncryptionVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestplusPlayVersion % scope,
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
        "org.json4s" %% "json4s-jackson" % json4sJacksonVersion,
        "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion,
        "org.json4s" %% "json4s-native" % json4sNativeVersion,
        "org.mockito" % "mockito-all" % mockitoAllVersion
      )
    }.test
  }

  def apply() = compile ++ Test()

}
