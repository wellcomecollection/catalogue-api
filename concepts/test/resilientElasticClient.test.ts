import { Client, errors } from "@elastic/elasticsearch";
import { ResilientElasticClient } from "../src/services/elasticsearch";

// Mock Client constructor so neither initial nor refreshed clients are real
jest.mock("@elastic/elasticsearch", () => {
  const actual = jest.requireActual("@elastic/elasticsearch");
  return {
    ...actual,
    Client: jest.fn().mockImplementation(() => ({})),
  };
});

// Mock getSecret so refreshClient() -> getElasticClientConfig() can resolve
jest.mock("../src/services/aws", () => ({
  getSecret: jest.fn().mockResolvedValue("mocked-value"),
}));

const MockedClient = Client as jest.Mock;

const getResilient = () =>
  ResilientElasticClient.create({ pipelineDate: "2025-01-01" });

describe("ResilientElasticClient", () => {
  beforeEach(() => {
    MockedClient.mockClear();
  });

  it("returns the result of a successful operation", async () => {
    const resilient = await getResilient();
    const operation = jest.fn().mockResolvedValue("ok");

    const result = await resilient.execute(operation);
    expect(result).toBe("ok");
    expect(operation).toHaveBeenCalledTimes(1);
    expect(MockedClient).toHaveBeenCalledTimes(1);
  });

  it("throws non-auth errors immediately without retrying", async () => {
    const resilient = await getResilient();
    const operation = jest.fn().mockRejectedValue(new Error("network failure"));

    await expect(resilient.execute(operation)).rejects.toThrow(
      "network failure"
    );
    expect(operation).toHaveBeenCalledTimes(1);
    expect(MockedClient).toHaveBeenCalledTimes(1);
  });

  it.each([401, 403])(
    "retries on %i auth errors and succeeds on subsequent attempt",
    async (statusCode) => {
      const resilient = await getResilient();

      const authError = new errors.ResponseError({
        statusCode,
        warnings: null,
        meta: {} as any,
        body: { error: "security_exception" },
      });

      const operation = jest
        .fn()
        .mockRejectedValueOnce(authError)
        .mockResolvedValueOnce("success");

      const result = await resilient.execute(operation);
      expect(result).toBe("success");
      expect(operation).toHaveBeenCalledTimes(2);
      expect(MockedClient).toHaveBeenCalledTimes(2);
    }
  );

  it("gives up after MAX_RETRIES auth failures and throws the last error", async () => {
    const resilient = await getResilient();

    const authError = new errors.ResponseError({
      statusCode: 401,
      warnings: null,
      meta: {} as any,
      body: { error: "security_exception" },
    });

    const operation = jest.fn().mockRejectedValue(authError);

    await expect(resilient.execute(operation)).rejects.toThrow(
      errors.ResponseError
    );
    // 3 attempts total (initial + 2 retries)
    expect(operation).toHaveBeenCalledTimes(3);
    expect(MockedClient).toHaveBeenCalledTimes(3);
  });

  it("recovers on the last possible attempt", async () => {
    const resilient = await getResilient();

    const authError = new errors.ResponseError({
      statusCode: 401,
      warnings: null,
      meta: {} as any,
      body: { error: "security_exception" },
    });

    const operation = jest
      .fn()
      .mockRejectedValueOnce(authError)
      .mockRejectedValueOnce(authError)
      .mockResolvedValueOnce("recovered");

    const result = await resilient.execute(operation);
    expect(result).toBe("recovered");
    expect(operation).toHaveBeenCalledTimes(3);
    expect(MockedClient).toHaveBeenCalledTimes(3);
  });
});
