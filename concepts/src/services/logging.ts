import { createLogger, transports, format } from "winston";

// This roughly matches the logback config used by the Scala apps
const formatter = format.combine(
  format.timestamp({ format: "HH:mm:ss.SSS" }),
  format.errors({ stack: true }),
  format.printf(({ level, message, timestamp, stack }) => {
    let log = `${timestamp} ${level.toUpperCase()} - ${message}`;
    if (stack) {
      log += "\n" + stack;
    }
    return log;
  })
);

const logger = createLogger({
  transports: [new transports.Console()],
  format: formatter,
  level: "http",
});

export const logStream = (level: string) => ({
  write: (message: string) => logger.log({ level, message: message.trim() }),
});

export default logger;
