import { useEffect, useMemo } from "react";
import { Link } from "@tanstack/react-router";
import { useAppStore } from "../application/store";
import type { Idea } from "../domain/types";
import { PublicLayout } from "../layouts/PublicLayout";
import { Icon } from "../shared/ui/Icon";
import "../styles/pages/cases.css";

interface CaseIdea extends Idea {
  owner: string;
}

function cover(text: string): string {
  const palettes = [
    ["#5c55e8", "#a178ff", "#edf0ff"],
    ["#173f47", "#4ba3a0", "#e4f4ef"],
    ["#33235d", "#e1739c", "#fae7ee"],
    ["#19345d", "#4d80df", "#e5edff"],
  ];
  let hash = 0;
  for (let index = 0; index < text.length; index += 1)
    hash = (Math.imul(hash, 31) + text.charCodeAt(index)) | 0;
  const palette = palettes[Math.abs(hash) % palettes.length]!;
  const safe = (text.length > 22 ? `${text.slice(0, 22)}…` : text).replace(
    /[&<>"']/g,
    "",
  );
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="675" viewBox="0 0 1200 675"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop stop-color="${palette[0]}"/><stop offset=".62" stop-color="${palette[1]}"/><stop offset="1" stop-color="${palette[2]}"/></linearGradient></defs><rect width="1200" height="675" rx="42" fill="url(#g)"/><rect x="120" y="105" width="960" height="465" rx="30" fill="#fff" opacity=".92"/><circle cx="161" cy="146" r="9" fill="#ff766e"/><circle cx="191" cy="146" r="9" fill="#ffc55d"/><circle cx="221" cy="146" r="9" fill="#5bd184"/><text x="155" y="310" fill="#171724" font-family="Arial,Microsoft YaHei,sans-serif" font-size="42" font-weight="700">${safe}</text><rect x="155" y="365" width="590" height="14" rx="7" fill="#171724" opacity=".1"/><rect x="155" y="400" width="440" height="14" rx="7" fill="#171724" opacity=".07"/></svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

export function CasesPage() {
  const { data } = useAppStore();
  useEffect(() => {
    document.title = "点成｜助力小想法建站";
  }, []);
  const cases = useMemo(() => {
    const result: CaseIdea[] = [];
    Object.entries(data.users).forEach(([owner, user]) =>
      user.ideas.forEach((idea) => {
        if (idea.type === "new" && idea.isPublic && idea.status === "已完成")
          result.push({ ...idea, owner });
      }),
    );
    return result.sort(
      (a, b) =>
        new Date(b.updatedAt || b.createdAt).getTime() -
        new Date(a.updatedAt || a.createdAt).getTime(),
    );
  }, [data.users]);
  return (
    <div className="cases-page global-shell-page">
      <PublicLayout active="cases">
        <main>
          <section className="hero">
            <div className="shell hero-inner">
              <div className="eyebrow">Cases</div>
              <h1>案例列表</h1>
            </div>
          </section>
          <section className="content">
            <div className="shell">
              <div className="grid">
                {cases.length ? (
                  cases.map((item) => (
                    <article
                      className="case-card"
                      key={`${item.owner}-${item.id}`}
                    >
                      <a
                        className="case-link"
                        href="https://google.com"
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label={`访问案例：${item.text}`}
                      >
                        <div className="cover">
                          <img
                            src={cover(item.text)}
                            alt={`${item.text}的案例图片`}
                          />
                          <span className="visit"><Icon name="arrow-up-right" /></span>
                        </div>
                        <div className="body">
                          <h2>{item.text}</h2>
                          <div className="details">
                            <div className="detail">
                              <small>评估等级</small>
                              <strong>
                                {item.level &&
                                item.level >= 1 &&
                                item.level <= 6
                                  ? `${item.level} 级`
                                  : "待评估"}
                              </strong>
                            </div>
                            <div className="detail">
                              <small>费用</small>
                              <strong>
                                {item.fee > 0
                                  ? `¥${item.fee.toFixed(2)}`
                                  : item.level && item.level <= 3
                                    ? "¥5/月"
                                    : "待确认"}
                              </strong>
                            </div>
                          </div>
                        </div>
                      </a>
                    </article>
                  ))
                ) : (
                  <div className="empty">
                    <div className="mark"><Icon name="diamond" /></div>
                    <h2>暂时没有已完成案例</h2>
                    <Link className="button" to="/" hash="top">
                      返回首页
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
