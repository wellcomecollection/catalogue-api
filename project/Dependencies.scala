import sbt._

object WellcomeDependencies {

  val defaultVersion = "30.6.2" // This is automatically bumped by the scala-libs release process, do not edit this line manually

  lazy val versions = new {
    val typesafe = defaultVersion
    val fixtures = defaultVersion
    val http = defaultVersion
    val json = defaultVersion
    val messaging = defaultVersion
    val monitoring = defaultVersion
    val storage = defaultVersion
    val elasticsearch = defaultVersion
    val internalModel = "5298.d1abe8731494bff7c1ca58597bbf2fdb4ca59327"
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

  val sierraLibrary: Seq[ModuleID] = library(
    name = "sierra",
    version = versions.http
  )

  val sierraTypesafeLibrary: Seq[ModuleID] = sierraLibrary ++ library(
    name = "sierra_typesafe",
    version = versions.http
  )

  private def library(name: String, version: String): Seq[ModuleID] = Seq(
    "weco" %% name % version,
    "weco" %% name % version % "test" classifier "tests"
  )
}

object ExternalDependencies {
  lazy val versions = new {
    val circe = "0.13.0"
    val scalatest = "3.2.3"
    val scalatestplus = "3.1.2.0"
    val scalacheckShapeless = "1.1.6"

    // This should match the version used in scala-libs
    // See https://github.com/wellcomecollection/scala-libs/blob/main/project/Dependencies.scala
    val akka = "2.6.14"
    val akkaHttp = "10.1.11"
    val aws2 = "2.11.14"
  }

  val circeOpticsDependencies = Seq(
    "io.circe" %% "circe-optics" % versions.circe
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

  val secretsDependencies = Seq(
    "software.amazon.awssdk" % "secretsmanager" % versions.aws2
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
    ExternalDependencies.circeOpticsDependencies ++
      WellcomeDependencies.elasticsearchTypesafeLibrary ++
      WellcomeDependencies.monitoringTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      WellcomeDependencies.monitoringLibrary ++
      WellcomeDependencies.httpTypesafeLibrary ++
      ExternalDependencies.akkaHttpDependencies

  val stacksDependencies: Seq[ModuleID] =
    ExternalDependencies.scalatestDependencies ++
      WellcomeDependencies.jsonLibrary ++
      WellcomeDependencies.monitoringTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      WellcomeDependencies.monitoringLibrary ++
      WellcomeDependencies.httpTypesafeLibrary ++
      WellcomeDependencies.sierraLibrary

  val snapshotGeneratorDependencies: Seq[ModuleID] =
    WellcomeDependencies.messagingTypesafeLibrary ++
      WellcomeDependencies.storageTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      ExternalDependencies.secretsDependencies

  val itemsDependencies: Seq[ModuleID] =
    WellcomeDependencies.sierraTypesafeLibrary

  val requestsDependencies: Seq[ModuleID] =
    WellcomeDependencies.sierraTypesafeLibrary
}
