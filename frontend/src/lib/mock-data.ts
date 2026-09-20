export type EndpointStatus = "ACTIVE" | "DISABLED";
export type EventStatus = "SUCCESS" | "FAILED" | "PENDING";
export type KeyStatus = "ACTIVE" | "REVOKED";

export interface Endpoint {
  id: string;
  target_url: string;
  description: string;
  secret_key: string;
  status: EndpointStatus;
  rate_limit_per_sec: number;
  timeout_ms: number;
  max_retries: number;
}

export interface ApiKey {
  id: string;
  key_name: string;
  key_prefix: string;
  status: KeyStatus;
  last_used_at: string | null;
  created_at: string;
}

export interface DeliveryAttempt {
  attempt: number;
  status_code: number | null;
  execution_time_ms: number;
  error_message: string | null;
  attempted_at: string;
  headers: Record<string, string>;
}

export interface WebhookEvent {
  id: string;
  event_type: string;
  endpoint_id: string;
  target_url: string;
  status: EventStatus;
  received_at: string;
  latency_ms: number;
  // attempts: DeliveryAttempt[];
  payload: Record<string, unknown>;
  attempts_count?: number;
}

export const metrics = {
  totalEvents: 1_284_930,
  successRate: 99.12,
  avgLatency: 143,
  activeEndpoints: 18,
  eventsDelta: "+12.4%",
  successDelta: "+0.3%",
  latencyDelta: "-8ms",
  endpointsDelta: "+2",
};

export const throughput = [
  { time: "00:00", delivered: 3120, failed: 22 },
  { time: "03:00", delivered: 2410, failed: 14 },
  { time: "06:00", delivered: 2980, failed: 31 },
  { time: "09:00", delivered: 5210, failed: 47 },
  { time: "12:00", delivered: 6890, failed: 62 },
  { time: "15:00", delivered: 7420, failed: 38 },
  { time: "18:00", delivered: 6310, failed: 51 },
  { time: "21:00", delivered: 4480, failed: 25 },
];

export const statusBreakdown = [
  { name: "Success", value: 27_412, key: "SUCCESS" as EventStatus },
  { name: "Failed", value: 291, key: "FAILED" as EventStatus },
  { name: "Pending", value: 138, key: "PENDING" as EventStatus },
];

export const endpoints: Endpoint[] = [
  {
    id: "ep_9f2a41c8",
    target_url: "https://api.acme-shop.com/hooks/orders",
    description: "Acme Shop order pipeline",
    secret_key: "whsec_7Yh2QpL9vTz0Kd1RmXcB4S",
    status: "ACTIVE",
    rate_limit_per_sec: 120,
    timeout_ms: 5000,
    max_retries: 5,
  },
  {
    id: "ep_3c7d0b19",
    target_url: "https://billing.northwind.io/webhooks/stripe",
    description: "Northwind billing sync",
    secret_key: "whsec_Kd83Lm09PqXzV1tRb7NfWa",
    status: "ACTIVE",
    rate_limit_per_sec: 60,
    timeout_ms: 8000,
    max_retries: 3,
  },
  {
    id: "ep_58ba2f77",
    target_url: "https://hooks.internal.svc/notify/slack",
    description: "Internal Slack notifier",
    secret_key: "whsec_Zm41Rt09YbNc2Ls8QpVd6H",
    status: "DISABLED",
    rate_limit_per_sec: 20,
    timeout_ms: 3000,
    max_retries: 2,
  },
  {
    id: "ep_c104e6a2",
    target_url: "https://analytics.lumen.dev/ingest/events",
    description: "Lumen analytics ingest",
    secret_key: "whsec_Qb72Nd10VmZx4Kt9LpRc3F",
    status: "ACTIVE",
    rate_limit_per_sec: 400,
    timeout_ms: 2500,
    max_retries: 4,
  },
  {
    id: "ep_7e91d4b5",
    target_url: "https://crm.fieldwork.co/api/v2/hook",
    description: "Fieldwork CRM contact sync",
    secret_key: "whsec_Lp39Vc82BnMz1Kt7QdRx5W",
    status: "ACTIVE",
    rate_limit_per_sec: 90,
    timeout_ms: 6000,
    max_retries: 6,
  },
];

export const apiKeys: ApiKey[] = [
  {
    id: "key_01",
    key_name: "Production gateway",
    key_prefix: "hs_live_4kQ9",
    status: "ACTIVE",
    last_used_at: "2026-09-17T10:41:02Z",
    created_at: "2026-02-11T08:12:44Z",
  },
  {
    id: "key_02",
    key_name: "Staging worker",
    key_prefix: "hs_test_7Zm2",
    status: "ACTIVE",
    last_used_at: "2026-09-16T22:03:19Z",
    created_at: "2026-04-02T13:55:10Z",
  },
  {
    id: "key_03",
    key_name: "Legacy migration script",
    key_prefix: "hs_live_1Ba8",
    status: "REVOKED",
    last_used_at: "2026-06-28T09:20:00Z",
    created_at: "2025-11-19T17:31:07Z",
  },
  {
    id: "key_04",
    key_name: "Partner sandbox",
    key_prefix: "hs_test_9Qd5",
    status: "ACTIVE",
    last_used_at: null,
    created_at: "2026-09-09T11:00:00Z",
  },
];

