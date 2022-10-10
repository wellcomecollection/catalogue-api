import AWS from 'aws-sdk'

const secretsmanager = new AWS.SecretsManager({ region: 'eu-west-1' })

export const getSecret = async (secretName: string) => {
  const data = await secretsmanager
    .getSecretValue({ SecretId: secretName })
    .promise()
  return data.SecretString
}
