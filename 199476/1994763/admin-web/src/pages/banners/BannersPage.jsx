import { useEffect, useRef, useState } from 'react';
import {
  Image as ImageIcon,
  ImagePlus,
  Pencil,
  Plus,
  Power,
  PowerOff,
  Trash2,
  X,
} from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import Pagination from '../../components/data/Pagination.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './BannersPage.css';

const PAGE_SIZE = 20;
const MODE_OPTIONS = [
  ['TEXT_ONLY', '仅文字'],
  ['IMAGE_ONLY', '仅图片'],
  ['IMAGE_TEXT', '图片和文字'],
];
const EMPTY_FORM = {
  displayMode: 'TEXT_ONLY',
  labelText: '',
  title: '',
  description: '',
  imageUrl: '',
  sortOrder: '0',
  enabled: true,
};

export default function BannersPage() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const fileInputRef = useRef(null);

  const load = async (targetPage = page) => {
    try {
      const data = await adminApi.banners(targetPage, PAGE_SIZE);
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
    setForm({ ...EMPTY_FORM, sortOrder: String((items.at(-1)?.sortOrder || 0) + 10) });
    setEditorOpen(true);
  };

  const openEdit = (item) => {
    setEditingId(item.id);
    setForm({
      displayMode: item.displayMode,
      labelText: item.labelText || '',
      title: item.title || '',
      description: item.description || '',
      imageUrl: item.imageUrl || '',
      sortOrder: String(item.sortOrder),
      enabled: item.enabled,
    });
    setEditorOpen(true);
  };

  const closeEditor = () => {
    if (saving || uploading) return;
    setEditorOpen(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const changeMode = (displayMode) => {
    setForm((current) => ({ ...current, displayMode }));
  };

  const uploadImage = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || uploading) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      message.warning('仅支持JPG、PNG或WebP图片');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      message.warning('Banner图片不能超过10MB');
      return;
    }
    setUploading(true);
    try {
      const result = await adminApi.uploadBannerImage(file);
      setForm((current) => ({ ...current, imageUrl: result.imageUrl || '' }));
      message.success('图片已上传');
    } catch (error) {
      message.error(error.message);
    } finally {
      setUploading(false);
    }
  };

  const validate = () => {
    const order = Number(form.sortOrder);
    if (!Number.isInteger(order) || order < 0 || order > 9999) {
      return message.warning('排序必须是0到9999之间的整数'), false;
    }
    if (form.displayMode !== 'IMAGE_ONLY' && !form.title.trim()) {
      return message.warning('请输入Banner标题'), false;
    }
    if (form.displayMode !== 'TEXT_ONLY' && !form.imageUrl) {
      return message.warning('请上传Banner图片'), false;
    }
    return true;
  };

  const payload = () => ({
    ...form,
    sortOrder: Number(form.sortOrder),
  });

  const save = async (event) => {
    event.preventDefault();
    if (!validate() || saving || uploading) return;
    setSaving(true);
    try {
      if (editingId) {
        await adminApi.updateBanner(editingId, payload());
        message.success('Banner已更新');
      } else {
        await adminApi.createBanner(payload());
        message.success('Banner已新增');
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

  const toggleEnabled = async (item) => {
    try {
      await adminApi.setBannerEnabled(item.id, !item.enabled);
      message.success(item.enabled ? 'Banner已停用' : 'Banner已启用');
      await load(page);
    } catch (error) {
      message.error(error.message);
    }
  };

  const remove = async () => {
    if (!deleteTarget) return;
    try {
      await adminApi.deleteBanner(deleteTarget.id);
      setDeleteTarget(null);
      message.success('Banner已删除');
      await load(items.length === 1 && page > 0 ? page - 1 : page);
    } catch (error) {
      message.error(error.message);
    }
  };

  const requiresImage = form.displayMode !== 'TEXT_ONLY';
  const requiresText = form.displayMode !== 'IMAGE_ONLY';

  return (
    <>
      <div className="page-title">
        <div>
          <h1>首页轮播</h1>
          <p>维护App首页展示的图片和文字内容</p>
        </div>
        <button className="primary" type="button" onClick={openCreate}>
          <Plus />新增Banner
        </button>
      </div>

      <div className="table-card banner-table">
        <table className="banner-data-table">
          <colgroup>
            <col className="banner-preview-column" />
            <col className="banner-mode-column" />
            <col className="banner-sort-column" />
            <col className="banner-status-column" />
            <col className="banner-update-column" />
            <col className="banner-action-column" />
          </colgroup>
          <thead>
            <tr>
              <th>预览</th>
              <th>展示方式</th>
              <th>排序</th>
              <th>状态</th>
              <th>最近更新</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>
                  <BannerPreview item={item} compact />
                </td>
                <td><b>{modeLabel(item.displayMode)}</b></td>
                <td>{item.sortOrder}</td>
                <td>
                  <span className={`status ${item.enabled ? 'active' : ''}`}>
                    {item.enabled ? '启用中' : '已停用'}
                  </span>
                </td>
                <td>
                  <b>{item.updatedBy || '管理员'}</b>
                  <small>{item.updatedAt ? new Date(item.updatedAt).toLocaleString() : '-'}</small>
                </td>
                <td>
                  <div className="banner-actions">
                    <button className="plain" type="button" onClick={() => openEdit(item)}>
                      <Pencil />编辑
                    </button>
                    <button className="plain" type="button" onClick={() => toggleEnabled(item)}>
                      {item.enabled ? <PowerOff /> : <Power />}
                      {item.enabled ? '停用' : '启用'}
                    </button>
                    <button className="danger" type="button" onClick={() => setDeleteTarget(item)}>
                      <Trash2 />删除
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!items.length && <div className="empty">暂无Banner</div>}
        <Pagination page={page} size={PAGE_SIZE} total={total} onChange={load} />
      </div>

      {editorOpen && (
        <div className="banner-dialog-layer" role="presentation">
          <button className="banner-dialog-mask" type="button" aria-label="关闭" onClick={closeEditor} />
          <section className="banner-editor" role="dialog" aria-modal="true" aria-labelledby="banner-editor-title">
            <header>
              <div>
                <h2 id="banner-editor-title">{editingId ? '编辑Banner' : '新增Banner'}</h2>
                <p>保存后，启用中的内容会在App下次进入或刷新首页时展示。</p>
              </div>
              <button type="button" aria-label="关闭" onClick={closeEditor}><X /></button>
            </header>
            <form onSubmit={save}>
              <div className="banner-form-body">
                <section>
                  <h3>展示方式</h3>
                  <div className="banner-mode-options">
                    {MODE_OPTIONS.map(([value, label]) => (
                      <button
                        className={form.displayMode === value ? 'active' : ''}
                        key={value}
                        type="button"
                        onClick={() => changeMode(value)}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                </section>

                {requiresImage && (
                  <section>
                    <h3>Banner图片</h3>
                    <div className="banner-image-field">
                      {form.imageUrl ? (
                        <img src={form.imageUrl} alt="Banner预览" />
                      ) : (
                        <div><ImageIcon /><span>还没有上传图片</span></div>
                      )}
                      <button type="button" onClick={() => fileInputRef.current?.click()} disabled={uploading}>
                        <ImagePlus />{uploading ? '上传中…' : form.imageUrl ? '更换图片' : '上传图片'}
                      </button>
                      <input
                        ref={fileInputRef}
                        type="file"
                        accept="image/jpeg,image/png,image/webp"
                        onChange={uploadImage}
                      />
                    </div>
                    <small>支持JPG、PNG和WebP，最大10MB。</small>
                  </section>
                )}

                {requiresText && (
                  <section className="banner-text-fields">
                    <h3>文字内容</h3>
                    <label>
                      <span>顶部短句（选填）</span>
                      <input
                        maxLength="30"
                        placeholder="例如 买房装修"
                        value={form.labelText}
                        onChange={(event) => setForm({ ...form, labelText: event.target.value })}
                      />
                    </label>
                    <label>
                      <span>标题</span>
                      <input
                        maxLength="80"
                        placeholder="请输入首页展示的主要内容"
                        value={form.title}
                        onChange={(event) => setForm({ ...form, title: event.target.value })}
                      />
                    </label>
                    <label>
                      <span>补充说明（选填）</span>
                      <textarea
                        maxLength="200"
                        rows="3"
                        placeholder="简单补充一句即可"
                        value={form.description}
                        onChange={(event) => setForm({ ...form, description: event.target.value })}
                      />
                    </label>
                  </section>
                )}

                <section className="banner-settings-row">
                  <label>
                    <span>排序</span>
                    <input
                      type="number"
                      min="0"
                      max="9999"
                      value={form.sortOrder}
                      onChange={(event) => setForm({ ...form, sortOrder: event.target.value })}
                    />
                    <small>数字越小越靠前。</small>
                  </label>
                  <label className="banner-enabled-field">
                    <input
                      type="checkbox"
                      checked={form.enabled}
                      onChange={(event) => setForm({ ...form, enabled: event.target.checked })}
                    />
                    <span>保存后立即启用</span>
                  </label>
                </section>

                <section>
                  <h3>实际效果</h3>
                  <BannerPreview item={form} />
                </section>
              </div>
              <footer>
                <button className="plain" type="button" onClick={closeEditor}>取消</button>
                <button className="primary" type="submit" disabled={saving || uploading}>
                  {saving ? '保存中…' : '保存'}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="删除这个Banner？"
        message="删除后不再展示，历史操作记录仍会保留。"
        confirmText="删除"
        danger
        onCancel={() => setDeleteTarget(null)}
        onConfirm={remove}
      />
    </>
  );
}

function modeLabel(mode) {
  return MODE_OPTIONS.find(([value]) => value === mode)?.[1] || mode;
}

function BannerPreview({ item, compact = false }) {
  const hasImage = item.displayMode !== 'TEXT_ONLY' && item.imageUrl;
  const hasText = item.displayMode !== 'IMAGE_ONLY';
  return (
    <div
      className={`banner-preview ${compact ? 'compact' : ''} ${hasImage ? 'with-image' : ''}`}
      style={hasImage ? { backgroundImage: `url("${item.imageUrl}")` } : undefined}
    >
      {hasImage && hasText && <i />}
      {hasText && (
        <div>
          {item.labelText && <em>{item.labelText}</em>}
          <b>{item.title || 'Banner标题'}</b>
          {item.description && <span>{item.description}</span>}
        </div>
      )}
    </div>
  );
}
