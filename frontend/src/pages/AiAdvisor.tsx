import { useState } from 'react'
import { api } from '../api/client'
import { Sparkles, RefreshCw } from 'lucide-react'

type Status = 'idle' | 'loading' | 'success' | 'error'

export default function AiAdvisor() {
  const [status, setStatus] = useState<Status>('idle')
  const [insight, setInsight] = useState('')
  const [errorMsg, setErrorMsg] = useState('')

  async function generate() {
    setStatus('loading')
    try {
      const { data } = await api.get('/ai/insights')
      setInsight(data.insight)
      setStatus('success')
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || 'AI Advisor is temporarily unavailable. Please try again shortly.')
      setStatus('error')
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="text-center">
        <div className="w-12 h-12 bg-brand-500/10 rounded-xl flex items-center justify-center mx-auto mb-3">
          <Sparkles className="text-brand-500" size={24} />
        </div>
        <h1 className="text-xl font-semibold">AI Financial Advisor</h1>
        <p className="text-sm text-gray-500 mt-1">Personalized recommendations based on your spending, powered by Gemini.</p>
      </div>

      <div className="card p-6 min-h-[220px] flex flex-col">
        {status === 'idle' && (
          <div className="flex-1 flex flex-col items-center justify-center text-center">
            <p className="text-sm text-gray-500 mb-4">Generate insights based on this month's spending and budgets.</p>
            <button className="btn-primary flex items-center gap-2" onClick={generate}>
              <Sparkles size={16} /> Generate Insights
            </button>
          </div>
        )}

        {status === 'loading' && (
          <div className="flex-1 flex flex-col items-center justify-center gap-3">
            <div className="skeleton h-4 w-3/4" />
            <div className="skeleton h-4 w-full" />
            <div className="skeleton h-4 w-5/6" />
            <p className="text-xs text-gray-400 mt-2">Analyzing your spending…</p>
          </div>
        )}

        {status === 'success' && (
          <div className="flex-1">
            <p className="whitespace-pre-line text-sm leading-relaxed">{insight}</p>
            <button className="btn-secondary flex items-center gap-2 mt-6 text-sm" onClick={generate}>
              <RefreshCw size={14} /> Regenerate
            </button>
          </div>
        )}

        {status === 'error' && (
          <div className="flex-1 flex flex-col items-center justify-center text-center">
            <p className="text-sm text-red-500 mb-4">{errorMsg}</p>
            <button className="btn-secondary flex items-center gap-2" onClick={generate}>
              <RefreshCw size={14} /> Try Again
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
