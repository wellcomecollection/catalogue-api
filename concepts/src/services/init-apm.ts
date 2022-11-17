import apm from "elastic-apm-node";
import z from "zod";

const apmEnvironmentVariables = z.object({
  apm_service_name: z.string().default("concepts-api"),
  apm_environment: z.string().default("local"),
  apm_server_url: z.string().url().optional(),
  apm_secret: z.string().optional(),
});
const environment = apmEnvironmentVariables.parse(process.env);

if (environment.apm_server_url && environment.apm_secret) {
  apm.start({
    serviceName: environment.apm_service_name,
    environment: environment.apm_environment,
    serverUrl: environment.apm_server_url,
    secretToken: environment.apm_secret,
    logLevel: "warn",
  });
}
