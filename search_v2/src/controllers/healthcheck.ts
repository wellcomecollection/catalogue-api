import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import { Config } from "../../config";

const healthcheckController = (config: Config): RequestHandler => {
  return asyncHandler(async (_req, res) => {
    res.status(200).json({
      status: "ok",
      config: {
        pipelineDate: config.pipelineDate,
        worksIndex: config.worksIndex,
        imagesIndex: config.imagesIndex,
      },
    });
  });
};

export default healthcheckController;
