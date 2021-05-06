import sbt._

object WellcomeDependencies {

  val defaultVersion = "26.12.0"

  lazy val versions = new {
    val typesafe = defaultVersion
    val fixtures = defaultVersion
    val http = defaultVersion
    val json = defaultVersion
    val messaging = defaultVersion
    val monitoring = defaultVersion
    val storage = defaultVersion
    val elasticsearch = defaultVersion
    val internalModel = "3814.f87f16965422e31758b8dc271798f79c1caa0b01"
  }

  val internalModel: Seq[ModuleID] = library(
    name = "internal_model",
    version = versions.internalModel
  )
  val jsonLibrary: Seq[ModuleID] = library(
    name = "json",
    version = versions.json
  )

  val fixturesLibrary: Seq[ModuleID] = library(
    name = "fixtures",
    version = versions.fixtures
  )

  val messagingLibrary: Seq[ModuleID] = library(
    name = "messaging",
    version = versions.messaging
  )

  val elasticsearchLibrary: Seq[ModuleID] = library(
    name = "elasticsearch",
    version = versions.elasticsearch
  )

  val elasticsearchTypesafeLibrary: Seq[ModuleID] = library(
    name = "elasticsearch_typesafe",
    version = versions.elasticsearch
  )

  val monitoringLibrary: Seq[ModuleID] = library(
    name = "monitoring",
    version = versions.monitoring
  )

  val monitoringTypesafeLibrary: Seq[ModuleID] = monitoringLibrary ++ library(
    name = "monitoring_typesafe",
    version = versions.monitoring
  )

  val storageLibrary: Seq[ModuleID] = library(
    name = "storage",
    version = versions.storage
  )

  val typesafeLibrary: Seq[ModuleID] = library(
    name = "typesafe_app",
    version = versions.typesafe
  ) ++ fixturesLibrary

  val storageTypesafeLibrary: Seq[ModuleID] = storageLibrary ++ library(
    name = "storage_typesafe",
    version = versions.storage
  )

  val messagingTypesafeLibrary: Seq[ModuleID] = messagingLibrary ++ library(
    name = "messaging_typesafe",
    version = versions.messaging
  ) ++ monitoringLibrary

  val httpLibrary: Seq[ModuleID] = library(
    name = "http",
    version = versions.http
  )

  val httpTypesafeLibrary: Seq[ModuleID] = library(
    name = "http_typesafe",
    version = versions.http
  )

  private def library(name: String, version: String): Seq[ModuleID] = Seq(
    "uk.ac.wellcome" %% name % version,
    "uk.ac.wellcome" %% name % version % "test" classifier "tests"
  )
}

object ExternalDependencies {
  lazy val versions = new {
    val circe = "0.13.0"
    val scalatest = "3.2.3"
    val scalatestplus = "3.1.2.0"
    val scalacheckShapeless = "1.1.6"
    val apm = "1.22.0"

    // This should match the version used in scala-libs
    // See https://github.com/wellcomecollection/scala-libs/blob/main/project/Dependencies.scala
    val akka = "2.6.10"
    val akkaHttp = "10.1.11"
  }

  val apmDependencies = Seq(
    "co.elastic.apm" % "apm-agent-attach" % versions.apm,
    "co.elastic.apm" % "apm-agent-api" % versions.apm
  )

  val circeOpticsDependencies = Seq(
    "io.circe" %% "circe-optics" % versions.circe
  )

  val wireMockDependencies = Seq(
    "com.github.tomakehurst" % "wiremock" % "2.25.1" % Test
  )

  val scalacheckDependencies = Seq(
    "org.scalatestplus" %% "scalacheck-1-14" % versions.scalatestplus % "test",
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % versions.scalacheckShapeless % "test"
  )

  val scalatestDependencies = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  )

  val akkaHttpDependencies = Seq(
    "com.typesafe.akka" %% "akka-testkit" % versions.akka % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % versions.akkaHttp % "test"
  )
}

object CatalogueDependencies {
  val displayModelDependencies: Seq[sbt.ModuleID] =
    WellcomeDependencies.internalModel ++
      WellcomeDependencies.elasticsearchLibrary ++
      WellcomeDependencies.elasticsearchTypesafeLibrary ++
      WellcomeDependencies.fixturesLibrary ++
      WellcomeDependencies.jsonLibrary ++
      ExternalDependencies.scalacheckDependencies ++
      WellcomeDependencies.httpLibrary

  val searchDependencies: Seq[ModuleID] =
    ExternalDependencies.apmDependencies ++
      ExternalDependencies.circeOpticsDependencies ++
      WellcomeDependencies.elasticsearchTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      ExternalDependencies.akkaHttpDependencies

  val stacksDependencies: Seq[ModuleID] =
    ExternalDependencies.scalatestDependencies ++
      ExternalDependencies.wireMockDependencies ++
      WellcomeDependencies.jsonLibrary ++
      WellcomeDependencies.monitoringTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      WellcomeDependencies.monitoringLibrary ++
      WellcomeDependencies.httpTypesafeLibrary

  val snapshotGeneratorDependencies: Seq[ModuleID] =
    WellcomeDependencies.messagingTypesafeLibrary ++
      WellcomeDependencies.storageTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary
}
