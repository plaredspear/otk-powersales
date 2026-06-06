.PHONY: setup test clean \
	run-local run-local-ios run-local-android \
	run-dev run-dev-ios run-dev-android \
	run-prod run-prod-ios run-prod-android \
	build-apk-dev build-apk-prod build-ios-dev build-ios-prod

## Flutter 실행기 (기본 fvm). fvm 미사용 시: make run-local FLUTTER=flutter
FLUTTER ?= fvm flutter

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
## 자동 디바이스 선택
run-local: setup
	$(FLUTTER) run --dart-define-from-file=config/local.json
## iOS 시뮬레이터
run-local-ios: setup
	$(FLUTTER) run -d iPhone --dart-define-from-file=config/local.json
## Android 에뮬레이터
run-local-android: setup
	$(FLUTTER) run -d android --dart-define-from-file=config/local.json

## ───────────── dev 실행 ─────────────
run-dev: setup
	$(FLUTTER) run --dart-define-from-file=config/dev.json
run-dev-ios: setup
	$(FLUTTER) run -d iPhone --dart-define-from-file=config/dev.json
run-dev-android: setup
	$(FLUTTER) run -d android --dart-define-from-file=config/dev.json

## ───────────── prod 실행 ─────────────
run-prod: setup
	$(FLUTTER) run --dart-define-from-file=config/prod.json
run-prod-ios: setup
	$(FLUTTER) run -d iPhone --dart-define-from-file=config/prod.json
run-prod-android: setup
	$(FLUTTER) run -d android --dart-define-from-file=config/prod.json

## ───────────── 빌드 (release) ─────────────
## APK 빌드
build-apk-dev: setup
	$(FLUTTER) build apk --release --dart-define-from-file=config/dev.json
build-apk-prod: setup
	$(FLUTTER) build apk --release --dart-define-from-file=config/prod.json
## iOS 빌드
build-ios-dev: setup
	$(FLUTTER) build ios --release --dart-define-from-file=config/dev.json
build-ios-prod: setup
	$(FLUTTER) build ios --release --dart-define-from-file=config/prod.json

## 테스트 실행
test:
	$(FLUTTER) test --reporter compact

## 빌드 캐시 정리
clean:
	$(FLUTTER) clean
	rm -rf .dart_tool build
