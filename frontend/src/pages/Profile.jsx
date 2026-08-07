import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, useRef } from 'react'
import { Link } from 'react-router-dom'
import {
  Avatar,
  Badge,
  Button,
  Card,
  Field,
  Input,
  PageHeader,
  Spinner,
  Textarea,
  Modal,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'

/**
 * Your own profile.
 *
 * The student and alumni forms differ in their fields but not in their shape, so this is one page
 * with two field sets rather than two pages that repeat the save logic.
 */
export default function Profile() {
  const { user, hasRole } = useAuth()
  const queryClient = useQueryClient()
  const isStudent = hasRole('STUDENT')

  const [form, setForm] = useState(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  // Crop Modal States
  const [imageUrl, setImageUrl] = useState('')
  const [zoom, setZoom] = useState(1)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const [isDragging, setIsDragging] = useState(false)
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 })
  const [photoUploading, setPhotoUploading] = useState(false)
  const fileInputRef = useRef(null)

  const profile = useQuery({
    queryKey: ['profile', 'me'],
    queryFn: () => (isStudent ? api.profiles.myStudent() : api.profiles.myAlumni()),
    enabled: hasRole('STUDENT', 'ALUMNI'),
  })

  const academics = useQuery({
    queryKey: ['batches'],
    queryFn: () => api.academics.list('batches'),
    enabled: hasRole('STUDENT', 'ALUMNI'),
  })

  useEffect(() => {
    if (profile.data) setForm({ ...profile.data, skills: (profile.data.skills ?? []).join(', ') })
  }, [profile.data])

  const handleAvatarClick = () => {
    fileInputRef.current?.click()
  }

  const handleFileChange = (e) => {
    const file = e.target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = () => {
        setImageUrl(reader.result)
        setZoom(1)
        setOffset({ x: 0, y: 0 })
      }
      reader.readAsDataURL(file)
    }
  }

  const handleMouseDown = (e) => {
    setIsDragging(true)
    setDragStart({ x: e.clientX - offset.x, y: e.clientY - offset.y })
  }

  const handleMouseMove = (e) => {
    if (!isDragging) return
    setOffset({
      x: e.clientX - dragStart.x,
      y: e.clientY - dragStart.y,
    })
  }

  const handleMouseUp = () => {
    setIsDragging(false)
  }

  const handleTouchStart = (e) => {
    if (e.touches.length !== 1) return
    setIsDragging(true)
    setDragStart({ x: e.touches[0].clientX - offset.x, y: e.touches[0].clientY - offset.y })
  }

  const handleTouchMove = (e) => {
    if (!isDragging || e.touches.length !== 1) return
    setOffset({
      x: e.touches[0].clientX - dragStart.x,
      y: e.touches[0].clientY - dragStart.y,
    })
  }

  const handleCropAndSave = () => {
    setPhotoUploading(true)
    const img = new Image()
    img.src = imageUrl
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = 300
      canvas.height = 300
      const ctx = canvas.getContext('2d')

      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, 300, 300)

      const ratio = Math.min(256 / img.width, 256 / img.height)
      const drawWidth = img.width * ratio
      const drawHeight = img.height * ratio

      ctx.save()
      ctx.translate(150 + offset.x * (300 / 256), 150 + offset.y * (300 / 256))
      ctx.scale(zoom * (300 / 256), zoom * (300 / 256))
      ctx.drawImage(img, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight)
      ctx.restore()

      canvas.toBlob(async (blob) => {
        if (blob) {
          try {
            const fileToUpload = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
            const response = await api.profiles.uploadPhoto(fileToUpload)
            setForm((prev) => ({ ...prev, profilePhotoUrl: response }))
            setImageUrl('')
            setMessage('Photo updated successfully. Save profile to persist changes!')
          } catch (err) {
            setError(errorMessage(err))
          } finally {
            setPhotoUploading(false)
          }
        }
      }, 'image/jpeg', 0.9)
    }
  }

  const validate = () => {
    if (form.headline && form.headline.length > 150) {
      return 'Headline cannot exceed 150 characters'
    }
    if (form.about && form.about.length > 1000) {
      return 'About section cannot exceed 1000 characters'
    }

    if (isStudent) {
      if (form.cgpa !== null && form.cgpa !== undefined && form.cgpa !== '') {
        const val = parseFloat(form.cgpa)
        if (isNaN(val) || val < 0 || val > 10) {
          return 'CGPA must be a number between 0.0 and 10.0'
        }
      }
      if (form.backlogs !== null && form.backlogs !== undefined && form.backlogs !== '') {
        const val = parseInt(form.backlogs, 10)
        if (isNaN(val) || val < 0) {
          return 'Backlogs must be a non-negative integer'
        }
      }
      if (form.graduationYear !== null && form.graduationYear !== undefined && form.graduationYear !== '') {
        const val = parseInt(form.graduationYear, 10)
        const currentYear = new Date().getFullYear()
        if (isNaN(val) || val < 1900 || val > currentYear + 10) {
          return `Graduation year must be a valid year (between 1900 and ${currentYear + 10})`
        }
      }
    } else {
      if (form.totalExperienceMonths !== null && form.totalExperienceMonths !== undefined && form.totalExperienceMonths !== '') {
        const val = parseInt(form.totalExperienceMonths, 10)
        if (isNaN(val) || val < 0) {
          return 'Experience must be a non-negative number of months'
        }
      }
    }

    const urlPattern = /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([\/\w .-]*)*\/?$/
    if (form.linkedinUrl && !urlPattern.test(form.linkedinUrl)) {
      return 'Please enter a valid LinkedIn URL'
    }
    if (form.githubUrl && !urlPattern.test(form.githubUrl)) {
      return 'Please enter a valid GitHub URL'
    }
    if (form.portfolioUrl && !urlPattern.test(form.portfolioUrl)) {
      return 'Please enter a valid Portfolio URL'
    }

    return null
  }

  const save = useMutation({
    mutationFn: () => {
      const body = {
        ...form,
        skills: form.skills
          .split(',')
          .map((skill) => skill.trim())
          .filter(Boolean),
      }
      return isStudent ? api.profiles.saveStudent(body) : api.profiles.saveAlumni(body)
    },
    onSuccess: () => {
      setMessage('Profile saved.')
      queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
    onError: (failure) => setError(errorMessage(failure)),
  })

  const handleSave = () => {
    setMessage('')
    setError('')
    const valError = validate()
    if (valError) {
      setError(valError)
      return
    }
    save.mutate()
  }

  if (!hasRole('STUDENT', 'ALUMNI')) {
    return (
      <Card>
        <PageHeader title={user?.fullName} subtitle={user?.email} />
        <p className="text-sm text-slate-600">
          Staff and administrator accounts do not have a public profile.
        </p>
        <Link to="/change-password">
          <Button variant="secondary" className="mt-4">
            Change password
          </Button>
        </Link>
      </Card>
    )
  }

  if (profile.isLoading || !form) return <Spinner />

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })
  const toggle = (field) => (event) => setForm({ ...form, [field]: event.target.checked })

  return (
    <div className="max-w-3xl">
      <PageHeader
        title="Your profile"
        subtitle={isStudent ? `${form.completenessPercentage ?? 0}% complete` : undefined}
        action={
          <Link to="/change-password">
            <Button variant="secondary">Change password</Button>
          </Link>
        }
      />

      <Card className="mb-5 flex items-center gap-4">
        <div 
          onClick={handleAvatarClick} 
          className="relative group cursor-pointer rounded-full overflow-hidden shrink-0 border border-slate-200"
          title="Click to change profile picture"
        >
          <Avatar name={user?.fullName} url={form.profilePhotoUrl} size={64} />
          <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
            <span className="text-[10px] text-white font-medium text-center px-1">Upload Photo</span>
          </div>
        </div>
        <div>
          <p className="font-medium">{user?.fullName}</p>
          <p className="text-sm text-slate-500">{user?.email}</p>
          {form.placed && <Badge tone="green">Placed at {form.placedCompany}</Badge>}
        </div>
        <input 
          type="file" 
          ref={fileInputRef} 
          className="hidden" 
          accept="image/*" 
          onChange={handleFileChange} 
        />
      </Card>

      <Card>
        <Field label="Headline" hint="One line people see first">
          <Input value={form.headline ?? ''} onChange={update('headline')} />
        </Field>

        <Field label="About">
          <Textarea value={form.about ?? ''} onChange={update('about')} />
        </Field>

        {isStudent ? (
          <>
            <div className="grid sm:grid-cols-3 gap-4">
              <Field label="CGPA">
                <Input type="number" step="0.01" value={form.cgpa ?? ''} onChange={update('cgpa')} />
              </Field>
              <Field label="Backlogs">
                <Input type="number" value={form.backlogs ?? 0} onChange={update('backlogs')} />
              </Field>
              <Field label="Graduating in">
                <Input
                  type="number"
                  value={form.graduationYear ?? ''}
                  onChange={update('graduationYear')}
                />
              </Field>
            </div>

            <Field label="Batch">
              <select className="input" value={form.batchId ?? ''} onChange={update('batchId')}>
                <option value="">Choose your batch</option>
                {academics.data?.map((batch) => (
                  <option key={batch.id} value={batch.id}>
                    {batch.name}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Where you are">
              <Input value={form.location ?? ''} onChange={update('location')} />
            </Field>
          </>
        ) : (
          <>
            <div className="grid sm:grid-cols-2 gap-4">
              <Field label="Company">
                <Input value={form.currentCompany ?? ''} onChange={update('currentCompany')} />
              </Field>
              <Field label="Designation">
                <Input value={form.currentDesignation ?? ''} onChange={update('currentDesignation')} />
              </Field>
              <Field label="Location">
                <Input value={form.currentLocation ?? ''} onChange={update('currentLocation')} />
              </Field>
              <Field label="Experience in months">
                <Input
                  type="number"
                  value={form.totalExperienceMonths ?? ''}
                  onChange={update('totalExperienceMonths')}
                />
              </Field>
            </div>

            <Field label="Mentorship areas" hint="What you are happy to be asked about">
              <Input value={form.mentorshipAreas ?? ''} onChange={update('mentorshipAreas')} />
            </Field>

            <label className="flex items-center gap-2 text-sm mb-3">
              <input type="checkbox" checked={form.willingToRefer ?? false} onChange={toggle('willingToRefer')} />
              I am happy to refer students at my company
            </label>

            <label className="flex items-center gap-2 text-sm mb-4">
              <input
                type="checkbox"
                checked={form.availableForMentorship ?? false}
                onChange={toggle('availableForMentorship')}
              />
              I am available to mentor
            </label>
          </>
        )}

        <Field label="Skills" hint="Comma separated">
          <Input value={form.skills} onChange={update('skills')} />
        </Field>

        <div className="grid sm:grid-cols-3 gap-4">
          <Field label="LinkedIn">
            <Input value={form.linkedinUrl ?? ''} onChange={update('linkedinUrl')} />
          </Field>
          <Field label="GitHub">
            <Input value={form.githubUrl ?? ''} onChange={update('githubUrl')} />
          </Field>
          <Field label="Portfolio">
            <Input value={form.portfolioUrl ?? ''} onChange={update('portfolioUrl')} />
          </Field>
        </div>

        <label className="flex items-center gap-2 text-sm mb-4">
          <input type="checkbox" checked={form.profileVisible ?? true} onChange={toggle('profileVisible')} />
          Show my profile in the directory
        </label>

        {isStudent && (
          <label className="flex items-center gap-2 text-sm mb-4">
            <input type="checkbox" checked={form.openToWork ?? true} onChange={toggle('openToWork')} />
            I am open to work
          </label>
        )}

        {message && <p className="text-sm text-green-700 mb-3">{message}</p>}
        {error && <p className="text-sm text-red-600 mb-3">{error}</p>}

        <Button onClick={handleSave} loading={save.isPending}>
          Save profile
        </Button>
      </Card>

      {imageUrl && (
        <Modal
          open={!!imageUrl}
          title="Crop Profile Photo"
          onClose={() => {
            setImageUrl('')
          }}
          footer={
            <div className="flex gap-2">
              <Button
                variant="secondary"
                onClick={() => {
                  setImageUrl('')
                }}
              >
                Cancel
              </Button>
              <Button onClick={handleCropAndSave} loading={photoUploading}>
                Crop & Save
              </Button>
            </div>
          }
        >
          <div className="flex flex-col items-center gap-4">
            <p className="text-xs text-slate-500 text-center">
              Drag the image to adjust position, and use the slider below to zoom.
            </p>
            <div 
              className="relative w-64 h-64 overflow-hidden rounded-full border-4 border-slate-200 bg-slate-100 cursor-move select-none"
              onMouseDown={handleMouseDown}
              onMouseMove={handleMouseMove}
              onMouseUp={handleMouseUp}
              onMouseLeave={handleMouseUp}
              onTouchStart={handleTouchStart}
              onTouchMove={handleTouchMove}
              onTouchEnd={handleMouseUp}
            >
              <img
                src={imageUrl}
                alt="Crop preview"
                style={{
                  transform: `translate(${offset.x}px, ${offset.y}px) scale(${zoom})`,
                  transformOrigin: 'center',
                }}
                className="w-full h-full object-contain pointer-events-none"
              />
            </div>
            
            <div className="w-full max-w-xs">
              <label className="label text-center mb-1">Zoom</label>
              <input
                type="range"
                min="1"
                max="3"
                step="0.05"
                value={zoom}
                onChange={(e) => setZoom(parseFloat(e.target.value))}
                className="w-full accent-brand-600"
              />
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}
