.PHONY: run run-ios run-android build-apk build-ios test setup clean

## STAGE: local | dev | prod (기본 local)
STAGE ?= local
CONFIG := config/$(STAGE).json

## Flutter 실행기 (기본 fvm). fvm 미사용 시: make run FLUTTER=flutter
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

## 앱 실행 (자동 디바이스 선택). 예: make run STAGE=dev
run: setup
	$(FLUTTER) run --dart-define-from-file=$(CONFIG)

## iOS 시뮬레이터에서 실행. 예: make run-ios STAGE=local
run-ios: setup
	$(FLUTTER) run -d iPhone --dart-define-from-file=$(CONFIG)

## Android 에뮬레이터에서 실행. 예: make run-android STAGE=dev
run-android: setup
	$(FLUTTER) run -d android --dart-define-from-file=$(CONFIG)

## APK 빌드 (release). 예: make build-apk STAGE=prod
build-apk: setup
	$(FLUTTER) build apk --release --dart-define-from-file=$(CONFIG)

## iOS 빌드 (release). 예: make build-ios STAGE=prod
build-ios: setup
	$(FLUTTER) build ios --release --dart-define-from-file=$(CONFIG)

## 테스트 실행
test:
	$(FLUTTER) test --reporter compact

## 빌드 캐시 정리
clean:
	$(FLUTTER) clean
	rm -rf .dart_tool build
