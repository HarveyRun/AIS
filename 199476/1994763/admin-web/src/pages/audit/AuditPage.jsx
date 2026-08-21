import { useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import { date, Empty } from '../users/UsersPage.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import '../shared/Page.css';
import { message } from '../../components/feedback/message.js';
export default function AuditPage() {
  const [data, setData] = useState({ items: [], total: 0 });
  const [page, setPage] = useState(0);
  const size = 20;
  const load = (targetPage) => {
    adminApi
      .logs(new URLSearchParams({ page: targetPage, size }).toString())
      .then(setData)
      .catch((e) => message.error(e.message));
  };
  useEffect(() => {
    load(0);
  }, []);
  return (
    <>
      <div className="page-title">
        <div>
          <h1>操作记录</h1>
          <p>重要管理操作均在此留痕</p>
        </div>
        <span>共 {data.total} 条</span>
      </div>
      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>管理员</th>
              <th>操作</th>
              <th>对象</th>
              <th>内容</th>
              <th>IP</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((x) => (
              <tr key={x.id}>
                <td>{x.adminName}</td>
                <td>{auditActionLabel(x.action)}</td>
                <td>
                  {auditTargetLabel(x.targetType)} #{x.targetId}
                </td>
                <td>{auditDetailLabel(x.action, x.detail)}</td>
                <td>{x.ipAddress}</td>
                <td>{date(x.createdAt)}</td>
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
    </>
  );
}

const ACTION_LABELS = {
  REVIEW_CERTIFICATION: '审核认证',
  CHANGE_CERTIFICATION_ENABLED: '变更认证状态',
  DELETE_CERTIFICATION: '删除认证',
  EDIT_CERTIFICATION: '编辑认证',
  CHANGE_USER_STATUS: '变更用户状态',
  CREATE_JOB: '新增岗位',
  UPDATE_JOB: '编辑岗位',
  DELETE_JOB: '删除岗位',
  PROCESS_WITHDRAWAL: '处理提现',
  UPDATE_FEEDBACK: '处理投诉反馈',
  UPDATE_COOPERATIONS: '处理商务合作',
  REPLY_CUSTOMER_SERVICE: '回复在线客服',
  CREATE_DISCOVERY_CATEGORY: '新增分类',
  UPDATE_DISCOVERY_CATEGORY: '编辑分类',
  DELETE_DISCOVERY_CATEGORY: '删除分类',
  CREATE_DISCOVERY_MATTER: '新增事情',
  UPDATE_DISCOVERY_MATTER: '编辑事情',
  DELETE_DISCOVERY_MATTER: '删除事情',
  CREATE_DISCOVERY_EXPERIENCE: '新增经历',
  UPDATE_DISCOVERY_EXPERIENCE: '编辑经历',
  DELETE_DISCOVERY_EXPERIENCE: '删除经历',
  CLASSIFY_EXPERIENCE: '调整经历关联',
  UPDATE_APP_TEST_ACCOUNT: '更新测试账号',
  CREATE_APP_TEST_ACCOUNT: '新增测试账号',
  DELETE_APP_TEST_ACCOUNT: '删除测试账号',
  UPDATE_PLATFORM_SERVICE_FEE: '调整平台服务费率',
  ENABLE_INVITATION_CAMPAIGN: '上架邀请答主活动',
  DISABLE_INVITATION_CAMPAIGN: '下架邀请答主活动',
  APPROVE_USER_INVITATION: '通过邀请审核',
  REJECT_USER_INVITATION: '驳回邀请审核',
  EXPORT_WITHDRAWAL_BATCH: '导出支付宝提现批次',
  RESET_ADMIN_PASSWORD: '重置后台账号密码',
  CREATE_ADMIN_USER: '新增后台账号',
  UPDATE_ADMIN_USER: '编辑后台账号',
  DELETE_ADMIN_USER: '删除后台账号',
  ASSIGN_ADMIN_ROLES: '配置账号角色',
  CREATE_ADMIN_ROLE: '新增角色',
  UPDATE_ADMIN_ROLE: '编辑角色',
  DELETE_ADMIN_ROLE: '删除角色',
  ASSIGN_ROLE_PERMISSIONS: '配置角色权限',
  CREATE_ADMIN_PERMISSION: '新增权限',
  UPDATE_ADMIN_PERMISSION: '编辑权限',
  DELETE_ADMIN_PERMISSION: '删除权限',
};

const TARGET_LABELS = {
  CERTIFICATION: '认证',
  USER: '用户',
  JOB: '岗位',
  WITHDRAWAL: '提现',
  FEEDBACK: '投诉反馈',
  COOPERATIONS: '商务合作',
  DISCOVERY_CATEGORY: '分类',
  DISCOVERY_MATTER: '事情',
  DISCOVERY_EXPERIENCE: '经历',
  APP_TEST_ACCOUNT: '测试账号',
  PLATFORM_FEE_SETTING: '平台服务费配置',
  INVITATION_CAMPAIGN: '邀请答主活动',
  USER_INVITATION: '邀请记录',
  WITHDRAWAL_BATCH: '支付宝提现批次',
  ADMIN_USER: '后台账号',
  ADMIN_ROLE: '角色',
  ADMIN_PERMISSION: '权限',
};

const DETAIL_LABELS = {
  true: '通过',
  false: '驳回',
  ACTIVE: '正常',
  DISABLED: '停用',
  PROCESSING: '处理中',
  EXPORTED: '支付处理中',
  COMPLETED: '已完成',
  FAILED: '失败',
  SUBMITTED: '待处理',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
  SOFT_DELETE: '逻辑删除',
  BASIC: '基础信息',
  EXPERIENCE: '亲身经历',
  IDENTITY: '实名认证',
  MAIN_JOB: '岗位认证',
};

function auditActionLabel(value) {
  return ACTION_LABELS[value] || value;
}

function auditTargetLabel(value) {
  return TARGET_LABELS[value] || value;
}

function auditDetailLabel(action, value) {
  if (value == null || value === '') return '—';
  if (action === 'CHANGE_CERTIFICATION_ENABLED') return String(value) === 'true' ? '启用' : '停用';
  return DETAIL_LABELS[String(value)] || value;
}
