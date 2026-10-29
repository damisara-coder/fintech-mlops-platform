import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Placeholder from './pages/Placeholder'
import Dashboard from './pages/Dashboard'
import ReviewQueue from './pages/ReviewQueue'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/transactions" element={<Placeholder title="Transactions" />} />
        <Route path="/review" element={<ReviewQueue />} />
        <Route path="/analytics" element={<Placeholder title="ML Analytics" />} />
        <Route path="/simulator" element={<Placeholder title="Simulator" />} />
        <Route path="/health" element={<Placeholder title="System Health" />} />
      </Route>
    </Routes>
  )
}