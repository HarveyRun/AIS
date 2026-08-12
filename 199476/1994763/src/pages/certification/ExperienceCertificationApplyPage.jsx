import { useState } from 'react';
import {
  AlertCircle,
  Download,
  Eye,
  FileArchive,
  Image,
  LockKeyhole,
  Trash2,
  Video,
} from 'lucide-react';
import CameraCapture from '../../components/certification/CameraCapture.jsx';
import MaterialPreview, {
  createMaterial,
  materialName,
  materialUrl,
} from '../../components/certification/MaterialPreview.jsx';
import Page from '../../components/layout/Page.jsx';
import { api } from '../../api/http.js';
import './CertificationPages.css';

const ONE_GB = 1024 * 1024 * 1024;
const FIVE_HUNDRED_MB = 500 * 1024 * 1024;

function existingMaterials(certification) {
  const materials = certification?.materials || [];
  return {
    archive: materials.find((item) => item.kind === 'archive' || /\.(rar|zip)$/i.test(materialName(item))) || '',
    video: materials.find((item) => item.kind === 'video' || /\.(mp4|mov|webm)$/i.test(materialName(item))) || '',
    photos: materials.filter((item) => item.kind === 'image' || /\.(jpg|jpeg|png|webp)$/i.test(materialName(item))),
  };
}

