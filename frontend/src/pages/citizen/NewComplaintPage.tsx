import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { MapPin, Loader2 } from 'lucide-react'
import { complaintsApi } from '../../api/complaints'
import type { WasteType, Priority } from '../../types'

interface FormData {
  title: string
  description: string
  address: string
  pincode: string
  wasteType: WasteType | ''
  priority: Priority
  imageUrl: string
  latitude: string
  longitude: string
}

const WASTE_TYPES: WasteType[] = ['ORGANIC', 'PLASTIC', 'ELECTRONIC', 'HAZARDOUS', 'CONSTRUCTION', 'MIXED']
const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

export default function NewComplaintPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [locating, setLocating] = useState(false)

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormData>({ defaultValues: { priority: 'MEDIUM', wasteType: '' } })

  const lat = watch('latitude')
  const lng = watch('longitude')

  const mutation = useMutation({
    mutationFn: (data: FormData) =>
      complaintsApi.create({
        title: data.title,
        description: data.description || undefined,
        address: data.address,
        pincode: data.pincode || undefined,
        wasteType: data.wasteType || undefined,
        priority: data.priority,
        imageUrl: data.imageUrl || undefined,
        latitude: data.latitude ? Number(data.latitude) : undefined,
        longitude: data.longitude ? Number(data.longitude) : undefined,
      }),
    onSuccess: () => {
      toast.success('Complaint filed successfully! You will earn reward points when it is resolved.')
      queryClient.invalidateQueries({ queryKey: ['my-complaints'] })
      navigate('/citizen/complaints')
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message ?? 'Failed to file complaint')
    },
  })

  const detectLocation = () => {
    if (!navigator.geolocation) {
      toast.error('Geolocation is not supported by your browser')
      return
    }
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setValue('latitude', String(pos.coords.latitude.toFixed(6)))
        setValue('longitude', String(pos.coords.longitude.toFixed(6)))
        setLocating(false)
        toast.success('Location detected')
      },
      () => {
        toast.error('Could not detect location. Please enter manually.')
        setLocating(false)
      }
    )
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-800">File a Complaint</h1>
        <p className="text-sm text-gray-500 mt-1">Report a waste issue in your area. Earn reward points when it gets resolved.</p>
      </div>

      <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="bg-white rounded-xl shadow-sm p-6 space-y-5">
        {/* Title */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Title <span className="text-red-500">*</span>
          </label>
          <input
            {...register('title', { required: 'Title is required', maxLength: { value: 150, message: 'Max 150 characters' } })}
            placeholder="e.g. Garbage pile near bus stop"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          {errors.title && <p className="text-red-500 text-xs mt-1">{errors.title.message}</p>}
        </div>

        {/* Description */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
          <textarea
            {...register('description')}
            rows={3}
            placeholder="Describe the issue in detail..."
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
          />
        </div>

        {/* Address */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Address <span className="text-red-500">*</span>
          </label>
          <input
            {...register('address', { required: 'Address is required' })}
            placeholder="Street, landmark, area..."
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          {errors.address && <p className="text-red-500 text-xs mt-1">{errors.address.message}</p>}
        </div>

        {/* Pincode */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Pincode</label>
          <input
            {...register('pincode', { pattern: { value: /^\d{6}$/, message: 'Enter a valid 6-digit pincode' } })}
            placeholder="e.g. 400001"
            maxLength={6}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          {errors.pincode && <p className="text-red-500 text-xs mt-1">{errors.pincode.message}</p>}
        </div>

        {/* Waste Type + Priority */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Waste Type</label>
            <select
              {...register('wasteType')}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="">Select type...</option>
              {WASTE_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Priority</label>
            <select
              {...register('priority')}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              {PRIORITIES.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>
        </div>

        {/* GPS Location */}
        <div>
          <div className="flex items-center justify-between mb-1">
            <label className="block text-sm font-medium text-gray-700">GPS Coordinates (optional)</label>
            <button
              type="button"
              onClick={detectLocation}
              disabled={locating}
              className="flex items-center gap-1.5 text-xs text-primary-600 hover:text-primary-800 font-medium disabled:opacity-60"
            >
              {locating ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <MapPin className="w-3.5 h-3.5" />}
              {locating ? 'Detecting...' : 'Use my location'}
            </button>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input
              {...register('latitude')}
              placeholder="Latitude"
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <input
              {...register('longitude')}
              placeholder="Longitude"
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          {lat && lng && (
            <p className="text-xs text-green-600 mt-1">📍 Location set: {lat}, {lng}</p>
          )}
        </div>

        {/* Image URL */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Image URL (optional)</label>
          <input
            {...register('imageUrl')}
            placeholder="https://..."
            type="url"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          <p className="text-xs text-gray-400 mt-1">Upload your image to Supabase Storage and paste the URL here</p>
        </div>

        {/* Submit */}
        <div className="flex gap-3 pt-2">
          <button
            type="button"
            onClick={() => navigate('/citizen/complaints')}
            className="flex-1 border border-gray-300 text-gray-600 py-2.5 rounded-lg text-sm hover:bg-gray-50 transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="flex-1 bg-primary-600 hover:bg-primary-700 text-white py-2.5 rounded-lg text-sm font-medium transition disabled:opacity-60"
          >
            {mutation.isPending ? 'Submitting...' : 'Submit Complaint'}
          </button>
        </div>
      </form>
    </div>
  )
}
