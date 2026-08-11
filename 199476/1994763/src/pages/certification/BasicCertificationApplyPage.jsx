import { useState } from 'react';
import { AlertCircle, Check, Download, Eye, FileCheck2, Image, LockKeyhole } from 'lucide-react';
import CameraCapture from '../../components/certification/CameraCapture.jsx';
import MaterialPreview, {
  createMaterial,
  materialName,
  materialUrl,
} from '../../components/certification/MaterialPreview.jsx';
import Page from '../../components/layout/Page.jsx';
import './CertificationPages.css';

const identityRequirements = [
  {
    title: '身份证正面',
    description: '现场拍摄，1张',
    accept: 'image/*',
    capture: 'environment',
    action: '拍摄',
    icon: Image,
  },
  {
    title: '身份证反面',
    description: '现场拍摄，1张',
    accept: 'image/*',
    capture: 'environment',
    action: '拍摄',
    icon: Image,
  },
  {
    title: '手持身份证',
    description: '现场拍摄，1张',
    accept: 'image/*',
    capture: 'user',
    action: '拍摄',
    icon: Image,
  },
];

const jobRequirements = [
  {
    title: '在职或从业证明',
    description: 'JPG、PNG 或 PDF，1份',
    accept: '.jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf',
    icon: FileCheck2,
  },
  {
    title: '工作年限证明',
    description: 'JPG、PNG 或 PDF，1份',
    accept: '.jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf',
    icon: FileCheck2,
  },
  {
    title: '职业或岗位证明',
    description: 'JPG、PNG 或 PDF，1份',
    accept: '.jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf',
    icon: FileCheck2,
  },
];

export default function BasicCertificationApplyPage({
  go,
  certId,
  certifications,
  setCertifications,
  notify,
  addNotice = () => {},
}) {
  const certification = certifications.find((item) => item.id === certId) || certifications[0];
  const identity = certification.type === '实名认证';
  const requirements = identity ? identityRequirements : jobRequirements;
  const [fileNames, setFileNames] = useState(() =>
    Object.fromEntries((certification.materials || []).map((file, index) => [index, file])),
  );
  const [error, setError] = useState('');
  const [cameraIndex, setCameraIndex] = useState(null);
  const [previewMaterial, setPreviewMaterial] = useState(null);
  const editable = ['填写中', '退回修改'].includes(certification.status);
  const completedCount = Object.keys(fileNames).length;

  const upload = (index, file) => {
    if (!file) return;
    const allowedTypes = ['image/jpeg', 'image/png', 'application/pdf'];
    const allowedExtension = /\.(jpg|jpeg|png|pdf)$/i.test(file.name);
    if (!allowedTypes.includes(file.type) && !allowedExtension) {
      setError('只支持 JPG、PNG 或 PDF 文件');
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      setError('单个文件不能超过20MB');
      return;
    }

    setError('');
    setFileNames((current) => ({
      ...current,
      [index]: createMaterial(file, file.type === 'application/pdf' ? 'document' : 'image'),
    }));
  };

  const captureIdentityPhoto = (blob) => {
    setFileNames((current) => ({
      ...current,
      [cameraIndex]: createMaterial(blob, 'image', `现场拍摄-${Date.now()}.jpg`),
    }));
    setCameraIndex(null);
    setError('');
  };

  const submit = () => {
    const materials = requirements.map((_, index) => fileNames[index]);
    setCertifications((current) =>
      current.map((item) =>
        item.id === certification.id
          ? {
              ...item,
              materials,
              status: '审核中',
            }
          : item,
      ),
    );
    notify('已经提交审核');
    addNotice({
      title: '认证已经提交',
      content: `${certification.title}已进入审核`,
      screen: 'certWork',
    });
    go('certWork');
  };

  return (
    <Page
      className="certification-detail-page"
      title={identity ? '身份信息' : '我的岗位'}
      back={() => go('certWork')}
    >
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
            <p>认证内容已显示在你的个人页面。</p>
          </div>
        </section>
      )}

      <section className="combined-materials">
        <header>
          <h2>{certification.title}</h2>
          <p>按下面的要求逐项完成认证。</p>
        </header>
        <div className="upload-list">
          {requirements.map((requirement, index) => {
            const MaterialIcon = requirement.icon;
            const completed = Boolean(fileNames[index]);
            return (
              <article
                className={completed ? 'material-card completed' : 'material-card'}
                key={requirement.title}
              >
                <i className={completed ? 'done' : ''}>
                  {completed ? <Check /> : <MaterialIcon />}
                </i>
                <div>
                  <b>{requirement.title}</b>
                  <small>
                    {completed ? materialName(fileNames[index]) : requirement.description}
                  </small>
                </div>
                {!editable && completed && materialUrl(fileNames[index]) && identity && (
                  <button
                    className="material-action"
                    type="button"
                    onClick={() => setPreviewMaterial(fileNames[index])}
                  >
                    <Eye />
                    预览
                  </button>
                )}
                {!editable && completed && materialUrl(fileNames[index]) && !identity && (
                  <a
                    className="material-action"
                    href={materialUrl(fileNames[index])}
                    download={materialName(fileNames[index])}
                  >
                    <Download />
                    下载
                  </a>
                )}
                {editable && identity && (
                  <button
                    className="upload-button"
                    type="button"
                    onClick={() => setCameraIndex(index)}
                  >
                    {completed ? '重新拍摄' : '拍摄'}
                  </button>
                )}
                {editable && !identity && (
                  <label className="upload-button">
                    <input
                      type="file"
                      hidden
                      accept={requirement.accept}
                      onChange={(event) => upload(index, event.target.files?.[0])}
                    />
                    {completed ? '重新上传' : '上传'}
                  </label>
                )}
              </article>
            );
          })}
        </div>
        {error && <small className="cert-material-error">{error}</small>}
      </section>

      {editable && (
        <div className="cert-draft-actions single-action">
          <button type="button" disabled={completedCount !== requirements.length} onClick={submit}>
            提交认证
          </button>
        </div>
      )}

      {cameraIndex !== null && (
        <>
          <button className="sheet-mask" type="button" onClick={() => setCameraIndex(null)} />
          <CameraCapture
            mode="photo"
            facingMode={cameraIndex === 2 ? 'user' : 'environment'}
            onCapture={captureIdentityPhoto}
            onClose={() => setCameraIndex(null)}
          />
        </>
      )}
      {previewMaterial && (
        <>
          <button className="sheet-mask" type="button" onClick={() => setPreviewMaterial(null)} />
          <MaterialPreview
            material={previewMaterial}
            type="image"
            onClose={() => setPreviewMaterial(null)}
          />
        </>
      )}
    </Page>
  );
}
