import 'dart:async';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app_router.dart';
import 'core/navigation/navigator_key.dart';
import 'core/services/fcm_token_registrar.dart';
import 'core/services/push_notification_service.dart';
import 'core/theme/app_theme.dart';
import 'presentation/providers/auth_provider.dart';
import 'presentation/providers/auth_state.dart';

void main() async {
  // Flutter 바인딩 초기화
  WidgetsFlutterBinding.ensureInitialized();

  // 한국어 locale 데이터 초기화
  await initializeDateFormatting('ko_KR', null);

  // Hive 초기화
  await Hive.initFlutter();

  // FCM 초기화. 네이티브 설정 파일(google-services.json / GoogleService-Info.plist)이
  // 아직 없으면 초기화가 실패하므로, 그 경우 graceful 하게 skip 한다.
  try {
    await Firebase.initializeApp();
    // 백그라운드/종료 상태 메시지 핸들러는 최상위 함수로 가능한 한 일찍 등록.
    FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
  } catch (_) {
    // 설정 파일 미존재 — 푸시 비활성 상태로 앱 구동.
  }

  runApp(
    const ProviderScope(
      child: OtokiApp(),
    ),
  );
}

class OtokiApp extends ConsumerStatefulWidget {
  const OtokiApp({super.key});

  @override
  ConsumerState<OtokiApp> createState() => _OtokiAppState();
}

class _OtokiAppState extends ConsumerState<OtokiApp>
    with WidgetsBindingObserver {
  StreamSubscription<String>? _tokenRefreshSub;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    // 인증 초기화(자동 로그인)는 SplashScreen 이 버전 게이트 통과 후 호출한다.
    // 여기서는 게이트와 무관한 FCM 초기화만 수행한다.
    Future.microtask(() {
      // FCM 권한 요청 + 토큰/리스너 등록 (설정 파일 없으면 내부에서 skip)
      final push = ref.read(pushNotificationServiceProvider);
      push.initialize();

      // FCM 토큰 갱신 시 인증 상태면 서버에 재등록 (Firebase 초기화된 경우만 구독)
      if (push.isAvailable) {
        _tokenRefreshSub = push.onTokenRefresh.listen((token) {
          if (ref.read(authProvider).isAuthenticated) {
            ref.read(fcmTokenRegistrarProvider).registerToken(token);
          }
        });
      }
    });
  }

  @override
  void dispose() {
    _tokenRefreshSub?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _checkGpsConsentOnResume();
    }
  }

  /// 포그라운드 복귀 시 GPS 동의 상태 선제 확인
  Future<void> _checkGpsConsentOnResume() async {
    final authState = ref.read(authProvider);
    // 인증 완료 상태에서만 확인
    if (!authState.isAuthenticated) return;

    try {
      final checkUseCase = ref.read(checkGpsConsentUseCaseProvider);
      final requiresConsent = await checkUseCase();
      if (requiresConsent) {
        final navigator = navigatorKey.currentState;
        if (navigator != null) {
          navigator.pushNamed(AppRouter.gpsConsent);
        }
      }
    } catch (_) {
      // 확인 실패 시 무시 (서버 강제 403이 보완)
    }
  }

  @override
  Widget build(BuildContext context) {
    // 인증 상태 변화 감지 → 화면 전환
    ref.listen<AuthState>(authProvider, (previous, next) {
      final navigator = navigatorKey.currentState;
      if (navigator == null) return;

      // 초기화 완료 전에는 무시
      if (!next.isInitialized) return;

      // 진행 중(로딩) 상태에는 화면 전환하지 않음 — 동일 화면 재진입(깜빡임) 방지.
      // 네비게이션 대상은 모두 종료 상태(authenticated/passwordChangeRequired/gpsConsent/login)이며,
      // 로딩은 항상 종료 상태로 이어지는 과도 상태이므로 건너뛰어도 안전하다.
      if (next.isLoading) return;

      if (next.isAuthenticated) {
        // 인증 완료 → 메인 화면
        navigator.pushNamedAndRemoveUntil(
          AppRouter.main,
          (route) => false,
        );
      } else if (next.passwordChangeRequired) {
        // 비밀번호 변경 필요 → 비밀번호 변경 화면
        navigator.pushNamedAndRemoveUntil(
          AppRouter.changePassword,
          (route) => false,
        );
      } else if (next.requiresGpsConsent && next.user != null) {
        // GPS 동의 필요 → GPS 동의 화면
        navigator.pushNamedAndRemoveUntil(
          AppRouter.gpsConsent,
          (route) => false,
        );
      } else if (next.user == null && !next.isLoading) {
        // 미인증 상태 → 로그인 화면
        navigator.pushNamedAndRemoveUntil(
          AppRouter.login,
          (route) => false,
        );
      }
    });

    return MaterialApp(
      title: '오뚜기 임직원 영업관리',
      theme: AppTheme.light,
      navigatorKey: navigatorKey,
      initialRoute: AppRouter.initialRoute,
      routes: AppRouter.routes,
      onUnknownRoute: AppRouter.onUnknownRoute,
      debugShowCheckedModeBanner: false,
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('ko'),
        Locale('en'),
      ],
      locale: const Locale('ko'),
    );
  }
}
