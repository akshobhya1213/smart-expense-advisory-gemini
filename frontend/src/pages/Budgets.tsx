import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { Budget, Category } from '../types'
import { EmptyState, PageHeader } from '../components/Common'
import { Plus, Trash2, X } from 'lucide-react'

function formatCurrency(n: number | null | undefined) {
  return `₹${(n ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`
}

export default function Budgets() {
  const now = new Date()
  const [year] = useState(now.getFullYear())
  const [month] = useState(now.getMonth() + 1)
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)

  async function load() {
    setLoading(true)
    const [budgetRes, catRes] = await Promise.all([
      api.get<Budget[]>('/budgets', { params: { year, month } }),
      api.get<Category[]>('/categories'),
    ])
    setBudgets(budgetRes.data)
    setCategories(catRes.data)
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  async function handleDelete(id: number) {
    if (!confirm('Delete this budget?')) return
    await api.delete(`/budgets/${id}`)
    load()
  }

  return (
    <div className="space-y-4">
      <PageHeader
        title="Budgets"
        subtitle={`${now.toLocaleString('default', { month: 'long' })} ${year}`}
        action={
          <button className="btn-primary flex items-center gap-1.5 text-sm" onClick={() => setModalOpen(true)}>
            <Plus size={16} /> Create Budget
          </button>
        }
      />

      {loading ? (
        <div className="grid md:grid-cols-2 gap-4">
          {Array.from({ length: 4 }).map((_, i) => <div key={i} className="skeleton h-32 rounded-xl" />)}
        </div>
      ) : budgets.length === 0 ? (
        <div className="card p-8">
          <EmptyState title="No budgets set for this month." subtitle="Create a budget to start tracking utilization." />
        </div>
      ) : (
        <div className="grid md:grid-cols-2 gap-4">
          {budgets.map(b => (
            <div key={b.id} className="card p-5">
              <div className="flex justify-between items-start mb-3">
                <div>
                  <p className="font-medium">{b.categoryName ?? 'Overall Monthly Budget'}</p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {b.status === 'EXCEEDED' ? 'Over budget' : b.status === 'APPROACHING' ? 'Approaching limit' : 'On track'}
                  </p>
                </div>
                <button className="text-gray-400 hover:text-red-500" onClick={() => handleDelete(b.id)}>
                  <Trash2 size={15} />
                </button>
              </div>
              <p className="text-lg font-semibold">{formatCurrency(b.spent)} <span className="text-gray-400 text-sm font-normal">/ {formatCurrency(b.limitAmount)}</span></p>
              <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden mt-3">
                <div
                  className={`h-full rounded-full ${b.status === 'EXCEEDED' ? 'bg-red-500' : b.status === 'APPROACHING' ? 'bg-amber-500' : 'bg-brand-500'}`}
                  style={{ width: `${Math.min(100, b.utilizationPercent ?? 0)}%` }}
                />
              </div>
              <p className="text-xs text-gray-400 mt-2">
                {(b.utilizationPercent ?? 0).toFixed(1)}% utilized · {formatCurrency(Math.max(0, b.remaining ?? 0))} remaining
              </p>
            </div>
          ))}
        </div>
      )}

      {modalOpen && (
        <BudgetModal categories={categories} year={year} month={month}
          onClose={() => setModalOpen(false)} onSaved={() => { setModalOpen(false); load() }} />
      )}
    </div>
  )
}

function BudgetModal({ categories, year, month, onClose, onSaved }: {
  categories: Category[]; year: number; month: number; onClose: () => void; onSaved: () => void
}) {
  const [categoryId, setCategoryId] = useState<string>('')
  const [limitAmount, setLimitAmount] = useState<number>(0)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      await api.post('/budgets', {
        categoryId: categoryId ? parseInt(categoryId) : null,
        year, month, limitAmount,
      })
      onSaved()
    } catch (err: any) {
      setError(err.response?.data?.message || 'Something went wrong. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 px-4">
      <div className="card w-full max-w-md p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="font-semibold">Create Budget</h2>
          <button onClick={onClose}><X size={18} /></button>
        </div>
        {error && <div className="bg-red-50 text-red-600 text-sm rounded-lg px-3 py-2 mb-4">{error}</div>}
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="text-sm font-medium mb-1 block">Scope</label>
            <select className="input" value={categoryId} onChange={e => setCategoryId(e.target.value)}>
              <option value="">Overall monthly budget</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label className="text-sm font-medium mb-1 block">Limit Amount (₹)</label>
            <input className="input" type="number" min="1" step="0.01" required value={limitAmount}
                   onChange={e => setLimitAmount(parseFloat(e.target.value))} />
          </div>
          <button className="btn-primary w-full" disabled={saving}>{saving ? 'Saving…' : 'Create Budget'}</button>
        </form>
      </div>
    </div>
  )
}
