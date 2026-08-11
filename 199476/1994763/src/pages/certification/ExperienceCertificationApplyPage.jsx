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
import './CertificationPages.css';

const ONE_GB = 1024 * 1024 * 1024;
const FIVE_HUNDRED_MB = 500 * 1024 * 1024;

function existingMaterials(certification) {
  const materials = certification?.materials || [];
  return {
    archive: materials.find((item) => /\.(rar|zip)$/i.test(materialName(item))) || '',
    video: materials.find((item) => /\.(mp4|mov|webm)$/i.test(materialName(item))) || '',
    photos: materials.filter((item) => /\.(jpg|jpeg|png|webp)$/i.test(materialName(item))),
  };
}

export default function ExperienceCertificationApplyPage({
  go,
  certId,
  certifications,
  setCertifications,
  notify,
  addNotice = () => {},
}) {
  const certification = certifications.find((item) => item.id === certId);
  const initialMaterials = existingMaterials(certification);
  const [name, setName] = useState(certification?.name || '');
  const [detail, setDetail] = useState(certification?.detail || '');
  const [archive, setArchive] = useState(initialMaterials.archive);
  const [video, setVideo] = useState(initialMaterials.video);
  const [photos, setPhotos] = useState(initialMaterials.photos);
  const [error, setError] = useState('');
  const [cameraMode, setCameraMode] = useState(null);
  const [preview, setPreview] = useState(null);
  const editable = ['填写中', '退回修改'].includes(certification?.status);

  if (!certification) {
    return <Page title="亲身经历" back={() => go('certExperience')} />;
  }

  const leavePage = () => {
    if (certification.isNew && certification.status === '填写中') {
      setCertifications((current) => current.filter((item) => item.id !== certification.id));
    }
    go('certExperience');
  };

  const selectArchive = (file) => {
    if (!file) return;
    if (!/\.(rar|zip)$/i.test(file.name)) {
      setError('只支持 RAR 或 ZIP 压缩包');
      return;
    }
    if (file.size > ONE_GB) {
      setError('压缩包不能超过1GB');
      return;
    }
    setError('');
    setArchive(createMaterial(file, 'archive'));
  };

  const selectVideo = (file) => {
    if (!file) return;
    if (file.size > FIVE_HUNDRED_MB) {
      setError('录像不能超过500MB');
      return;
    }
    setError('');
    setVideo(createMaterial(file, 'video', `现场录制-${Date.now()}.webm`));
    setCameraMode(null);
  };

  const takePhoto = (blob) => {
    if (!blob) return;
    if (photos.length >= 5) {
      setError('照片最多拍摄5张');
      return;
    }
    setError('');
    setPhotos((current) => [
      ...current,
      createMaterial(blob, 'image', `现场拍摄-${Date.now()}.jpg`),
    ]);
    setCameraMode(null);
  };

  const submit = () => {
    const materials = [archive, video, ...photos].filter(Boolean);
    setCertifications((current) =>
      current.map((item) =>
        item.id === certification.id
          ? {
              ...item,
              name,
              title: name,
              description: detail,
              detail,
              materials,
              status: '审核中',
              isNew: false,
            }
          : item,
      ),
    );
    notify('已经提交审核');
    addNotice({
      title: '经历认证已经提交',
      content: `${name}已进入审核`,
      screen: 'certExperience',
    });
    go('certExperience');
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
            title="压缩包"
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
            title="录制录像"
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
            title="拍摄照片"
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
        {error && <small className="cert-material-error">{error}</small>}
      </section>

      {editable && (
        <div className="cert-draft-actions single-action">
          <button type="button" disabled={!name.trim() || !detail.trim()} onClick={submit}>
            提交认证
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
