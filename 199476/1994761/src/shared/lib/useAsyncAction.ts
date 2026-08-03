import { useCallback, useState } from "react";

export function useAsyncAction() {
  const [isPending, setPending] = useState(false);
  const run = useCallback(
    async <T>(operation: () => Promise<T>): Promise<T> => {
      setPending(true);
      try {
        return await operation();
      } finally {
        setPending(false);
      }
    },
    [],
  );
  return { isPending, run };
}

export function errorMessage(
  error: unknown,
  fallback = "操作失败，请稍后重试。",
): string {
  return error instanceof Error && error.message ? error.message : fallback;
}
