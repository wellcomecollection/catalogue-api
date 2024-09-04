import sbt._

object WellcomeDependencies {

  val defaultVersion = "32.42.0" // This is automatically bumped by the scala-libs release process, do not edit this line manually

  lazy val versions = new {
    val typesafe = defaultVersion
    val fixtures = defaultVersion
    val http = defaultVersion
    val json = defaultVersion
    val messaging = defaultVersion
    val monitoring = defaultVersion
    val storage = defaultVersion
    val elasticsearch = defaultVersion
  }

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
    val circeOptics = "0.14.1"
    val scalatest = "3.2.3"
    val scalatestplus = "3.1.2.0"
    val scalacheckShapeless = "1.1.8"
    val scalacsv = "1.3.10"

    // This should match the version used in scala-libs
    // See https://github.com/wellcomecollection/scala-libs/blob/main/project/Dependencies.scala
    val pekko = "1.0.3"
    val pekkoHttp = "1.0.1"
    val aws2 = "2.25.28"
  }

  val circeOpticsDependencies = Seq(
    "io.circe" %% "circe-optics" % versions.circeOptics
  )

  val scalacheckDependencies = Seq(
    "org.scalatestplus" %% "scalacheck-1-14" % versions.scalatestplus % "test",
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % versions.scalacheckShapeless % "test"
  )

  val scalatestDependencies = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  )

  val pekkoHttpDependencies = Seq(
    "org.apache.pekko" %% "pekko-testkit" % versions.pekko % "test",
    "org.apache.pekko" %% "pekko-http-testkit" % versions.pekkoHttp % "test"
  )

  val secretsDependencies = Seq(
    "software.amazon.awssdk" % "secretsmanager" % versions.aws2,
    "software.amazon.awssdk" % "sts" % versions.aws2
  )

  val otherAwsDependencies = Seq(
    "software.amazon.awssdk" % "sso" % versions.aws2
  )

  val scalacsvDependencies = Seq(
    "com.github.tototoshi" %% "scala-csv" % versions.scalacsv
  )
}

object CatalogueDependencies {
  val displayModelDependencies: Seq[ModuleID] =
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
      ExternalDependencies.pekkoHttpDependencies ++
      ExternalDependencies.scalacsvDependencies ++
      ExternalDependencies.secretsDependencies ++
      ExternalDependencies.otherAwsDependencies

  val searchDependencies: Seq[ModuleID] =
    ExternalDependencies.circeOpticsDependencies

  val stacksDependencies: Seq[ModuleID] =
    ExternalDependencies.scalatestDependencies ++
      WellcomeDependencies.sierraLibrary

  val snapshotGeneratorDependencies: Seq[ModuleID] =
    WellcomeDependencies.messagingTypesafeLibrary ++
      WellcomeDependencies.elasticsearchLibrary ++
      WellcomeDependencies.elasticsearchTypesafeLibrary ++
      WellcomeDependencies.storageLibrary ++
      WellcomeDependencies.typesafeLibrary

  val itemsDependencies: Seq[ModuleID] =
    WellcomeDependencies.sierraTypesafeLibrary ++
      ExternalDependencies.secretsDependencies

  val requestsDependencies: Seq[ModuleID] =
    WellcomeDependencies.sierraTypesafeLibrary
}