export default function ExperienceCertificationApplyPage({
  go,
  certId,
  certifications,
  notify,
  addNotice = () => {},
  refreshCurrentScreen,
}) {
  const isNew = certId === 'new-experience';
  const certification = isNew
    ? {
        id: 'new-experience',
        type: '其它经历认证',
        title: '一段亲身经历',
        description: '',
        status: '填写中',
        name: '',
        detail: '',
        materials: [],
      }
    : certifications.find((item) => item.id === certId);
  const initialMaterials = existingMaterials(certification);
  const [name, setName] = useState(certification?.name || '');
  const [detail, setDetail] = useState(certification?.detail || '');
  const [archive, setArchive] = useState(initialMaterials.archive);
  const [video, setVideo] = useState(initialMaterials.video);
  const [photos, setPhotos] = useState(initialMaterials.photos);
  const [cameraMode, setCameraMode] = useState(null);
  const [preview, setPreview] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const editable = ['填写中', '退回修改'].includes(certification?.status);

  if (!certification) {
    return <Page title="亲身经历" back={() => go('certExperience')} />;
  }

  const leavePage = () => {
    go('certExperience');
  };

  const selectArchive = (file) => {
    if (!file) return;
    if (!/\.(rar|zip)$/i.test(file.name)) {
      notify('只支持 RAR 或 ZIP 压缩包', 'warning');
      return;
    }
    if (file.size > ONE_GB) {
      notify('压缩包不能超过1GB', 'warning');
      return;
    }
    setArchive(createMaterial(file, 'archive'));
  };

  const selectVideo = (file) => {
    if (!file) return;
    if (file.size > FIVE_HUNDRED_MB) {
      notify('录像不能超过500MB', 'warning');
      return;
    }
    setVideo(createMaterial(file, 'video', `现场录制-${Date.now()}.webm`));
    setCameraMode(null);
  };

  const takePhoto = (blob) => {
    if (!blob) return;
    if (photos.length >= 5) {
      notify('照片最多拍摄5张', 'warning');
      return;
    }
    setPhotos((current) => [
      ...current,
      createMaterial(blob, 'image', `现场拍摄-${Date.now()}.jpg`),
    ]);
    setCameraMode(null);
  };

  const submit = async () => {
    if (!name.trim()) {
      notify('请填写经历标题', 'warning');
      return;
    }
    if (!detail.trim()) {
      notify('请填写经历简述', 'warning');
      return;
    }
    if (!archive) {
      notify('请上传一个 RAR 或 ZIP 压缩包', 'warning');
      return;
    }
    const materials = [archive, video, ...photos].filter(Boolean);
    const files = materials.map((material) => material.file).filter(Boolean);
    if (files.length !== materials.length) {
      notify('请重新拍摄或上传要提交的认证材料', 'warning');
      return;
    }
    try {
      setSubmitting(true);
      await api.submitExperienceCertification(
        isNew ? null : certification.serverId,
        name.trim(),
        detail.trim(),
        files,
      );
      await refreshCurrentScreen();
      notify('已经提交审核', 'success');
      addNotice({ title: '经历认证已经提交', content: `${name.trim()}已进入审核`, screen: 'certExperience' });
      go('certExperience');
    } catch (requestError) {
      notify(requestError.message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Page className="certification-detail-page" title="亲身经历" back={leavePage}>
      {certification.status === '审核中' && (
        <section className="cert-edit-notice reviewing">
          <LockKeyhole />
          <div>
            <h2>正在审核</h2>
            <p>审核完成后会通知你。</p>
          </div>
        </section>
      )}

      {certification.status === '退回修改' && (
        <section className="cert-edit-notice returned">
          <AlertCircle />
          <div>
            <h2>请修改后重新提交</h2>
            <p>{certification.feedback || '请根据审核说明修改内容。'}</p>
          </div>
        </section>
      )}

      {certification.status === '已认证' && (
        <section className="cert-edit-notice approved">
          <LockKeyhole />
          <div>
            <h2>已经通过认证</h2>
            <p>这段经历已经公开显示。</p>
          </div>
        </section>
      )}

      <section className="cert-form combined-cert-form">
        <label>经历标题</label>
        <input
          disabled={!editable}
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="例如：经历过劳动仲裁"
        />
        <label>简述</label>
        <textarea
          disabled={!editable}
          value={detail}
          onChange={(event) => setDetail(event.target.value)}
          placeholder="简单说明事情发生和处理的经过"
        />
      </section>

      <section className="combined-materials experience-materials">
        <header>
          <h2>证明材料</h2>
          <p>上传材料是认证的唯一依据。</p>
        </header>

        <div className="upload-list">
          <MaterialRow
            Icon={FileArchive}
            title="压缩包（必填）"
            description={archive ? materialName(archive) : 'RAR 或 ZIP，最大1GB，最多1个'}
            material={archive}
            materialType="archive"
            editable={editable}
            accept=".rar,.zip,application/zip,application/x-rar-compressed"
            action="上传"
            onChange={(files) => selectArchive(files?.[0])}
            onRemove={() => setArchive('')}
          />
          <MaterialRow
            Icon={Video}
            title="录制录像（选填）"
            description={video ? materialName(video) : '最大500MB，最多1个'}
            material={video}
            materialType="video"
            onPreview={() => setPreview({ material: video, type: 'video' })}
            editable={editable}
            action="录制"
            onCapture={() => setCameraMode('video')}
            onRemove={() => setVideo('')}
          />
          <MaterialRow
            Icon={Image}
            title="拍摄照片（选填）"
            description={photos.length ? `已拍摄${photos.length}张，最多5张` : '现场拍摄，最多5张'}
            editable={editable}
            action="拍摄"
            disabled={photos.length >= 5}
            onCapture={() => setCameraMode('photo')}
          />
          {photos.length > 0 && (
            <div className="photo-material-grid">
              {photos.map((photo, index) => (
                <div className="photo-material-item" key={`${materialName(photo)}-${index}`}>
                  <button
                    type="button"
                    onClick={() => setPreview({ material: photo, type: 'image' })}
                  >
                    {materialUrl(photo) ? (
                      <img src={materialUrl(photo)} alt={`照片${index + 1}`} />
                    ) : (
                      <Image />
                    )}
                    <span>照片{index + 1}</span>
                    <Eye />
                  </button>
                  {editable && (
                    <button
                      className="photo-material-remove"
                      type="button"
                      aria-label={`删除照片${index + 1}`}
                      onClick={() =>
                        setPhotos((current) =>
                          current.filter((_, itemIndex) => itemIndex !== index),
                        )
                      }
                    >
                      <Trash2 />
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {editable && (
        <div className="cert-draft-actions single-action">
          <button type="button" disabled={submitting || !name.trim() || !detail.trim() || !archive} onClick={submit}>
            {submitting ? '正在提交' : '提交认证'}
          </button>
        </div>
      )}
      {cameraMode && (
        <>
          <button className="sheet-mask" type="button" onClick={() => setCameraMode(null)} />
          <CameraCapture
            mode={cameraMode}
            facingMode="environment"
            onCapture={cameraMode === 'photo' ? takePhoto : selectVideo}
            onClose={() => setCameraMode(null)}
            notify={notify}
          />
        </>
      )}
      {preview && (
        <>
          <button className="sheet-mask" type="button" onClick={() => setPreview(null)} />
          <MaterialPreview
            material={preview.material}
            type={preview.type}
            onClose={() => setPreview(null)}
          />
        </>
      )}
    </Page>
  );
}

function MaterialRow({
  Icon,
  title,
  description,
  material,
  materialType,
  onPreview,
  editable,
  accept,
  action,
  disabled = false,
  onChange,
  onCapture,
  onRemove,
}) {
  return (
    <article className={material ? 'material-card completed' : 'material-card'}>
      <i>
        <Icon />
      </i>
      <div>
        <b>{title}</b>
        <small>{description}</small>
      </div>
      {!editable && material && materialType === 'archive' && materialUrl(material) && (
        <a
          className="material-action"
          href={materialUrl(material)}
          download={materialName(material)}
        >
          <Download />
          下载
        </a>
      )}
      {!editable && material && materialType === 'video' && materialUrl(material) && (
        <button className="material-action" type="button" onClick={onPreview}>
          <Eye />
          预览
        </button>
      )}
      {editable && onCapture && (
        <button
          className={`upload-button ${disabled ? 'disabled' : ''}`}
          type="button"
          disabled={disabled}
          onClick={onCapture}
        >
          {disabled ? '已满' : action}
        </button>
      )}
      {editable && !onCapture && (
        <label className={`upload-button ${disabled ? 'disabled' : ''}`}>
          <input
            type="file"
            hidden
            accept={accept}
            disabled={disabled}
            onChange={(event) => {
              onChange(event.target.files);
              event.target.value = '';
            }}
          />
          {disabled ? '已满' : action}
        </label>
      )}
      {editable && material && onRemove && (
        <button className="material-remove" type="button" onClick={onRemove}>
          <Trash2 />
        </button>
      )}
    </article>
  );
}
