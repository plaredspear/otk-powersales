.PHONY: run run-ios run-android test setup clean

CONFIG ?= config/dev.json

## 초기 설정: config/dev.json 생성 (최초 1회)
setup:
	@if [ ! -f config/dev.json ]; then \
		cp config/dev.json.example config/dev.json; \
		echo "config/dev.json created from template"; \
	else \
		echo "config/dev.json already exists"; \
	fi
	flutter pub get

## 앱 실행 (자동 디바이스 선택)
run: setup
	flutter run --dart-define-from-file=$(CONFIG)

## iOS 시뮬레이터에서 실행
run-ios: setup
	flutter run -d ios --dart-define-from-file=$(CONFIG)

## Android 에뮬레이터에서 실행
run-android: setup
	flutter run -d android --dart-define-from-file=$(CONFIG)

## 테스트 실행
test:
	flutter test --reporter compact

## 빌드 캐시 정리
clean:
	flutter clean
	rm -rf .dart_tool build
