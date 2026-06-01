# otoki-mobile-web

Heroku 레거시(`otg_PowerSales-master`) 현장 페이지를 재구현하는 **모바일앱용 web**.
`mobile/`(Flutter), `web/`(본사 admin) 과 **별개의 앱**이며 같은 backend `/api/v1/mobile/*` 를 사용한다.

- 스택: React 19 + Vite 5 + Ant Design v5 + TanStack Query + Zustand + axios
- 렌더링: CSR(SPA) — Capacitor WebView/OTA 전제 (SSR 미사용, 근거: `docs/plan/heroku-web-wave1-decisions.md`)
- 인증: JWT(audience: mobile), 토큰 키 `mw_*` 로 admin web 과 분리

## 개발

```bash
cd mobile-web
npm install
npm run dev         # http://localhost:5174 (backend :8080 로 /api 프록시)
npm run type-check  # tsc --noEmit
npm run build
```

## 현재 범위 (Wave 1)

공지 · 교육 · 거래처 · 제품상세 · 유통기한 · 클레임 · 물류클레임 · 월매출 · 행사매출 · 설정.
상세 결정/가정/후속작업은 `docs/plan/heroku-web-wave1-decisions.md` 참조.
