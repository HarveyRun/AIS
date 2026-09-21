import { useEffect, useState } from 'react';
import { X, ExternalLink, Search } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import { date, Empty, Status } from '../users/UsersPage.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import '../shared/Page.css';
import { message } from '../../components/feedback/message.js';

const EMPTY_REVIEW_SCORES = {
  materialSupportScore: '',
  commonRelevanceScore: '',
  learnabilityScore: '',
  clarityScore: '',
  logicConsistencyScore: '',
};

const meta = {
  certifications: ['认证审核', '审核实名认证和用户发布的经历'],
  inquiries: ['询问管理', '查看询问状态和资金流转'],
  withdrawals: ['普通提现处理', '核对并处理用户主动提交的提现申请'],
  feedback: ['投诉反馈', '处理产品反馈与用户投诉'],
  cooperations: ['商务合作', '查看并跟进商务合作申请'],
};
export default function RecordsPage({ type }) {
  const { can } = useAdminAccess();
  const [data, setData] = useState({ items: [], total: 0 });
  const [status, setStatus] = useState('');
  const [category, setCategory] = useState('');
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [selected, setSelected] = useState(null);
  const [modalMode, setModalMode] = useState('view');
  const [materials, setMaterials] = useState([]);
  const [previewMaterial, setPreviewMaterial] = useState(null);
  const [reason, setReason] = useState('');
  const [reviewScores, setReviewScores] = useState(EMPTY_REVIEW_SCORES);
  const [informationSpecificityScore, setInformationSpecificityScore] = useState('');
  const [resolution, setResolution] = useState('');
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
        category: type === 'certifications' ? category : '',
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
  }, [type, status, category, appliedKeyword]);
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
      setReviewScores(EMPTY_REVIEW_SCORES);
      setInformationSpecificityScore('');
      setResolution(row.resolution || '');
      setMaterials([]);
      setPreviewMaterial(null);
      if (type === 'certifications') {
        setMaterials(await adminApi.materials(row.id));
      }
    } catch (e) {
      setSelected(null);
      message.error(e.message);
    }
  };
  const review = async (approved = true) => {
    const reviewingExperience = selected.category === 'EXPERIENCE';
    if (!reviewingExperience && !approved && !reason.trim()) {
      message.warning('驳回时请填写原因');
      return;
    }
    if (reviewingExperience) {
      const missing = SCORE_DEFINITIONS.some(({ key }) => reviewScores[key] === '');
      if (missing) {
        message.warning('请先完成全部五个参考维度评分');
        return;
      }
      if (informationSpecificityScore === '') {
        message.warning('请完成信息具体程度评分');
        return;
      }
    }
    try {
      const resolvedApproved = reviewingExperience
        ? Number(informationSpecificityScore) > 5
        : approved;
      await adminApi.review(selected.id, {
        approved: resolvedApproved,
        reason: reason.trim(),
        informationSpecificityScore: reviewingExperience
          ? Number(informationSpecificityScore)
          : null,
        ...Object.fromEntries(
          Object.entries(reviewScores).map(([key, value]) => [
            key,
            value === '' ? null : Number(value),
          ]),
        ),
      });
      setSelected(null);
      await load();
      message.success(resolvedApproved ? '认证已通过' : '认证未通过');
    } catch (e) {
      message.error(e.message);
    }
  };
  const process = async (statusValue) => {
    try {
      if (type === 'withdrawals') await adminApi.withdrawalStatus(selected.id, statusValue);
      else {
        if (type === 'feedback' && ['RESOLVED', 'CLOSED'].includes(statusValue) && !resolution.trim()) {
          message.warning('请填写处理结果');
          return;
        }
        await adminApi.recordStatus(type, selected.id, statusValue, resolution.trim());
      }
      setSelected(null);
      await load();
      message.success(type === 'withdrawals' ? '提现状态已更新' : '处理状态已更新');
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
        {type === 'certifications' && (
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">全部认证</option>
            <option value="BASIC">实名认证</option>
            <option value="EXPERIENCE">经历认证</option>
          </select>
        )}
        {supportsUserSearch && <button type="submit">查询</button>}
      </form>
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
                  {type === 'certifications' && can('CERTIFICATION_REVIEW') && row.category === 'EXPERIENCE' && row.status === 'APPROVED' && row.mediaProcessingStatus === 'FAILED' && (
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
                .filter(([key]) => shouldShowDetailField(key))
                .map(([k, v]) => (
                  <div key={k}>
                    <span>{detailFieldLabel(k, selected)}</span>
                    <b>
                      {formatDetailValue(k, v, type)}
                    </b>
                  </div>
                ))}
            </div>
            {type === 'certifications' && (
              <>
                {selected.category === 'EXPERIENCE' && (
                  <div className="review-standard strict">
                    <strong>经历审核标准</strong>
                    <p>
                      先判断内容是否提供了足以理解这段经历的具体信息；达不到基础信息要求时应驳回。
                      通过基础审核后，再完成下方五项评分。评分仅作为公开参考，不决定审核是否通过。
                    </p>
                  </div>
                )}
                {selected.type === 'IDENTITY' && (
                  <div className="review-standard">
                    <strong>实名认证审核</strong>
                    <p>核对身份证正面、反面及手持身份证照片是否清晰、完整并属于同一人。</p>
                  </div>
                )}
                <h3>
                  {selected.category === 'EXPERIENCE' ? '证明资料（选填）' : '认证材料'}
                </h3>
                <div className="materials">
                  {proofMaterials(materials, selected.category).map((m) => (
                    <a
                      href={m.url}
                      target="_blank"
                      rel="noreferrer"
                      key={m.id}
                      onClick={isImageMaterial(m) ? (event) => {
                        event.preventDefault();
                        setPreviewMaterial(m);
                      } : undefined}
                    >
                      <span>{m.name}</span>
                      <ExternalLink />
                    </a>
                  ))}
                  {!proofMaterials(materials, selected.category).length && (
                    <p className="materials-empty">
                      {selected.category === 'EXPERIENCE'
                        ? '用户未提供证明资料（选填，不影响审核）'
                        : '暂无认证材料'}
                    </p>
                  )}
                </div>
                {selected.category === 'EXPERIENCE' && selected.referenceIndex != null && (
                  <ReviewScoreSummary record={selected} />
                )}
                {selected.category === 'EXPERIENCE' && signatureMaterials(materials).length > 0 && (
                  <>
                    <h3>签字确认</h3>
                    <div className="materials">
                      {signatureMaterials(materials).map((material) => (
                        <a href={material.url} target="_blank" rel="noreferrer" key={material.id}>
                          <span>{material.name}</span>
                          <ExternalLink />
                        </a>
                      ))}
                    </div>
                  </>
                )}
                {modalMode === 'review' && selected.status === 'PENDING' && (
                  <>
                    {selected.category === 'EXPERIENCE' && (
                      <>
                        <p className="review-optional-note">
                          证明资料为选填项。未提供证明资料时，材料支撑度可评0分，不影响根据经历内容正常审核。
                        </p>
                        <ReviewScoreEditor values={reviewScores} onChange={setReviewScores} />
                        <InformationSpecificityDecision
                          value={informationSpecificityScore}
                          onChange={setInformationSpecificityScore}
                          enabled={SCORE_DEFINITIONS.every(({ key }) => reviewScores[key] !== '')}
                        />
                        <footer>
                          <button
                            className="primary"
                            disabled={informationSpecificityScore === ''}
                            onClick={() => review()}
                          >
                            确认审核结果
                          </button>
                        </footer>
                      </>
                    )}
                    {selected.category !== 'EXPERIENCE' && (
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
                <>
                  {type === 'feedback' && (
                    <textarea
                      value={resolution}
                      onChange={(event) => setResolution(event.target.value)}
                      maxLength={1000}
                      placeholder="填写给用户查看的处理结果"
                    />
                  )}
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
                </>
              )}
          </section>
        </>
      )}
      {previewMaterial && (
        <>
          <div className="material-preview-mask" onClick={() => setPreviewMaterial(null)} />
          <section className="material-preview-dialog" role="dialog" aria-modal="true" aria-label="认证照片预览">
            <header>
              <strong>{previewMaterial.name || '认证照片'}</strong>
              <button type="button" aria-label="关闭" onClick={() => setPreviewMaterial(null)}>
                <X />
              </button>
            </header>
            <div className="material-preview-body">
              <img src={materialPreviewUrl(previewMaterial.url)} alt={previewMaterial.name || '认证照片'} />
            </div>
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
    ['TEXT_LIMIT_REACHED', '文字额度已用完'],
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
  experienceLocation: '发生地点',
  experienceStartDate: '开始时间',
  experienceEndDate: '结束时间',
  experienceCount: '已经历的次数',
  experienceRole: '本人当时的身份',
  experienceAgeRange: '当时年龄段',
  experienceEducation: '当时学历',
  experienceJob: '当时职业',
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
  resolution: '处理结果',
  resolvedAt: '处理完成时间',
  handledBy: '处理人',
  riskLevel: '风险等级',
  riskReasons: '风险说明',
};

const SCORE_DEFINITIONS = [
  {
    key: 'materialSupportScore',
    label: '证明材料支撑度',
    anchors: [
      '未提交任何证明材料',
      '材料与经历关系较弱',
      '能够证明部分人物、时间或事项',
      '多份材料基本支撑主要经过',
      '材料直接覆盖关键人物、时间和事件',
      '材料形成完整且相互印证的证据链',
    ],
  },
  {
    key: 'commonRelevanceScore',
    label: '普遍相关程度',
    anchors: [
      '几乎只发生在极少数特殊人员或环境中',
      '普通人极少遇到',
      '特定群体可能遇到',
      '不少普通人在某个人生阶段可能遇到',
      '普通个人或家庭较常遇到',
      '大部分普通人在生活中都有较高概率遇到',
    ],
  },
  {
    key: 'learnabilityScore',
    label: '可借鉴程度',
    anchors: [
      '只有个人感受，没有可供参考的信息',
      '只说明结果，基本没有过程',
      '包含少量处理经过或注意事项',
      '能够了解大致处理路径和关键选择',
      '关键步骤、取舍、问题和结果较完整',
      '能够明显帮助类似处境的人少走弯路',
    ],
  },
  {
    key: 'clarityScore',
    label: '表述清晰程度',
    anchors: [
      '基本无法理解',
      '内容严重碎片化，无法确认主要意思',
      '可以勉强理解，但需要大量猜测',
      '主要意思能够理解，存在少量跳跃或重复',
      '表达清楚、重点明确、阅读顺畅',
      '简洁准确、层次清楚，几乎没有理解成本',
    ],
  },
  {
    key: 'logicConsistencyScore',
    label: '逻辑自洽程度',
    anchors: [
      '存在无法解释的核心矛盾',
      '人物、时间或结果存在严重冲突',
      '多处前后关系不清，影响理解',
      '没有明显矛盾，但部分过程缺少衔接',
      '时间、人物、经过和结果基本连贯',
      '内容完整连贯，各部分能够相互对应',
    ],
  },
];

const INFORMATION_SPECIFICITY_DEFINITION = {
  key: 'informationSpecificityScore',
  label: '信息具体程度',
  anchors: [
    '无法判断用户经历了什么事情',
    '只有笼统结论或感受，关键事实基本缺失',
    '能够识别事情，但重要背景或过程缺失较多',
    '主要人物、阶段和关键情况足以理解这段经历',
    '关键背景、经过和结果具体，基本不需要猜测',
    '事实边界清晰、细节充分，能够完整理解主要经历',
  ],
};
function headers(type) {
  return {
    certifications: ['用户', '认证', '状态', '提交时间', '操作'],
    inquiries: ['双方UID', '询问内容', '金额', '状态', '操作'],
    withdrawals: ['用户', '支付宝收款账户', '提现金额', '风险', '状态', '操作'],
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
              ? '亲身经历'
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
          <Status value={r.riskLevel || 'LOW'} />
          {r.riskReasons && <small>{r.riskReasons}</small>}
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
  return '认证详情';
}
function certificationTitle(record) {
  return record.title;
}
function certificationTypeName(type) {
  return { EXPERIENCE: '亲身经历', IDENTITY: '实名认证' }[type] || type;
}

function mediaProcessingLabel(status) {
  return {
    PENDING: '证明资料等待整理',
    PROCESSING: '证明资料整理中',
    READY: '证明资料已整理',
    FAILED: '证明资料整理失败',
    NOT_REQUIRED: '未提交证明资料',
  }[status] || '未开始整理';
}

const DETAIL_VALUE_LABELS = {
  EXPERIENCE: '亲身经历',
  BASIC: '基础认证',
  IDENTITY: '实名认证',
  PRODUCT: '产品反馈',
  COMPLAINT: '投诉',
  PENDING: '待处理',
  ACTIVE: '交流中',
  TEXT_LIMIT_REACHED: '文字额度已用完',
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

function shouldShowDetailField(key) {
  return ![
    'id',
    'detailMode',
    'detailVideo',
    'detailVideoUrl',
    'narrativeMode',
    'materialSupportScore',
    'commonRelevanceScore',
    'learnabilityScore',
    'clarityScore',
    'logicConsistencyScore',
    'informationSpecificityScore',
    'referenceIndex',
  ].includes(key);
}

function ReviewScoreEditor({ values, onChange }) {
  const selectedValues = SCORE_DEFINITIONS
    .map(({ key }) => values[key])
    .filter((value) => value !== '');
  const referenceIndex = selectedValues.length === SCORE_DEFINITIONS.length
    ? selectedValues.reduce((total, value) => total + Number(value), 0) * 2
    : null;

  return (
    <section className="review-score-editor">
      <div className="review-score-heading">
        <div>
          <strong>经历参考评分</strong>
          <p>五项分别按 0—10 分评价，公开指数按五项简单平均计算。</p>
        </div>
        <b>{referenceIndex == null ? '待完成' : `${referenceIndex}/100`}</b>
      </div>
      <div className="review-score-list">
        {SCORE_DEFINITIONS.map((definition) => (
          <div key={definition.key} className="review-score-item">
            <div className="review-score-item-heading">
              <span>{definition.label}</span>
              <b>{values[definition.key] === '' ? '未评分' : `${values[definition.key]} 分`}</b>
            </div>
            <ScoreButtons
              value={values[definition.key]}
              onChange={(score) => onChange((current) => ({
                ...current,
                [definition.key]: score,
              }))}
            />
            <ScoreAnchors definition={definition} />
          </div>
        ))}
      </div>
    </section>
  );
}

function InformationSpecificityDecision({ value, onChange, enabled }) {
  const passed = value !== '' && Number(value) > 5;
  return (
    <section className={`review-score-decision${enabled ? '' : ' disabled'}`}>
      <div className="review-score-heading">
        <div>
          <strong>审核通过标准：信息具体程度</strong>
          <p>先完成上方五个参考维度，再评定本项。0—5 分不通过，6—10 分通过。</p>
        </div>
        <b className={value === '' ? '' : passed ? 'passed' : 'rejected'}>
          {value === '' ? '待评定' : passed ? '通过' : '不通过'}
        </b>
      </div>
      <ScoreButtons value={value} onChange={onChange} disabled={!enabled} />
      <ScoreAnchors definition={INFORMATION_SPECIFICITY_DEFINITION} />
    </section>
  );
}

function ScoreButtons({ value, onChange, disabled = false }) {
  return (
    <div className="review-score-scale" aria-label="选择评分">
      {Array.from({ length: 11 }, (_, score) => (
        <button
          key={score}
          type="button"
          className={Number(value) === score && value !== '' ? 'selected' : ''}
          disabled={disabled}
          onClick={() => onChange(String(score))}
        >
          {score}
        </button>
      ))}
    </div>
  );
}

function ScoreAnchors({ definition }) {
  return (
    <div className="review-score-anchor-list">
      {definition.anchors.map((anchor, index) => (
        <div key={anchor}>
          <b>{index * 2}分</b>
          <span>{anchor}</span>
        </div>
      ))}
    </div>
  );
}

function ReviewScoreSummary({ record }) {
  return (
    <section className="review-score-summary">
      <div className="review-score-heading">
        <div>
          <strong>经历参考评分</strong>
          <p>公开页面仅展示综合参考指数，不展示各维度明细。</p>
        </div>
        <b>{record.referenceIndex}/100</b>
      </div>
      <div className="review-score-summary-grid">
        {SCORE_DEFINITIONS.map((definition) => (
          <div key={definition.key}>
            <span>{definition.label}</span>
            <b>{record[definition.key]} 分</b>
          </div>
        ))}
        {record.informationSpecificityScore != null && (
          <div>
            <span>信息具体程度</span>
            <b>{record.informationSpecificityScore} 分 · {record.informationSpecificityScore > 5 ? '通过' : '不通过'}</b>
          </div>
        )}
      </div>
    </section>
  );
}

function detailFieldLabel(key, record) {
  if (key === 'description' && record.category === 'EXPERIENCE') {
    return '文字叙述（必填，最多400字）';
  }
  return labels[key] || key;
}

function proofMaterials(items, category) {
  if (category !== 'EXPERIENCE') {
    return items.filter((item) => item.kind !== 'DETAIL_VIDEO');
  }
  return items.filter((item) => ['PROOF_ARCHIVE', 'ARCHIVE'].includes(item.kind));
}

function isImageMaterial(material) {
  return String(material?.contentType || '').toLowerCase().startsWith('image/')
    || String(material?.kind || '').toUpperCase().startsWith('IDENTITY_');
}

function materialPreviewUrl(url) {
  if (!url) return '';
  if (!/^https?:\/\//i.test(url)) return url;
  return `/api/public/media/image?url=${encodeURIComponent(url)}`;
}

function signatureMaterials(items) {
  return items.filter((item) => item.kind === 'SIGNATURE');
}
