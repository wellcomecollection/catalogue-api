import { Image, DigitalLocation } from "../../src/types";

let imageCounter = 0;

const defaultLocation: DigitalLocation = {
  locationType: {
    id: "iiif-image",
    label: "IIIF Image API",
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
};

type ImageOptions = {
  id?: string;
  sourceTitle?: string;
  sourceId?: string;
};

export const image = (options: ImageOptions = {}): Image => {
  imageCounter++;
  return {
    id: options.id ?? `image-${imageCounter}`,
    locations: [defaultLocation],
    source: {
      id: options.sourceId ?? `work-${imageCounter}`,
      title: options.sourceTitle ?? `Source Work ${imageCounter}`,
      type: "Work",
    },
    type: "Image",
  };
};

export const imageWithThumbnail = (options: ImageOptions = {}): Image => ({
  ...image(options),
  thumbnail: {
    ...defaultLocation,
    url: "https://iiif.wellcomecollection.org/image/test-thumb.jpg/info.json",
  },
});

export const imageWithContributors = (options: ImageOptions = {}): Image => ({
  ...image(options),
  source: {
    ...image(options).source,
    contributors: [
      {
        agent: {
          id: "agent-1",
          label: "Test Artist",
          type: "Person",
        },
        roles: [
          {
            label: "Artist",
            type: "ContributionRole",
          },
        ],
        type: "Contributor",
      },
    ],
  },
});

// Create indexed image document
export const indexedImage = (
  imageDoc: Image,
  options: {
    filterableValues?: Record<string, unknown>;
    aggregatableValues?: Record<string, unknown>;
    vectorValues?: { features?: number[] };
  } = {}
) => ({
  id: imageDoc.id,
  display: imageDoc,
  filterableValues: options.filterableValues ?? {},
  aggregatableValues: options.aggregatableValues ?? {},
  vectorValues: options.vectorValues,
});
