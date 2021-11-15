import sbt._

object WellcomeDependencies {

  val defaultVersion = "30.11.4" // This is automatically bumped by the scala-libs release process, do not edit this line manually

  lazy val versions = new {
    val typesafe = defaultVersion
    val fixtures = defaultVersion
    val http = defaultVersion
    val json = defaultVersion
    val messaging = defaultVersion
    val monitoring = defaultVersion
    val storage = defaultVersion
    val elasticsearch = defaultVersion
    val internalModel = "6003.3c48f227b111f60fc314d6d9273d6c68f4cef284"
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
    val scalacsv = "1.3.5"

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

  // TODO: Do we need these???
  val akkaHttpDependencies = Seq(
    "com.typesafe.akka" %% "akka-testkit" % versions.akka % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % versions.akkaHttp % "test"
  )

  val secretsDependencies = Seq(
    "software.amazon.awssdk" % "secretsmanager" % versions.aws2
  )

  val scalacsvDependencies = Seq(
    "com.github.tototoshi" %% "scala-csv" % versions.scalacsv
  )
}

object CatalogueDependencies {
  val displayModelDependencies: Seq[ModuleID] =
    WellcomeDependencies.internalModel ++
      WellcomeDependencies.fixturesLibrary ++
      WellcomeDependencies.jsonLibrary ++
      ExternalDependencies.scalacheckDependencies ++
      WellcomeDependencies.httpLibrary

  val searchCommonDependencies: Seq[ModuleID] =
    WellcomeDependencies.elasticsearchLibrary ++
      WellcomeDependencies.elasticsearchTypesafeLibrary ++
      WellcomeDependencies.monitoringTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      WellcomeDependencies.monitoringLibrary ++
      WellcomeDependencies.httpTypesafeLibrary ++
      ExternalDependencies.akkaHttpDependencies ++
      ExternalDependencies.scalacsvDependencies

  val searchDependencies: Seq[ModuleID] =
    ExternalDependencies.circeOpticsDependencies

  val stacksDependencies: Seq[ModuleID] =
    ExternalDependencies.scalatestDependencies ++
      WellcomeDependencies.sierraLibrary

  val snapshotGeneratorDependencies: Seq[ModuleID] =
    WellcomeDependencies.messagingTypesafeLibrary ++
      WellcomeDependencies.elasticsearchLibrary ++
      WellcomeDependencies.elasticsearchTypesafeLibrary ++
      WellcomeDependencies.storageTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      ExternalDependencies.secretsDependencies

  val itemsDependencies: Seq[ModuleID] =
    WellcomeDependencies.sierraTypesafeLibrary

  val requestsDependencies: Seq[ModuleID] =
    WellcomeDependencies.sierraTypesafeLibrary
}
