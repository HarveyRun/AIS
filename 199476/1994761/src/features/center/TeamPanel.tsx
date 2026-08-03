import { useRef, useState } from "react";
import { appService } from "../../application/appService";
import { useAppStore } from "../../application/store";
import { fileApi } from "../../api/client";
import {
  formatBytes,
  formatDate,
  makeId,
  statusClass,
} from "../../shared/lib/format";
import { useToast } from "../../shared/ui/Toast";
import { errorMessage, useAsyncAction } from "../../shared/lib/useAsyncAction";

export function TeamPanel() {
  const { user, sessionEmail } = useAppStore();
  const notify = useToast();
  const fileRef = useRef<HTMLInputElement>(null);
  const [skill, setSkill] = useState("");
  const [time, setTime] = useState("偶尔参与");
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState("");
  const { isPending, run } = useAsyncAction();
  if (!user) return null;
  const application = user.teamApplication;
  return (
    <section className="panel active">
      <p className="panel-lead">
        加入我们指成为平台的长期合作者。提交申请时必须上传简历。
      </p>
      <div>
        {application ? (
          <div className="application">
            <span className={`status ${statusClass(application.status)}`}>
              {application.status || "待审核"}
            </span>
            <h2>团队申请已提交</h2>
            <p>
              可以负责：{application.skill}
              <br />
              可投入时间：{application.time}
              <br />
              简历：{application.resumeName}
              <br />
              提交时间：{formatDate(application.createdAt)}
            </p>
            {application.resumeId && (
              <button
                className="text-button"
                type="button"
                onClick={async () => {
                  const found = await fileApi.download(
                    application.resumeId!,
                    application.resumeName || "简历.pdf",
                  );
                  if (!found) notify("没有找到已上传的简历文件。");
                }}
              >
                下载简历
              </button>
            )}{" "}
            <button
              className="text-button danger"
              type="button"
              disabled={isPending}
              onClick={async () => {
                if (!window.confirm("确定撤回团队申请？")) return;
                try {
                  await run(async () => {
                    await appService.submitTeamApplication(sessionEmail, null);
                  });
                  notify("申请已撤回。");
                } catch (reason) {
                  notify(errorMessage(reason, "申请撤回失败，请稍后重试。"));
                }
              }}
            >
              撤回申请
            </button>
          </div>
        ) : (
          <form
            className="form-card"
            onSubmit={async (event) => {
              event.preventDefault();
              if (skill.trim().length < 2) {
                setError("请填写可以负责的内容。");
                return;
              }
              if (!file) {
                setError("必须上传简历。");
                return;
              }
              if (
                !/\.pdf$/i.test(file.name) ||
                (file.type && file.type !== "application/pdf")
              ) {
                setError("简历只支持 PDF 格式。");
                return;
              }
              if (file.size > 20 * 1024 * 1024) {
                setError("简历不能超过 20 MB。");
                return;
              }
              setError("");
              const resumeId = makeId("resume");
              try {
                await run(async () => {
                  await fileApi.upload({
                    id: resumeId,
                    email: sessionEmail,
                    kind: "resume",
                    blob: file,
                  });
                  try {
                    await appService.submitTeamApplication(sessionEmail, {
                      skill: skill.trim(),
                      time,
                      resumeId,
                      resumeName: file.name,
                      resumeSize: file.size,
                      status: "待审核",
                      createdAt: new Date().toISOString(),
                    });
                  } catch (reason) {
                    await fileApi.remove(resumeId).catch(() => undefined);
                    throw reason;
                  }
                });
              } catch (reason) {
                setError(errorMessage(reason, "申请提交失败，请稍后重试。"));
                return;
              }
              notify("团队申请已提交。");
            }}
          >
            <div className="grid">
              <div className="field">
                <label htmlFor="team-skill">你可以负责什么</label>
                <input
                  id="team-skill"
                  maxLength={60}
                  required
                  placeholder="例如：视觉设计"
                  value={skill}
                  onChange={(event) => setSkill(event.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="team-time">可投入时间</label>
                <select
                  id="team-time"
                  value={time}
                  onChange={(event) => setTime(event.target.value)}
                >
                  <option>偶尔参与</option>
                  <option>每周少量时间</option>
                  <option>可以稳定参与</option>
                </select>
              </div>
              <div className="field full">
                <label>上传简历</label>
                <div className="file-drop">
                  <input
                    ref={fileRef}
                    type="file"
                    accept=".pdf,application/pdf"
                    required
                    onChange={(event) =>
                      setFile(event.target.files?.[0] || null)
                    }
                  />
                  <div>
                    <b>
                      {file
                        ? `${file.name} · ${formatBytes(file.size)}`
                        : "点击选择简历"}
                    </b>
                    <span>仅支持 PDF，20 MB 以内</span>
                  </div>
                </div>
              </div>
            </div>
            <div className="form-error">{error}</div>
            <button className="button primary" type="submit" disabled={isPending}>
              {isPending ? "提交中…" : "提交申请"}
            </button>
          </form>
        )}
      </div>
    </section>
  );
}
