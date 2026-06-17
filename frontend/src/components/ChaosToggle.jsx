import { useState, useEffect } from 'react'
import { chaosApi } from '../api/orderApi'

export default function ChaosToggle() {
  const [enabled, setEnabled] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    chaosApi.getStatus().then(d => setEnabled(d.chaosEnabled)).catch(() => {})
  }, [])

  const toggle = async () => {
    setLoading(true)
    try {
      const data = await chaosApi.toggle()
      setEnabled(data.chaosEnabled)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      background: '#1e293b', borderRadius: 10, padding: '10px 16px',
      border: `1px solid ${enabled ? '#ef4444' : '#334155'}`,
      transition: 'border-color 0.2s',
    }}>
      <span style={{ fontSize: 14, fontWeight: 600 }}>Chaos Mode</span>
      <span style={{ fontSize: 12, color: '#64748b', flex: 1 }}>
        {enabled
          ? '⚡ Payment worker will randomly fail or decline'
          : 'Payment worker running normally'}
      </span>
      <button
        onClick={toggle}
        disabled={loading}
        style={{
          width: 48, height: 26, borderRadius: 13, border: 'none', cursor: 'pointer',
          background: enabled ? '#ef4444' : '#334155',
          position: 'relative', transition: 'background 0.2s',
        }}
      >
        <span style={{
          display: 'block', width: 20, height: 20, borderRadius: '50%',
          background: '#fff',
          position: 'absolute', top: 3,
          left: enabled ? 25 : 3,
          transition: 'left 0.2s',
        }} />
      </button>
    </div>
  )
}
