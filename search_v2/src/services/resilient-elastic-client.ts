import { Client } from "@elastic/elasticsearch";
import {
  SearchRequest,
  SearchResponse,
  GetRequest,
  GetResponse,
  ClusterHealthRequest,
  ClusterHealthResponse,
} from "@elastic/elasticsearch/lib/api/types";
import log from "./logging";

type ClientFactory = () => Promise<Client>;

/**
 * A wrapper around the Elasticsearch client that handles credential expiry.
 *
 * When the client receives a 401 or 403 response from Elasticsearch, it will:
 * 1. Refresh the client by calling the factory function to get new credentials
 * 2. Retry the failed request with the new client
 *
 * Features:
 * - Auto-retry on 401/403 responses
 * - Cooldown throttling to prevent excessive credential refreshes
 * - Thread-safe refresh (only one refresh at a time via promise coordination)
 */
export class ResilientElasticClient {
  private client: Client;
  private clientFactory: ClientFactory;
  private minRefreshIntervalMs: number;
  private lastRefreshTime: number = 0;
  private refreshPromise: Promise<void> | null = null;

  constructor(
    initialClient: Client,
    clientFactory: ClientFactory,
    minRefreshIntervalMs: number = 2000
  ) {
    this.client = initialClient;
    this.clientFactory = clientFactory;
    this.minRefreshIntervalMs = minRefreshIntervalMs;
  }

  /**
   * Get the underlying Elasticsearch client.
   * This is needed for direct access to client methods like search, get, etc.
   */
  getClient(): Client {
    return this.client;
  }

  /**
   * Execute a request with automatic retry on auth failures.
   *
   * @param operation - A function that performs the ES operation using the client
   * @param getStatusCode - A function to extract status code from the response (for meta responses)
   * @returns The result of the operation
   */
  private async executeWithRetry<T>(
    operation: (client: Client) => Promise<T>,
    getStatusCode?: (response: T) => number | undefined
  ): Promise<T> {
    const currentClient = this.client;

    try {
      const response = await operation(currentClient);

      // Check for auth failures that need retry (if we can get status code)
      const statusCode = getStatusCode?.(response);
      if (statusCode === 401 || statusCode === 403) {
        log.warn(
          `Received ${statusCode} from Elasticsearch, refreshing client and retrying...`
        );
        await this.refreshClient(currentClient);
        return operation(this.client);
      }

      return response;
    } catch (error: unknown) {
      // Handle errors that might indicate auth issues
      if (this.isAuthError(error)) {
        log.warn(
          "Received auth error from Elasticsearch, refreshing client and retrying..."
        );
        await this.refreshClient(currentClient);
        return operation(this.client);
      }
      throw error;
    }
  }

  /**
   * Perform a search operation with automatic retry on auth failures.
   */
  async search<TDocument = unknown>(
    params: SearchRequest
  ): Promise<SearchResponse<TDocument>> {
    return this.executeWithRetry((client) => client.search<TDocument>(params));
  }

  /**
   * Perform a get operation with automatic retry on auth failures.
   */
  async get<TDocument = unknown>(
    params: GetRequest
  ): Promise<GetResponse<TDocument>> {
    return this.executeWithRetry((client) => client.get<TDocument>(params));
  }

  /**
   * Check cluster health with automatic retry on auth failures.
   */
  async clusterHealth(
    params?: ClusterHealthRequest
  ): Promise<ClusterHealthResponse> {
    return this.executeWithRetry((client) => client.cluster.health(params));
  }

  private isAuthError(error: unknown): boolean {
    if (error && typeof error === "object") {
      const statusCode = (error as { statusCode?: number }).statusCode;
      if (statusCode === 401 || statusCode === 403) {
        return true;
      }
      // Also check for meta.statusCode in ResponseError
      const meta = (error as { meta?: { statusCode?: number } }).meta;
      if (meta?.statusCode === 401 || meta?.statusCode === 403) {
        return true;
      }
    }
    return false;
  }

  private async refreshClient(failedClient: Client): Promise<void> {
    // If the client has already been refreshed by another request, don't refresh again
    if (this.client !== failedClient) {
      log.info("Elasticsearch client already refreshed by another request.");
      return;
    }

    // If a refresh is already in progress, wait for it
    if (this.refreshPromise) {
      log.info("Waiting for ongoing client refresh...");
      await this.refreshPromise;
      return;
    }

    // Check cooldown period
    const now = Date.now();
    if (now - this.lastRefreshTime < this.minRefreshIntervalMs) {
      log.warn(
        `Refresh requested too soon (last refresh ${
          now - this.lastRefreshTime
        }ms ago). ` +
          `Skipping, waiting on cooldown: ${this.minRefreshIntervalMs}ms`
      );
      return;
    }

    // Start the refresh process
    this.refreshPromise = this.doRefresh();
    try {
      await this.refreshPromise;
    } finally {
      this.refreshPromise = null;
    }
  }

  private async doRefresh(): Promise<void> {
    const oldClient = this.client;
    try {
      log.info("Refreshing Elasticsearch client...");
      this.client = await this.clientFactory();
      this.lastRefreshTime = Date.now();
      await oldClient.close();
      log.info("Elasticsearch client refreshed.");
    } catch (error) {
      log.error("Failed to refresh Elasticsearch client", error);
      throw error;
    }
  }
}

/**
 * Create a resilient Elasticsearch client with automatic retry on auth failures.
 */
export const createResilientClient = async (
  clientFactory: ClientFactory,
  minRefreshIntervalMs: number = 2000
): Promise<ResilientElasticClient> => {
  const initialClient = await clientFactory();
  return new ResilientElasticClient(
    initialClient,
    clientFactory,
    minRefreshIntervalMs
  );
};
