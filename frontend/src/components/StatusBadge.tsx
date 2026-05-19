import type { ComplaintStatus, Priority, RouteStatus } from '../types'

const STATUS_STYLES: Record<ComplaintStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  ASSIGNED: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-indigo-100 text-indigo-700',
  RESOLVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
}

const PRIORITY_STYLES: Record<Priority, string> = {
  LOW: 'bg-gray-100 text-gray-600',
  MEDIUM: 'bg-amber-100 text-amber-700',
  HIGH: 'bg-orange-100 text-orange-700',
  URGENT: 'bg-red-100 text-red-700',
}

const ROUTE_STATUS_STYLES: Record<RouteStatus, string> = {
  SCHEDULED: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-amber-100 text-amber-700',
  COMPLETED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-600',
}

export function StatusBadge({ status }: { status: ComplaintStatus }) {
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLES[status]}`}>
      {status.replace('_', ' ')}
    </span>
  )
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${PRIORITY_STYLES[priority]}`}>
      {priority}
    </span>
  )
}

export function RouteStatusBadge({ status }: { status: RouteStatus }) {
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${ROUTE_STATUS_STYLES[status]}`}>
      {status.replace('_', ' ')}
    </span>
  )
}
