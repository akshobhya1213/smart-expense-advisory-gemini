import { Component, ErrorInfo, ReactNode } from 'react'
import { AlertTriangle } from 'lucide-react'

interface Props { children: ReactNode }
interface State { hasError: boolean }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // Logged to the browser console only — never sent anywhere, no internals shown to the user.
    console.error('Unhandled UI error:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-surface px-4">
          <div className="card p-8 max-w-sm text-center">
            <AlertTriangle className="mx-auto mb-3 text-amber-500" size={32} />
            <p className="font-medium text-gray-700 mb-1">Something went wrong.</p>
            <p className="text-sm text-gray-500 mb-4">Please refresh the page.</p>
            <button className="btn-primary" onClick={() => window.location.reload()}>Refresh</button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
