import { useRef, useState } from 'react'

// 백엔드 S3TestController 응답을 그대로 JSON 으로 렌더한다. 응답 본문의
// 필드 구조는 backend/src/main/kotlin/com/example/demo/S3TestController.kt 참조.
type OpResult =
  | { kind: 'idle' }
  | { kind: 'loading'; label: string }
  | { kind: 'ok'; label: string; body: unknown; status: number }
  | { kind: 'error'; label: string; message: string; status?: number; body?: unknown }

const BASE = '/api/s3'

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
    // CloudFront SPA fallback 이 404 를 index.html 로 둔갑시키는 경우가 있다.
    // 2xx 라도 JSON 이 아니면 에러로 취급해 성공 오인을 막는다.
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

// 바이너리 다운로드. Content-Disposition 을 존중해 파일로 저장되도록 blob URL
// 을 만들어 임시 anchor 로 클릭한다. 네트워크/응답 상태는 별도 결과 영역에 요약.
async function downloadBlob(
  label: string,
  url: string,
  fallbackName: string,
  setResult: (r: OpResult) => void,
) {
  setResult({ kind: 'loading', label })
  try {
    const res = await fetch(url)
    if (!res.ok) {
      const text = await res.text().catch(() => '')
      setResult({ kind: 'error', label, status: res.status, message: `HTTP ${res.status}`, body: text })
      return
    }
    const disposition = res.headers.get('content-disposition') ?? ''
    const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition)
    const name = match ? decodeURIComponent(match[1]) : fallbackName
    const blob = await res.blob()
    const href = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = href
    a.download = name
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(href)
    setResult({
      kind: 'ok',
      label,
      status: res.status,
      body: {
        savedAs: name,
        size: blob.size,
        contentType: res.headers.get('content-type'),
      },
    })
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

// 백엔드가 항상 diag/ 접두사를 붙이지만, UI 에서도 같은 값을 보여줘야 사용자가
// 실제 저장 위치를 헷갈리지 않는다.
function withDiagPrefix(key: string) {
  return key.startsWith('diag/') ? key : `diag/${key}`
}

function S3Tester() {
  const [key, setKey] = useState('test-key.txt')
  const [value, setValue] = useState('hello s3')
  const [listPrefix, setListPrefix] = useState('')
  const [uploadKey, setUploadKey] = useState('')
  const [ping, setPing] = useState<OpResult>({ kind: 'idle' })
  const [op, setOp] = useState<OpResult>({ kind: 'idle' })
  const [uploadOp, setUploadOp] = useState<OpResult>({ kind: 'idle' })
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [selectedFileName, setSelectedFileName] = useState<string>('')

  const fullKey = withDiagPrefix(key.trim())

  const doPing = () => callJson('PING', `${BASE}/ping`, undefined, setPing)

  const doWriteText = () => {
    if (!key.trim()) return
    const params = new URLSearchParams({ value })
    callJson(
      `POST text ${fullKey}`,
      `${BASE}/text/${encodeURIComponent(key.trim())}?${params.toString()}`,
      { method: 'POST' },
      setOp,
    )
  }

  const doReadText = () => {
    if (!key.trim()) return
    callJson(
      `GET text ${fullKey}`,
      `${BASE}/text/${encodeURIComponent(key.trim())}`,
      undefined,
      setOp,
    )
  }

  const doDownload = () => {
    if (!key.trim()) return
    const fallbackName = key.trim().split('/').pop() || 'download.bin'
    downloadBlob(
      `GET download ${fullKey}`,
      `${BASE}/objects/${encodeURIComponent(key.trim())}/download`,
      fallbackName,
      setOp,
    )
  }

  const doDelete = () => {
    if (!key.trim()) return
    callJson(
      `DELETE ${fullKey}`,
      `${BASE}/objects/${encodeURIComponent(key.trim())}`,
      { method: 'DELETE' },
      setOp,
    )
  }

  const doList = () => {
    const qs = listPrefix.trim() ? `?prefix=${encodeURIComponent(listPrefix.trim())}` : ''
    callJson(`GET list ${listPrefix || '(all)'}`, `${BASE}/objects${qs}`, undefined, setOp)
  }

  const onFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSelectedFileName(e.target.files?.[0]?.name ?? '')
  }

  const doUploadFile = () => {
    const file = fileInputRef.current?.files?.[0]
    if (!file) {
      setUploadOp({ kind: 'error', label: 'UPLOAD', message: '파일을 먼저 선택하세요.' })
      return
    }
    const form = new FormData()
    form.append('file', file)
    // key 입력을 비워두면 백엔드가 업로드 파일의 originalFilename 을 키로 쓴다.
    // 텍스트 섹션의 `key` 와 독립된 상태로 관리해야 파일 업로드 시 텍스트용
    // 키가 실수로 우선 채택되는 것을 막을 수 있다.
    const qs = uploadKey.trim() ? `?key=${encodeURIComponent(uploadKey.trim())}` : ''
    callJson(
      `POST upload ${file.name}`,
      `${BASE}/objects${qs}`,
      { method: 'POST', body: form },
      setUploadOp,
    )
  }

  return (
    <div className="card redis-card">
      <h2 className="redis-title">S3 업로드/읽기/다운로드 테스트</h2>

      <section className="redis-section">
        <div className="redis-section-head">
          <span>라운드트립 (PUT → GET → DEL)</span>
          <button type="button" onClick={doPing}>
            Ping
          </button>
        </div>
        <ResultBlock result={ping} />
      </section>

      <section className="redis-section">
        <div className="redis-section-head">
          <span>텍스트 객체</span>
        </div>
        <div className="redis-form">
          <label className="redis-field">
            <span>key</span>
            <input
              value={key}
              onChange={(e) => setKey(e.target.value)}
              placeholder="test-key.txt"
              spellCheck={false}
            />
          </label>
          <label className="redis-field">
            <span>value</span>
            <input
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder="hello s3"
              spellCheck={false}
            />
          </label>
        </div>
        <div className="redis-actions">
          <button type="button" onClick={doWriteText}>
            Write
          </button>
          <button type="button" onClick={doReadText}>
            Read
          </button>
          <button type="button" onClick={doDownload}>
            Download
          </button>
          <button type="button" onClick={doDelete}>
            Delete
          </button>
        </div>
        <p className="redis-hint">
          저장 키는 자동으로 <code>{fullKey || 'diag/'}</code> 로 저장됩니다.
        </p>
        <ResultBlock result={op} />
      </section>

      <section className="redis-section">
        <div className="redis-section-head">
          <span>파일 업로드 (multipart)</span>
        </div>
        <div className="redis-form">
          <label className="redis-field">
            <span>file</span>
            <input ref={fileInputRef} type="file" onChange={onFileChange} />
          </label>
          <label className="redis-field">
            <span>key (선택)</span>
            <input
              value={uploadKey}
              onChange={(e) => setUploadKey(e.target.value)}
              placeholder="비우면 원본 파일명 사용"
              spellCheck={false}
            />
          </label>
        </div>
        <div className="redis-actions">
          <button type="button" onClick={doUploadFile}>
            Upload
          </button>
        </div>
        <p className="redis-hint">
          저장 키: <code>{withDiagPrefix(uploadKey.trim() || selectedFileName || '(파일 선택 필요)')}</code>
          {' · '}선택된 파일: <code>{selectedFileName || '없음'}</code>
        </p>
        <ResultBlock result={uploadOp} />
      </section>

      <section className="redis-section">
        <div className="redis-section-head">
          <span>객체 리스트 (diag/ 네임스페이스 한정)</span>
        </div>
        <div className="redis-form">
          <label className="redis-field">
            <span>prefix</span>
            <input
              value={listPrefix}
              onChange={(e) => setListPrefix(e.target.value)}
              placeholder="(비우면 diag/ 전체)"
              spellCheck={false}
            />
          </label>
        </div>
        <div className="redis-actions">
          <button type="button" onClick={doList}>
            List
          </button>
        </div>
      </section>

      <p className="status-source">
        <code>{BASE}/*</code>
      </p>
    </div>
  )
}

export default S3Tester
