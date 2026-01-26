import { Client } from "@elastic/elasticsearch";
import { ResilientElasticClient } from "../src/services/resilient-elastic-client";

// Mock client creation helper
const createMockClient = (searchBehavior: () => Promise<unknown>) => {
  const mockClose = jest.fn().mockResolvedValue(undefined);
  const mockSearch = jest.fn().mockImplementation(searchBehavior);
  const mockGet = jest.fn();
  const mockClusterHealth = jest.fn().mockResolvedValue({ status: "green" });

  return {
    search: mockSearch,
    get: mockGet,
    cluster: { health: mockClusterHealth },
    close: mockClose,
  } as unknown as Client;
};

// Helper to create an error with statusCode
const createAuthError = (statusCode: 401 | 403, message: string) => {
  const error = new Error(message);
  (error as Error & { statusCode: number }).statusCode = statusCode;
  return error;
};

const createError = (statusCode: number, message: string) => {
  const error = new Error(message);
  (error as Error & { statusCode: number }).statusCode = statusCode;
  return error;
};

describe("ResilientElasticClient", () => {
  describe("retry on auth failures", () => {
    it("retries on 401", async () => {
      let searchCallCount = 0;
      let factoryCallCount = 0;

      // First client fails with 401
      const mockClient1 = createMockClient(() => {
        searchCallCount++;
        return Promise.reject(createAuthError(401, "Unauthorized"));
      });

      // Second client succeeds
      const mockClient2 = createMockClient(() => {
        searchCallCount++;
        return Promise.resolve({ hits: { hits: [{ _id: "doc1" }] } });
      });

      const clientFactory = jest.fn().mockImplementation(async () => {
        factoryCallCount++;
        return factoryCallCount === 1 ? mockClient2 : mockClient2;
      });

      const resilientClient = new ResilientElasticClient(
        mockClient1,
        clientFactory,
        0 // No cooldown for tests
      );

      const response = await resilientClient.search({ index: "test" });

      expect(response.hits.hits).toHaveLength(1);
      expect(searchCallCount).toBe(2);
      expect(factoryCallCount).toBe(1); // Refresh called once
    });

    it("retries on 403", async () => {
      let searchCallCount = 0;
      let factoryCallCount = 0;

      const mockClient1 = createMockClient(() => {
        searchCallCount++;
        return Promise.reject(createAuthError(403, "Forbidden"));
      });

      const mockClient2 = createMockClient(() => {
        searchCallCount++;
        return Promise.resolve({ hits: { hits: [{ _id: "doc1" }] } });
      });

      const clientFactory = jest.fn().mockImplementation(async () => {
        factoryCallCount++;
        return mockClient2;
      });

      const resilientClient = new ResilientElasticClient(
        mockClient1,
        clientFactory,
        0
      );

      const response = await resilientClient.search({ index: "test" });

      expect(response.hits.hits).toHaveLength(1);
      expect(searchCallCount).toBe(2);
      expect(factoryCallCount).toBe(1);
    });

    it("does not retry on 404", async () => {
      let searchCallCount = 0;

      const mockClient = createMockClient(() => {
        searchCallCount++;
        return Promise.reject(createError(404, "Not Found"));
      });

      const clientFactory = jest.fn();

      const resilientClient = new ResilientElasticClient(
        mockClient,
        clientFactory,
        0
      );

      await expect(resilientClient.search({ index: "test" })).rejects.toThrow(
        "Not Found"
      );
      expect(searchCallCount).toBe(1);
      expect(clientFactory).not.toHaveBeenCalled();
    });

    it("does not retry on 500", async () => {
      let searchCallCount = 0;

      const mockClient = createMockClient(() => {
        searchCallCount++;
        return Promise.reject(createError(500, "Server Error"));
      });

      const clientFactory = jest.fn();

      const resilientClient = new ResilientElasticClient(
        mockClient,
        clientFactory,
        0
      );

      await expect(resilientClient.search({ index: "test" })).rejects.toThrow(
        "Server Error"
      );
      expect(searchCallCount).toBe(1);
      expect(clientFactory).not.toHaveBeenCalled();
    });

    it("only retries once", async () => {
      let searchCallCount = 0;
      let factoryCallCount = 0;

      // Both clients fail with 401
      const mockClient1 = createMockClient(() => {
        searchCallCount++;
        return Promise.reject(createAuthError(401, "Unauthorized"));
      });

      const mockClient2 = createMockClient(() => {
        searchCallCount++;
        return Promise.reject(createAuthError(401, "Unauthorized"));
      });

      const clientFactory = jest.fn().mockImplementation(async () => {
        factoryCallCount++;
        return mockClient2;
      });

      const resilientClient = new ResilientElasticClient(
        mockClient1,
        clientFactory,
        0
      );

      await expect(resilientClient.search({ index: "test" })).rejects.toThrow(
        "Unauthorized"
      );
      expect(searchCallCount).toBe(2);
      expect(factoryCallCount).toBe(1); // Only one refresh
    });
  });

  describe("cooldown throttling", () => {
    it("throttles refresh requests within cooldown period", async () => {
      let factoryCallCount = 0;

      // All clients fail with 401
      const createFailingClient = () =>
        createMockClient(() => {
          return Promise.reject(createAuthError(401, "Unauthorized"));
        });

      const initialClient = createFailingClient();

      const clientFactory = jest.fn().mockImplementation(async () => {
        factoryCallCount++;
        return createFailingClient();
      });

      const resilientClient = new ResilientElasticClient(
        initialClient,
        clientFactory,
        100 // 100ms cooldown
      );

      // First request - should refresh
      await expect(resilientClient.search({ index: "test" })).rejects.toThrow();
      expect(factoryCallCount).toBe(1);

      // Second request immediately after (within cooldown) - should NOT refresh
      await expect(resilientClient.search({ index: "test" })).rejects.toThrow();
      expect(factoryCallCount).toBe(1); // No new refresh

      // Wait for cooldown to expire
      await new Promise((resolve) => setTimeout(resolve, 150));

      // Third request after cooldown - should refresh
      await expect(resilientClient.search({ index: "test" })).rejects.toThrow();
      expect(factoryCallCount).toBe(2); // New refresh
    });

    it("allows configurable cooldown period", async () => {
      let factoryCallCount = 0;

      const createFailingClient = () =>
        createMockClient(() => {
          return Promise.reject(createAuthError(401, "Unauthorized"));
        });

      const clientFactory = jest.fn().mockImplementation(async () => {
        factoryCallCount++;
        return createFailingClient();
      });

      const resilientClient = new ResilientElasticClient(
        createFailingClient(),
        clientFactory,
        50 // 50ms cooldown
      );

      // First request - should refresh
      await expect(resilientClient.search({ index: "test" })).rejects.toThrow();
      expect(factoryCallCount).toBe(1);

      // Wait for cooldown to expire
      await new Promise((resolve) => setTimeout(resolve, 100));

      // Second request after cooldown - should refresh
      await expect(resilientClient.search({ index: "test" })).rejects.toThrow();
      expect(factoryCallCount).toBe(2);
    });
  });

  describe("concurrent requests", () => {
    it("only one request triggers refresh during concurrent 401 errors", async () => {
      let factoryCallCount = 0;

      const createFailingClient = () =>
        createMockClient(() => {
          return Promise.reject(createAuthError(401, "Unauthorized"));
        });

      const clientFactory = jest.fn().mockImplementation(async () => {
        factoryCallCount++;
        // Add a small delay to simulate real network conditions
        await new Promise((resolve) => setTimeout(resolve, 10));
        return createFailingClient();
      });

      const resilientClient = new ResilientElasticClient(
        createFailingClient(),
        clientFactory,
        0
      );

      // Create concurrent requests that all get 401
      const promises = [
        resilientClient.search({ index: "test" }).catch(() => {}),
        resilientClient.search({ index: "test" }).catch(() => {}),
        resilientClient.search({ index: "test" }).catch(() => {}),
      ];

      await Promise.all(promises);

      // Only one refresh should have been triggered
      expect(factoryCallCount).toBe(1);
    });
  });

  describe("getClient", () => {
    it("returns the underlying client", async () => {
      const mockClient = createMockClient(() =>
        Promise.resolve({ hits: { hits: [] } })
      );

      const clientFactory = jest.fn();

      const resilientClient = new ResilientElasticClient(
        mockClient,
        clientFactory,
        0
      );

      const client = resilientClient.getClient();
      expect(client).toBe(mockClient);
    });
  });

  describe("clusterHealth", () => {
    it("performs cluster health check with retry on auth failure", async () => {
      let factoryCallCount = 0;

      const mockClient1 = {
        search: jest.fn(),
        get: jest.fn(),
        cluster: {
          health: jest
            .fn()
            .mockRejectedValue(createAuthError(401, "Unauthorized")),
        },
        close: jest.fn().mockResolvedValue(undefined),
      } as unknown as Client;

      const mockClient2 = {
        search: jest.fn(),
        get: jest.fn(),
        cluster: {
          health: jest.fn().mockResolvedValue({ status: "green" }),
        },
        close: jest.fn().mockResolvedValue(undefined),
      } as unknown as Client;

      const clientFactory = jest.fn().mockImplementation(async () => {
        factoryCallCount++;
        return mockClient2;
      });

      const resilientClient = new ResilientElasticClient(
        mockClient1,
        clientFactory,
        0
      );

      const result = await resilientClient.clusterHealth();

      expect(result.status).toBe("green");
      expect(factoryCallCount).toBe(1);
    });
  });
});
