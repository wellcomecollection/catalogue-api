import java.io.File

import sbt.{IO, Project}
import scala.reflect.io.Directory
import io.circe.generic.auto._
import io.circe.syntax._

object Metadata {
  // Clear out the .sbt_metadata directory before every invocation of sbt,
  // so if we change the project structure old metadata entries will
  // be deleted.
  val directory = new Directory(new File(".sbt_metadata"))
  directory.deleteRecursively()

  def write(
    project: Project,
    folder: String,
  ) = {
    // Here we write a bit of metadata about the project, and the other
    // local projects it depends on.  This is used by the build scripts
    // to find docker-compose and Dockerfiles.
    // See https://www.scala-sbt.org/release/docs/Howto-Generating-Files.html
    val file = new File(s".sbt_metadata/${project.id}.json")

    case class ProjectMetadata(id: String, folder: String)

    val metadata = ProjectMetadata(id = project.id, folder = folder)

    IO.write(file, metadata.asJson.spaces2)
  }
}
