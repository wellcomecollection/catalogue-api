import { Client, errors } from "@elastic/elasticsearch";
import Mock from "@elastic/elasticsearch-mock";

type Identifiable = {
  id: string;
};

type MockParams<T extends Identifiable> = {
  index: string;
  docs: T[];
};

const docResponse =
  <T extends Identifiable>(index: string) =>
  (doc: T) => ({
    _index: index,
    _id: doc.id,
    _version: 1,
    found: true,
    _source: {
      display: doc,
    },
  });

export const mockedElasticsearchClient = <T extends Identifiable>({
  index,
  docs,
}: MockParams<T>) => {
  const mock = new Mock();
  const docsMap = docs.reduce(
    (map, doc) => map.set(doc.id, doc),
    new Map<string, T>()
  );

  mock.add(
    {
      method: "GET",
      path: `/${index}/_doc/:id`,
    },
    ({ path }) => {
      const id = typeof path === "string" ? path.split("/")[3] : undefined;
      if (id && docsMap.has(id)) {
        return docResponse(index)(docsMap.get(id)!);
      } else {
        return new errors.ResponseError({
          statusCode: 404,
          warnings: null,
          meta: {} as any,
          body: {
            _index: index,
            _id: id,
            found: false,
          },
        });
      }
    }
  );

  mock.add(
    {
      method: ["GET", "POST"],
      path: `/${index}/_search`,
    },
    () => ({
      took: 1234,
      timed_out: false,
      hits: {
        total: {
          value: docs.length,
          relation: "eq",
        },
        hits: docs.map(docResponse(index)),
      },
    })
  );

  return new Client({
    node: "http://test:9200",
    Connection: mock.getConnection(),
  });
};
