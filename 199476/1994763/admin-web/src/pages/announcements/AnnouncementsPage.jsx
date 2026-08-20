import { useEffect, useState } from 'react';
import {
  BellRing,
  CalendarClock,
  Pencil,
  Plus,
  Send,
  Trash2,
  Undo2,
  Users,
  X,
} from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './AnnouncementsPage.css';

const PAGE_SIZE = 20;
const EMPTY_FORM = {
  title: '',
  content: '',
  mode: 'DRAFT',
  scheduledAt: '',
};

const STATUS_TEXT = {
  DRAFT: '草稿',
  SCHEDULED: '等待发布',
  PUBLISHED: '已发布',
  WITHDRAWN: '已撤回',
};

export default function AnnouncementsPage() {
  const { can } = useAdminAccess();
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null);

  const load = async (targetPage = page) => {
    try {
      const result = await adminApi.announcements(targetPage, PAGE_SIZE);
      setItems(result.items || []);
      setTotal(result.total || 0);
      setPage(result.page || 0);
    } catch (error) {
      message.error(error.message);
    }
  };

  useEffect(() => {
    load(0);
  }, []);

  const openCreate = () => {
    setEditingId(null);
    setForm({ ...EMPTY_FORM });
    setEditorOpen(true);
  };

  const openEdit = (item) => {
    setEditingId(item.id);
    setForm({
      title: item.title,
      content: item.content,
      mode: item.status === 'SCHEDULED' ? 'SCHEDULED' : 'DRAFT',
      scheduledAt: toInputDate(item.scheduledAt),
    });
    setEditorOpen(true);
  };

  const closeEditor = () => {
    if (saving) return;
    setEditorOpen(false);
    setEditingId(null);
    setForm({ ...EMPTY_FORM });
  };

  const validate = () => {
    if (!form.title.trim()) {
      message.warning('请填写通知标题');
      return false;
    }
    if (!form.content.trim()) {
      message.warning('请填写通知正文');
      return false;
    }
    if (form.mode === 'SCHEDULED' && !form.scheduledAt) {
      message.warning('请选择发布时间');
      return false;
    }
    return true;
  };

  const submit = (event) => {
    event.preventDefault();
    if (!validate() || saving) return;
    if (form.mode === 'NOW') {
      setConfirmAction({ type: 'FORM_NOW' });
      return;
    }
    saveForm();
  };

  const saveForm = async () => {
    setSaving(true);
    try {
      const body = {
        title: form.title.trim(),
        content: form.content.trim(),
        mode: form.mode,
        scheduledAt: form.mode === 'SCHEDULED'
          ? form.scheduledAt
          : null,
      };
      if (editingId) {
        await adminApi.updateAnnouncement(editingId, body);
      } else {
        await adminApi.createAnnouncement(body);
      }
      const published = form.mode === 'NOW';
      closeEditorAfterSave();
      message.success(published ? '通知已发布' : '通知已保存');
      await load(editingId ? page : 0);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const closeEditorAfterSave = () => {
    setEditorOpen(false);
    setEditingId(null);
    setForm({ ...EMPTY_FORM });
    setConfirmAction(null);
  };

  const runAction = async () => {
    const action = confirmAction;
    if (!action || saving) return;
    if (action.type === 'FORM_NOW') {
      setConfirmAction(null);
      await saveForm();
      return;
    }

    setSaving(true);
    try {
      if (action.type === 'PUBLISH') {
        await adminApi.publishAnnouncement(action.item.id);
        message.success('通知已发布');
      }
      if (action.type === 'WITHDRAW') {
        await adminApi.withdrawAnnouncement(action.item.id);
        message.success('通知已撤回');
      }
      if (action.type === 'DELETE') {
        await adminApi.deleteAnnouncement(action.item.id);
        message.success('通知已删除');
      }
      setConfirmAction(null);
      const targetPage = action.type === 'DELETE' && items.length === 1 && page > 0
        ? page - 1
        : page;
      await load(targetPage);
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
          <h1>通知管理</h1>
          <p>向当前所有用户发布平台通知</p>
        </div>
        {can('ANNOUNCEMENT_CREATE') && <button
          className="primary announcement-create"
          type="button"
          onClick={openCreate}
        >
          <Plus />
          新增通知
        </button>}
      </div>

      <section className="announcement-summary">
        <BellRing />
        <div>
          <b>全体用户通知</b>
          <span>发布时固定接收用户范围，之后新注册的用户不会收到旧通知。</span>
        </div>
      </section>

      <div className="table-card announcement-table">
        <table>
          <thead>
            <tr>
              <th>通知内容</th>
              <th>接收范围</th>
              <th>状态</th>
              <th>发布时间</th>
              <th>最近更新</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td className="announcement-content-cell">
                  <b>{item.title}</b>
                  <small>{item.content}</small>
                </td>
                <td>
                  <b>全体用户</b>
                  <small>
                    {item.status === 'PUBLISHED'
                      ? `${item.recipientCount} 人`
                      : '发布时确定人数'}
                  </small>
                </td>
                <td>
                  <span className={`status ${statusClass(item.status)}`}>
                    {STATUS_TEXT[item.status] || item.status}
                  </span>
                </td>
                <td>
                  {item.status === 'SCHEDULED'
                    ? date(item.scheduledAt)
                    : date(item.publishedAt)}
                </td>
                <td>
                  <b>{item.updatedBy || '管理员'}</b>
                  <small>{date(item.updatedAt)}</small>
                </td>
                <td className="row-actions announcement-actions">
                  {can('ANNOUNCEMENT_EDIT') && item.status !== 'PUBLISHED' && (
                    <button
                      className="plain"
                      type="button"
                      onClick={() => openEdit(item)}
                    >
                      <Pencil />编辑
                    </button>
                  )}
                  {can('ANNOUNCEMENT_PUBLISH') && ['DRAFT', 'SCHEDULED', 'WITHDRAWN'].includes(item.status) && (
                    <button
                      className="plain"
                      type="button"
                      onClick={() => setConfirmAction({
                        type: 'PUBLISH',
                        item,
                      })}
                    >
                      <Send />立即发布
                    </button>
                  )}
                  {can('ANNOUNCEMENT_WITHDRAW') && ['PUBLISHED', 'SCHEDULED'].includes(item.status) && (
                    <button
                      className="plain"
                      type="button"
                      onClick={() => setConfirmAction({
                        type: 'WITHDRAW',
                        item,
                      })}
                    >
                      <Undo2 />撤回
                    </button>
                  )}
                  {can('ANNOUNCEMENT_DELETE') && <button
                    className="danger"
                    type="button"
                    onClick={() => setConfirmAction({
                      type: 'DELETE',
                      item,
                    })}
                  >
                    <Trash2 />删除
                  </button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!items.length && <div className="empty">暂无通知</div>}
        <Pagination
          page={page}
          size={PAGE_SIZE}
          total={total}
          onChange={load}
        />
      </div>

      {editorOpen && (
        <div className="announcement-dialog-layer" role="presentation">
          <button
            className="announcement-dialog-mask"
            type="button"
            aria-label="关闭"
            onClick={closeEditor}
          />
          <section
            className="announcement-editor"
            role="dialog"
            aria-modal="true"
            aria-labelledby="announcement-editor-title"
          >
            <header>
              <div>
                <i><BellRing /></i>
                <div>
                  <h2 id="announcement-editor-title">
                    {editingId ? '编辑通知' : '新增通知'}
                  </h2>
                  <p>接收范围：当前全体用户</p>
                </div>
              </div>
              <button
                type="button"
                aria-label="关闭"
                onClick={closeEditor}
              >
                <X />
              </button>
            </header>
            <form onSubmit={submit}>
              <label>
                <span>通知标题</span>
                <input
                  autoFocus
                  maxLength="120"
                  placeholder="请输入标题"
                  value={form.title}
                  onChange={(event) => setForm((current) => ({
                    ...current,
                    title: event.target.value,
                  }))}
                />
                <small>{form.title.length}/120</small>
              </label>
              <label>
                <span>通知正文</span>
                <textarea
                  maxLength="2000"
                  rows="8"
                  placeholder="请输入需要告知用户的内容"
                  value={form.content}
                  onChange={(event) => setForm((current) => ({
                    ...current,
                    content: event.target.value,
                  }))}
                />
                <small>{form.content.length}/2000</small>
              </label>
              <fieldset>
                <legend>发布方式</legend>
                <div className="announcement-mode-options">
                  {can('ANNOUNCEMENT_PUBLISH') && <ModeButton
                    value="DRAFT"
                    current={form.mode}
                    icon={Pencil}
                    title="保存草稿"
                    description="稍后手动发布"
                    onSelect={(mode) => setForm((current) => ({
                      ...current,
                      mode,
                    }))}
                  />}
                  {can('ANNOUNCEMENT_PUBLISH') && <ModeButton
                    value="NOW"
                    current={form.mode}
                    icon={Send}
                    title="立即发布"
                    description="保存后立即送达"
                    onSelect={(mode) => setForm((current) => ({
                      ...current,
                      mode,
                    }))}
                  />}
                  <ModeButton
                    value="SCHEDULED"
                    current={form.mode}
                    icon={CalendarClock}
                    title="定时发布"
                    description="到达时间后自动发布"
                    onSelect={(mode) => setForm((current) => ({
                      ...current,
                      mode,
                    }))}
                  />
                </div>
              </fieldset>
              {form.mode === 'SCHEDULED' && (
                <label>
                  <span>发布时间</span>
                  <input
                    type="datetime-local"
                    min={minimumScheduleTime()}
                    value={form.scheduledAt}
                    onChange={(event) => setForm((current) => ({
                      ...current,
                      scheduledAt: event.target.value,
                    }))}
                  />
                </label>
              )}
              <div className="announcement-audience">
                <Users />
                <div>
                  <b>发送给当前全体用户</b>
                  <span>发布完成后，每位用户拥有独立的已读状态。</span>
                </div>
              </div>
              <footer>
                <button
                  className="plain"
                  type="button"
                  disabled={saving}
                  onClick={closeEditor}
                >
                  取消
                </button>
                <button
                  className="primary"
                  type="submit"
                  disabled={saving}
                >
                  {saving ? '处理中…' : submitText(form.mode)}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(confirmAction)}
        title={confirmTitle(confirmAction)}
        message={confirmMessage(confirmAction)}
        confirmText={confirmButton(confirmAction)}
        danger={confirmAction?.type === 'DELETE'}
        busy={saving}
        onCancel={() => setConfirmAction(null)}
        onConfirm={runAction}
      />
    </>
  );
}

function ModeButton({
  value,
  current,
  icon: Icon,
  title,
  description,
  onSelect,
}) {
  return (
    <button
      className={current === value ? 'selected' : ''}
      type="button"
      onClick={() => onSelect(value)}
    >
      <Icon />
      <b>{title}</b>
      <span>{description}</span>
    </button>
  );
}

function statusClass(status) {
  if (status === 'PUBLISHED') return 'active';
  if (status === 'SCHEDULED') return 'processing';
  if (status === 'WITHDRAWN') return 'suspended';
  return '';
}

function submitText(mode) {
  if (mode === 'NOW') return '立即发布';
  if (mode === 'SCHEDULED') return '设置定时发布';
  return '保存草稿';
}

function confirmTitle(action) {
  if (!action) return '';
  if (['FORM_NOW', 'PUBLISH'].includes(action.type)) return '发布全体通知';
  if (action.type === 'WITHDRAW') return '撤回通知';
  return '删除通知';
}

function confirmMessage(action) {
  if (!action) return '';
  if (action.type === 'FORM_NOW') {
    return '确认立即发送给当前所有用户吗？发布后不能修改正文。';
  }
  if (action.type === 'PUBLISH') {
    return `确认立即发布“${action.item.title}”吗？`;
  }
  if (action.type === 'WITHDRAW') {
    return `撤回后，用户通知列表中将不再显示“${action.item.title}”。`;
  }
  return `确认删除“${action.item.title}”吗？该操作为逻辑删除。`;
}

function confirmButton(action) {
  if (!action) return '确认';
  if (['FORM_NOW', 'PUBLISH'].includes(action.type)) return '立即发布';
  if (action.type === 'WITHDRAW') return '撤回';
  return '删除';
}

function date(value) {
  return value ? new Date(value).toLocaleString() : '—';
}

function toInputDate(value) {
  if (!value) return '';
  const dateValue = new Date(value);
  const offset = dateValue.getTimezoneOffset() * 60 * 1000;
  return new Date(dateValue.getTime() - offset).toISOString().slice(0, 16);
}

function minimumScheduleTime() {
  const value = new Date(Date.now() + 2 * 60 * 1000);
  const offset = value.getTimezoneOffset() * 60 * 1000;
  return new Date(value.getTime() - offset).toISOString().slice(0, 16);
}
