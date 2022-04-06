import java.io.File
import java.util.UUID
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider

def setupProject(
  project: Project,
  folder: String,
  localDependencies: Seq[Project] = Seq(),
  externalDependencies: Seq[ModuleID] = Seq()
): Project = {

  Metadata.write(project, folder, localDependencies)

  val dependsOn = localDependencies
    .map { project: Project =>
      ClasspathDependency(
        project = project,
        configuration = Some("compile->compile;test->test")
      )
    }

  project
    .in(new File(folder))
    .settings(Common.settings: _*)
    .enablePlugins(DockerComposePlugin)
    .enablePlugins(JavaAppPackaging)
    .dependsOn(dependsOn: _*)
    .settings(libraryDependencies ++= externalDependencies)
}

lazy val display = setupProject(
  project,
  "common/display",
  externalDependencies = CatalogueDependencies.displayModelDependencies
)

lazy val search_common = setupProject(
  project,
  "common/search",
  localDependencies = Seq(display),
  externalDependencies = CatalogueDependencies.searchCommonDependencies
)

lazy val stacks = setupProject(
  project,
  "common/stacks",
  localDependencies = Seq(display),
  externalDependencies = CatalogueDependencies.stacksDependencies
)

lazy val search = setupProject(
  project,
  "search",
  localDependencies = Seq(display, search_common),
  externalDependencies = CatalogueDependencies.searchDependencies
)

lazy val items = setupProject(
  project,
  "items",
  localDependencies = Seq(stacks, search_common),
  externalDependencies = CatalogueDependencies.itemsDependencies
)

lazy val requests = setupProject(
  project,
  "requests",
  localDependencies = Seq(stacks, search_common),
  externalDependencies = CatalogueDependencies.requestsDependencies
)

lazy val snapshot_generator = setupProject(
  project,
  "snapshots/snapshot_generator",
  localDependencies = Seq(display, search_common),
  externalDependencies = CatalogueDependencies.snapshotGeneratorDependencies
)

// AWS Credentials to read from S3

s3CredentialsProvider := { _ =>
  val builder = new STSAssumeRoleSessionCredentialsProvider.Builder(
    "arn:aws:iam::760097843905:role/platform-ci",
    UUID.randomUUID().toString
  )
  builder.build()
}
