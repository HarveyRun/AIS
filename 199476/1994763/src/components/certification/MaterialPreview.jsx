import { Download, X } from 'lucide-react';

export function materialName(material) {
  return typeof material === 'string' ? material : material?.name || '';
}

export function materialUrl(material) {
  return typeof material === 'object' ? material?.url || '' : '';
}

export function createMaterial(file, kind, name = file.name) {
  return {
    kind,
    name,
    size: file.size,
    url: URL.createObjectURL(file),
  };
}

export function formatMaterialSize(size) {
  if (!size) return '';
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)}MB`;
  return `${Math.max(1, Math.round(size / 1024))}KB`;
}

export default function MaterialPreview({ material, type, onClose }) {
  const url = materialUrl(material);

  return (
    <div className="material-preview" role="dialog" aria-modal="true">
      <header>
        <div>
          <b>{materialName(material)}</b>
          {material?.size && <span>{formatMaterialSize(material.size)}</span>}
        </div>
        <button type="button" aria-label="关闭预览" onClick={onClose}>
          <X />
        </button>
      </header>
      <div className="material-preview-content">
        {type === 'image' && <img src={url} alt={materialName(material)} />}
        {type === 'video' && <video src={url} controls playsInline />}
      </div>
      {url && (
        <a href={url} download={materialName(material)}>
          <Download />
          保存到本机
        </a>
      )}
    </div>
  );
}
