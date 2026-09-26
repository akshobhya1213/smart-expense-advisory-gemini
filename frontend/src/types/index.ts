export interface User {
  userId: number
  fullName: string
  email: string
}

export interface Category {
  id: number
  name: string
  icon: string
}

export type PaymentMethod = 'CASH' | 'CARD' | 'UPI' | 'NET_BANKING' | 'WALLET' | 'OTHER'

export interface Expense {
  id: number
  amount: number
  description: string
  categoryId: number
  categoryName: string
  date: string
  paymentMethod: PaymentMethod
  notes?: string
  createdAt: string
  updatedAt: string
}

export interface ExpenseRequest {
  amount: number
  description: string
  categoryId?: number
  date: string
  paymentMethod: PaymentMethod
  notes?: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface Budget {
  id: number
  categoryId: number | null
  categoryName: string | null
  year: number
  month: number
  limitAmount: number
  spent: number
  remaining: number
  utilizationPercent: number
  status: 'NORMAL' | 'APPROACHING' | 'EXCEEDED'
}

export interface MonthlySummary {
  year: number
  month: number
  totalExpenses: number
  previousMonthExpenses: number
  changePercent: number
  transactionCount: number
  averageDailySpend: number
}

export interface CategoryBreakdownItem {
  categoryId: number
  categoryName: string
  totalAmount: number
  percentage: number
  transactionCount: number
}

export interface CategoryBreakdown {
  year: number
  month: number
  categories: CategoryBreakdownItem[]
  highestSpendingCategory: string
}

export interface BudgetAnalytics {
  year: number
  month: number
  totalBudget: number
  totalSpent: number
  overallUtilization: number
  categoryBudgets: Budget[]
}
