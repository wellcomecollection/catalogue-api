import { Work, Availability, Format } from "../../src/types";

let workCounter = 0;

type WorkOptions = {
  id?: string;
  title?: string;
  type?: "Work" | "Collection" | "Series" | "Section";
  availabilities?: Availability[];
  workType?: Format;
};

export const work = (options: WorkOptions = {}): Work => {
  workCounter++;
  return {
    id: options.id ?? `work-${workCounter}`,
    title: options.title ?? `Test Work ${workCounter}`,
    alternativeTitles: [],
    ...(options.workType && { workType: options.workType }),
    availabilities: options.availabilities ?? [],
    type: options.type ?? "Work",
  };
};

export const workWithThumbnail = (options: WorkOptions = {}): Work => ({
  ...work(options),
  thumbnail: {
    locationType: {
      id: "iiif-presentation",
      label: "IIIF Presentation API",
      type: "LocationType",
    },
    url: "https://iiif.wellcomecollection.org/image/test.jpg/info.json",
    license: {
      id: "cc-by",
      label: "Attribution 4.0 International (CC BY 4.0)",
      url: "http://creativecommons.org/licenses/by/4.0/",
      type: "License",
    },
    accessConditions: [],
    type: "DigitalLocation",
  },
});

export const workWithEditionAndDuration = (
  options: WorkOptions = {}
): Work => ({
  ...work(options),
  edition: "Special edition",
  duration: 3600,
});

export const workWithSubjects = (options: WorkOptions = {}): Work => ({
  ...work(options),
  subjects: [
    {
      id: "subject-1",
      label: "Test Subject",
      concepts: [
        {
          id: "concept-1",
          label: "Test Concept",
          type: "Concept",
        },
      ],
      type: "Subject",
    },
  ],
});

export const workWithContributors = (options: WorkOptions = {}): Work => ({
  ...work(options),
  contributors: [
    {
      agent: {
        id: "agent-1",
        label: "Test Author",
        type: "Person",
      },
      roles: [
        {
          label: "Author",
          type: "ContributionRole",
        },
      ],
      type: "Contributor",
    },
  ],
});

export const workWithProduction = (options: WorkOptions = {}): Work => ({
  ...work(options),
  production: [
    {
      label: "London, 1900",
      places: [
        {
          label: "London",
          type: "Place",
        },
      ],
      agents: [],
      dates: [
        {
          id: "1900",
          label: "1900",
          type: "Period",
        },
      ],
      type: "ProductionEvent",
    },
  ],
});

export const workWithWorkType = (
  workTypeId: string,
  workTypeLabel: string,
  options: WorkOptions = {}
): Work => ({
  ...work(options),
  workType: {
    id: workTypeId,
    label: workTypeLabel,
    type: "Format",
  },
});

// Create indexed work document
export const indexedWork = (
  workDoc: Work,
  options: {
    type?: string;
    redirectTo?: string;
    filterableValues?: Record<string, unknown>;
    aggregatableValues?: Record<string, unknown>;
  } = {}
) => ({
  id: workDoc.id,
  display: workDoc,
  type: options.type ?? "Visible",
  ...(options.redirectTo && { redirectTo: options.redirectTo }),
  filterableValues: options.filterableValues ?? {},
  aggregatableValues: options.aggregatableValues ?? {},
});
