import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="text-center py-24">
      <h1 className="text-xl">Page not found</h1>
      <Link to="/" className="text-sm text-brand-600 mt-2 inline-block">Back home</Link>
    </div>
  )
}
