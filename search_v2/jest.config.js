/** @type {import('ts-jest/dist/types').InitialOptionsTsJest} */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  // Integration tests have longer timeouts
  testTimeout: 30000,
  // Separate test paths
  testPathIgnorePatterns: ["/node_modules/"],
};
