.PHONY: setup test clean devices \
	run-local run-local-ios run-local-android \
	run-dev run-dev-ios run-dev-android \
	run-prod run-prod-ios run-prod-android \
	run-local-device run-dev-device run-prod-device \
	build-apk-dev build-apk-prod build-ios-dev build-ios-prod build-ipa-dev build-ipa-prod

## Flutter 실행기 (기본 fvm). fvm 미사용 시: make run-local FLUTTER=flutter
FLUTTER ?= fvm flutter

## 실제 iOS 기기 지정용 변수.
##   DEVICE 를 비워두면 USB 로 연결된 실물 iOS 기기(시뮬레이터 제외) UDID 를 자동 탐지한다.
##   여러 기기가 연결돼 자동 탐지가 모호하면 다음처럼 명시한다:
##     make run-dev-device DEVICE=00008120-001A...
##   연결된 기기 목록은 `make devices` 로 확인.
DEVICE ?=
## DEVICE 미지정 시 flutter devices 출력에서 첫 실물 iOS 기기 UDID 를 자동 추출.
##   targetPlatform == "ios" 이면서 emulator == false (시뮬레이터 제외) 인 첫 항목.
IOS_DEVICE = $(if $(DEVICE),$(DEVICE),$(shell $(FLUTTER) devices --machine 2>/dev/null \
	| python3 -c 'import sys,json; d=[x for x in json.load(sys.stdin) if x.get("targetPlatform")=="ios" and not x.get("emulator")]; print(d[0]["id"] if d else "")'))

## 초기 설정: config/<stage>.json 생성 (최초 1회)
setup:
	@for stage in local dev prod; do \
		if [ ! -f config/$$stage.json ]; then \
			cp config/$$stage.json.example config/$$stage.json; \
			echo "config/$$stage.json created from template"; \
		fi; \
	done
	$(FLUTTER) pub get

## ───────────── local 실행 ─────────────
## local 은 dev flavor(.dev Bundle ID / Firebase dev) 위에서 동작한다.
## 자동 디바이스 선택
run-local: setup
	$(FLUTTER) run --flavor dev --dart-define-from-file=config/local.json
## iOS 시뮬레이터
run-local-ios: setup
	$(FLUTTER) run -d iPhone --flavor dev --dart-define-from-file=config/local.json
## Android 에뮬레이터
run-local-android: setup
	$(FLUTTER) run -d android --flavor dev --dart-define-from-file=config/local.json
## 실제 iOS 기기 (USB 연결). DEVICE 미지정 시 연결된 실물 기기 자동 선택.
##   주의: 실기기에서 local 백엔드 접속 시 config/local.json 의 API URL 이
##   localhost 가 아니라 Mac 의 LAN IP 여야 한다.
run-local-device: setup
	$(FLUTTER) run -d "$(IOS_DEVICE)" --flavor dev --dart-define-from-file=config/local.json

## ───────────── dev 실행 ─────────────
run-dev: setup
	$(FLUTTER) run --flavor dev --dart-define-from-file=config/dev.json
run-dev-ios: setup
	$(FLUTTER) run -d iPhone --flavor dev --dart-define-from-file=config/dev.json
run-dev-android: setup
	$(FLUTTER) run -d android --flavor dev --dart-define-from-file=config/dev.json
## 실제 iOS 기기 (USB 연결). DEVICE 미지정 시 연결된 실물 기기 자동 선택.
run-dev-device: setup
	$(FLUTTER) run -d "$(IOS_DEVICE)" --flavor dev --dart-define-from-file=config/dev.json

## ───────────── prod 실행 ─────────────
run-prod: setup
	$(FLUTTER) run --flavor prod --dart-define-from-file=config/prod.json
run-prod-ios: setup
	$(FLUTTER) run -d iPhone --flavor prod --dart-define-from-file=config/prod.json
run-prod-android: setup
	$(FLUTTER) run -d android --flavor prod --dart-define-from-file=config/prod.json
## 실제 iOS 기기 (USB 연결). DEVICE 미지정 시 연결된 실물 기기 자동 선택.
run-prod-device: setup
	$(FLUTTER) run -d "$(IOS_DEVICE)" --flavor prod --dart-define-from-file=config/prod.json

## ───────────── 빌드 (release) ─────────────
## APK 빌드
##   직접 배포(웹 다운로드)용 — ARM 2종만 포함하고 x86_64(에뮬레이터 전용, 약 28MB)는 제외한다.
##   --target-platform 은 Flutter 엔진 빌드 단계의 ABI 를 줄이고, 실제 x86_64 .so 패키징 제외는
##   android/app/build.gradle 의 packagingOptions.jniLibs.excludes 가 담당한다. 구형 32비트폰 호환 유지.
build-apk-dev: setup
	$(FLUTTER) build apk --release --flavor dev --dart-define-from-file=config/dev.json \
		--target-platform android-arm,android-arm64
build-apk-prod: setup
	$(FLUTTER) build apk --release --flavor prod --dart-define-from-file=config/prod.json \
		--target-platform android-arm,android-arm64
## iOS 빌드
build-ios-dev: setup
	$(FLUTTER) build ios --release --flavor dev --dart-define-from-file=config/dev.json
build-ios-prod: setup
	$(FLUTTER) build ios --release --flavor prod --dart-define-from-file=config/prod.json
## iOS Enterprise In-House IPA 빌드 (dev)
##   archive + export 를 한 번에 수행. ios/ExportOptions-Enterprise-Dev.plist 와
##   In-House provisioning profile(dev) 이 준비되어 있어야 한다.
build-ipa-dev: setup
	$(FLUTTER) build ipa --release --flavor dev \
		--dart-define-from-file=config/dev.json \
		--export-options-plist=ios/ExportOptions-Enterprise-Dev.plist

## iOS Enterprise In-House IPA 빌드 (prod)
##   archive + export 를 한 번에 수행. ios/ExportOptions-Enterprise.plist 와
##   In-House provisioning profile 이 준비되어 있어야 한다.
build-ipa-prod: setup
	$(FLUTTER) build ipa --release --flavor prod \
		--dart-define-from-file=config/prod.json \
		--export-options-plist=ios/ExportOptions-Enterprise.plist

## 테스트 실행
test:
	$(FLUTTER) test --reporter compact

## 연결된 기기 목록 + 자동 선택될 실물 iOS 기기 UDID 확인
devices:
	$(FLUTTER) devices
	@echo "── 자동 선택될 실물 iOS 기기: $(IOS_DEVICE)"

## 빌드 캐시 정리
clean:
	$(FLUTTER) clean
	rm -rf .dart_tool build
