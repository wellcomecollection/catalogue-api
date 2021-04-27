import sbt._

object WellcomeDependencies {

  val defaultVersion = "26.7.0"

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

  private def library(name: String, version: String): Seq[ModuleID] = Seq(
    "uk.ac.wellcome" %% name % version,
    "uk.ac.wellcome" %% name % version % "test" classifier "tests"
  )
}

object ExternalDependencies {
  lazy val versions = new {
    val akkaStreamAlpakka = "1.1.2"
    val apacheCommons = "1.9"
    val circe = "0.13.0"
    val fastparse = "2.3.0"
    val mockito = "1.9.5"
    val scalatest = "3.2.3"
    val scalatestplus = "3.1.2.0"
    val scalatestplusMockitoArtifactId = "mockito-3-2"
    val scalacheckShapeless = "1.1.6"
    val scalacsv = "1.3.5"
    val scalaGraph = "1.12.5"
    val apm = "1.22.0"
    val enumeratum = "1.6.1"
    val enumeratumScalacheck = "1.6.1"
    val jsoup = "1.13.1"
    val scalaJHttp = "2.3.0"
  }
  val enumeratumDependencies = Seq(
    "com.beachape" %% "enumeratum" % versions.enumeratum,
    "com.beachape" %% "enumeratum-scalacheck" % versions.enumeratumScalacheck % "test"
  )

  val scribeJavaDependencies = Seq(
    "com.github.dakatsuka" %% "akka-http-oauth2-client" % "0.2.0")

  val apmDependencies = Seq(
    "co.elastic.apm" % "apm-agent-attach" % versions.apm,
    "co.elastic.apm" % "apm-agent-api" % versions.apm
  )

  val alpakkaS3Dependencies = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % versions.akkaStreamAlpakka
  )

  val apacheCommonsDependencies = Seq(
    "org.apache.commons" % "commons-text" % versions.apacheCommons
  )

  val circeOpticsDependencies = Seq(
    "io.circe" %% "circe-optics" % versions.circe
  )

  val mockitoDependencies: Seq[ModuleID] = Seq(
    "org.mockito" % "mockito-core" % versions.mockito % "test",
    "org.scalatestplus" %% versions.scalatestplusMockitoArtifactId % versions.scalatestplus % "test")

  val wireMockDependencies = Seq(
    "com.github.tomakehurst" % "wiremock" % "2.25.1" % Test
  )

  val scalacheckDependencies = Seq(
    "org.scalatestplus" %% "scalacheck-1-14" % versions.scalatestplus % "test",
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % versions.scalacheckShapeless % "test"
  )

  val scalacsvDependencies = Seq(
    "com.github.tototoshi" %% "scala-csv" % versions.scalacsv
  )

  val scalaGraphDependencies = Seq(
    "org.scala-graph" %% "graph-core" % versions.scalaGraph
  )

  val scalatestDependencies = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  )

  val parseDependencies = Seq(
    "com.lihaoyi" %% "fastparse" % versions.fastparse
  )

  val javaxDependencies = Seq(
    "javax.xml.bind" % "jaxb-api" % "2.3.0",
    "com.sun.xml.bind" % "jaxb-ri" % "2.3.0"
  )

  val scalaXmlDependencies = Seq(
    "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
  )

  val jsoupDependencies = Seq(
    "org.jsoup" % "jsoup" % versions.jsoup
  )

  val scalaJDependencies = Seq(
    "org.scalaj" %% "scalaj-http" % versions.scalaJHttp
  )
}

object CatalogueDependencies {

  val displayModelDependencies =
    WellcomeDependencies.internalModel ++
      WellcomeDependencies.elasticsearchLibrary ++
      WellcomeDependencies.elasticsearchTypesafeLibrary ++
      WellcomeDependencies.fixturesLibrary ++
      WellcomeDependencies.jsonLibrary ++
      ExternalDependencies.scalacheckDependencies ++
      WellcomeDependencies.httpLibrary

  val elasticsearchTypesafeDependencies: Seq[ModuleID] =
    WellcomeDependencies.typesafeLibrary

  val searchDependencies: Seq[ModuleID] =
    ExternalDependencies.apmDependencies ++
      ExternalDependencies.circeOpticsDependencies ++
      WellcomeDependencies.elasticsearchTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary

  val stacksDependencies: Seq[ModuleID] =
    ExternalDependencies.scalatestDependencies ++
      ExternalDependencies.wireMockDependencies ++
      WellcomeDependencies.jsonLibrary ++
      WellcomeDependencies.monitoringTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      WellcomeDependencies.monitoringLibrary

  val snapshotGeneratorDependencies: Seq[ModuleID] =
    WellcomeDependencies.messagingTypesafeLibrary ++
      WellcomeDependencies.storageLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      ExternalDependencies.alpakkaS3Dependencies
}
