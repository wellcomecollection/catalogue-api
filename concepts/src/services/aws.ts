import {
  SecretsManagerClient,
  GetSecretValueCommand,
} from "@aws-sdk/client-secrets-manager";

const client = new SecretsManagerClient({});

export const getSecret = async (id: string): Promise<string | undefined> => {
  try {
    const result = await client.send(
      new GetSecretValueCommand({ SecretId: id })
    );
    return result.SecretString;
  } catch (e) {
    console.error(`Error fetching secret '${id}`, e);
    return undefined;
  }
};