const eventTypes = [
  "order.created",
  "order.refunded",
  "invoice.paid",
  "invoice.payment_failed",
  "customer.updated",
  "subscription.canceled",
  "shipment.dispatched",
];

function payloadFor(type: string, i: number): Record<string, unknown> {
  const base = {
    id: `evt_${(1000 + i).toString(36)}`,
    type,
    created: 1789000000 + i * 617,
    livemode: i % 3 !== 0,
  };
  if (type.startsWith("order")) {
    return {
      ...base,
      data: {
        object: {
          order_id: `ord_${9000 + i}`,
          customer: { id: `cus_${300 + i}`, email: `buyer${i}@example.com` },
          amount_total: 1999 + i * 137,
          currency: "usd",
          items: [
            { sku: "HS-TSHIRT-01", qty: 1 + (i % 3), price: 1999 },
            { sku: "HS-STICKER-04", qty: 2, price: 499 },
          ],
        },
      },
    };
  }
  if (type.startsWith("invoice")) {
    return {
      ...base,
      data: {
        object: {
          invoice_id: `in_${4400 + i}`,
          subscription: `sub_${770 + i}`,
          amount_due: 4900,
          attempt_count: (i % 4) + 1,
          hosted_invoice_url: `https://pay.example.com/i/${4400 + i}`,
        },
      },
    };
  }
  return {
    ...base,
    data: {
      object: {
        resource_id: `res_${5500 + i}`,
        metadata: { region: i % 2 ? "eu-west-1" : "us-east-1", tier: "pro" },
      },
    },
  };
}

function attemptsFor(status: EventStatus, i: number, at: string): DeliveryAttempt[] {
  const headers = {
    "content-type": "application/json",
    "user-agent": "hook-shuttle/1.4",
    "x-hookshuttle-signature": `t=178900${i},v1=9f2c${i}a7b41e0d`,
    "x-hookshuttle-delivery": `dlv_${7000 + i}`,
  };
  if (status === "SUCCESS") {
    const retried = i % 5 === 0;
    return [
      ...(retried
        ? [
            {
              attempt: 1,
              status_code: 502,
              execution_time_ms: 3021,
              error_message: "Bad gateway from upstream",
              attempted_at: at,
              headers,
            },
          ]
        : []),
      {
        attempt: retried ? 2 : 1,
        status_code: 200,
        execution_time_ms: 88 + (i % 7) * 13,
        error_message: null,
        attempted_at: at,
        headers,
      },
    ];
  }
  if (status === "FAILED") {
    return [1, 2, 3].map((n) => ({
      attempt: n,
      status_code: n === 3 ? null : 500,
      execution_time_ms: n === 3 ? 5000 : 412 + n * 90,
      error_message:
        n === 3 ? "Timeout after 5000ms (no response)" : "Internal Server Error from target",
      attempted_at: at,
      headers,
    }));
  }
  return [
    {
      attempt: 1,
      status_code: null,
      execution_time_ms: 0,
      error_message: null,
      attempted_at: at,
      headers,
    },
  ];
}

export const webhookEvents: WebhookEvent[] = Array.from({ length: 42 }, (_, i) => {
  const status: EventStatus = i % 11 === 3 ? "FAILED" : i % 17 === 5 ? "PENDING" : "SUCCESS";
  const endpoint = endpoints[i % endpoints.length]!;
  const type = eventTypes[i % eventTypes.length]!;
  const received = new Date(Date.UTC(2026, 8, 17, 10, 55) - i * 7 * 60 * 1000).toISOString();
  return {
    id: `evt_${(0x5f2a01 + i * 7331).toString(16)}`,
    event_type: type,
    endpoint_id: endpoint.id,
    target_url: endpoint.target_url,
    status,
    received_at: received,
    latency_ms: status === "PENDING" ? 0 : 74 + ((i * 37) % 480),
    attempts: attemptsFor(status, i, received),
    payload: payloadFor(type, i),
  };
});

export const eventTypeOptions = eventTypes;

export function randomKeyMaterial(live: boolean) {
  const chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  let out = "";
  for (let i = 0; i < 32; i++) out += chars[Math.floor(Math.random() * chars.length)];
  return `hs_${live ? "live" : "test"}_${out}`;
}
