import apm from "elastic-apm-node";
import { ErrorRequestHandler } from "express";

export type ErrorResponse = {
  httpStatus: number;
  label: string;
  description?: string;
  errorType: "http";
  type: "Error";
};

export class HttpError extends Error {
  public readonly status: number;
  public readonly label: string;
  public readonly description?: string;

  constructor({
    status,
    label,
    description,
  }: {
    status: number;
    label: string;
    description?: string;
  }) {
    super(label);
    Object.setPrototypeOf(this, HttpError.prototype);

    this.status = status;
    this.label = label;
    this.description = description;
  }

  get responseJson(): ErrorResponse {
    return {
      httpStatus: this.status,
      label: this.label,
      description: this.description,
      errorType: "http",
      type: "Error",
    };
  }
}

export const errorHandler: ErrorRequestHandler = (err, req, res, next) => {
  if (err instanceof HttpError) {
    res.status(err.status).json(err.responseJson);
  } else {
    // Log this to prevent it getting swallowed
    console.trace(err);
    if (apm.isStarted()) {
      apm.captureError(err);
    }
    const httpError = new HttpError({
      status: 500,
      label: "Server Error",
    });
    res.status(httpError.status).json(httpError.responseJson);
  }
};
