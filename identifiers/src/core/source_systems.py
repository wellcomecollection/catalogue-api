"""The source systems this API advertises, for consumers building lookups."""

from core.models import SourceSystem

# From IdentifierType.scala in catalogue-pipeline (common/internal_model/src/main/
# scala/weco/catalogue/internal_model/identifiers/) at 5d85dad275, curated to the
# systems the registry holds for Work, Image and Item. Labels follow
# catalogue_graph/src/models/identifier_schemes.py, which the works API displays.
SOURCE_SYSTEMS: tuple[SourceSystem, ...] = (
    SourceSystem(id="axiell-guid", label="Axiell GUID", types=("Work",)),
    SourceSystem(id="calm-record-id", label="Calm RecordIdentifier", types=("Work",)),
    SourceSystem(
        id="ebsco-alt-lookup", label="EBSCO lookup identifier", types=("Work",)
    ),
    SourceSystem(id="folio-instance", label="Folio instance", types=("Work",)),
    SourceSystem(id="folio-item", label="Folio item", types=("Item",)),
    SourceSystem(id="mets", label="METS", types=("Work",)),
    SourceSystem(id="mets-image", label="METS image", types=("Image",)),
    SourceSystem(
        id="miro-image-number", label="Miro image number", types=("Work", "Image")
    ),
    SourceSystem(
        id="sierra-system-number", label="Sierra system number", types=("Work", "Item")
    ),
    SourceSystem(id="tei-manuscript-id", label="Tei manuscript id", types=("Work",)),
)
