import { ReactNode } from 'react'
import { AlertTriangle, Inbox } from 'lucide-react'

export function StatCard({ label, value, sub, trend }: { label: string; value: string; sub?: string; trend?: 'up' | 'down' | null }) {
  return (
    <div className="card p-4">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="text-2xl font-semibold mt-1">{value}</p>
      {sub && (
        <p className={`text-xs mt-1 ${trend === 'up' ? 'text-red-500' : trend === 'down' ? 'text-green-600' : 'text-gray-400'}`}>
          {sub}
        </p>
      )}
    </div>
  )
}

export function EmptyState({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 text-gray-400">
      <Inbox size={32} className="mb-3" />
      <p className="font-medium text-gray-600">{title}</p>
      <p className="text-sm mt-1">{subtitle}</p>
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message?: string; onRetry?: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 text-gray-400">
      <AlertTriangle size={32} className="mb-3 text-amber-500" />
      <p className="font-medium text-gray-600">{message || 'Something went wrong.'}</p>
      <p className="text-sm mt-1">Please try again.</p>
      {onRetry && (
        <button className="btn-secondary mt-4" onClick={onRetry}>Retry</button>
      )}
    </div>
  )
}

export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`skeleton ${className}`} />
}

export function CardSkeletonGrid({ count = 4 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="card p-4">
          <Skeleton className="h-4 w-20 mb-2" />
          <Skeleton className="h-7 w-28" />
        </div>
      ))}
    </div>
  )
}

export function PageHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="flex items-center justify-between mb-6 flex-wrap gap-3">
      <div>
        <h1 className="text-xl font-semibold">{title}</h1>
        {subtitle && <p className="text-sm text-gray-500 mt-0.5">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}
