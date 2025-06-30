import sbt._

object WellcomeDependencies {
  lazy val versions = new {
    val typesafe = "32.43.2"
    val fixtures = "32.43.2"
    val http = "32.43.2"
    val json = "32.43.2"
    val messaging = "32.43.2"
    val monitoring = "32.43.2"
    val storage = "32.43.2"
    val elasticsearch = "32.43.2"
    val sierra = "32.43.2"
  }

  val jsonLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "json" % versions.json,
    "org.wellcomecollection" %% "json" % versions.json % "test" classifier "tests"
  )

  val fixturesLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "fixtures" % versions.fixtures,
    "org.wellcomecollection" %% "fixtures" % versions.fixtures % "test" classifier "tests"
  )

  val messagingLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "messaging" % versions.messaging,
    "org.wellcomecollection" %% "messaging" % versions.messaging % "test" classifier "tests"
  )

  val elasticsearchLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "elasticsearch" % versions.elasticsearch,
    "org.wellcomecollection" %% "elasticsearch" % versions.elasticsearch % "test" classifier "tests"
  )

  val elasticsearchTypesafeLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "elasticsearch_typesafe" % versions.elasticsearch,
    "org.wellcomecollection" %% "elasticsearch_typesafe" % versions.elasticsearch % "test" classifier "tests"
  )

  val monitoringLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "monitoring" % versions.monitoring,
    "org.wellcomecollection" %% "monitoring" % versions.monitoring % "test" classifier "tests"
  )

  val monitoringTypesafeLibrary: Seq[ModuleID] = monitoringLibrary ++ Seq(
    "org.wellcomecollection" %% "monitoring_typesafe" % versions.monitoring,
    "org.wellcomecollection" %% "monitoring_typesafe" % versions.monitoring % "test" classifier "tests"
  )

  val storageLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "storage" % versions.storage,
    "org.wellcomecollection" %% "storage" % versions.storage % "test" classifier "tests"
  )

  val typesafeLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "typesafe_app" % versions.typesafe,
    "org.wellcomecollection" %% "typesafe_app" % versions.typesafe % "test" classifier "tests"
  ) ++ fixturesLibrary

  val messagingTypesafeLibrary: Seq[ModuleID] = messagingLibrary ++ Seq(
    "org.wellcomecollection" %% "messaging_typesafe" % versions.messaging,
    "org.wellcomecollection" %% "messaging_typesafe" % versions.messaging % "test" classifier "tests"
  ) ++ monitoringLibrary

  val httpLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "http" % versions.http,
    "org.wellcomecollection" %% "http" % versions.http % "test" classifier "tests"
  )

  val httpTypesafeLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "http_typesafe" % versions.http,
    "org.wellcomecollection" %% "http_typesafe" % versions.http % "test" classifier "tests"
  )
  val sierraLibrary: Seq[ModuleID] = Seq(
    "org.wellcomecollection" %% "sierra" % versions.sierra,
    "org.wellcomecollection" %% "sierra" % versions.sierra % "test" classifier "tests"
  )

  val sierraTypesafeLibrary: Seq[ModuleID] = sierraLibrary ++ Seq(
    "org.wellcomecollection" %% "sierra_typesafe" % versions.sierra,
    "org.wellcomecollection" %% "sierra_typesafe" % versions.sierra % "test" classifier "tests"
  )
}

object ExternalDependencies {
  lazy val versions = new {
    val circeOptics = "0.14.1"
    val scalatest = "3.2.19"
    val scalatestplus = "3.1.4.0"
    val scalacheckShapeless = "1.1.8"
    val scalacsv = "1.3.10"

    // This should match the version used in scala-libs
    // See https://github.com/wellcomecollection/scala-libs/blob/main/project/Dependencies.scala
    val pekko = "1.1.1"
    val pekkoHttp = "1.1.0"

    val aws2 = "2.29.6"
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
      WellcomeDependencies.sierraLibrary ++
      ExternalDependencies.secretsDependencies

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
