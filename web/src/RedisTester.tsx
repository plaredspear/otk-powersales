import { useState } from 'react'

// 백엔드 RedisTestController 응답 타입. 키는 서버에서 항상 diag: 접두사가
// 붙은 전체 키가 반환되므로 화면에서도 그 값을 그대로 노출한다.
// 결과는 화면에 JSON 으로 바로 렌더하므로 응답 본문 타입은 별도로 좁히지
// 않는다. 필요 시 백엔드 RedisTestController 의 응답 data class 참조.
type OpResult =
  | { kind: 'idle' }
  | { kind: 'loading'; label: string }
  | { kind: 'ok'; label: string; body: unknown; status: number }
  | { kind: 'error'; label: string; message: string; status?: number; body?: unknown }

const BASE = '/api/redis'

async function callJson(
  label: string,
  input: RequestInfo,
  init: RequestInit | undefined,
  setResult: (r: OpResult) => void,
) {
  setResult({ kind: 'loading', label })
  try {
    const res = await fetch(input, init)
    const text = await res.text()
    const contentType = res.headers.get('content-type') ?? ''
    const isJson = contentType.includes('application/json')
    let body: unknown = text
    let parseFailed = false
    try {
      body = text ? JSON.parse(text) : null
    } catch {
      parseFailed = true
    }
    // CloudFront SPA fallback 등으로 백엔드 404 가 index.html(200) 로 둔갑하는
    // 경우가 있다. 2xx 라도 JSON 이 아니면 에러로 취급해 사용자가 성공으로
    // 오인하지 않도록 한다.
    if (!res.ok || !isJson || parseFailed) {
      setResult({
        kind: 'error',
        label,
        status: res.status,
        message: !res.ok
          ? `HTTP ${res.status}`
          : `non-JSON response (content-type: ${contentType || 'unknown'})`,
        body,
      })
      return
    }
    setResult({ kind: 'ok', label, body, status: res.status })
  } catch (err) {
    setResult({
      kind: 'error',
      label,
      message: err instanceof Error ? err.message : String(err),
    })
  }
}

function ResultBlock({ result }: { result: OpResult }) {
  if (result.kind === 'idle') {
    return <p className="redis-hint">버튼을 눌러 결과를 확인하세요.</p>
  }
  if (result.kind === 'loading') {
    return <p className="redis-hint">{result.label} 요청 중...</p>
  }
  const tone = result.kind === 'ok' ? 'ok' : 'down'
  const status = result.kind === 'ok' ? result.status : (result.status ?? '—')
  const body = result.kind === 'ok' ? result.body : (result.body ?? result.message)
  return (
    <div className={`redis-result redis-result-${tone}`}>
      <div className="redis-result-header">
        <span className="redis-result-label">{result.label}</span>
        <span className="redis-result-status">HTTP {String(status)}</span>
      </div>
      <pre className="redis-result-body">
        <code>{JSON.stringify(body, null, 2)}</code>
      </pre>
    </div>
  )
}

// 백엔드가 항상 diag: 접두사를 붙이지만, UI 에서도 같은 값을 보여줘야
// 사용자가 실제로 어떤 키가 쓰였는지 헷갈리지 않는다.
function withDiagPrefix(key: string) {
  return key.startsWith('diag:') ? key : `diag:${key}`
}

function RedisTester() {
  const [key, setKey] = useState('test-key')
  const [value, setValue] = useState('hello')
  const [ttl, setTtl] = useState('60')
  const [ping, setPing] = useState<OpResult>({ kind: 'idle' })
  const [op, setOp] = useState<OpResult>({ kind: 'idle' })

  const fullKey = withDiagPrefix(key.trim())

  const doPing = () => callJson('PING', `${BASE}/ping`, undefined, setPing)

  const doWrite = () => {
    if (!key.trim()) return
    const params = new URLSearchParams({ value })
    if (ttl.trim()) params.set('ttl', ttl.trim())
    callJson(
      `POST ${fullKey}`,
      `${BASE}/${encodeURIComponent(key.trim())}?${params.toString()}`,
      { method: 'POST' },
      setOp,
    )
  }

  const doRead = () => {
    if (!key.trim()) return
    callJson(
      `GET ${fullKey}`,
      `${BASE}/${encodeURIComponent(key.trim())}`,
      undefined,
      setOp,
    )
  }

  const doDelete = () => {
    if (!key.trim()) return
    callJson(
      `DELETE ${fullKey}`,
      `${BASE}/${encodeURIComponent(key.trim())}`,
      { method: 'DELETE' },
      setOp,
    )
  }

  return (
    <div className="card redis-card">
      <h2 className="redis-title">Redis 읽기/쓰기 테스트</h2>

      <section className="redis-section">
        <div className="redis-section-head">
          <span>라운드트립 (SET → GET → DEL)</span>
          <button type="button" onClick={doPing}>
            Ping
          </button>
        </div>
        <ResultBlock result={ping} />
      </section>

      <section className="redis-section">
        <div className="redis-section-head">
          <span>수동 조작</span>
        </div>
        <div className="redis-form">
          <label className="redis-field">
            <span>key</span>
            <input
              value={key}
              onChange={(e) => setKey(e.target.value)}
              placeholder="test-key"
              spellCheck={false}
            />
          </label>
          <label className="redis-field">
            <span>value</span>
            <input
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder="hello"
              spellCheck={false}
            />
          </label>
          <label className="redis-field redis-field-ttl">
            <span>ttl(s)</span>
            <input
              value={ttl}
              onChange={(e) => setTtl(e.target.value)}
              placeholder="60"
              inputMode="numeric"
            />
          </label>
        </div>
        <div className="redis-actions">
          <button type="button" onClick={doWrite}>
            Write
          </button>
          <button type="button" onClick={doRead}>
            Read
          </button>
          <button type="button" onClick={doDelete}>
            Delete
          </button>
        </div>
        <p className="redis-hint">
          저장 키는 자동으로 <code>{fullKey || 'diag:'}</code> 로 저장됩니다.
        </p>
        <ResultBlock result={op} />
      </section>

      <p className="status-source">
        <code>{BASE}/*</code>
      </p>
    </div>
  )
}

export default RedisTester
