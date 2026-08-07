import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  Select,
  Spinner,
  Tabs,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'

const TYPES = [
  { value: 'departments', label: 'Departments', parent: null },
  { value: 'programs', label: 'Programs', parent: 'departments' },
  { value: 'branches', label: 'Branches', parent: 'programs' },
  { value: 'batches', label: 'Batches', parent: 'programs' },
]

/**
 * Master data for all four unit types.
 *
 * One screen, because the backend exposes one parameterised endpoint: switching the tab changes a
 * path segment and nothing else.
 */
export default function Academics() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const [type, setType] = useState('departments')
  const [adding, setAdding] = useState(false)
  const [form, setForm] = useState({ code: '', name: '', parentId: '', startYear: '', endYear: '' })
  const [error, setError] = useState('')

  const definition = TYPES.find((entry) => entry.value === type)

  const units = useQuery({
    queryKey: ['academics', type],
    queryFn: () => api.academics.list(type),
  })

  const parents = useQuery({
    queryKey: ['academics', definition.parent],
    queryFn: () => api.academics.list(definition.parent),
    enabled: Boolean(definition.parent),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['academics'] })

  const create = useMutation({
    mutationFn: () =>
      api.academics.create(type, {
        institutionId: user.institutionId,
        code: form.code,
        name: form.name,
        parentId: form.parentId ? Number(form.parentId) : null,
        startYear: form.startYear ? Number(form.startYear) : null,
        endYear: form.endYear ? Number(form.endYear) : null,
        placementOpen: type === 'batches',
      }),
    onSuccess: () => {
      setAdding(false)
      setForm({ code: '', name: '', parentId: '', startYear: '', endYear: '' })
      invalidate()
    },
    onError: (failure) => setError(errorMessage(failure)),
  })

  const [actionError, setActionError] = useState('')

  const setActive = useMutation({
    mutationFn: ({ id, active }) =>
      active ? api.academics.reactivate(type, id) : api.academics.deactivate(type, id),
    onSuccess: () => {
      setActionError('')
      invalidate()
    },
    onError: (failure) => setActionError(errorMessage(failure)),
  })

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <>
      <PageHeader
        title="Academics"
        subtitle="Departments, programs, branches and batches"
        action={<Button onClick={() => setAdding(true)}>Add {singular(type)}</Button>}
      />

      <Tabs active={type} onChange={setType} tabs={TYPES.map((entry) => ({ value: entry.value, label: entry.label }))} />

      {actionError && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded-lg border border-red-200">
          {actionError}
        </div>
      )}

      {units.isLoading && <Spinner />}
      {units.data?.length === 0 && <EmptyState title={`No ${type} yet`} />}

      <div className="space-y-2">
        {units.data?.map((unit) => (
          <Card key={unit.id} className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-medium">
                {unit.name} <span className="text-slate-400 text-sm">({unit.code})</span>
              </p>
              <p className="text-sm text-slate-500">
                {unit.parentName ?? 'Top level'}
                {unit.startYear && ` · ${unit.startYear}–${unit.endYear ?? ''}`}
              </p>
            </div>

            <div className="flex items-center gap-2">
              {unit.placementOpen && <Badge tone="green">Placement open</Badge>}
              <Badge tone={unit.active ? 'green' : 'red'}>
                {unit.active ? 'Active' : 'Inactive'}
              </Badge>
              <Button
                variant={unit.active ? 'secondary' : 'primary'}
                loading={setActive.isPending && setActive.variables?.id === unit.id}
                onClick={() => setActive.mutate({ id: unit.id, active: !unit.active })}
              >
                {unit.active ? 'Deactivate' : 'Activate'}
              </Button>
            </div>
          </Card>
        ))}
      </div>

      <Modal
        open={adding}
        title={`Add a ${singular(type)}`}
        onClose={() => setAdding(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setAdding(false)}>
              Cancel
            </Button>
            <Button onClick={() => create.mutate()} loading={create.isPending}>
              Add
            </Button>
          </>
        }
      >
        <Field label="Code">
          <Input required value={form.code} onChange={update('code')} />
        </Field>
        <Field label="Name">
          <Input required value={form.name} onChange={update('name')} />
        </Field>

        {definition.parent && (
          <Field label={singular(definition.parent)}>
            <Select required value={form.parentId} onChange={update('parentId')}>
              <option value="">Choose one</option>
              {parents.data?.map((parent) => (
                <option key={parent.id} value={parent.id}>
                  {parent.name}
                </option>
              ))}
            </Select>
          </Field>
        )}

        {type === 'batches' && (
          <div className="grid grid-cols-2 gap-3">
            <Field label="Starts">
              <Input type="number" value={form.startYear} onChange={update('startYear')} />
            </Field>
            <Field label="Ends">
              <Input type="number" value={form.endYear} onChange={update('endYear')} />
            </Field>
          </div>
        )}

        {error && <p className="text-sm text-red-600">{error}</p>}
      </Modal>
    </>
  )
}

const singular = (type) => type.replace(/s$/, '')
