import yargs = require("yargs");
import crypto from "crypto";
import { SingleBar, Presets } from "cli-progress";
import fs from "fs";
import path from "path";
import percentile from "percentile";
import { measureQueryTime } from "./queryExecution";

const argv = yargs(process.argv.slice(2)).options({
  index: { type: "string", demandOption: true },
  nSamples: { type: "number", default: 200, alias: "n" },
  withAggs: { type: "boolean", default: true },
}).argv;

const mean = (arr: number[]): number =>
  arr.reduce((a, b) => a + b) / arr.length;

const { commonWords } = JSON.parse(
  fs.readFileSync(path.resolve(__dirname, "common-words.json"), "utf8")
);
const randomCommonWord = () =>
  commonWords[Math.floor(Math.random() * commonWords.length)];

const main = async () => {
  const { nSamples, index, withAggs } = await argv;
  const query = await fs.promises.readFile(
    path.resolve(__dirname, "query-templates", `${index}.json`),
    "utf8"
  );
  const aggs = await fs.promises.readFile(
    path.resolve(__dirname, "query-templates", "aggregations.json"),
    "utf8"
  );

  const queryHash = crypto.createHash("md5").update(query).digest("hex");

  const progressBar = new SingleBar({}, Presets.shades_classic);
  progressBar.start(nSamples, 0);
  const results = [];
  for (let _ of Array.from({ length: nSamples })) {
    try {
      const took = await measureQueryTime({
        index,
        query,
        searchTerm: randomCommonWord(),
        aggs: withAggs ? aggs : undefined,
      });
      results.push(took);
    } catch (e) {
      console.warn(e);
    } finally {
      progressBar.increment();
    }
  }
  progressBar.stop();

  const stats = {
    index,
    queryHash,
    mean: mean(results),
    median: percentile(50, results),
    p95: percentile(95, results),
    max: Math.max(...results),
    failed: nSamples - results.length,
  };
  console.log(stats);
};

main();
