import { useState } from 'react';
import {
  AlertCircle,
  Check,
  Eye,
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

const FIVE_HUNDRED_MB = 500 * 1024 * 1024;

const identityRequirements = [
  { title: '身份证正面', description: '现场拍摄，1张', facingMode: 'environment' },
  { title: '身份证反面', description: '现场拍摄，1张', facingMode: 'environment' },
  { title: '手持身份证', description: '现场拍摄，1张', facingMode: 'user' },
];

function splitJobMaterials(materials = []) {
  return {
    video: materials.find((item) => item.kind === 'video' || /\.(mp4|mov|webm)$/i.test(materialName(item))) || '',
    photos: materials.filter((item) => item.kind === 'image' || /\.(jpg|jpeg|png|webp)$/i.test(materialName(item))),
  };
}

export default function BasicCertificationApplyPage({
  go,
  certId,
  certifications,
  notify,
  addNotice = () => {},
  refreshCurrentScreen,
}) {
  const certification = certifications.find((item) => item.id === certId) || certifications[0];
  const identity = certification.type === '实名认证';
  const existingJobMaterials = splitJobMaterials(certification.materials);
  const [identityPhotos, setIdentityPhotos] = useState(() =>
    Object.fromEntries((certification.materials || []).map((file, index) => [index, file])),
  );
  const [video, setVideo] = useState(existingJobMaterials.video);
  const [photos, setPhotos] = useState(existingJobMaterials.photos);
  const [cameraIndex, setCameraIndex] = useState(null);
  const [cameraMode, setCameraMode] = useState(null);
  const [preview, setPreview] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const editable = ['填写中', '退回修改'].includes(certification.status);

  const captureIdentityPhoto = (blob) => {
    setIdentityPhotos((current) => ({
      ...current,
      [cameraIndex]: createMaterial(blob, 'image', `现场拍摄-${Date.now()}.jpg`),
    }));
    setCameraIndex(null);
  };

  const captureJobMaterial = (blob) => {
    if (cameraMode === 'video') {
      if (blob.size > FIVE_HUNDRED_MB) {
        notify('录像不能超过500MB', 'warning');
        return;
      }
      setVideo(createMaterial(blob, 'video', `现场录制-${Date.now()}.webm`));
    } else {
      if (photos.length >= 5) {
        notify('照片最多拍摄5张', 'warning');
        return;
      }
      setPhotos((current) => [
        ...current,
        createMaterial(blob, 'image', `现场拍摄-${Date.now()}.jpg`),
      ]);
    }
    setCameraMode(null);
  };

  const submit = async () => {
    const materials = identity
      ? identityRequirements.map((_, index) => identityPhotos[index])
      : [video, ...photos].filter(Boolean);
    const files = materials.map((material) => material.file).filter(Boolean);
    if (files.length !== materials.length) {
      notify('请重新拍摄或上传全部认证材料', 'warning');
      return;
    }
    try {
      setSubmitting(true);
      await api.submitBasicCertification(
        identity ? 'IDENTITY' : 'MAIN_JOB',
        '',
        files,
      );
      await refreshCurrentScreen();
      notify('已经提交审核', 'success');
      addNotice({ title: '认证已经提交', content: `${identity ? '身份信息' : '岗位材料'}已进入审核`, screen: 'certWork' });
      go('certWork');
    } catch (requestError) {
      notify(requestError.message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const identityCompleted = Object.keys(identityPhotos).length === identityRequirements.length;
  const jobCompleted = Boolean(video || photos.length);

  return (
    <Page
      className="certification-detail-page"
      title={identity ? '身份信息' : '我的岗位'}
      back={() => go('certWork')}
    >
      <CertificationStatus certification={certification} />

      <section className="combined-materials">
        <header>
          <h2>{certification.title}</h2>
          <p>上传的材料是认证的唯一依据。</p>
        </header>

        {identity ? (
          <IdentityMaterials
            editable={editable}
            photos={identityPhotos}
            onCapture={setCameraIndex}
            onPreview={(material) => setPreview({ material, type: 'image' })}
          />
        ) : (
          <JobMaterials
            editable={editable}
            video={video}
            photos={photos}
            onCapture={setCameraMode}
            onPreview={setPreview}
            onRemoveVideo={() => setVideo('')}
            onRemovePhoto={(index) =>
              setPhotos((current) => current.filter((_, itemIndex) => itemIndex !== index))
            }
          />
        )}

      </section>

      {editable && (
        <div className="cert-draft-actions single-action">
          <button
            type="button"
            disabled={submitting || (identity ? !identityCompleted : !jobCompleted)}
            onClick={submit}
          >
            {submitting ? '正在提交' : '提交认证'}
          </button>
        </div>
      )}

      {cameraIndex !== null && (
        <CameraLayer
          mode="photo"
          facingMode={identityRequirements[cameraIndex].facingMode}
          onCapture={captureIdentityPhoto}
          onClose={() => setCameraIndex(null)}
          notify={notify}
        />
      )}
      {cameraMode && (
        <CameraLayer
          mode={cameraMode}
          facingMode="environment"
          onCapture={captureJobMaterial}
          onClose={() => setCameraMode(null)}
          notify={notify}
        />
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

function CertificationStatus({ certification }) {
  if (certification.status === '审核中') {
    return <StatusNotice className="reviewing" title="正在审核" text="审核完成后会通知你。" />;
  }
  if (certification.status === '退回修改') {
    return (
      <StatusNotice
        className="returned"
        title="请修改后重新提交"
        text={certification.feedback || '请根据审核说明修改内容。'}
        Icon={AlertCircle}
      />
    );
  }
  if (certification.status === '已认证') {
    return <StatusNotice className="approved" title="已经通过认证" text="认证内容已显示在你的个人页面。" />;
  }
  return null;
}

function StatusNotice({ className, title, text, Icon = LockKeyhole }) {
  return (
    <section className={`cert-edit-notice ${className}`}>
      <Icon />
      <div><h2>{title}</h2><p>{text}</p></div>
    </section>
  );
}

function IdentityMaterials({ editable, photos, onCapture, onPreview }) {
  return (
    <div className="upload-list">
      {identityRequirements.map((requirement, index) => {
        const material = photos[index];
        return (
          <article className={material ? 'material-card completed' : 'material-card'} key={requirement.title}>
            <i className={material ? 'done' : ''}>{material ? <Check /> : <Image />}</i>
            <div><b>{requirement.title}</b><small>{material ? materialName(material) : requirement.description}</small></div>
            {!editable && material && materialUrl(material) && (
              <button className="material-action" type="button" onClick={() => onPreview(material)}><Eye />预览</button>
            )}
            {editable && (
              <button className="upload-button" type="button" onClick={() => onCapture(index)}>
                {material ? '重新拍摄' : '拍摄'}
              </button>
            )}
          </article>
        );
      })}
    </div>
  );
}

function JobMaterials({
  editable,
  video,
  photos,
  onCapture,
  onPreview,
  onRemoveVideo,
  onRemovePhoto,
}) {
  return (
    <div className="upload-list">
      <JobMaterialRow
        Icon={Video}
        title="录制录像（二选一）"
        description={video ? materialName(video) : '与拍摄照片至少提交一种，最大500MB'}
        material={video}
        editable={editable}
        onCapture={() => onCapture('video')}
        onPreview={() => onPreview({ material: video, type: 'video' })}
        onRemove={onRemoveVideo}
      />
      <JobMaterialRow
        Icon={Image}
        title="拍摄照片（二选一）"
        description={photos.length ? `已拍摄${photos.length}张，最多5张` : '与录制录像至少提交一种，最多5张'}
        editable={editable}
        disabled={photos.length >= 5}
        onCapture={() => onCapture('photo')}
      />
      {photos.length > 0 && (
        <div className="photo-material-grid">
          {photos.map((photo, index) => (
            <div className="photo-material-item" key={`${materialName(photo)}-${index}`}>
              <button type="button" onClick={() => onPreview({ material: photo, type: 'image' })}>
                {materialUrl(photo) ? <img src={materialUrl(photo)} alt={`照片${index + 1}`} /> : <Image />}
                <span>照片{index + 1}</span><Eye />
              </button>
              {editable && (
                <button className="photo-material-remove" type="button" aria-label={`删除照片${index + 1}`} onClick={() => onRemovePhoto(index)}>
                  <Trash2 />
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function JobMaterialRow({
  Icon,
  title,
  description,
  material,
  editable,
  disabled = false,
  onCapture,
  onPreview,
  onRemove,
}) {
  return (
    <article className={material ? 'material-card completed' : 'material-card'}>
      <i><Icon /></i>
      <div><b>{title}</b><small>{description}</small></div>
      {!editable && material && onPreview && materialUrl(material) && (
        <button className="material-action" type="button" onClick={onPreview}><Eye />预览</button>
      )}
      {editable && onCapture && (
        <button className={`upload-button ${disabled ? 'disabled' : ''}`} type="button" disabled={disabled} onClick={onCapture}>
          {disabled ? '已满' : material ? '重新录制' : title === '拍摄照片' ? '拍摄' : '录制'}
        </button>
      )}
      {editable && material && onRemove && (
        <button className="material-remove" type="button" onClick={onRemove}><Trash2 /></button>
      )}
    </article>
  );
}

function CameraLayer({ mode, facingMode, onCapture, onClose, notify }) {
  return (
    <>
      <button className="sheet-mask" type="button" onClick={onClose} />
      <CameraCapture
        mode={mode}
        facingMode={facingMode}
        onCapture={onCapture}
        onClose={onClose}
        notify={notify}
      />
    </>
  );
}
