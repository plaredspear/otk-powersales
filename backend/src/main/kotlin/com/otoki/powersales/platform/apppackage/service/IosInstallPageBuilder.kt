package com.otoki.powersales.platform.apppackage.service

import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * iOS OTA 설치 안내 HTML 페이지 빌더.
 *
 * itms-services:// 스킴은 사람 간 공유(카톡/문자/메일)에 부적합하다 — http(s) 가 아니라
 * 자동 하이퍼링크가 안 되고, 인앱 브라우저는 커스텀 스킴을 못 연다. 그래서 평범한 https
 * URL 로 본 안내 페이지를 열게 하고, 페이지 안의 "설치" 링크(<a href="itms-services://...">)
 * 를 Safari 컨텍스트에서 클릭하게 해 OTA 설치를 트리거한다.
 *
 * 페이지가 직접 itms-services 링크를 노출하므로 manifest URL 은 절대 URL 이어야 한다
 * (iPhone 이 manifest 를 별도 fetch). presigned IPA URL 은 manifest 엔드포인트가 fetch
 * 시점에 새로 발급하므로 본 페이지는 만료 개념이 없다.
 */
@Component
class IosInstallPageBuilder {

    /**
     * @param manifestUrl manifest.plist 절대 URL (https). itms-services url 파라미터로 주입.
     * @param title 앱 표시 제목
     * @param versionName 표시용 버전
     */
    fun build(manifestUrl: String, title: String, versionName: String): String {
        val installHref = "itms-services://?action=download-manifest&url=${urlEncode(manifestUrl)}"
        val t = escapeHtml(title)
        val v = escapeHtml(versionName)
        return """<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$t 설치</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; margin: 0; padding: 24px;
           background: #f5f5f7; color: #1d1d1f; -webkit-text-size-adjust: 100%; }
    .card { max-width: 420px; margin: 40px auto; background: #fff; border-radius: 16px;
            padding: 28px 24px; box-shadow: 0 2px 12px rgba(0,0,0,.08); text-align: center; }
    h1 { font-size: 20px; margin: 0 0 4px; }
    .ver { color: #86868b; font-size: 14px; margin-bottom: 24px; }
    .btn { display: block; width: 100%; box-sizing: border-box; padding: 15px; border-radius: 12px;
           background: #007aff; color: #fff; font-size: 17px; font-weight: 600; text-decoration: none;
           border: none; cursor: pointer; }
    .btn:active { background: #0062cc; }
    .warn { display: none; margin-top: 20px; padding: 14px; border-radius: 12px;
            background: #fff4e5; color: #8a5a00; font-size: 14px; line-height: 1.5; text-align: left; }
    .help { margin-top: 20px; font-size: 13px; color: #86868b; line-height: 1.6; text-align: left; }
  </style>
</head>
<body>
  <div class="card">
    <h1>$t</h1>
    <div class="ver">버전 $v</div>
    <a id="installBtn" class="btn" href="$installHref">설치하기</a>
    <div id="browserWarn" class="warn">
      이 화면이 카카오톡·다른 앱 안에서 열렸다면 설치되지 않습니다.<br>
      우측 상단 메뉴에서 <b>Safari 로 열기</b> 를 선택한 뒤 다시 설치를 눌러주세요.
    </div>
    <div class="help">
      · iPhone Safari 에서만 설치할 수 있습니다.<br>
      · 설치 후 첫 실행 시 <b>설정 › 일반 › VPN 및 기기 관리</b> 에서 개발자를 신뢰해야 앱이 열립니다.
    </div>
  </div>
  <script>
    // 인앱 브라우저(카카오톡/네이버/페이스북 등) 또는 비 Safari 환경이면 경고를 노출한다.
    (function () {
      var ua = navigator.userAgent || '';
      var isIOS = /iPhone|iPad|iPod/.test(ua);
      var isSafari = /Safari/.test(ua) && !/CriOS|FxiOS|EdgiOS/.test(ua);
      var isInApp = /KAKAOTALK|NAVER|Instagram|FBAN|FBAV|Line|DaumApps/i.test(ua);
      if (!isIOS || isInApp || !isSafari) {
        document.getElementById('browserWarn').style.display = 'block';
      }
    })();
  </script>
</body>
</html>
"""
    }

    private fun urlEncode(raw: String): String =
        URLEncoder.encode(raw, StandardCharsets.UTF_8)

    private fun escapeHtml(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
