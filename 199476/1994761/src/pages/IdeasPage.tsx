import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { Link } from "@tanstack/react-router";
import { appService } from "../application/appService";
import { useAppNavigate } from "../application/navigation";
import { useAppStore } from "../application/store";
import type { Idea } from "../domain/types";
import { PublicLayout } from "../layouts/PublicLayout";
import { formatDate } from "../shared/lib/format";
import { errorMessage } from "../shared/lib/useAsyncAction";
import { useToast } from "../shared/ui/Toast";
import { Icon } from "../shared/ui/Icon";
import "../styles/pages/ideas.css";

interface PublicIdea extends Idea {
  author: string;
  owner: string;
}

export function IdeasPage() {
  const { data, user, sessionEmail } = useAppStore();
  const notify = useToast();
  const navigate = useAppNavigate();
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<"new" | "likes">("new");
  const [pendingLike, setPendingLike] = useState<string | null>(null);
  useEffect(() => {
    document.title = "点成｜助力小想法建站";
  }, []);
  const items = useMemo(() => {
    const result: PublicIdea[] = [];
    Object.entries(data.users).forEach(([owner, account]) =>
      account.ideas
        .filter((idea) => idea.type === "new" && idea.isPublic)
        .forEach((idea) =>
          result.push({ ...idea, author: account.name || "一位用户", owner }),
        ),
    );
    return result
      .filter((idea) => idea.text.toLowerCase().includes(query.toLowerCase()))
      .sort((a, b) =>
        sort === "likes"
          ? b.likedBy.length - a.likedBy.length
          : new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
      );
  }, [data.users, query, sort]);
  return (
    <div className="ideas-page global-shell-page">
      <PublicLayout>
        <main>
          <section className="hero">
            <div className="shell">
              <div className="eyebrow">Ideas</div>
              <h1>用户想法</h1>
              <p>为你认同的想法点赞</p>
            </div>
          </section>
          <section className="content">
            <div className="shell">
              <div className="toolbar">
                <div className="toolbar-inner">
                  <label className="search">
                    <span aria-hidden="true"><Icon name="search" /></span>
                    <input
                      aria-label="搜索想法"
                      type="search"
                      placeholder="搜索想法"
                      value={query}
                      onChange={(event) =>
                        setQuery(event.target.value.trimStart())
                      }
                    />
                  </label>
                  <div className="sort" aria-label="想法排序">
                    <button
                      className={sort === "new" ? "active" : ""}
                      type="button"
                      aria-pressed={sort === "new"}
                      onClick={() => setSort("new")}
                    >
                      最新
                    </button>
                    <button
                      className={sort === "likes" ? "active" : ""}
                      type="button"
                      aria-pressed={sort === "likes"}
                      onClick={() => setSort("likes")}
                    >
                      最多点赞
                    </button>
                  </div>
                </div>
              </div>
              <div className="result-line">共 {items.length} 个想法</div>
              <div className="grid">
                {items.length ? (
                  items.map((idea, index) => {
                    const liked = Boolean(
                      user && idea.likedBy.includes(sessionEmail),
                    );
                    return (
                      <article
                        className="card"
                        key={`${idea.owner}-${idea.id}`}
                        style={{ "--card-index": index } as CSSProperties}
                      >
                        <div className="card-top">
                          <div className="author">
                            <span className="avatar">
                              {idea.author.slice(0, 1)}
                            </span>
                            {idea.author}
                          </div>
                          <span className="status">
                            {idea.status || "待评估"}
                          </span>
                        </div>
                        <h2>{idea.text}</h2>
                        <div className="card-foot">
                          <span className="date">
                            {formatDate(idea.createdAt, false)}
                          </span>
                          <button
                            className={`like ${liked ? "liked" : ""}`}
                            type="button"
                            aria-pressed={liked}
                            disabled={pendingLike === idea.id}
                            onClick={async () => {
                              if (!user) {
                                void navigate("/login", { redirect: "/ideas" });
                                return;
                              }
                              setPendingLike(idea.id);
                              try {
                                const nowLiked = await appService.toggleLike(
                                  idea.owner,
                                  idea.id,
                                  sessionEmail,
                                );
                                notify(nowLiked ? "已点赞。" : "已取消点赞。");
                              } catch (error) {
                                notify(errorMessage(error, "操作失败，请稍后重试。"));
                              } finally {
                                setPendingLike(null);
                              }
                            }}
                          >
                            <span><Icon name={liked ? "heart-filled" : "heart"} /></span>
                            <b>{idea.likedBy.length}</b>
                          </button>
                        </div>
                      </article>
                    );
                  })
                ) : (
                  <div className="empty">
                    <b>暂时没有符合条件的想法</b>
                    <span>可以换个关键词，或者先提出一个新的。</span>
                    <br />
                    <Link className="button" to="/" hash="top">
                      提出想法
                    </Link>
                  </div>
                )}
              </div>
            </div>
          </section>
        </main>
      </PublicLayout>
    </div>
  );
}
