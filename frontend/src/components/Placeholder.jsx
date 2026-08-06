import { Card } from './ui'

/**
 * What a page that nobody has written yet renders.
 *
 * When you build your page, delete this import and the component below — nothing else in the
 * project uses it.
 */
export default function Placeholder({ title, owner, notes }) {
  return (
    <Card className="max-w-2xl mx-auto mt-10 p-6">
      <h1 className="text-lg">{title}</h1>
      <p className="text-sm text-slate-500 mt-1">Not built yet — owner: {owner}</p>
      {notes && <p className="text-sm text-slate-600 mt-3">{notes}</p>}
    </Card>
  )
}
