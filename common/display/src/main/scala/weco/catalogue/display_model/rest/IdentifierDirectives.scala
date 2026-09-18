package weco.catalogue.display_model.rest

trait IdentifierDirectives {

  /** Returns true if an ID looks like a canonical ID; false otherwise.
    *
    * This is a weak heuristic that lets us reject requests that are for something
    * that obviously isn't a canonical ID (e.g. empty strings, JavaScript injections).
    *
    */
  def looksLikeCanonicalId(id: String): Boolean =
    id.matches("""^[a-z0-9]+$""")
}
