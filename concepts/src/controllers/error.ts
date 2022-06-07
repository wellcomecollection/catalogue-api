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
  const httpError =
    err instanceof HttpError
      ? err
      : new HttpError({
          status: 500,
          label: "Server error",
        });

  res.status(httpError.status).json(httpError.responseJson);
};
