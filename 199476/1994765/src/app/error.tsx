"use client";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="mx-auto flex max-w-6xl flex-col items-center px-4 py-24 text-center">
      <p className="text-6xl" aria-hidden>
        ⚠️
      </p>
      <h1 className="mt-4 text-2xl font-bold text-slate-900">页面出错了</h1>
      <p className="mt-2 max-w-md text-sm text-slate-500">
        {error.digest ? `错误编号：${error.digest}` : "发生了意外错误。"}
        如果反复出现，可能是内容数据问题，请运行 npm run content:check 排查。
      </p>
      <button
        onClick={reset}
        className="mt-6 rounded-xl bg-teal-600 px-5 py-2.5 font-medium text-white hover:bg-teal-700"
      >
        重试
      </button>
    </div>
  );
}
