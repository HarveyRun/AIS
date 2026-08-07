import { ArrowLeft } from 'lucide-react';

export default function Page({ title, back, children }) {
  return (
    <div className="page">
      <header className="page-head">
        <button type="button" onClick={back} aria-label="返回">
          <ArrowLeft />
        </button>
        <b>{title}</b>
        <span />
      </header>
      {children}
    </div>
  );
}
