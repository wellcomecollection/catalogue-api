import { URL } from "url";
import { z } from "zod";

// Default dates - keep in sync with ElasticConfig.scala
const defaults = {
  pipelineDate: "2025-10-02",
  indexDateWorks: "2025-11-20",
  indexDateImages: "2025-10-02",
};

const environmentSchema = z.object({
  PUBLIC_ROOT_URL: z
    .string()
    .url()
    .default("https://api.wellcomecollection.org/catalogue/v2"),
  PIPELINE_DATE: z.string().optional(),
  INDEX_DATE_WORKS: z.string().optional(),
  INDEX_DATE_IMAGES: z.string().optional(),
});

const environment = environmentSchema.parse(process.env);

export type Config = {
  pipelineDate: string;
  indexDateWorks: string;
  indexDateImages: string;
  worksIndex: string;
  imagesIndex: string;
  publicRootUrl: URL;
  defaultPageSize: number;
  maxPageSize: number;
};

export const getConfig = (): Config => {
  const pipelineDate = environment.PIPELINE_DATE ?? defaults.pipelineDate;
  const indexDateWorks =
    environment.INDEX_DATE_WORKS ?? defaults.indexDateWorks;
  const indexDateImages =
    environment.INDEX_DATE_IMAGES ?? defaults.indexDateImages;

  return {
    pipelineDate,
    indexDateWorks,
    indexDateImages,
    worksIndex: `works-indexed-${indexDateWorks}`,
    imagesIndex: `images-indexed-${indexDateImages}`,
    publicRootUrl: new URL(environment.PUBLIC_ROOT_URL),
    defaultPageSize: 10,
    maxPageSize: 100,
  };
};
