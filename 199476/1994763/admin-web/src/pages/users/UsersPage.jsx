import { useEffect, useState } from 'react';
import {
  Search,
  ShieldBan,
  ShieldCheck,
  X,
} from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import Pagination from '../../components/data/Pagination.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './UsersPage.css';

const PAGE_SIZE = 20;
const EMPTY_PENALTY = {
  duration: 'DAYS_3',
  reason: '',
};

const PENALTY_DURATIONS = [
  { value: 'DAYS_3', label: '3天' },
  { value: 'DAYS_7', label: '7天' },
  { value: 'DAYS_15', label: '15天' },
  { value: 'PERMANENT', label: '永久' },
];

export default function UsersPage() {
  const [data, setData] = useState({ items: [], total: 0 });
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [penaltyTarget, setPenaltyTarget] = useState(null);
  const [restoreTarget, setRestoreTarget] = useState(null);
  const [penalty, setPenalty] = useState(EMPTY_PENALTY);
  const [saving, setSaving] = useState(false);

  const load = async (targetPage = page) => {
    try {
      const query = new URLSearchParams({
        keyword,
        status,
        page: targetPage,
        size: PAGE_SIZE,
      });
      const result = await adminApi.users(query.toString());
      setData(result);
      setPage(targetPage);
    } catch (error) {
      message.error(error.message);
    }
  };

  useEffect(() => {
    load(0);
  }, [status]);

  const openPenalty = (user) => {
    setPenaltyTarget(user);
    setPenalty(EMPTY_PENALTY);
  };

  const closePenalty = () => {
    if (saving) return;
    setPenaltyTarget(null);
    setPenalty(EMPTY_PENALTY);
  };

  const submitPenalty = async (event) => {
    event.preventDefault();
    if (!penaltyTarget || saving) return;
    const reason = penalty.reason.trim();
    if (!reason) {
      message.warning('请填写处罚原因');
      return;
    }
    setSaving(true);
    try {
      await adminApi.userStatus(penaltyTarget.id, {
        status: 'SUSPENDED',
        duration: penalty.duration,
        reason,
      });
      setPenaltyTarget(null);
      setPenalty(EMPTY_PENALTY);
      message.success('处罚已生效');
      await load(page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const restore = async () => {
    if (!restoreTarget || saving) return;
    setSaving(true);
    try {
      await adminApi.userStatus(restoreTarget.id, {
        status: 'ACTIVE',
      });
      setRestoreTarget(null);
      message.success('账号已解除封禁');
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
          <h1>用户管理</h1>
          <p>查看用户资料、账户状态并处理违规行为</p>
        </div>
        <span>共 {data.total} 人</span>
      </div>

      <div className="toolbar">
        <label>
          <Search />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && load(0)}
            placeholder="搜索UID、手机号或昵称"
          />
        </label>
        <select
          value={status}
          onChange={(event) => setStatus(event.target.value)}
        >
          <option value="">全部状态</option>
          <option value="ACTIVE">正常</option>
          <option value="SUSPENDED">封禁中</option>
        </select>
        <button
          type="button"
          onClick={() => load(0)}
        >
          查询
        </button>
      </div>

      <div className="table-card user-table-card">
        <table>
          <thead>
            <tr>
              <th>用户</th>
              <th>手机号</th>
              <th>答主状态</th>
              <th>账户状态</th>
              <th>可用 / 冻结</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((user) => (
              <tr key={user.id}>
                <td>
                  <b>{user.nickname || `UID ${user.uid}`}</b>
                  <small>UID {user.uid}</small>
                </td>
                <td>{user.phone}</td>
                <td>
                  <Status value={user.answererStatus} />
                </td>
                <td>
                  <AccountStatus user={user} />
                </td>
                <td>
                  ¥{user.availableBalance} / ¥{user.frozenBalance}
                </td>
                <td>{date(user.createdAt)}</td>
                <td>
                  {user.accountStatus === 'ACTIVE' ? (
                    <button
                      className="danger user-action"
                      type="button"
                      onClick={() => openPenalty(user)}
                    >
                      <ShieldBan />
                      违规处理
                    </button>
                  ) : (
                    <button
                      className="plain user-action"
                      type="button"
                      onClick={() => setRestoreTarget(user)}
                    >
                      <ShieldCheck />
                      解除封禁
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <Empty />}
        <Pagination
          page={page}
          size={PAGE_SIZE}
          total={data.total}
          onChange={load}
        />
      </div>

      {penaltyTarget && (
        <div className="user-penalty-layer" role="presentation">
          <button
            className="user-penalty-mask"
            type="button"
            aria-label="关闭"
            onClick={closePenalty}
          />
          <section
            className="user-penalty-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="penalty-title"
          >
            <header>
              <div className="user-penalty-icon">
                <ShieldBan />
              </div>
              <button
                type="button"
                aria-label="关闭"
                onClick={closePenalty}
              >
                <X />
              </button>
            </header>
            <div className="user-penalty-heading">
              <h2 id="penalty-title">违规处理</h2>
              <p>
                {penaltyTarget.nickname || `UID ${penaltyTarget.uid}`}
                <span>UID {penaltyTarget.uid}</span>
              </p>
            </div>
            <form onSubmit={submitPenalty}>
              <fieldset>
                <legend>封禁时长</legend>
                <div className="user-penalty-durations">
                  {PENALTY_DURATIONS.map((item) => (
                    <button
                      key={item.value}
                      className={penalty.duration === item.value ? 'selected' : ''}
                      type="button"
                      onClick={() => setPenalty((current) => ({
                        ...current,
                        duration: item.value,
                      }))}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
              </fieldset>
              <label>
                <span>处罚原因</span>
                <textarea
                  autoFocus
                  maxLength="300"
                  rows="5"
                  placeholder="请填写用户违反的规则及处罚说明"
                  value={penalty.reason}
                  onChange={(event) => setPenalty((current) => ({
                    ...current,
                    reason: event.target.value,
                  }))}
                />
                <small>{penalty.reason.length}/300</small>
              </label>
              <p className="user-penalty-notice">
                确认后立即生效，该用户会退出登录并看到处罚内容。
              </p>
              <footer>
                <button
                  className="plain"
                  type="button"
                  disabled={saving}
                  onClick={closePenalty}
                >
                  取消
                </button>
                <button
                  className="danger-confirm"
                  type="submit"
                  disabled={saving}
                >
                  {saving ? '处理中…' : '确认处罚'}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(restoreTarget)}
        title="解除账号封禁"
        message={`确认恢复“${restoreTarget?.nickname || `UID ${restoreTarget?.uid || ''}`}”的登录和使用权限吗？`}
        confirmText="解除封禁"
        busy={saving}
        onCancel={() => setRestoreTarget(null)}
        onConfirm={restore}
      />
    </>
  );
}

function AccountStatus({ user }) {
  if (user.accountStatus === 'ACTIVE') {
    return <span className="status active">正常</span>;
  }

  return (
    <div className="user-penalty-status">
      <span className="status suspended">
        {user.banUntil ? '限期封禁' : '永久封禁'}
      </span>
      <small>
        {user.banUntil ? `至 ${date(user.banUntil)}` : user.banReason || '违反平台规则'}
      </small>
    </div>
  );
}

export const date = (value) => (
  value ? new Date(value).toLocaleString() : '—'
);

export const Status = ({ value }) => (
  <span className={`status ${String(value).toLowerCase()}`}>
    {{
      ACTIVE: '正常',
      SUSPENDED: '已停用',
      APPROVED: '已通过',
      NOT_APPLIED: '未申请',
      PENDING: '待处理',
      PROCESSING: '处理中',
      COMPLETED: '已完成',
      REJECTED: '已驳回',
      FAILED: '失败',
      SUBMITTED: '待处理',
      RESOLVED: '已解决',
      CLOSED: '已关闭',
      REFUNDED: '已退款',
      SETTLED: '已结算',
      CANCELLED: '已撤销',
      EXPIRED: '已过期',
      AWAITING_CONFIRMATION: '待确认结束',
    }[value] || value || '—'}
  </span>
);

export const Empty = () => <div className="empty">暂无数据</div>;
