# dev flavor Firebase 설정

Firebase 콘솔에서 **dev iOS 앱**(`com.otoki.pwrs.mobile.dev`)을 등록한 뒤
받은 `GoogleService-Info.plist` 를 **이 폴더에** 넣으세요.

    mobile/ios/config/dev/GoogleService-Info.plist   ← 번들 ID: com.otoki.pwrs.mobile.dev
    mobile/ios/config/prod/GoogleService-Info.plist  ← 번들 ID: com.otoki.pwrs.mobile

빌드 시 Xcode "Run Script" 빌드 단계(Firebase config)가 CONFIGURATION(-dev / -prod)에
따라 알맞은 plist 를 앱 번들로 복사합니다. 파일명은 두 폴더 모두 `GoogleService-Info.plist` 로 동일해야 합니다.
