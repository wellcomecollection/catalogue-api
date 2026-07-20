package weco.api.search.openapi

import io.circe.Json

import java.io.File
import scala.io.Source

/** Loads reference/catalogue.yaml, the source of truth for the API reference docs.
  */
object OpenApiSpec {
  private val specPath = "reference/catalogue.yaml"

  lazy val parsed: Json = {
    val file = repoFile(specPath)
    val source = Source.fromFile(file)

    try io.circe.yaml.parser
      .parse(source.mkString)
      .fold(
        err =>
          throw new RuntimeException(
            s"Could not parse ${file.getPath}: ${err.message}"
        ),
        identity
      )
    finally source.close()
  }

  /** sbt does not guarantee which directory the JVM starts in, so walk up until we
    * find the file rather than assuming a relative path.
    */
  def repoFile(relativePath: String): File =
    Iterator
      .iterate(new File(".").getAbsoluteFile)(_.getParentFile)
      .takeWhile(_ != null)
      .map(new File(_, relativePath))
      .find(_.exists())
      .getOrElse(
        throw new RuntimeException(
          s"Could not find $relativePath in any parent of ${new File(".").getAbsolutePath}"
        )
      )
}
