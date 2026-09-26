import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts'
import { api } from '../api/client'
import { MonthlySummary, CategoryBreakdown, Budget, Expense, Page } from '../types'
import { StatCard, CardSkeletonGrid, ErrorState, EmptyState, PageHeader } from '../components/Common'
import { Plus, PiggyBank, BarChart3, Sparkles } from 'lucide-react'

const COLORS = ['#4F7CFF', '#22C55E', '#F59E0B', '#EF4444', '#8B5CF6', '#06B6D4', '#EC4899', '#84CC16']

function formatCurrency(n: number | null | undefined) {
  return `₹${(n ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`
}

export default function Dashboard() {
  const [summary, setSummary] = useState<MonthlySummary | null>(null)
  const [breakdown, setBreakdown] = useState<CategoryBreakdown | null>(null)
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [recent, setRecent] = useState<Expense[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1

  async function load() {
    setLoading(true)
    setError(false)
    try {
      const [summaryRes, breakdownRes, budgetRes, expensesRes] = await Promise.all([
        api.get<MonthlySummary>('/analytics/monthly', { params: { year, month } }),
        api.get<CategoryBreakdown>('/analytics/category', { params: { year, month } }),
        api.get<Budget[]>('/budgets', { params: { year, month } }),
        api.get<Page<Expense>>('/expenses', { params: { size: 5, sort: 'date,desc' } }),
      ])
      setSummary(summaryRes.data)
      setBreakdown(breakdownRes.data)
      setBudgets(budgetRes.data)
      setRecent(expensesRes.data.content)
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  if (loading) {
    return (
      <div className="space-y-6">
        <CardSkeletonGrid count={4} />
      </div>
    )
  }

  if (error) return <ErrorState onRetry={load} />

  const netBalance = (summary?.totalExpenses ?? 0)
  const savingsRate = summary && summary.totalExpenses > 0
    ? Math.max(0, 100 - summary.changePercent)
    : 0
  const totalBudget = budgets.reduce((s, b) => s + b.limitAmount, 0)
  const overallUtil = totalBudget > 0 ? (summary!.totalExpenses / totalBudget) * 100 : 0

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        subtitle={`${now.toLocaleString('default', { month: 'long' })} ${year}`}
        action={
          <div className="flex gap-2">
            <Link to="/expenses" className="btn-primary flex items-center gap-1.5 text-sm">
              <Plus size={16} /> Add Expense
            </Link>
          </div>
        }
      />

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total Spending" value={formatCurrency(summary?.totalExpenses ?? 0)}
          sub={`${(summary?.changePercent ?? 0) >= 0 ? '+' : ''}${(summary?.changePercent ?? 0).toFixed(1)}% vs last month`}
          trend={summary && summary.changePercent >= 0 ? 'up' : 'down'} />
        <StatCard label="Monthly Budget" value={formatCurrency(totalBudget)} sub={`${(overallUtil ?? 0).toFixed(0)}% utilized`} />
        <StatCard label="Transactions" value={String(summary?.transactionCount ?? 0)} />
        <StatCard label="Net Balance" value={formatCurrency(netBalance)} sub="This month" />
      </div>

      <div className="grid lg:grid-cols-3 gap-4">
        <div className="card p-5 lg:col-span-2">
          <h2 className="font-semibold mb-4">Category Breakdown</h2>
          {breakdown && breakdown.categories.length > 0 ? (
            <div className="flex items-center gap-6 flex-wrap">
              <div className="w-48 h-48">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={breakdown.categories} dataKey="totalAmount" nameKey="categoryName"
                         innerRadius={50} outerRadius={80} paddingAngle={2}>
                      {breakdown.categories.map((_, i) => (
                        <Cell key={i} fill={COLORS[i % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(v: number) => formatCurrency(v)} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <div className="flex-1 space-y-2 min-w-[200px]">
                {breakdown.categories.map((c, i) => (
                  <div key={c.categoryId} className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-full" style={{ background: COLORS[i % COLORS.length] }} />
                      {c.categoryName}
                    </div>
                    <div className="text-gray-500">{formatCurrency(c.totalAmount)} · {(c.percentage ?? 0).toFixed(0)}%</div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <EmptyState title="No expenses found." subtitle="Add your first expense to start tracking." />
          )}
        </div>

        <div className="card p-5">
          <h2 className="font-semibold mb-4">Budgets</h2>
          <div className="space-y-4">
            {budgets.length === 0 && <EmptyState title="No budgets set." subtitle="Create one to track utilization." />}
            {budgets.map(b => (
              <div key={b.id}>
                <div className="flex justify-between text-sm mb-1">
                  <span className="font-medium">{b.categoryName ?? 'Overall'}</span>
                  <span className="text-gray-500">{formatCurrency(b.spent)} / {formatCurrency(b.limitAmount)}</span>
                </div>
                <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full ${b.status === 'EXCEEDED' ? 'bg-red-500' : b.status === 'APPROACHING' ? 'bg-amber-500' : 'bg-brand-500'}`}
                    style={{ width: `${Math.min(100, b.utilizationPercent ?? 0)}%` }}
                  />
                </div>
                <p className="text-xs text-gray-400 mt-1">
                  {(b.utilizationPercent ?? 0).toFixed(0)}% · {formatCurrency(Math.max(0, b.remaining ?? 0))} remaining
                </p>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="card p-5">
        <div className="flex justify-between items-center mb-4">
          <h2 className="font-semibold">Recent Transactions</h2>
          <Link to="/expenses" className="text-sm text-brand-600 font-medium">View all</Link>
        </div>
        {recent.length === 0 ? (
          <EmptyState title="No expenses found." subtitle="Add your first expense to start tracking." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-400 border-b border-gray-100">
                  <th className="py-2 font-medium">Description</th>
                  <th className="py-2 font-medium">Category</th>
                  <th className="py-2 font-medium">Date</th>
                  <th className="py-2 font-medium">Payment</th>
                  <th className="py-2 font-medium text-right">Amount</th>
                </tr>
              </thead>
              <tbody>
                {recent.map(e => (
                  <tr key={e.id} className="border-b border-gray-50 last:border-0">
                    <td className="py-2.5">{e.description}</td>
                    <td className="py-2.5 text-gray-500">{e.categoryName}</td>
                    <td className="py-2.5 text-gray-500">{e.date}</td>
                    <td className="py-2.5 text-gray-500">{e.paymentMethod}</td>
                    <td className="py-2.5 text-right font-medium">{formatCurrency(e.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="grid grid-cols-3 gap-3">
        <Link to="/budgets" className="card p-4 flex items-center gap-3 hover:shadow-md transition-shadow">
          <PiggyBank className="text-brand-500" size={20} /> <span className="text-sm font-medium">Create Budget</span>
        </Link>
        <Link to="/analytics" className="card p-4 flex items-center gap-3 hover:shadow-md transition-shadow">
          <BarChart3 className="text-brand-500" size={20} /> <span className="text-sm font-medium">View Analytics</span>
        </Link>
        <Link to="/ai-advisor" className="card p-4 flex items-center gap-3 hover:shadow-md transition-shadow">
          <Sparkles className="text-brand-500" size={20} /> <span className="text-sm font-medium">AI Advisor</span>
        </Link>
      </div>
    </div>
  )
}
