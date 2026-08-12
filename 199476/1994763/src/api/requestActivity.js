let pendingCount = 0;
const listeners = new Set();

function emit() {
  listeners.forEach((listener) => listener());
}

export function beginRequest() {
  pendingCount += 1;
  emit();
}

export function endRequest() {
  pendingCount = Math.max(0, pendingCount - 1);
  emit();
}

export function subscribeRequestActivity(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getPendingRequestCount() {
  return pendingCount;
}
