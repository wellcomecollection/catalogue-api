import { RequestHandler } from "express";

const startedAt = new Date().toISOString();

/**
 * Lets deploy tooling confirm which commit is live; must never be cached.
 * The contract this implements: https://github.com/wellcometrust/backplane/blob/main/docs/spec/09-dora-metrics.md#971-the-manifest-endpoint
 */
const manifestController = (): RequestHandler => {
  return (req, res) => {
    res.set("Cache-Control", "no-store");
    res.status(200).json({
      commit: process.env.BUILD_COMMIT || "unknown",
      startedAt,
    });
  };
};

export default manifestController;
