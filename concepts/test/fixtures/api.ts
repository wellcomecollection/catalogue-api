import supertest from "supertest";
import { Concept } from "../../src/types";
import createApp from "../../src/app";
import { mockedElasticsearchClient, SearchRequest } from "./elasticsearch";

export const previewCluster = "test-preview";

export const mockedApi = (
  concepts: Concept[],
  previewConcepts: Concept[] = [],
  {
    previewClientDropped = false,
    onSearch,
  }: {
    previewClientDropped?: boolean;
    onSearch?: (request: SearchRequest) => void;
  } = {}
) => {
  const index = "test-index";
  const previewIndex = "test-preview-index";
  const elastic = mockedElasticsearchClient({
    index,
    docs: concepts,
    onSearch,
  });
  const previewElastic = mockedElasticsearchClient({
    index: previewIndex,
    docs: previewConcepts,
  });

  const app = createApp(
    {
      elastic,
      additionalElastic: previewClientDropped
        ? {}
        : { [previewCluster]: previewElastic },
    },
    {
      conceptsIndex: index,
      pipelineDate: "2022-02-22",
      additionalClusters: {
        [previewCluster]: {
          pipelineDate: "2022-02-23",
          conceptsIndex: previewIndex,
        },
      },
      publicRootUrl: new URL("http://concepts.test"),
    }
  );

  return supertest.agent(app);
};
