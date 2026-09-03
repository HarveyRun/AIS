import { useEffect, useState } from 'react';
import { X, ExternalLink, Search } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import { date, Empty, Status } from '../users/UsersPage.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import '../shared/Page.css';
import { message } from '../../components/feedback/message.js';
const meta = {
  certifications: ['认证审核', '审核实名认证与亲身经历'],
  inquiries: ['询问管理', '查看询问状态和资金流转'],
  withdrawals: ['普通提现处理', '核对并处理用户主动提交的提现申请'],
  feedback: ['投诉反馈', '处理产品反馈与用户投诉'],
  cooperations: ['商务合作', '查看并跟进商务合作申请'],
};
export default function RecordsPage({ type }) {
  const { can } = useAdminAccess();
  const [data, setData] = useState({ items: [], total: 0 });
  const [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [certificationCategory, setCertificationCategory] = useState('EXPERIENCE');
  const [experienceType, setExperienceType] = useState('MONETIZED');
  const [selected, setSelected] = useState(null);
  const [modalMode, setModalMode] = useState('view');
  const [materials, setMaterials] = useState([]);
  const [reason, setReason] = useState('');
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [page, setPage] = useState(0);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deletingCertification, setDeletingCertification] = useState(false);
  const [operatingCertificationId, setOperatingCertificationId] = useState(null);
  const [exportingWithdrawals, setExportingWithdrawals] = useState(false);
  const size = 20;
  const supportsUserSearch = ['certifications', 'inquiries', 'withdrawals'].includes(type);
  const load = (targetPage = page) => {
    return adminApi
      .table(type, new URLSearchParams({
        status,
        category: type === 'certifications' ? certificationCategory : '',
        experienceType:
          type === 'certifications' && certificationCategory === 'EXPERIENCE'
            ? experienceType
            : '',
        keyword: supportsUserSearch ? appliedKeyword : '',
        page: targetPage,
        size,
      }).toString())
      .then(setData)
      .catch((e) => message.error(e.message));
  };
  useEffect(() => {
    setSelected(null);
    setPage(0);
    load(0);
  }, [type, status, certificationCategory, experienceType, appliedKeyword]);
  const search = (event) => {
    event.preventDefault();
    const nextKeyword = keyword.trim();
    setPage(0);
    if (nextKeyword === appliedKeyword) {
      load(0);
      return;
    }
    setAppliedKeyword(nextKeyword);
  };
  const exportWithdrawals = async () => {
    try {
      setExportingWithdrawals(true);
      const file = await adminApi.exportWithdrawals();
      saveFile(file);
      await load();
      message.success('普通提现批次已导出');
    } catch (error) {
      message.error(error.message);
    } finally {
      setExportingWithdrawals(false);
    }
  };
  const downloadWithdrawalBatch = async (batchNo) => {
    try {
      const file = await adminApi.downloadWithdrawalBatch(batchNo);
      saveFile(file);
    } catch (error) {
      message.error(error.message);
    }
  };
  const open = async (row, mode = 'view') => {
    try {
      setSelected(row);
      setModalMode(mode);
      setReason('');
      setEditTitle(row.title || '');
      setEditDescription(row.description || '');
      setMaterials([]);
      if (type === 'certifications') {
        setMaterials(await adminApi.materials(row.id));
      }
    } catch (e) {
      setSelected(null);
      message.error(e.message);
    }
  };
  const review = async (approved) => {
    if (!approved && !reason.trim()) {
      message.warning('驳回时请填写原因');
      return;
    }
    try {
      await adminApi.review(selected.id, {
        approved,
        reason: reason.trim(),
      });
      setSelected(null);
      await load();
      message.success(approved ? '认证已通过' : '认证已驳回');
    } catch (e) {
      message.error(e.message);
    }
  };
  const process = async (statusValue) => {
    try {
      if (type === 'withdrawals') await adminApi.withdrawalStatus(selected.id, statusValue);
      else await adminApi.recordStatus(type, selected.id, statusValue);
      setSelected(null);
      await load();
      message.success(type === 'withdrawals' ? '提现状态已更新' : '处理状态已更新');
    } catch (e) {
      message.error(e.message);
    }
  };
  const saveCertification = async () => {
    try {
      await adminApi.updateCertification(selected.id, {
        title: editTitle.trim(),
        description: editDescription.trim(),
      });
      message.success('基础信息认证已修改');
      setSelected(null);
      await load();
    } catch (e) {
      message.error(e.message);
    }
  };
  const toggleCertification = async (row) => {
    try {
      setOperatingCertificationId(row.id);
      await adminApi.setCertificationEnabled(row.id, !row.enabled);
      message.success(row.enabled ? '认证已停用' : '认证已启用');
      await load();
    } catch (e) {
      message.error(e.message);
    } finally {
      setOperatingCertificationId(null);
    }
  };
  const confirmRemoveCertification = async () => {
    if (!deleteTarget) return;
    try {
      setDeletingCertification(true);
      await adminApi.deleteCertification(deleteTarget.id);
      message.success('认证已删除');
      setDeleteTarget(null);
      await load();
    } catch (e) {
      message.error(e.message);
    } finally {
      setDeletingCertification(false);
    }
  };
  const retryCertificationMedia = async (row) => {
    try {
      setOperatingCertificationId(row.id);
      await adminApi.retryCertificationMedia(row.id);
      message.success('已重新开始整理证明资料');
      await load();
    } catch (e) {
      message.error(e.message);
    } finally {
      setOperatingCertificationId(null);
    }
  };
  return (
    <>
      <div className="page-title">
        <div>
          <h1>{meta[type][0]}</h1>
          <p>{meta[type][1]}</p>
        </div>
        <div className="page-title-actions">
          <span>共 {data.total} 条</span>
          {type === 'withdrawals' && can('WITHDRAWAL_EXPORT') && (
            <button
              type="button"
              className="primary"
              disabled={exportingWithdrawals}
              onClick={exportWithdrawals}
            >
              {exportingWithdrawals ? '导出中' : '导出普通提现'}
            </button>
          )}
        </div>
      </div>
      <form className="toolbar" onSubmit={search}>
        {supportsUserSearch && (
          <label>
            <Search aria-hidden="true" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="输入用户UID或手机号"
              maxLength={20}
            />
          </label>
        )}
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">全部状态</option>
          {statusOptions[type].map(([value, label]) => (
            <option value={value} key={value}>
              {label}
            </option>
          ))}
        </select>
        {supportsUserSearch && <button type="submit">查询</button>}
      </form>
      {type === 'certifications' && (
        <div className="certification-tabs">
          <button
            className={certificationCategory === 'EXPERIENCE' && experienceType === 'MONETIZED' ? 'active' : ''}
            onClick={() => {
              setCertificationCategory('EXPERIENCE');
              setExperienceType('MONETIZED');
            }}
          >
            干货变现
          </button>
          <button
            className={certificationCategory === 'EXPERIENCE' && experienceType === 'PUBLIC_WELFARE' ? 'active' : ''}
            onClick={() => {
              setCertificationCategory('EXPERIENCE');
              setExperienceType('PUBLIC_WELFARE');
            }}
          >
            公益分享
          </button>
          <button className={certificationCategory === 'BASIC' ? 'active' : ''} onClick={() => setCertificationCategory('BASIC')}>实名认证</button>
        </div>
      )}
      <div className="table-card">
        <table>
          <thead>
            <tr>
              {headers(type).map((x) => (
                <th key={x}>{x}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.items.map((row) => (
              <tr key={row.id}>
                {cells(type, row)}
                <td className="row-actions">
                  <button className="plain" onClick={() => open(row, 'view')}>查看</button>
                  {type === 'certifications' && can('CERTIFICATION_REVIEW') && row.status === 'PENDING' && (
                    <button className="primary" onClick={() => open(row, 'review')}>审核</button>
                  )}
                  {type === 'certifications' && can('CERTIFICATION_EDIT') && row.category === 'BASIC' && (
                    <button className="plain" onClick={() => open(row, 'edit')}>编辑</button>
                  )}
                  {type === 'certifications' && can('CERTIFICATION_TOGGLE') && row.status === 'APPROVED' && (
                    <button
                      className="plain"
                      disabled={operatingCertificationId === row.id}
                      onClick={() => toggleCertification(row)}
                    >
                      {row.enabled ? '停用' : '启用'}
                    </button>
                  )}
                  {type === 'certifications' && can('CERTIFICATION_DELETE') && (
                    <button className="danger" onClick={() => setDeleteTarget(row)}>删除</button>
                  )}
                  {type === 'certifications' && can('CERTIFICATION_REVIEW') && row.category === 'EXPERIENCE' && row.status === 'APPROVED' && ['FAILED', 'NOT_REQUIRED'].includes(row.mediaProcessingStatus) && (
                    <button
                      className="plain"
                      disabled={operatingCertificationId === row.id}
                      onClick={() => retryCertificationMedia(row)}
                    >
                      重新整理
                    </button>
                  )}
                  {type !== 'certifications' && can(processPermission(type)) && (
                    ((type === 'withdrawals' && ['PROCESSING', 'EXPORTED'].includes(row.status))
                      || (['feedback', 'cooperations'].includes(type) && !['RESOLVED', 'CLOSED'].includes(row.status)))
                    && <button className="primary" onClick={() => open(row, 'process')}>处理</button>
                  )}
                  {type === 'withdrawals' && can('WITHDRAWAL_EXPORT') && row.batchNo && (
                    <button className="plain" onClick={() => downloadWithdrawalBatch(row.batchNo)}>
                      下载批次
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <Empty />}
      </div>
      <Pagination
        page={page}
        size={size}
        total={data.total}
        onChange={(next) => {
          setPage(next);
          load(next);
        }}
      />
      {selected && (
        <>
          <div className="modal-mask" onClick={() => setSelected(null)} />
          <section className="detail-modal" role="dialog" aria-modal="true" aria-labelledby="record-detail-title">
            <header>
              <div>
                <h2 id="record-detail-title">{modalTitle(type, modalMode)}</h2>
                <p>编号 #{selected.id}</p>
              </div>
              <button type="button" aria-label="关闭" onClick={() => setSelected(null)}>
                <X />
              </button>
            </header>
            <div className="detail-fields">
              {Object.entries(selected)
                .filter(([k]) => k !== 'id')
                .map(([k, v]) => (
                  <div key={k}>
                    <span>{labels[k] || k}</span>
                    <b>
                      {formatDetailValue(k, v, type)}
                    </b>
                  </div>
                ))}
            </div>
            {type === 'certifications' && (
              <>
                {selected.category === 'EXPERIENCE' && (
                  <div className={`review-standard ${selected.experienceBusinessType === 'PUBLIC_WELFARE' ? 'simple' : 'strict'}`}>
                    <strong>
                      {selected.experienceBusinessType === 'PUBLIC_WELFARE'
                        ? '公益分享审核标准'
                        : '干货变现审核标准'}
                    </strong>
                    <p>
                      {selected.experienceBusinessType === 'PUBLIC_WELFARE'
                        ? '仅审核内容及材料中是否存在敏感信息。'
                        : '除敏感信息外，需核验本人经历及证明材料，并判断时间、人物、经过和前后逻辑是否合理。'}
                    </p>
                  </div>
                )}
                <h3>认证材料</h3>
                <div className="materials">
                  {materials.map((m) => (
                    <a href={m.url} target="_blank" rel="noreferrer" key={m.id}>
                      <span>{m.name}</span>
                      <ExternalLink />
                    </a>
                  ))}
                </div>
                {modalMode === 'edit' && selected.category === 'BASIC' && (
                  <div className="certification-edit-panel">
                    <h3>编辑基础信息认证</h3>
                    {selected.type === 'IDENTITY' && (
                      <label className="review-job-field">
                        <span>认证名称</span>
                        <input value={editTitle} onChange={(event) => setEditTitle(event.target.value)} />
                      </label>
                    )}
                    <label className="review-job-field"><span>说明</span><input value={editDescription} onChange={(event) => setEditDescription(event.target.value)} placeholder="选填" /></label>
                    <button className="plain" type="button" onClick={saveCertification}>保存认证信息</button>
                  </div>
                )}
                {modalMode === 'review' && selected.status === 'PENDING' && (
                  <>
                    <textarea
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      placeholder="驳回时必须填写原因"
                    />
                    <footer>
                      <button className="danger" onClick={() => review(false)}>
                        驳回
                      </button>
                      <button className="primary" onClick={() => review(true)}>
                        通过认证
                      </button>
                    </footer>
                  </>
                )}
              </>
            )}
            {modalMode === 'process' && type === 'withdrawals' && ['PROCESSING', 'EXPORTED'].includes(selected.status) && (
              <footer>
                <button className="danger" onClick={() => process('FAILED')}>
                  标记失败并退款
                </button>
                <button className="primary" onClick={() => process('COMPLETED')}>
                  确认已到账
                </button>
              </footer>
            )}
            {modalMode === 'process' && ['feedback', 'cooperations'].includes(type) &&
              !['RESOLVED', 'CLOSED'].includes(selected.status) && (
                <footer>
                  <button className="plain" onClick={() => process('PROCESSING')}>
                    处理中
                  </button>
                  <button
                    className="primary"
                    onClick={() => process(type === 'feedback' ? 'RESOLVED' : 'CLOSED')}
                  >
                    完成处理
                  </button>
                </footer>
              )}
          </section>
        </>
      )}
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="删除认证"
        message="确定删除这条认证吗？删除后不会再参与平台业务。"
        confirmText="确认删除"
        danger
        busy={deletingCertification}
        onCancel={() => !deletingCertification && setDeleteTarget(null)}
        onConfirm={confirmRemoveCertification}
      />
    </>
  );
}
const statusOptions = {
  certifications: [
    ['PENDING', '待审核'],
    ['APPROVED', '已通过'],
    ['REJECTED', '已驳回'],
  ],
  inquiries: [
    ['PENDING', '待接受'],
    ['ACTIVE', '交流中'],
    ['AWAITING_CONFIRMATION', '待确认结束'],
    ['COMPLETED', '已完成'],
    ['REJECTED', '未接受'],
    ['CANCELLED', '已撤销'],
    ['EXPIRED', '已过期'],
    ['REFUNDED', '已退款'],
  ],
  withdrawals: [
    ['PROCESSING', '处理中'],
    ['EXPORTED', '已导出'],
    ['COMPLETED', '已完成'],
    ['FAILED', '失败'],
  ],
  feedback: [
    ['SUBMITTED', '待处理'],
    ['PROCESSING', '处理中'],
    ['RESOLVED', '已解决'],
  ],
  cooperations: [
    ['SUBMITTED', '待处理'],
    ['PROCESSING', '处理中'],
    ['CLOSED', '已关闭'],
  ],
};
const labels = {
  uid: '用户UID',
  nickname: '昵称',
  category: '认证分类',
  type: '类型',
  title: '标题',
  description: '说明',
  experienceBusinessType: '发布类型',
  upgradeSourceId: '升级来源编号',
  status: '状态',
  rejectionReason: '驳回原因',
  submittedAt: '提交时间',
  topic: '主题',
  question: '询问内容',
  amount: '金额',
  serviceFeeRate: '平台服务费率',
  serviceFeeAmount: '平台服务费',
  answererIncomeAmount: '回答方收入',
  fundsStatus: '资金状态',
  mediaProcessingStatus: '证明资料整理状态',
  mediaProcessingError: '整理失败原因',
  mediaProcessedAt: '整理完成时间',
  createdAt: '创建时间',
  questionerUid: '提问者UID',
  answererUid: '回答者UID',
  payeeName: '收款人',
  alipayAccount: '支付宝账号',
  batchNo: '导出批次',
  exportedAt: '导出时间',
  contact: '联系方式',
  content: '内容',
  targetUid: '投诉对象UID',
};
function headers(type) {
  return {
    certifications: ['用户', '认证', '状态', '提交时间', '操作'],
    inquiries: ['双方UID', '询问内容', '金额', '状态', '操作'],
    withdrawals: ['用户', '支付宝收款账户', '提现金额', '状态', '操作'],
    feedback: ['用户', '类型', '内容', '状态', '操作'],
    cooperations: ['用户', '联系方式', '内容', '状态', '操作'],
  }[type];
}
function cells(type, r) {
  if (type === 'certifications')
    return (
      <>
        <td>
          <b>{r.nickname || `UID ${r.uid}`} {r.testData && <TestDataBadge />}</b>
          <small>UID {r.uid}</small>
        </td>
        <td>
          <b>{certificationTitle(r)}</b>
          <small>
            {r.category === 'EXPERIENCE'
              ? r.experienceBusinessType === 'PUBLIC_WELFARE'
                ? '公益分享'
                : '干货变现'
              : certificationTypeName(r.type)}
          </small>
        </td>
        <td>
          <Status value={r.status} />
          {r.status === 'APPROVED' && r.enabled === false && <small>已停用</small>}
          {r.category === 'EXPERIENCE' && r.status === 'APPROVED' && (
            <small>
              {mediaProcessingLabel(r.mediaProcessingStatus)}
              {r.mediaProcessingStatus === 'FAILED' && r.mediaProcessingError
                ? `：${r.mediaProcessingError}`
                : ''}
            </small>
          )}
        </td>
        <td>{date(r.submittedAt)}</td>
      </>
    );
  if (type === 'inquiries')
    return (
      <>
        <td>
          {r.questionerUid} → {r.answererUid} {r.testData && <TestDataBadge />}
        </td>
        <td>
          <b>{r.topic || '未填写主题'}</b>
          <small>{r.question}</small>
        </td>
        <td>
          ¥{r.amount}
          <small>
            {r.clientPlatform === 'IOS' ? 'iOS' : 'Android'} · 服务费 ¥{r.serviceFeeAmount} · 回答方 ¥{r.answererIncomeAmount}
          </small>
        </td>
        <td>
          <Status value={r.status} />
        </td>
      </>
    );
  if (type === 'withdrawals')
    return (
      <>
        <td>
          {r.nickname || r.uid} {r.testData && <TestDataBadge />}
          <small>UID {r.uid}</small>
        </td>
        <td>
          {r.payeeName || '—'}
          <small>{r.alipayAccount || '—'}</small>
        </td>
        <td>
          ¥{r.amount}
          <small>全额到账</small>
        </td>
        <td>
          <Status value={r.status} />
        </td>
      </>
    );
  return (
    <>
      <td>
        {r.nickname || r.uid} {r.testData && <TestDataBadge />}
        <small>UID {r.uid}</small>
      </td>
      <td>{r.category || r.contact}</td>
      <td className="long-cell">{r.content}</td>
      <td>
        <Status value={r.status} />
      </td>
    </>
  );
}
function TestDataBadge() {
  return <span className="test-data-badge">测试数据</span>;
}
function saveFile(file) {
  const url = URL.createObjectURL(file.blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = file.filename;
  link.click();
  URL.revokeObjectURL(url);
}
function modalTitle(type, mode) {
  if (type !== 'certifications') {
    if (mode === 'process') {
      return {
        withdrawals: '处理提现',
        feedback: '处理投诉反馈',
        cooperations: '处理商务合作',
      }[type] || `处理${meta[type][0]}`;
    }
    return `${meta[type][0]}详情`;
  }
  if (mode === 'review') return '审核认证';
  if (mode === 'edit') return '编辑认证';
  return '认证详情';
}
function certificationTitle(record) {
  if (record.type === 'IDENTITY') return '身份信息';
  return record.title;
}
function certificationTypeName(type) {
  return { IDENTITY: '实名认证', EXPERIENCE: '亲身经历' }[type] || type;
}

function mediaProcessingLabel(status) {
  return {
    PENDING: '证明资料等待整理',
    PROCESSING: '证明资料整理中',
    READY: '证明资料已整理',
    FAILED: '证明资料整理失败',
    NOT_REQUIRED: '未开始整理',
  }[status] || '未开始整理';
}

const DETAIL_VALUE_LABELS = {
  BASIC: '基础信息',
  EXPERIENCE: '亲身经历',
  IDENTITY: '实名认证',
  PUBLIC_WELFARE: '公益分享',
  MONETIZED: '干货变现',
  PRODUCT: '产品反馈',
  COMPLAINT: '投诉',
  PENDING: '待处理',
  ACTIVE: '交流中',
  AWAITING_CONFIRMATION: '待确认结束',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已撤销',
  EXPIRED: '已过期',
  REFUNDED: '已退款',
  PROCESSING: '处理中',
  EXPORTED: '已导出',
  COMPLETED: '已完成',
  FAILED: '失败',
  SUBMITTED: '待处理',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
  FROZEN: '已冻结',
  SETTLED: '已结算',
  UNSETTLED: '未结算',
};

function processPermission(type) {
  return {
    withdrawals: 'WITHDRAWAL_PROCESS',
    feedback: 'FEEDBACK_PROCESS',
    cooperations: 'COOPERATION_PROCESS',
  }[type] || '__NONE__';
}

function formatDetailValue(key, value, recordType) {
  if (value == null || value === '') return '—';
  if (key.toLowerCase().includes('time') || key.endsWith('At')) return date(value);
  if (key === 'enabled') return value ? '已启用' : '已停用';
  if (typeof value === 'boolean') return value ? '是' : '否';
  if (key === 'status' && recordType === 'certifications' && value === 'PENDING') return '待审核';
  return DETAIL_VALUE_LABELS[String(value)] || String(value);
}
