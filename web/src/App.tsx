import { useEffect, useState } from 'react'
import './App.css'

type Indicator = { status: string }

type HealthResponse = {
  status: string
  components?: {
    db?: Indicator
    redis?: Indicator
  }
}

type CheckState =
  | { kind: 'loading' }
  | { kind: 'ok'; overall: string; db?: string; redis?: string }
  | { kind: 'error'; message: string }

// CloudFront 가 /api/* 를 ALB 로 프록시하므로 상대 경로 호출 = same-origin.
const HEALTH_URL = '/api/health'

function StatusRow({ label, status }: { label: string; status?: string }) {
  const tone =
    status === 'UP' ? 'ok' : status === undefined ? 'muted' : 'down'
  return (
    <div className={`status-row status-${tone}`}>
      <span className="status-label">{label}</span>
      <span className="status-value">{status ?? 'N/A'}</span>
    </div>
  )
}

function App() {
  const [state, setState] = useState<CheckState>({ kind: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    fetch(HEALTH_URL, { signal: controller.signal })
      .then(async (res) => {
        const body = (await res.json()) as HealthResponse
        setState({
          kind: 'ok',
          overall: body.status,
          db: body.components?.db?.status,
          redis: body.components?.redis?.status,
        })
      })
      .catch((err: unknown) => {
        if (err instanceof DOMException && err.name === 'AbortError') return
        setState({
          kind: 'error',
          message: err instanceof Error ? err.message : String(err),
        })
      })
    return () => controller.abort()
  }, [])

  return (
    <div className="app">
      <header className="header">
        <h1>Otoki</h1>
        <p className="subtitle">Backend 연결 상태</p>
      </header>

      <main className="main">
        <div className="card status-card">
          {state.kind === 'loading' && <p>상태 확인 중...</p>}
          {state.kind === 'error' && (
            <p className="status-error">오류: {state.message}</p>
          )}
          {state.kind === 'ok' && (
            <>
              <StatusRow label="Overall" status={state.overall} />
              <StatusRow label="Database" status={state.db} />
              <StatusRow label="Redis" status={state.redis} />
            </>
          )}
          <p className="status-source">
            <code>{HEALTH_URL}</code>
          </p>
        </div>
      </main>

      <footer className="footer">
        <p>Otoki &copy; {new Date().getFullYear()}</p>
      </footer>
    </div>
  )
}

export default App
