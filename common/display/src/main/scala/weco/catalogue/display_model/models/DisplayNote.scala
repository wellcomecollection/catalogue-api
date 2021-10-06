package weco.catalogue.display_model.models

import io.circe.generic.extras.JsonKey
import io.swagger.v3.oas.annotations.media.Schema
import weco.catalogue.internal_model.work._

@Schema(
  name = "Note",
  description = "A note associated with the work."
)
case class DisplayNote(
  @Schema(description = "The note contents.") contents: List[String],
  @Schema(description = "The type of note.") noteType: DisplayNoteType,
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "Note"
)

@Schema(
  name = "NoteType",
  description = "Indicates the type of note associated with the work."
)
case class DisplayNoteType(
  @Schema(
    `type` = "String"
  ) id: String,
  @Schema(
    `type` = "String"
  ) label: String,
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "NoteType"
)

object DisplayNote {

  def apply(note: Note): DisplayNote =
    DisplayNote(
      contents = List(note.contents),
      noteType = DisplayNoteType(note)
    )

  def merge(notes: List[DisplayNote]): List[DisplayNote] =
    notes
      .groupBy(_.noteType)
      .toList
      .map {
        case (noteType, notes) =>
          DisplayNote(notes.flatMap(_.contents), noteType)
      }
}

object DisplayNoteType {

  def apply(note: Note): DisplayNoteType =
    DisplayNoteType(id = note.noteType.id, label = note.noteType.label)
}
