import type { CapacitorConfig } from '@capacitor/cli';

/**
 * Capacitor 쉘 설정 — 모바일웹(`mobile-web/`) 을 iOS/Android 앱으로 래핑.
 *
 * 배포 모델(2026-06 결정): server.url 원격 로드.
 *   - CAP_SERVER_URL 미설정 → 패키지에 동봉된 baseline 번들(dist) 로 동작 (오프라인/심사용 시드).
 *   - CAP_SERVER_URL 설정    → 그 URL 을 직접 로드. 배포 = 웹 호스트에 새 빌드 업로드 → 앱 즉시 반영.
 * 운영 중 오프라인 요구가 드러나면 baseline 번들 + OTA(Capgo/Capawesome) 로 전환 (React 코드 불변).
 *
 * appId 는 병행 운영하는 기존 Flutter 앱(com.ottogi.mobile) 과 분리.
 */
const serverUrl = process.env.CAP_SERVER_URL;

const config: CapacitorConfig = {
  appId: 'com.ottogi.mobileweb',
  appName: '오뚜기 현장',
  webDir: 'dist',
  server: {
    // https scheme 로 secure-context 확보 (geolocation/카메라/BarcodeDetector 요구사항).
    androidScheme: 'https',
    ...(serverUrl
      ? {
          url: serverUrl,
          // 운영 호스트는 HTTPS 전제. http:// 일 때만 평문 허용(개발 LAN 등).
          cleartext: serverUrl.startsWith('http://'),
        }
      : {}),
  },
};

export default config;
