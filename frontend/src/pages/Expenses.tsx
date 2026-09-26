import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { Expense, ExpenseRequest, Category, Page, PaymentMethod } from '../types'
import { EmptyState, PageHeader } from '../components/Common'
import { Plus, Pencil, Trash2, Download, X } from 'lucide-react'

const PAYMENT_METHODS: PaymentMethod[] = ['CASH', 'CARD', 'UPI', 'NET_BANKING', 'WALLET', 'OTHER']

function formatCurrency(n: number | null | undefined) {
  return `₹${(n ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`
}

export default function Expenses() {
  const [page, setPage] = useState<Page<Expense> | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [pageNum, setPageNum] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Expense | null>(null)

  async function loadCategories() {
    try {
      const { data } = await api.get<Category[]>('/categories')
      setCategories(Array.isArray(data) ? data : [])
    } catch {
      setCategories([])
    }
  }

  async function loadExpenses() {
    setLoading(true)
    try {
      const { data } = await api.get<Page<Expense>>('/expenses', {
        params: {
          search: search || undefined,
          categoryId: categoryId || undefined,
          page: pageNum,
          size: 10,
          sort: 'date,desc',
        },
      })
      setPage({ ...data, content: data.content ?? [] })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadCategories() }, [])
  useEffect(() => { loadExpenses() }, [search, categoryId, pageNum])

  async function handleDelete(id: number) {
    if (!confirm('Delete this expense?')) return
    await api.delete(`/expenses/${id}`)
    loadExpenses()
  }

  function exportCsv() {
    if (!page) return
    const header = 'Description,Category,Date,Payment Method,Amount,Notes\n'
    const rows = page.content.map(e =>
      [e.description, e.categoryName, e.date, e.paymentMethod, e.amount, e.notes ?? ''].join(',')
    ).join('\n')
    const blob = new Blob([header + rows], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'expenses.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="space-y-4">
      <PageHeader
        title="Expenses"
        subtitle="Manage and track all your transactions"
        action={
          <div className="flex gap-2">
            <button className="btn-secondary flex items-center gap-1.5 text-sm" onClick={exportCsv}>
              <Download size={16} /> Export CSV
            </button>
            <button
              className="btn-primary flex items-center gap-1.5 text-sm"
              onClick={() => { setEditing(null); setModalOpen(true) }}
            >
              <Plus size={16} /> Add Expense
            </button>
          </div>
        }
      />

      <div className="flex gap-3 flex-wrap">
        <input className="input max-w-xs" placeholder="Search description or notes…"
               value={search} onChange={e => { setSearch(e.target.value); setPageNum(0) }} />
        <select className="input max-w-[180px]" value={categoryId} onChange={e => { setCategoryId(e.target.value); setPageNum(0) }}>
          <option value="">All categories</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </div>

      <div className="card p-5">
        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => <div key={i} className="skeleton h-10 w-full" />)}
          </div>
        ) : !page || page.content.length === 0 ? (
          <EmptyState title="No expenses found." subtitle="Add your first expense to start tracking." />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-400 border-b border-gray-100">
                    <th className="py-2 font-medium">Description</th>
                    <th className="py-2 font-medium">Category</th>
                    <th className="py-2 font-medium">Date</th>
                    <th className="py-2 font-medium">Payment</th>
                    <th className="py-2 font-medium text-right">Amount</th>
                    <th className="py-2 font-medium text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {page.content.map(e => (
                    <tr key={e.id} className="border-b border-gray-50 last:border-0">
                      <td className="py-2.5">{e.description}</td>
                      <td className="py-2.5 text-gray-500">{e.categoryName}</td>
                      <td className="py-2.5 text-gray-500">{e.date}</td>
                      <td className="py-2.5 text-gray-500">{e.paymentMethod}</td>
                      <td className="py-2.5 text-right font-medium">{formatCurrency(e.amount)}</td>
                      <td className="py-2.5 text-right">
                        <button className="text-gray-400 hover:text-brand-600 mr-2" onClick={() => { setEditing(e); setModalOpen(true) }}>
                          <Pencil size={15} />
                        </button>
                        <button className="text-gray-400 hover:text-red-500" onClick={() => handleDelete(e.id)}>
                          <Trash2 size={15} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex justify-between items-center mt-4 text-sm text-gray-500">
              <span>Page {page.number + 1} of {page.totalPages || 1} · {page.totalElements} total</span>
              <div className="flex gap-2">
                <button className="btn-secondary" disabled={page.number === 0} onClick={() => setPageNum(p => p - 1)}>Previous</button>
                <button className="btn-secondary" disabled={page.number + 1 >= page.totalPages} onClick={() => setPageNum(p => p + 1)}>Next</button>
              </div>
            </div>
          </>
        )}
      </div>

      {modalOpen && (
        <ExpenseModal
          categories={categories}
          expense={editing}
          onClose={() => setModalOpen(false)}
          onSaved={() => { setModalOpen(false); loadExpenses() }}
        />
      )}
    </div>
  )
}

