const BASE = '/api'

async function handleResponse(res) {
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.json()
}

export const orderApi = {
  createOrder: (payload) =>
    fetch(`${BASE}/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }).then(handleResponse),

  getAllOrders: () =>
    fetch(`${BASE}/orders`).then(handleResponse),

  getOrderLogs: (orderId) =>
    fetch(`${BASE}/orders/${orderId}/logs`).then(handleResponse),
}

export const chaosApi = {
  getStatus: () =>
    fetch(`${BASE}/chaos`).then(handleResponse),

  toggle: () =>
    fetch(`${BASE}/chaos/toggle`, { method: 'POST' }).then(handleResponse),
}
