package weco.api.search.management

import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import java.time.Instant

/** Serves the commit a service was built from, so deploy tooling can tell which code
  * is live. BUILD_COMMIT is baked into the image by builds/build_sbt_image.sh.
  *
  * The contract this implements: https://github.com/wellcomecollection/deploy-tracker/blob/main/SPEC.md#the-manifest-endpoint
  */
object ManifestRoute {

  val route: Route = routeFor(sys.env.getOrElse("BUILD_COMMIT", "unknown"))

  private[management] def routeFor(
    commit: String,
    startedAt: String = Instant.now().toString
  ): Route = {
    // Anything else would break the JSON assembled below.
    val safeCommit = commit.filter(c => c.isLetterOrDigit || "._-".contains(c))
    val body = s"""{"commit":"$safeCommit","startedAt":"$startedAt"}"""

    path("manifest") {
      get {
        respondWithHeader(RawHeader("Cache-Control", "no-store")) {
          complete(HttpEntity(ContentTypes.`application/json`, body))
        }
      }
    }
  }
}