function ExpenseModal({ categories, expense, onClose, onSaved }: {
  categories: Category[]; expense: Expense | null; onClose: () => void; onSaved: () => void
}) {
  const [form, setForm] = useState<ExpenseRequest>({
    amount: expense?.amount ?? 0,
    description: expense?.description ?? '',
    categoryId: expense?.categoryId ?? categories[0]?.id ?? 0,
    date: expense?.date ?? new Date().toISOString().slice(0, 10),
    paymentMethod: expense?.paymentMethod ?? 'CARD',
    notes: expense?.notes ?? '',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!expense && form.categoryId <= 0 && categories.length > 0) {
      setForm(prev => ({ ...prev, categoryId: categories[0].id }))
    }
  }, [categories, expense, form.categoryId])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!Number.isFinite(form.amount) || form.amount <= 0) {
      setError('Please enter a valid amount.')
      return
    }
    if (!Number.isInteger(form.categoryId) || form.categoryId <= 0) {
      setError('Please select a category.')
      return
    }

    setSaving(true)
    setError('')
    try {
      if (expense) {
        await api.put(`/expenses/${expense.id}`, form)
      } else {
        await api.post('/expenses', form)
      }
      onSaved()
    } catch (err: any) {
      setError(err.response?.data?.message || 'Something went wrong. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const noCategories = categories.length === 0 && !expense

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 px-4">
      <div className="card w-full max-w-md p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="font-semibold">{expense ? 'Edit Expense' : 'Add Expense'}</h2>
          <button onClick={onClose}><X size={18} /></button>
        </div>
        {error && <div className="bg-red-50 text-red-600 text-sm rounded-lg px-3 py-2 mb-4">{error}</div>}
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="text-sm font-medium mb-1 block">Description</label>
            <input className="input" required value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-sm font-medium mb-1 block">Amount (₹)</label>
              <input className="input" type="number" step="0.01" min="0.01" required value={form.amount}
                     onChange={e => setForm({ ...form, amount: parseFloat(e.target.value) })} />
            </div>
            <div>
              <label className="text-sm font-medium mb-1 block">Date</label>
              <input className="input" type="date" required value={form.date} onChange={e => setForm({ ...form, date: e.target.value })} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-sm font-medium mb-1 block">Category</label>
              <select className="input" value={form.categoryId} onChange={e => setForm({ ...form, categoryId: Number(e.target.value) })} disabled={noCategories}>
                {noCategories && <option value={0}>No categories available</option>}
                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-sm font-medium mb-1 block">Payment Method</label>
              <select className="input" value={form.paymentMethod} onChange={e => setForm({ ...form, paymentMethod: e.target.value as PaymentMethod })}>
                {PAYMENT_METHODS.map(m => <option key={m} value={m}>{m.replace('_', ' ')}</option>)}
              </select>
            </div>
          </div>
          <div>
            <label className="text-sm font-medium mb-1 block">Notes (optional)</label>
            <textarea className="input" rows={2} value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} />
          </div>
          <button className="btn-primary w-full" disabled={saving || noCategories}>
            {saving ? 'Saving…' : noCategories ? 'Waiting for categories…' : expense ? 'Save Changes' : 'Add Expense'}
          </button>
        </form>
      </div>
    </div>
  )
}
