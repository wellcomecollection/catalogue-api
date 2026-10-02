import supertest from "supertest";
import { Concept } from "../../src/types";
import createApp from "../../src/app";
import { mockedElasticsearchClient } from "./elasticsearch";

export const previewCluster = "test-preview";

export const mockedApi = (
  concepts: Concept[],
  previewConcepts: Concept[] = []
) => {
  const index = "test-index";
  const previewIndex = "test-preview-index";
  const elastic = mockedElasticsearchClient({ index, docs: concepts });
  const previewElastic = mockedElasticsearchClient({
    index: previewIndex,
    docs: previewConcepts,
  });

  const app = createApp(
    { elastic, additionalElastic: { [previewCluster]: previewElastic } },
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
