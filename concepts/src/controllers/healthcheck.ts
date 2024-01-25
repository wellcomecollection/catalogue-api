import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import { Config } from "../../config";

type PathParams = { id: string };

const healthcheckController = (config: Config): RequestHandler<PathParams> => {
  return asyncHandler(async (req, res) => {
    res.status(200).json({
      status: "ok",
      config,
    });
  });
};

export default healthcheckController;
