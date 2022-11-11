import {
  createLogger,
  transports,
  format,
  config,
  LeveledLogMethod,
} from "winston";

// This roughly matches the logback config used by the Scala apps
const formatter = format.combine(
  format.timestamp({ format: "HH:mm:ss.SSS" }),
  format.errors({ stack: true }),
  format.printf(
    ({ level, message, timestamp }) =>
      `${timestamp} ${level.toUpperCase()} - ${message}`
  )
);

// This is here because APM demands that it is given a logger with
// these log levels, which winston does not provide
declare module "winston" {
  interface Logger {
    fatal: LeveledLogMethod;
    trace: LeveledLogMethod;
  }
}

const logger = createLogger({
  transports: [new transports.Console()],
  format: formatter,
  levels: {
    ...config.npm.levels,
    fatal: 0,
    trace: 5,
  },
});

export default logger;
