// Drops the API Gateway extensions from the published Identifiers API spec:
// they configure the gateway and mean nothing to a reader.
const isAws = (key) => key.startsWith("x-amazon-apigateway-");

module.exports = function stripAwsExtensions() {
  return {
    id: "weco",
    decorators: {
      oas3: {
        "strip-aws-extensions": () => ({
          any: {
            leave(node) {
              if (node && typeof node === "object" && !Array.isArray(node)) {
                Object.keys(node)
                  .filter(isAws)
                  .forEach((key) => delete node[key]);
              }
            },
          },
        }),
      },
    },
  };
};
