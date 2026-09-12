import { useEffect, useState } from 'react';
import {
  Image as ImageIcon,
  Search,
  ShieldCheck,
  UserRound,
  Video,
  X,
} from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './CuratedChatPage.css';

const statusText = {
  PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回',
  ACTIVE: '使用中', EXPIRED: '已到期', SUSPENDED: '已暂停', INACTIVE: '未开通',
};

export default function CuratedChatPage() {
  const { can } = useAdminAccess();
  const [query, setQuery] = useState({ keyword: '', status: '' });
  const [data, setData] = useState({ content: [], totalElements: 0 });
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [materials, setMaterials] = useState([]);
  const [materialsLoading, setMaterialsLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const size = 20;

  const load = (target = page) => adminApi.curatedApplications({ ...query, page: target, size })
    .then(setData).catch((e) => message.error(e.message));

  useEffect(() => { load(0); }, []);

  const refreshSelected = async () => {
    const refreshed = (await adminApi.curatedApplications({ keyword: selected.uid, page: 0, size: 20 }))
      .content.find((item) => item.id === selected.id);
    if (refreshed) setSelected(refreshed);
  };

  const open = async (item) => {
    setSelected(item);
    setMaterials([]);
    setMaterialsLoading(true);
    try { setMaterials(await adminApi.curatedMaterials(item.id)); }
    catch (e) { message.error(e.message); }
    finally { setMaterialsLoading(false); }
  };

  const review = async (body) => {
    if (busy) return;
    try {
      setBusy(true);
      await adminApi.reviewCuratedJob(selected.id, body);
      message.success(body.approved ? '审核已通过' : '已驳回');
      await Promise.all([load(page), refreshSelected()]);
    } catch (e) { message.error(e.message); }
    finally { setBusy(false); }
  };

  const changeMembership = async (active) => {
    if (busy) return;
    try {
      setBusy(true);
      await adminApi.setCuratedMembershipStatus(
        selected.user_id, active, active ? '后台恢复严选直聊资格' : '后台暂停严选直聊资格',
      );
      message.success(active ? '会员资格已恢复' : '会员资格已暂停');
      await Promise.all([load(page), refreshSelected()]);
    } catch (e) { message.error(e.message); }
    finally { setBusy(false); }
  };

  const close = () => { if (!busy) setSelected(null); };

  return <>
    <div className="page-title"><div><h1>严选直聊</h1><p>查看统一实名认证状态，审核十年以上岗位经历并管理成员资格</p></div><span>共 {data.totalElements || 0} 条</span></div>
    <div className="toolbar">
      <label><Search/><input value={query.keyword} placeholder="搜索UID、手机号或昵称" onChange={(e) => setQuery((value) => ({ ...value, keyword: e.target.value }))}/></label>
      <select value={query.status} onChange={(e) => setQuery((value) => ({ ...value, status: e.target.value }))}>
        <option value="">全部状态</option><option value="PENDING">待审核</option><option value="APPROVED">已通过</option><option value="REJECTED">已驳回</option>
      </select>
      <button onClick={() => { setPage(0); load(0); }}>查询</button>
    </div>
    <div className="table-card"><table><thead><tr><th>用户</th><th>实名认证</th><th>岗位认证</th><th>审核岗位</th><th>会员状态</th><th>更新时间</th><th>操作</th></tr></thead>
      <tbody>{data.content.map((item) => <tr key={item.id}>
        <td><b>{item.nickname || `UID ${item.uid}`}</b><small>UID {item.uid} · {item.phone}</small></td>
        <td><Status value={item.identity_status}/></td><td><Status value={item.job_status}/></td>
        <td>{item.job_title ? `${item.job_title} · ${item.job_years}年` : '—'}</td><td><Status value={item.membership_status || 'INACTIVE'}/></td>
        <td>{formatDate(item.updated_at)}</td><td><button className="plain" onClick={() => open(item)}>查看处理</button></td>
      </tr>)}</tbody></table>{!data.content.length && <div className="empty">暂无申请记录</div>}</div>
    <Pagination page={page} size={size} total={data.totalElements || 0} onChange={(next) => { setPage(next); load(next); }}/>

    {selected && <><button className="modal-mask" aria-label="关闭" onClick={close}/><aside className="detail-modal curated-review">
      <header className="curated-review-header">
        <div className="curated-review-heading"><span><UserRound/></span><div><h2>审核严选申请</h2><p>{selected.nickname || `UID ${selected.uid}`}</p></div></div>
        <button className="icon-button" disabled={busy} onClick={close} aria-label="关闭"><X/></button>
      </header>
      <div className="curated-review-body">
        <section className="curated-applicant-summary">
          <div><span>用户UID</span><b>{selected.uid}</b></div><div><span>注册手机号</span><b>{selected.phone}</b></div>
          <div><span>实名认证</span><Status value={selected.identity_status}/></div><div><span>岗位认证</span><Status value={selected.job_status}/></div>
        </section>
        <section className="curated-identity-summary">
          <div><UserRound/><div><b>实名认证</b><p>与 App 全局实名认证共用一套数据，请前往左侧“认证审核”统一处理。</p></div></div>
          <Status value={selected.identity_status}/>
        </section>
        <ReviewPanel
          key={`${selected.id}-${selected.job_status}`}
          current={selected.job_status}
          materials={materials}
          materialsLoading={materialsLoading}
          canReview={can('CURATED_CHAT_JOB_REVIEW')}
          busy={busy}
          onSubmit={review}
        />
        {can('CURATED_CHAT_MEMBER_MANAGE') && ['ACTIVE', 'SUSPENDED'].includes(selected.membership_status) &&
          <section className="curated-member-control"><div><div><b>会员资格</b><p>暂停后将无法进入成员列表、收发严选消息或发起语音。</p></div><Status value={selected.membership_status}/></div>
            <button className={selected.membership_status === 'ACTIVE' ? 'danger' : 'primary'} disabled={busy} onClick={() => changeMembership(selected.membership_status === 'SUSPENDED')}>{selected.membership_status === 'ACTIVE' ? '暂停会员资格' : '恢复会员资格'}</button>
          </section>}
      </div>
    </aside></>}
  </>;
}

function Status({ value }) {
  return <span className={`status ${String(value || '').toLowerCase()}`}>{statusText[value] || value || '—'}</span>;
}

function ReviewPanel({ current, materials, materialsLoading, canReview, busy, onSubmit }) {
  const relevant = materials.filter((item) => item.materialType === 'JOB');
  return <section className="curated-review-panel">
    <div className="curated-panel-heading"><div><h3>核对岗位资料</h3><p>核对现场拍摄或录制的材料，并确认职位与累计从业年限。</p></div><Status value={current}/></div>
    <MaterialSection items={relevant} loading={materialsLoading}/>
    {canReview ? <ReviewForm busy={busy} onSubmit={onSubmit}/> : <p className="curated-no-permission">当前账号没有此项审核权限</p>}
  </section>;
}

function MaterialSection({ items, loading }) {
  if (loading) return <div className="curated-materials-loading">资料加载中…</div>;
  if (!items.length) return <div className="curated-materials-empty">未提交岗位证明资料</div>;
  return <div className="curated-material-grid">{items.map((item, index) => <MaterialCard key={item.id} label={`岗位资料 ${index + 1}`} item={item}/>)}</div>;
}

function MaterialCard({ label, item }) {
  if (!item) return <div className="curated-material-card missing"><ImageIcon/><b>{label}</b><small>未提交</small></div>;
  const MediaIcon = item.mediaType === 'VIDEO' ? Video : ImageIcon;
  return <a className="curated-material-card" href={item.url} target="_blank" rel="noreferrer"><MediaIcon/><b>{label}</b><small>{item.mediaType === 'VIDEO' ? '查看录像' : '查看照片'}</small></a>;
}

function ReviewForm({ busy, onSubmit }) {
  const [approved, setApproved] = useState(true);
  const [form, setForm] = useState({ jobTitle: '', jobYears: '', reason: '' });
  const submit = (event) => { event.preventDefault(); onSubmit({ approved, jobTitle: form.jobTitle, jobYears: form.jobYears ? Number(form.jobYears) : null, reason: form.reason }); };
  return <form className="curated-review-form" onSubmit={submit}>
    <div className="curated-form-title"><h3>审核结论</h3><p>选择结论后，填写该结论对应的必要信息。</p></div>
    <div className="review-choice"><button type="button" className={approved ? 'selected' : ''} onClick={() => setApproved(true)}><ShieldCheck/>审核通过</button><button type="button" className={!approved ? 'selected reject' : ''} onClick={() => setApproved(false)}>审核驳回</button></div>
    {approved ? <div className="curated-fields-row">
      <Field label="用户职位名称" value={form.jobTitle} maxLength={80} required onChange={(value) => setForm((state) => ({ ...state, jobTitle: value }))}/>
      <Field label="累计职位年限" type="number" min="10" max="80" suffix="年" value={form.jobYears} required onChange={(value) => setForm((state) => ({ ...state, jobYears: value }))}/>
    </div> : <Field label="驳回原因" value={form.reason} maxLength={500} required placeholder="请写明需要用户重新提交的内容" onChange={(value) => setForm((state) => ({ ...state, reason: value }))}/>} 
    <div className="curated-submit-row"><span>提交后会立即通知用户</span><button className="primary" disabled={busy}>{busy ? '正在提交…' : approved ? '确认通过' : '确认驳回'}</button></div>
  </form>;
}

function Field({ label, value, onChange, type = 'text', suffix, placeholder, required = false, maxLength, min, max }) {
  return <label className="curated-field"><span>{label}</span><div><input type={type} value={value} required={required} maxLength={maxLength} min={min} max={max} placeholder={placeholder} onChange={(event) => onChange(event.target.value)}/>{suffix && <em>{suffix}</em>}</div></label>;
}

function formatDate(value) { return value ? new Date(value).toLocaleString('zh-CN') : '—'; }
