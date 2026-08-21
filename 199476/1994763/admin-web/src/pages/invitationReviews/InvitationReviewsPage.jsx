import { useEffect, useState } from 'react';
import { ExternalLink, Search, X } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import { date, Empty, Status } from '../users/UsersPage.jsx';
import '../shared/Page.css';
import './InvitationReviewsPage.css';

const PAGE_SIZE = 20;

export default function InvitationReviewsPage() {
  const { can } = useAdminAccess();
  const [data, setData] = useState({ items: [], total: 0 });
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [status, setStatus] = useState('PENDING');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [materials, setMaterials] = useState([]);
  const [reason, setReason] = useState('');
  const [processing, setProcessing] = useState(false);
  const [handheldPreview, setHandheldPreview] = useState(null);

  useEffect(() => {
    load(0, appliedKeyword, status);
  }, [status]);

  const load = async (nextPage = page, nextKeyword = appliedKeyword, nextStatus = status) => {
    try {
      const result = await adminApi.invitationReviews({
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
    const nextKeyword = keyword.trim();
    setAppliedKeyword(nextKeyword);
    await load(0, nextKeyword, status);
  };

  const open = async (item) => {
    setSelected(item);
    setMaterials([]);
    setReason('');
    try {
      setMaterials(await adminApi.invitationIdentityMaterials(item.id));
    } catch (error) {
      message.error(error.message);
    }
  };

  const review = async (approved) => {
    if (!selected || processing) return;
    if (!approved && !reason.trim()) {
      message.warning('驳回时请填写原因');
      return;
    }
    try {
      setProcessing(true);
      await adminApi.reviewInvitation(selected.id, approved, reason.trim());
      message.success(approved ? '邀请已通过，红包已发放' : '邀请已驳回');
      setSelected(null);
      await load(page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setProcessing(false);
    }
  };

  const previewInviteeHandheld = async (item) => {
    try {
      const material = await adminApi.invitationInviteeHandheldMaterial(item.id);
      setHandheldPreview({ item, material });
    } catch (error) {
      message.error(error.message);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>邀请审核</h1>
          <p>核对邀请人 UID、真实姓名和实名认证材料，通过后发放红包</p>
        </div>
        <span>共 {data.total} 条</span>
      </div>

      <form className="toolbar" onSubmit={search}>
        <label>
          <Search aria-hidden="true" />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="邀请人/受邀人UID、手机号或真实姓名"
            maxLength={30}
          />
        </label>
        <select value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="">全部状态</option>
          <option value="PENDING">待审核</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已驳回</option>
        </select>
        <button type="submit">查询</button>
      </form>

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>邀请人</th>
              <th>受邀人</th>
              <th>红包</th>
              <th>状态</th>
              <th>提交时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((item) => (
              <tr key={item.id}>
                <td>
                  <b>{item.inviterRealName}</b>
                  <small>UID {item.inviterUid} · {item.inviterPhone}</small>
                </td>
                <td>
                  <button
                    type="button"
                    className="invitee-identity-link"
                    onClick={() => previewInviteeHandheld(item)}
                  >
                    <b>{item.inviteeNickname || `UID ${item.inviteeUid}`}</b>
                    <small>UID {item.inviteeUid} · {item.inviteePhone}</small>
                  </button>
                </td>
                <td>¥{formatAmount(item.rewardAmount)}</td>
                <td><Status value={item.status} /></td>
                <td>{date(item.createdAt)}</td>
                <td className="row-actions">
                  <button type="button" className="plain" onClick={() => open(item)}>
                    {item.status === 'PENDING' && can('INVITATION_REVIEW') ? '审核' : '查看'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <Empty />}
      </div>

      <Pagination
        page={page}
        size={PAGE_SIZE}
        total={data.total}
        onChange={(nextPage) => load(nextPage)}
      />

      {selected && (
        <>
          <div className="modal-mask" onClick={() => setSelected(null)} />
          <section className="detail-modal invitation-review-drawer" role="dialog" aria-modal="true">
            <header>
              <div>
                <h2>邀请审核</h2>
                <p>记录 #{selected.id}</p>
              </div>
              <button type="button" aria-label="关闭" onClick={() => setSelected(null)}>
                <X />
              </button>
            </header>

            <div className="invitation-review-name">
              <span>用户填写的对方真实姓名</span>
              <strong>{selected.inviterRealName}</strong>
            </div>

            <div className="detail-fields">
              <div><span>邀请人 UID</span><b>{selected.inviterUid}</b></div>
              <div><span>邀请人手机号</span><b>{selected.inviterPhone}</b></div>
              <div>
                <span>受邀人</span>
                <button
                  type="button"
                  className="invitee-detail-link"
                  onClick={() => previewInviteeHandheld(selected)}
                >
                  {selected.inviteeNickname || `UID ${selected.inviteeUid}`}
                </button>
              </div>
              <div><span>受邀人手机号</span><b>{selected.inviteePhone}</b></div>
              <div><span>红包金额</span><b>¥{formatAmount(selected.rewardAmount)}</b></div>
              <div><span>状态</span><b>{statusLabel(selected.status)}</b></div>
              {selected.reviewedBy && <div><span>审核人</span><b>{selected.reviewedBy}</b></div>}
              {selected.reviewedAt && <div><span>审核时间</span><b>{date(selected.reviewedAt)}</b></div>}
            </div>

            <h3>邀请人实名认证材料</h3>
            <div className="invitation-review-materials">
              {materials.map((material) => <Material key={material.id} material={material} />)}
              {!materials.length && <p>暂无可查看的认证材料</p>}
            </div>

            {selected.reviewReason && (
              <div className="invitation-review-reason">
                <span>驳回原因</span>
                <p>{selected.reviewReason}</p>
              </div>
            )}

            {selected.status === 'PENDING' && can('INVITATION_REVIEW') && (
              <>
                <label className="invitation-review-reject-reason">
                  <span>驳回原因</span>
                  <textarea
                    value={reason}
                    onChange={(event) => setReason(event.target.value)}
                    placeholder="驳回时必填"
                    maxLength={300}
                  />
                </label>
                <footer>
                  <button type="button" className="danger" disabled={processing} onClick={() => review(false)}>
                    驳回
                  </button>
                  <button type="button" className="primary" disabled={processing} onClick={() => review(true)}>
                    {processing ? '处理中' : '通过并发放红包'}
                  </button>
                </footer>
              </>
            )}
          </section>
        </>
      )}

      {handheldPreview && (
        <>
          <div className="invitee-preview-mask" onClick={() => setHandheldPreview(null)} />
          <section className="invitee-handheld-preview" role="dialog" aria-modal="true">
            <header>
              <div>
                <h2>受邀人手持身份证照片</h2>
                <p>
                  {handheldPreview.item.inviteeNickname
                    || `UID ${handheldPreview.item.inviteeUid}`}
                </p>
              </div>
              <button type="button" aria-label="关闭" onClick={() => setHandheldPreview(null)}>
                <X />
              </button>
            </header>
            <div className="invitee-preview-name">
              <span>邀请中填写的真实姓名</span>
              <strong>{handheldPreview.item.inviterRealName}</strong>
            </div>
            <a href={handheldPreview.material.url} target="_blank" rel="noreferrer">
              <img src={handheldPreview.material.url} alt="受邀人手持身份证照片" />
            </a>
            <p className="invitee-preview-tip">点击照片可查看原图</p>
          </section>
        </>
      )}
    </>
  );
}

function Material({ material }) {
  const kind = String(material.kind || '').toUpperCase();
  if (kind === 'IMAGE') {
    return (
      <a className="invitation-material-image" href={material.url} target="_blank" rel="noreferrer">
        <img src={material.url} alt={material.name} />
        <span>{material.name}</span>
      </a>
    );
  }
  if (kind === 'VIDEO') {
    return (
      <div className="invitation-material-video">
        <video src={material.url} controls preload="metadata" />
        <span>{material.name}</span>
      </div>
    );
  }
  return (
    <a className="invitation-material-file" href={material.url} target="_blank" rel="noreferrer">
      <span>{material.name}</span>
      <ExternalLink />
    </a>
  );
}

function formatAmount(value) {
  const amount = Number(value || 0);
  return Number.isInteger(amount) ? String(amount) : amount.toFixed(2).replace(/0+$/, '');
}

function statusLabel(status) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[status] || status;
}
