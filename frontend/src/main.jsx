import { Component, StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import './styles.css';

// If a screen ever crashes, show a message instead of a blank white page.
class ErrorBoundary extends Component {
  state = { failed: false };
  static getDerivedStateFromError() { return { failed: true }; }
  componentDidCatch(error) { console.error('UI error:', error); }
  render() {
    if (!this.state.failed) return this.props.children;
    return (
      <div className="crash">
        <h1>Something went wrong</h1>
        <p>The page hit an unexpected problem. Reloading usually fixes it.</p>
        <button type="button" className="btn btn-primary btn-md" onClick={() => window.location.reload()}>Reload</button>
      </div>
    );
  }
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ErrorBoundary>
      <AuthProvider>
        <ToastProvider>
          <App />
        </ToastProvider>
      </AuthProvider>
    </ErrorBoundary>
  </StrictMode>,
);
