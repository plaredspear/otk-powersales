/**
 * 디바이스 기능 (web 표준 폴백).
 *
 * 결정(Wave 3): 모바일앱용 web 이므로 web 표준 API 로 구현한다.
 * - GPS: navigator.geolocation (web 표준, HTTPS 필요 — 레거시 동등)
 * - 카메라/사진: <input capture=environment> (PhotoInput 컴포넌트)
 * - 바코드: BarcodeDetector(지원 시) / 미지원 시 수동 입력 폴백
 * Capacitor 쉘 단계에서 네이티브 브릿지(@capacitor-mlkit/barcode-scanning,
 * @capacitor/camera, @capacitor/geolocation)로 교체 가능하도록 이 모듈에 격리한다.
 */

export interface GeoPosition {
  latitude: number;
  longitude: number;
}

export function getCurrentPosition(): Promise<GeoPosition> {
  return new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject(new Error('이 기기에서 위치를 사용할 수 없습니다'));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
      (err) => reject(new Error(err.message || '위치 정보를 가져오지 못했습니다')),
      { enableHighAccuracy: true, timeout: 10000 }
    );
  });
}

/** BarcodeDetector 지원 여부 (Chromium 계열 지원, iOS Safari 미지원). */
export function isBarcodeDetectorSupported(): boolean {
  return typeof (window as unknown as { BarcodeDetector?: unknown }).BarcodeDetector !== 'undefined';
}

/**
 * 이미지 파일에서 바코드 1건 디코드 (BarcodeDetector 지원 시).
 * 미지원/실패 시 null. 카메라 스트림 스캐너는 Capacitor 단계에서 제공.
 */
export async function detectBarcodeFromFile(file: File): Promise<string | null> {
  if (!isBarcodeDetectorSupported()) return null;
  try {
    const Detector = (window as unknown as { BarcodeDetector: new () => {
      detect: (src: ImageBitmapSource) => Promise<Array<{ rawValue: string }>>;
    } }).BarcodeDetector;
    const detector = new Detector();
    const bitmap = await createImageBitmap(file);
    const results = await detector.detect(bitmap);
    return results[0]?.rawValue ?? null;
  } catch {
    return null;
  }
}
