# mobile

A new Flutter project.

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Lab: Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://docs.flutter.dev/cookbook)

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev/), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

## iOS 빌드 셋업 (CocoaPods 버전 고정)

기기/CI 간 `ios/Podfile.lock` 의 SPEC CHECKSUM 재현성을 위해 CocoaPods 버전을
`ios/Gemfile` 로 고정한다(현재 `1.16.2`). Flutter 는 `ios/Gemfile` 이 있으면 빌드 시
`bundle exec pod install` 을 자동으로 사용하므로, 아래 1회 셋업만 거치면 `flutter run` /
빌드가 핀된 버전을 그대로 탄다.

### 전제 — Ruby ≥ 3.0

macOS 기본 시스템 Ruby(2.6)는 최신 gem 의존성을 설치하지 못한다. Homebrew Ruby 등
**Ruby 3.0 이상을 활성 ruby 로 둔다.**

```bash
brew install ruby
# zsh PATH 등록 (이미 했다면 생략)
echo 'export PATH="/opt/homebrew/opt/ruby/bin:$PATH"' >> ~/.zshrc
exec zsh
ruby --version   # 3.x 이상인지 확인
```

### 최초 1회 — bundler 로 CocoaPods 설치

```bash
cd mobile/ios
bundle install        # Gemfile/Gemfile.lock 기준 cocoapods 1.16.2 설치
```

이후 `flutter run` / `flutter build ios` 가 자동으로 `bundle exec pod install` 을 호출한다.
수동으로 팟을 재설치할 때도 전역 `pod` 가 아니라 `bundle exec pod install` 을 쓴다
(전역 `pod` 를 쓰면 Gemfile 핀이 무시되어 체크섬이 다시 어긋날 수 있음).

> `Podfile.lock` 의 checksum 이 다시 바뀌어 diff 가 뜬다면 거의 CocoaPods 버전이 핀과
> 다른 경우다 — `bundle exec pod install` 로 재생성하면 핀 버전 기준값으로 정규화된다.
