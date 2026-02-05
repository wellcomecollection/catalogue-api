package weco.api.search

import org.apache.pekko.http.scaladsl.server.Route

/** 
 * Common trait for all API routers.
 * Ensures that routers expose a routes method for building HTTP routes.
 */
trait ApiRouter {
  def routes: Route
}
