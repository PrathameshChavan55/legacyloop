import { Link } from 'react-router-dom'
import { Button } from '../components/ui'

export default function NotFound() {
  return (
    <div className="min-h-screen grid place-items-center p-6 text-center">
      <div>
        <p className="text-6xl font-bold text-brand-600">404</p>
        <h1 className="text-xl mt-4">We could not find that page</h1>
        <p className="text-sm text-slate-500 mt-2">The link may be old, or the page may have moved.</p>
        <Link to="/">
          <Button className="mt-6">Go home</Button>
        </Link>
      </div>
    </div>
  )
}
