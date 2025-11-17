import { useState } from 'react'
import { transformApex, runJob } from './api'

export default function App() {
  const [apex, setApex] = useState('')
  const [target, setTarget] = useState('java')
  const [output, setOutput] = useState('')
  const [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false)

  const onTransform = async () => {
    setBusy(true)
    try {
      const res = await transformApex(apex, target)
      if (target === 'java') {
        setOutput(res.javaCode || '')
        setNotes(res.notes || '')
      } else {
        setOutput(res.jsCode || '')
        setNotes(res.notes || '')
      }
    } catch (e) {
      setOutput('')
      setNotes(e?.message || 'Transform failed')
    } finally {
      setBusy(false)
    }
  }

  const onRun = async (jobPath) => {
    setBusy(true)
    try {
      const res = await runJob(jobPath, { batchSize: 1000, maxConcurrency: 4, dryRun: true })
      setNotes(JSON.stringify(res, null, 2))
    } catch (e) {
      setNotes(e?.message || 'Job failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 24, fontFamily: 'Inter, system-ui, Arial' }}>
      <h1>Apex Code Transformation Assistant</h1>
      <p>Paste APEX on the left, pick a target, and transform.</p>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <textarea
          value={apex}
          onChange={(e) => setApex(e.target.value)}
          placeholder="Paste Apex code..."
          style={{ width: '100%', height: 360, fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace' }}
        />
        <textarea
          value={output}
          readOnly
          placeholder="Transformed code will appear here..."
          style={{ width: '100%', height: 360, background: '#0f172a', color: '#e2e8f0', fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace' }}
        />
      </div>
      <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginTop: 12 }}>
        <label>Target:</label>
        <select value={target} onChange={(e) => setTarget(e.target.value)}>
          <option value="java">Java (Spring Boot)</option>
          <option value="js">JavaScript (Node)</option>
        </select>
        <button onClick={onTransform} disabled={busy || !apex}>Transform</button>
        <span style={{ opacity: 0.7 }}>{busy ? 'Working…' : ''}</span>
      </div>
      <h2 style={{ marginTop: 24 }}>Jobs (Heroku)</h2>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <button onClick={() => onRun('product-purchase')} disabled={busy}>ProductPurchase</button>
        <button onClick={() => onRun('revenue-file-import')} disabled={busy}>RevenueFileImport</button>
        <button onClick={() => onRun('account-plan-reporting')} disabled={busy}>AccountPlanReporting</button>
        <button onClick={() => onRun('cpq-quote-oppty-sync')} disabled={busy}>CPQ Quote→Oppty Sync</button>
        <button onClick={() => onRun('opportunity-split')} disabled={busy}>Opportunity Split</button>
      </div>
      <pre style={{ marginTop: 16, background: '#f1f5f9', padding: 12, whiteSpace: 'pre-wrap' }}>{notes}</pre>
    </div>
  )
}




