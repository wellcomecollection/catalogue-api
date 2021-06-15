import axios from "axios";
import { promises as fs } from "fs";
import path from "path";

type SearchTemplate = {
  id: string;
  index: string;
  query: string;
};

const getSearchTemplate = async (
  env: "prod" | "stage"
): Promise<SearchTemplate | undefined> => {
  const prefix = env === "prod" ? "api" : "api-stage";
  const url = `https://${prefix}.wellcomecollection.org/catalogue/v2/search-templates.json`;
  const { data } = await axios.get(url);
  return (data.templates as SearchTemplate[]).find(
    ({ id }) => id === "multi_matcher_search_query"
  );
};

const main = async () => {
  const templateDir = path.resolve(__dirname, "query-templates");
  for (const env of ["prod", "stage"] as const) {
    const template = await getSearchTemplate(env);
    if (template) {
      const filename = `${template.index}.json`;
      const prettyTemplate = JSON.stringify(
        JSON.parse(template.query),
        null,
        2
      );
      await fs.writeFile(path.resolve(templateDir, filename), prettyTemplate);
    }
  }
};

main();
