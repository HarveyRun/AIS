import { useEffect, useState } from 'react';
import {
  CircleStop,
  Pencil,
  Plus,
  Rocket,
  Send,
  Smartphone,
  Trash2,
  X,
} from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import Pagination from '../../components/data/Pagination.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './AppVersionPage.css';

const PAGE_SIZE = 20;
const EMPTY_FORM = {
  platform: 'ANDROID',
  versionName: '',
  versionCode: '',
  minimumSupportedVersionCode: '',
  title: '发现新版本',
  updateContent: '',
  downloadUrl: '',
};

export default function AppVersionPage() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const load = async (targetPage = page) => {
    try {
      const data = await adminApi.appVersions(targetPage, PAGE_SIZE);
      setItems(data.items || []);
      setTotal(data.total || 0);
      setPage(data.page || 0);
    } catch (error) {
      message.error(error.message);
    }
  };

  useEffect(() => {
    load(0);
  }, []);

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setEditorOpen(true);
  };

  const openEdit = (item) => {
    setEditingId(item.id);
    setForm({
      platform: item.platform,
      versionName: item.versionName,
      versionCode: String(item.versionCode),
      minimumSupportedVersionCode: String(item.minimumSupportedVersionCode),
      title: item.title,
      updateContent: item.updateContent,
      downloadUrl: item.downloadUrl,
    });
    setEditorOpen(true);
  };

  const closeEditor = () => {
    if (saving) return;
    setEditorOpen(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const payload = () => ({
    ...form,
    versionCode: Number(form.versionCode),
    minimumSupportedVersionCode: Number(form.minimumSupportedVersionCode),
  });

  const validate = () => {
    const versionCode = Number(form.versionCode);
    const minimum = Number(form.minimumSupportedVersionCode);
    if (!form.versionName.trim()) return message.warning('请输入版本名称'), false;
    if (!Number.isInteger(versionCode) || versionCode < 1) {
      return message.warning('版本号必须是大于0的整数'), false;
    }
    if (!Number.isInteger(minimum) || minimum < 1 || minimum > versionCode) {
      return message.warning('最低可用版本号不能大于当前版本号'), false;
    }
    if (!form.title.trim()) return message.warning('请输入更新标题'), false;
    if (!form.updateContent.trim()) return message.warning('请输入更新内容'), false;
    if (!/^https?:\/\//i.test(form.downloadUrl.trim())) {
      return message.warning('请输入正确的备用下载地址'), false;
    }
    return true;
  };

  const save = async (event) => {
    event.preventDefault();
    if (!validate() || saving) return;
    setSaving(true);
    try {
      if (editingId) {
        await adminApi.updateAppVersion(editingId, payload());
        message.success('版本信息已更新');
      } else {
        await adminApi.createAppVersion(payload());
        message.success('版本记录已新增');
      }
      setEditorOpen(false);
      setEditingId(null);
      setForm(EMPTY_FORM);
      await load(editingId ? page : 0);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const togglePublished = async (item) => {
    try {
      if (item.published) {
        await adminApi.unpublishAppVersion(item.id);
        message.success('已停止该版本更新');
      } else {
        await adminApi.publishAppVersion(item.id);
        message.success('版本已发布，App将按规则提示更新');
      }
      await load(page);
    } catch (error) {
      message.error(error.message);
    }
  };

  const remove = async () => {
    if (!deleteTarget) return;
    try {
      await adminApi.deleteAppVersion(deleteTarget.id);
      setDeleteTarget(null);
      message.success('版本记录已删除');
      await load(items.length === 1 && page > 0 ? page - 1 : page);
    } catch (error) {
      message.error(error.message);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>App版本管理</h1>
          <p>发布更新提醒，并控制低版本是否必须更新</p>
        </div>
        <button className="primary app-version-create" type="button" onClick={openCreate}>
          <Plus />新增版本
        </button>
      </div>

      <section className="app-version-summary">
        <Rocket />
        <div>
          <b>版本判断规则</b>
          <span>低于最新版本会收到更新提醒；低于最低可用版本将无法跳过更新。</span>
        </div>
      </section>

      <div className="table-card app-version-table">
        <table>
          <thead>
            <tr>
              <th>版本</th>
              <th>最低可用版本号</th>
              <th>更新说明</th>
              <th>状态</th>
              <th>最近更新</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>
                  <b>{item.versionName}</b>
                  <small>Android · 版本号 {item.versionCode}</small>
                </td>
                <td>{item.minimumSupportedVersionCode}</td>
                <td className="app-version-content">
                  <b>{item.title}</b>
                  <small>{item.updateContent}</small>
                </td>
                <td>
                  <span className={`status ${item.published ? 'active' : ''}`}>
                    {item.published ? '发布中' : '未发布'}
                  </span>
                </td>
                <td>
                  <b>{item.updatedBy || '管理员'}</b>
                  <small>{item.updatedAt ? new Date(item.updatedAt).toLocaleString() : '-'}</small>
                </td>
                <td className="row-actions">
                  <button className="plain" type="button" onClick={() => openEdit(item)}>
                    <Pencil />编辑
                  </button>
                  <button className="plain" type="button" onClick={() => togglePublished(item)}>
                    {item.published ? <CircleStop /> : <Send />}
                    {item.published ? '停止' : '发布'}
                  </button>
                  <button className="danger" type="button" onClick={() => setDeleteTarget(item)}>
                    <Trash2 />删除
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!items.length && <div className="empty">暂无版本记录</div>}
        <Pagination page={page} size={PAGE_SIZE} total={total} onChange={load} />
      </div>

      {editorOpen && (
        <div className="app-version-dialog-layer" role="presentation">
          <button
            className="app-version-dialog-mask"
            type="button"
            aria-label="关闭"
            onClick={closeEditor}
          />
          <section
            className="app-version-editor"
            role="dialog"
            aria-modal="true"
            aria-labelledby="app-version-editor-title"
          >
            <header>
              <div className="app-version-editor-heading">
                <i><Smartphone /></i>
                <div>
                  <h2 id="app-version-editor-title">
                    {editingId ? '编辑版本' : '新增版本'}
                  </h2>
                  <p>保存后不会立即生效，确认无误后再发布。</p>
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
            <form onSubmit={save}>
              <div className="app-version-form-section">
                <h3>版本信息</h3>
                <div className="app-version-grid">
                  <label>
                    <span>版本名称</span>
                    <input
                      autoFocus
                      maxLength="30"
                      placeholder="例如 1.1.0"
                      value={form.versionName}
                      onChange={(event) => setForm({
                        ...form,
                        versionName: event.target.value,
                      })}
                    />
                  </label>
                  <label>
                    <span>版本号</span>
                    <input
                      type="number"
                      min="1"
                      placeholder="例如 2"
                      value={form.versionCode}
                      onChange={(event) => setForm({
                        ...form,
                        versionCode: event.target.value,
                      })}
                    />
                  </label>
                  <label>
                    <span>最低可用版本号</span>
                    <input
                      type="number"
                      min="1"
                      placeholder="低于该版本时强制更新"
                      value={form.minimumSupportedVersionCode}
                      onChange={(event) => setForm({
                        ...form,
                        minimumSupportedVersionCode: event.target.value,
                      })}
                    />
                    <small>低于此版本号的 App 无法跳过更新。</small>
                  </label>
                </div>
              </div>

              <div className="app-version-form-section">
                <h3>更新内容</h3>
                <label>
                  <span>弹窗标题</span>
                  <input
                    maxLength="80"
                    value={form.title}
                    onChange={(event) => setForm({
                      ...form,
                      title: event.target.value,
                    })}
                  />
                </label>
                <label>
                  <span>更新说明</span>
                  <textarea
                    maxLength="1000"
                    rows="5"
                    placeholder={'每项内容单独换行，例如：\n修复聊天图片加载问题\n优化版本更新体验'}
                    value={form.updateContent}
                    onChange={(event) => setForm({
                      ...form,
                      updateContent: event.target.value,
                    })}
                  />
                </label>
                <label>
                  <span>备用下载地址</span>
                  <input
                    maxLength="500"
                    placeholder="应用商店无法打开时使用，例如 https://..."
                    value={form.downloadUrl}
                    onChange={(event) => setForm({
                      ...form,
                      downloadUrl: event.target.value,
                    })}
                  />
                </label>
              </div>
              <footer>
                <button
                  className="plain"
                  type="button"
                  onClick={closeEditor}
                >
                  取消
                </button>
                <button
                  className="primary"
                  type="submit"
                  disabled={saving}
                >
                  {saving ? '保存中…' : '保存'}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="删除版本记录？"
        message={deleteTarget?.published
          ? '该版本正在发布中，删除后App将立即停止收到这条更新提示。'
          : '删除后该版本记录不再显示。'}
        confirmText="删除"
        danger
        onCancel={() => setDeleteTarget(null)}
        onConfirm={remove}
      />
    </>
  );
}
