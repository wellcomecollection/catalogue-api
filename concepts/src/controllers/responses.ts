export type ErrorResponse = {
  httpStatus: number;
  label: string;
  description: string;
  errorType: "http";
  type: "Error";
};

export const errorResponse = ({
  status,
  label,
  description,
}: {
  status: number;
  label: string;
  description: string;
}): ErrorResponse => ({
  httpStatus: status,
  label,
  description,
  errorType: "http",
  type: "Error",
});
