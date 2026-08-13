import './Pagination.css';

export default function Pagination({ page, size, total, onChange }) {
  const pages = Math.max(1, Math.ceil(total / size));

  return (
    <div className="pagination">
      <span>
        第 {page + 1} / {pages} 页
      </span>
      <button type="button" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        上一页
      </button>
      <button type="button" disabled={page + 1 >= pages} onClick={() => onChange(page + 1)}>
        下一页
      </button>
    </div>
  );
}
