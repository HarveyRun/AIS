import { useEffect, useState } from 'react';
import { Eye, Search, X } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import { date, Empty, Status } from '../users/UsersPage.jsx';
import '../shared/Page.css';
import './ContentContributionsPage.css';

const PAGE_SIZE = 20;
const EMPTY_REVIEW = {
  decision: 'ADOPTED',
  standardMatterName: '',
  rewardAmount: 1,
  reason: '',
  violationLevel: 9,
};

export default function ContentContributionsPage() {
  const { can } = useAdminAccess();
  const [data, setData] = useState({ items: [], total: 0 });
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [status, setStatus] = useState('PENDING');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [review, setReview] = useState(EMPTY_REVIEW);
  const [confirming, setConfirming] = useState(false);
  const [saving, setSaving] = useState(false);
  const [original, setOriginal] = useState('');

  useEffect(() => {
    load(0, appliedKeyword, status);
  }, [status]);

  const load = async (nextPage = page, nextKeyword = appliedKeyword, nextStatus = status) => {
    try {
      const result = await adminApi.contentContributions({
        keyword: nextKeyword,
        status: nextStatus,
        page: nextPage,
        size: PAGE_SIZE,
      });
      setData(result);
      setPage(nextPage);
    } catch (error) {
      message.error(error.message);
    }
  };

  const search = async (event) => {
    event.preventDefault();
    const next = keyword.trim();
    setAppliedKeyword(next);
    await load(0, next, status);
  };

  const open = async (item) => {
    try {
      const detail = await adminApi.contentContribution(item.id);
      setSelected(detail);
      setReview({ ...EMPTY_REVIEW, standardMatterName: detail.matterName });
      setOriginal('');
    } catch (error) {
      message.error(error.message);
    }
  };

  const viewOriginal = async () => {
    if (!selected) return;
    try {
      const result = await adminApi.contentContributionOriginal(selected.id);
      setOriginal(formatOriginal(result.rawPayloadJson));
    } catch (error) {
      message.error(error.message);
    }
  };

  const validate = () => {
    if (review.decision === 'ADOPTED' && !review.standardMatterName.trim()) {
      message.warning('请输入对应的标准事项名称');
      return false;
    }
    if (review.decision !== 'ADOPTED' && !review.reason.trim()) {
      message.warning('请填写本次审核原因');
      return false;
    }
    return true;
  };

  const submit = async () => {
    if (!selected || saving) return;
    try {
      setSaving(true);
      const body = {
        decision: review.decision,
        standardMatterName:
          review.decision === 'ADOPTED' ? review.standardMatterName.trim() : null,
        rewardAmount: review.decision === 'ADOPTED' ? review.rewardAmount : null,
        reason: review.reason.trim(),
        violationLevel:
          review.decision === 'VIOLATION_REJECTED' ? review.violationLevel : null,
      };
      if (review.decision === 'VIOLATION_REJECTED') {
        await adminApi.reviewContentContributionViolation(selected.id, body);
      } else {
        await adminApi.reviewContentContribution(selected.id, body);
      }
      message.success(review.decision === 'ADOPTED' ? '已采用并发放奖励' : '审核已完成');
      setConfirming(false);
      setSelected(null);
      await load(page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>内容共建</h1>
          <p>用户提交的是候选内容；审核结论按整条提交统一处理，不会直接写入线上事项或岗位</p>
        </div>
        <span>共 {data.total} 条</span>
      </div>

      <form className="toolbar" onSubmit={search}>
        <label>
          <Search aria-hidden="true" />
          <input
            value={keyword}
            maxLength={40}
            placeholder="用户UID、手机号或事情名称"
            onChange={(event) => setKeyword(event.target.value)}
          />
        </label>
        <select value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="">全部状态</option>
          <option value="PENDING">待审核</option>
          <option value="ADOPTED">已采用</option>
          <option value="REJECTED">未采用</option>
          <option value="VIOLATION_REJECTED">违规驳回</option>
        </select>
        <button type="submit">查询</button>
      </form>

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>提交用户</th>
              <th>候选事项</th>
              <th>岗位</th>
              <th>状态</th>
              <th>奖励</th>
              <th>提交时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((item) => (
              <tr key={item.id}>
                <td>
                  <b>{item.userName}</b>
                  <small>UID {item.userUid} · {item.userPhone}</small>
                </td>
                <td><b>{item.matterName}</b></td>
                <td>{item.jobs.length} 个</td>
                <td><Status value={item.status} /></td>
                <td>{item.rewardAmount ? `¥${formatAmount(item.rewardAmount)}` : '-'}</td>
                <td>{date(item.createdAt)}</td>
                <td className="row-actions">
                  <button type="button" className="plain" onClick={() => open(item)}>
                    {item.status === 'PENDING' && can('CONTENT_CONTRIBUTION_REVIEW') ? '审核' : '查看'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <Empty />}
      </div>

      <Pagination page={page} size={PAGE_SIZE} total={data.total} onChange={load} />

      {selected && (
        <>
          <button
            type="button"
            className="modal-mask"
            aria-label="关闭"
            onClick={() => !saving && setSelected(null)}
          />
          <section className="detail-modal contribution-drawer" role="dialog" aria-modal="true">
            <header>
              <div>
                <h2>{selected.status === 'PENDING' ? '审核共建内容' : '共建内容详情'}</h2>
                <p>提交记录 #{selected.id}</p>
              </div>
              <button type="button" aria-label="关闭" onClick={() => setSelected(null)}>
                <X />
              </button>
            </header>

            <div className="detail-fields">
              <div><span>提交用户</span><b>{selected.userName}</b></div>
              <div><span>UID / 手机号</span><b>{selected.userUid} / {selected.userPhone}</b></div>
              <div><span>候选事项</span><b>{selected.matterName}</b></div>
              <div><span>状态</span><b>{statusLabel(selected.status)}</b></div>
            </div>

            <div className="contribution-section-title">
              <h3>用户填写的岗位（{selected.jobs.length}）</h3>
              {selected.status === 'PENDING' && can('CONTENT_CONTRIBUTION_VIOLATION') && (
                <button type="button" className="original-button" onClick={viewOriginal}>
                  <Eye />查看原始内容
                </button>
              )}
            </div>
            <div className="contribution-jobs">
              {selected.jobs.map((job, index) => (
                <article key={job.id}>
                  <span>{String(index + 1).padStart(2, '0')}</span>
                  <div>
                    <b>{job.jobName}</b>
                    <p>{job.responsibility}</p>
                  </div>
                </article>
              ))}
            </div>

            {original && (
              <div className="contribution-original">
                <b>敏感词处理前的原始内容</b>
                <pre>{original}</pre>
              </div>
            )}

            {selected.status === 'PENDING' && can('CONTENT_CONTRIBUTION_REVIEW') ? (
              <div className="contribution-review">
                <h3>整条内容统一审核</h3>
                <div className="contribution-decisions">
                  {[
                    ['ADOPTED', '采用'],
                    ['REJECTED', '不采用'],
                    ...(can('CONTENT_CONTRIBUTION_VIOLATION')
                      ? [['VIOLATION_REJECTED', '违规驳回']]
                      : []),
                  ].map(([value, label]) => (
                    <button
                      type="button"
                      key={value}
                      className={review.decision === value ? 'selected' : ''}
                      onClick={() => setReview((current) => ({ ...current, decision: value }))}
                    >
                      {label}
                    </button>
                  ))}
                </div>

                {review.decision === 'ADOPTED' && (
                  <>
                    <label>
                      <span>对应标准事项</span>
                      <input
                        value={review.standardMatterName}
                        maxLength={40}
                        placeholder="用于判断同一用户是否已因该事项获得奖励"
                        onChange={(event) =>
                          setReview((current) => ({
                            ...current,
                            standardMatterName: event.target.value,
                          }))
                        }
                      />
                    </label>
                    <label>
                      <span>内容共建奖励</span>
                      <div className="contribution-rewards">
                        {[1, 2, 3].map((amount) => (
                          <button
                            type="button"
                            key={amount}
                            className={review.rewardAmount === amount ? 'selected' : ''}
                            onClick={() => setReview((current) => ({
                              ...current,
                              rewardAmount: amount,
                            }))}
                          >
                            {amount}元
                          </button>
                        ))}
                      </div>
                    </label>
                  </>
                )}

                {review.decision === 'VIOLATION_REJECTED' && (
                  <label>
                    <span>违规等级</span>
                    <select
                      value={review.violationLevel}
                      onChange={(event) => setReview((current) => ({
                        ...current,
                        violationLevel: Number(event.target.value),
                      }))}
                    >
                      {Array.from({ length: 10 }, (_, level) => (
                        <option value={level} key={level}>{level}级</option>
                      ))}
                    </select>
                  </label>
                )}

                <label>
                  <span>{review.decision === 'ADOPTED' ? '审核备注（选填）' : '审核原因'}</span>
                  <textarea
                    value={review.reason}
                    maxLength={500}
                    placeholder={review.decision === 'ADOPTED' ? '可填写内部审核说明' : '请填写明确原因'}
                    onChange={(event) =>
                      setReview((current) => ({ ...current, reason: event.target.value }))
                    }
                  />
                </label>
                <p className="contribution-final-tip">审核确定后不能再次修改，请确认整条内容后统一处理。</p>
                <footer>
                  <button type="button" className="primary" onClick={() => validate() && setConfirming(true)}>
                    确认审核结果
                  </button>
                </footer>
              </div>
            ) : (
              <ReviewResult item={selected} />
            )}
          </section>
        </>
      )}

      <ConfirmDialog
        open={confirming}
        title="确认完成本次审核？"
        message={`本次将整条内容标记为“${decisionLabel(review.decision)}”，确定后不能修改。`}
        confirmText="确定提交"
        danger={review.decision === 'VIOLATION_REJECTED'}
        busy={saving}
        onCancel={() => !saving && setConfirming(false)}
        onConfirm={submit}
      />
    </>
  );
}

function ReviewResult({ item }) {
  return (
    <div className="contribution-result">
      <h3>审核结果</h3>
      <div className="detail-fields">
        {item.standardMatterName && <div><span>对应标准事项</span><b>{item.standardMatterName}</b></div>}
        {item.rewardAmount && <div><span>奖励</span><b>¥{formatAmount(item.rewardAmount)}</b></div>}
        {item.violationLevel !== null && item.violationLevel !== undefined && (
          <div><span>违规等级</span><b>{item.violationLevel}级</b></div>
        )}
        {item.reviewedBy && <div><span>审核人</span><b>{item.reviewedBy}</b></div>}
        {item.reviewedAt && <div><span>审核时间</span><b>{date(item.reviewedAt)}</b></div>}
      </div>
      {item.reviewReason && <p className="contribution-result-reason">{item.reviewReason}</p>}
    </div>
  );
}

function formatOriginal(raw) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw || '';
  }
}

function formatAmount(value) {
  const number = Number(value || 0);
  return Number.isInteger(number) ? String(number) : number.toFixed(2).replace(/0+$/, '');
}

function statusLabel(status) {
  return {
    PENDING: '待审核',
    ADOPTED: '已采用',
    REJECTED: '未采用',
    VIOLATION_REJECTED: '违规驳回',
  }[status] || status;
}

function decisionLabel(decision) {
  return {
    ADOPTED: '采用',
    REJECTED: '不采用',
    VIOLATION_REJECTED: '违规驳回',
  }[decision] || decision;
}
