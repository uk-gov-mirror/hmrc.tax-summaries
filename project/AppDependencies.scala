import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    filters,
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-26" % "2.23.0",
    "uk.gov.hmrc"       %% "domain"                    % "5.9.0-play-26",
    "uk.gov.hmrc"       %% "time"                      % "3.9.0",
    "com.github.fge"    % "json-schema-validator"      % "2.2.6",
    "uk.gov.hmrc"       %% "auth-client"               % "3.0.0-play-26",
    "org.reactivemongo" %% "play2-reactivemongo"       % "0.18.8-play26"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "hmrctest"                  % "3.9.0-play-26",
    "com.typesafe.play"      %% "play-test"                 % PlayVersion.current,
    "org.scalatestplus.play" %% "scalatestplus-play"        % "3.1.0",
    "org.jsoup"              % "jsoup"                      % "1.13.1",
    "com.github.fge"         % "json-schema-validator"      % "2.2.6",
    "org.mockito"            % "mockito-all"                % "1.10.19",
    "org.scalacheck"         %% "scalacheck"                % "1.14.3",
    "com.github.tomakehurst" % "wiremock-jre8"              % "2.26.1"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
