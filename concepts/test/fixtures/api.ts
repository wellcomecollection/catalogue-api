import supertest from "supertest";
import { Concept } from "../../src/types";
import createApp from "../../src/app";
import { mockedElasticsearchClient } from "./elasticsearch";

export const mockedApi = (concepts: Concept[]) => {
  const index = "test-index";
  const elastic = mockedElasticsearchClient({ index, docs: concepts });

  const app = createApp(
    { elastic },
    {
      conceptsIndex: index,
      pipelineDate: "2022-02-22",
      publicRootUrl: new URL("http://concepts.test"),
    }
  );

  return supertest.agent(app);
};
