// This code is copied from the catalogue-pipeline repo.
//
// Do not edit this file -- edit the copy in the pipeline repo, commit
// those changes to main and then copy them into this repo.

package weco.catalogue.source_model.sierra.source

object OpacMsg {
  val OnlineRequest = "f"
  val ManualRequest = "n"
  val OpenShelves = "o"
  val ByAppointment = "a"
  val AtDigitisation = "b"
  val DonorPermission = "q"
  val Unavailable = "u"
  val StaffUseOnly = "s"
  val AskAtDesk = "i"
}
