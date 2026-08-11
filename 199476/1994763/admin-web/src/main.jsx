import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './app/App.jsx';
import AppErrorBoundary from './components/feedback/AppErrorBoundary.jsx';
import GlobalMessage from './components/feedback/GlobalMessage.jsx';
import './styles/global.css';
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <AppErrorBoundary>
      <BrowserRouter>
        <GlobalMessage />
        <App />
      </BrowserRouter>
    </AppErrorBoundary>
  </React.StrictMode>,
);
