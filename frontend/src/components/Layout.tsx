import { NavLink, Outlet } from 'react-router-dom'

const links = [
  { to: '/', label: 'Dashboard' },
  { to: '/transactions', label: 'Transactions' },
  { to: '/review', label: 'Human Review' },
  { to: '/analytics', label: 'ML Analytics' },
  { to: '/simulator', label: 'Simulator' },
  { to: '/health', label: 'System Health' },
]

export default function Layout() {
  return (
    <div className="min-h-screen">
      <header className="flex flex-wrap items-center gap-6 border-b border-line px-6 py-3">
        <div className="flex items-center gap-2 font-bold">
          <span className="h-5 w-5 rounded-md bg-accent" />
          SentinelRisk
        </div>
        <nav className="flex flex-wrap gap-1">
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.to === '/'}
              className={({ isActive }) =>
                `rounded-lg px-3 py-1.5 text-sm ${
                  isActive ? 'bg-accent text-white' : 'text-mute hover:text-white'
                }`
              }
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="mx-auto max-w-6xl p-6">
        <Outlet />
      </main>
    </div>
  )
}