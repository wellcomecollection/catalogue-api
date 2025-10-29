const {
  GetSecretValueCommand,
  SecretsManagerClient,
} = require("@aws-sdk/client-secrets-manager");

module.exports = {
  addApiKey: addApiKey,
};

let stageSecretId = "catalogue_api/items/stage/api_key";
let prodSecretId = "catalogue_api/items/prod/api_key";

const secretsManager = new SecretsManagerClient({
  region: "eu-west-1",
});
let apiKey;

function addApiKey(requestParams, context, ee, next) {
  let isStage = context["vars"]["$environment"] == "stage";
  const secretId = isStage ? stageSecretId : prodSecretId;

  if (apiKey) {
    requestParams.headers["x-api-key"] = apiKey;
    return next();
  } else {
    secretsManager.send(
      new GetSecretValueCommand({ SecretId: secretId }),
      function (err, data) {
        if (err) {
          console.log(err, err.stack);
          process.exit(1);
        } else {
          apiKey = data["SecretString"];
          requestParams.headers["x-api-key"] = apiKey;

          return next();
        }
      }
    );
  }
}
