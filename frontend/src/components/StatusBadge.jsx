const palette = {
  PENDING:              { bg: '#334155', text: '#94a3b8', label: 'Pending' },
  INVENTORY_RESERVED:   { bg: '#1e3a5f', text: '#60a5fa', label: 'Inv. Reserved' },
  PAYMENT_AUTHORIZED:   { bg: '#1e3a5f', text: '#38bdf8', label: 'Pmt. Authorized' },
  FRAUD_CLEARED:        { bg: '#14532d', text: '#86efac', label: 'Fraud Cleared' },
  COMPLETED:            { bg: '#14532d', text: '#4ade80', label: 'Completed' },
  FAILED:               { bg: '#450a0a', text: '#f87171', label: 'Failed' },
  COMPENSATED:          { bg: '#431407', text: '#fb923c', label: 'Compensated' },
}

export default function StatusBadge({ status }) {
  const p = palette[status] ?? { bg: '#334155', text: '#94a3b8', label: status }
  return (
    <span style={{
      background: p.bg,
      color: p.text,
      padding: '2px 10px',
      borderRadius: 9999,
      fontSize: 12,
      fontWeight: 600,
      letterSpacing: '0.05em',
      textTransform: 'uppercase',
      whiteSpace: 'nowrap',
    }}>
      {p.label}
    </span>
  )
}
