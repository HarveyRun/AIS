import { Component } from 'react';
import './AppErrorBoundary.css';

export default class AppErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { failed: false };
  }

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch(error) {
    console.error('管理端页面运行异常', error);
  }

  render() {
    if (this.state.failed) {
      return (
        <div className="admin-fatal-error">
          <section>
            <h1>页面暂时无法显示</h1>
            <p>请刷新页面重试。如果问题仍然存在，请检查后端服务是否正常。</p>
            <button type="button" onClick={() => window.location.reload()}>
              重新加载
            </button>
          </section>
        </div>
      );
    }
    return this.props.children;
  }
}
