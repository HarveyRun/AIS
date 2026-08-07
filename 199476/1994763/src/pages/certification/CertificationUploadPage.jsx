import { useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  Banknote,
  Bell,
  BriefcaseBusiness,
  Building2,
  CalendarDays,
  Camera,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleUserRound,
  Clock3,
  Coins,
  FileCheck2,
  HeartHandshake,
  Info,
  Landmark,
  LockKeyhole,
  MessageCircleMore,
  MessageSquareWarning,
  MoreHorizontal,
  PlusCircle,
  Search,
  Send,
  ShieldCheck,
  SlidersHorizontal,
  Smartphone,
  Sparkles,
  Star,
  UsersRound,
  WalletCards,
  X,
  Zap,
} from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import CertItem from '../../components/certification/CertItem.jsx';
import './CertificationPages.css';

export default function CertificationUploadPage({ go, type, notify }) {
  const identity = type === '实名认证';
  const list = identity
    ? ['身份证正面', '身份证反面', '手持身份证']
    : ['从业或经历证明压缩包', '平台指定录像材料', '平台指定现场拍照'];
  const [done, setDone] = useState([]);
  const [fileNames, setFileNames] = useState({});
  const upload = (index, file) => {
    if (!file) return;
    if (!identity && index === 0 && !file.name.toLowerCase().endsWith('.zip')) {
      notify('请上传 .zip 压缩包');
      return;
    }
    setDone((current) => [...new Set([...current, index])]);
    setFileNames((current) => ({ ...current, [index]: file.name }));
  };
  return (
    <Page title="提交证明材料" back={() => go('certApply')}>
      <section className="upload-head">
        <span>{type}</span>
        <h1>请准备好下面这些材料</h1>
        <p>
          {identity ? '确认身份需要下面三项材料。' : '请按下面的要求逐项提交，我们会认真核对。'}
        </p>
      </section>
      <section className="upload-list">
        {list.map((x, i) => (
          <article key={x}>
            <i className={done.includes(i) ? 'done' : ''}>{done.includes(i) ? <Check /> : i + 1}</i>
            <div>
              <b>{x}</b>
              <small>
                {fileNames[i] ||
                  (identity
                    ? '按当前清单拍照提交'
                    : i === 0
                      ? '请上传 .zip 压缩包'
                      : i === 1
                        ? '按清单要求现场录像'
                        : '按清单要求现场拍照')}
              </small>
            </div>
            <label className="upload-button">
              <input
                type="file"
                hidden
                accept={
                  identity
                    ? 'image/*'
                    : i === 0
                      ? '.zip,application/zip'
                      : i === 1
                        ? 'video/*'
                        : 'image/*'
                }
                capture={identity || i === 2 ? 'environment' : undefined}
                onChange={(event) => upload(i, event.target.files?.[0])}
              />
              {done.includes(i)
                ? '已上传'
                : i === 0 && !identity
                  ? '选压缩包'
                  : i === 1 && !identity
                    ? '开始录像'
                    : '开始拍照'}
            </label>
          </article>
        ))}
      </section>
      <button
        disabled={done.length !== list.length}
        className="sticky-primary"
        onClick={() => {
          notify('材料已经提交，请耐心等待结果');
          go('certs');
        }}
      >
        提交材料
      </button>
    </Page>
  );
}
