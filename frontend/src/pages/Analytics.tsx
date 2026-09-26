import { useEffect, useState } from 'react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { api } from '../api/client'
import { MonthlySummary, CategoryBreakdown, BudgetAnalytics } from '../types'
import { StatCard, CardSkeletonGrid, ErrorState, PageHeader } from '../components/Common'

function formatCurrency(n: number | null | undefined) {
  return `₹${(n ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`
}

export default function Analytics() {
  const now = new Date()
  const [year] = useState(now.getFullYear())
  const [month] = useState(now.getMonth() + 1)
  const [summary, setSummary] = useState<MonthlySummary | null>(null)
  const [breakdown, setBreakdown] = useState<CategoryBreakdown | null>(null)
  const [budgetAnalytics, setBudgetAnalytics] = useState<BudgetAnalytics | null>(null)
  const [trend, setTrend] = useState<{ date: string; amount: number }[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  async function load() {
    setLoading(true)
    setError(false)
    try {
      const startDate = `${year}-${String(month).padStart(2, '0')}-01`
      const endDate = new Date(year, month, 0).toISOString().slice(0, 10)

      const [summaryRes, breakdownRes, budgetRes, trendRes] = await Promise.all([
        api.get<MonthlySummary>('/analytics/monthly', { params: { year, month } }),
        api.get<CategoryBreakdown>('/analytics/category', { params: { year, month } }),
        api.get<BudgetAnalytics>('/analytics/budget', { params: { year, month } }),
        api.get('/analytics/trends', { params: { granularity: 'daily', startDate, endDate } }),
      ])
      setSummary(summaryRes.data)
      setBreakdown(breakdownRes.data)
      setBudgetAnalytics(budgetRes.data)
      setTrend(trendRes.data.points.map((p: any) => ({ date: p.date.slice(5), amount: p.amount })))
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  if (loading) return <CardSkeletonGrid count={4} />
  if (error) return <ErrorState onRetry={load} />

  return (
    <div className="space-y-6">
      <PageHeader title="Analytics" subtitle={`${now.toLocaleString('default', { month: 'long' })} ${year}`} />

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total Spending" value={formatCurrency(summary?.totalExpenses ?? 0)} />
        <StatCard label="Previous Month" value={formatCurrency(summary?.previousMonthExpenses ?? 0)} />
        <StatCard label="Change vs Last Month" value={`${(summary?.changePercent ?? 0).toFixed(1)}%`}
          trend={(summary?.changePercent ?? 0) >= 0 ? 'up' : 'down'} />
        <StatCard label="Avg Daily Spend" value={formatCurrency(summary?.averageDailySpend ?? 0)} />
      </div>

      <div className="card p-5">
        <h2 className="font-semibold mb-4">Daily Spending Trend</h2>
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={trend}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
              <XAxis dataKey="date" fontSize={12} stroke="#9CA3AF" />
              <YAxis fontSize={12} stroke="#9CA3AF" />
              <Tooltip formatter={(v: number) => formatCurrency(v)} />
              <Line type="monotone" dataKey="amount" stroke="#4F7CFF" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid lg:grid-cols-2 gap-4">
        <div className="card p-5">
          <h2 className="font-semibold mb-4">Category Comparison</h2>
          <div className="space-y-3">
            {breakdown?.categories.map(c => (
              <div key={c.categoryId}>
                <div className="flex justify-between text-sm mb-1">
                  <span>{c.categoryName}</span>
                  <span className="text-gray-500">{formatCurrency(c.totalAmount)} · {c.transactionCount} txns</span>
                </div>
                <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div className="h-full bg-brand-500 rounded-full" style={{ width: `${c.percentage}%` }} />
                </div>
              </div>
            ))}
          </div>
          <p className="text-sm text-gray-500 mt-4">
            Highest spending category: <span className="font-medium text-ink-800">{breakdown?.highestSpendingCategory}</span>
          </p>
        </div>

        <div className="card p-5">
          <h2 className="font-semibold mb-4">Budget Utilization</h2>
          <StatCard label="Overall Utilization" value={`${(budgetAnalytics?.overallUtilization ?? 0).toFixed(1)}%`}
            sub={`${formatCurrency(budgetAnalytics?.totalSpent ?? 0)} of ${formatCurrency(budgetAnalytics?.totalBudget ?? 0)}`} />
          <div className="mt-4 space-y-2">
            {budgetAnalytics?.categoryBudgets.map(b => (
              <div key={b.id} className="flex justify-between text-sm">
                <span>{b.categoryName ?? 'Overall'}</span>
                <span className={`font-medium ${b.status === 'EXCEEDED' ? 'text-red-500' : b.status === 'APPROACHING' ? 'text-amber-500' : 'text-green-600'}`}>
                  {b.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
