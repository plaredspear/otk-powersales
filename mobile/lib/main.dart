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
import 'core/session/session_reset_controller.dart';
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

  runApp(const AppBootstrap());
}

/// 루트 부트스트랩 위젯.
///
/// [SessionResetController] 의 신호를 받아 루트 `ProviderScope` 를 새 인스턴스로
/// 교체(key 갱신)함으로써, 로그아웃 시 모든 Provider 캐시를 한 번에 폐기한다.
/// 로그아웃으로 재생성된 세션(generation > 0)은 스플래시/버전 게이트를 건너뛰고
/// 곧바로 로그인 화면에서 시작한다.
class AppBootstrap extends StatefulWidget {
  const AppBootstrap({super.key});

  @override
  State<AppBootstrap> createState() => _AppBootstrapState();
}

class _AppBootstrapState extends State<AppBootstrap> {
  final ValueNotifier<int> _generation =
      SessionResetController.instance.generation;

  @override
  void initState() {
    super.initState();
    _generation.addListener(_onReset);
  }

  @override
  void dispose() {
    _generation.removeListener(_onReset);
    super.dispose();
  }

  void _onReset() => setState(() {});

  @override
  Widget build(BuildContext context) {
    final generation = _generation.value;
    return ProviderScope(
      // key 가 바뀌면 ProviderScope 전체가 재생성되어 모든 Provider 가 폐기된다.
      key: ValueKey<int>(generation),
      child: OtokiApp(startAtLogin: generation > 0),
    );
  }
}

class OtokiApp extends ConsumerStatefulWidget {
  const OtokiApp({super.key, this.startAtLogin = false});

  /// 로그아웃 재생성 세션 여부. true 면 스플래시 대신 로그인 화면에서 시작한다.
  final bool startAtLogin;

  @override
  ConsumerState<OtokiApp> createState() => _OtokiAppState();
}

class _OtokiAppState extends ConsumerState<OtokiApp>
    with WidgetsBindingObserver {
  StreamSubscription<String>? _tokenRefreshSub;

  /// authProvider 리스너가 마지막으로 네비게이션한 라우트.
  ///
  /// 동일 목적지로의 중복 push 를 막아 로그인 화면이 무한히 쌓이는 루프를 차단한다.
  /// (예: LoginScreen 진입 후 savedEmployeeNumber 로드로 authState 가 다시 emit 되어도
  /// 목적지는 여전히 login 이므로 재 push 하지 않는다.)
  String? _lastAuthRoute;

  @override
  void initState() {
    super.initState();
    // 로그아웃 재생성 세션은 initialRoute 가 이미 login 이므로, 동일 목적지 재 push 를
    // 막기 위해 마지막 네비게이션 라우트를 login 으로 맞춰둔다.
    if (widget.startAtLogin) {
      _lastAuthRoute = AppRouter.login;
    }
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

      // 상태에서 네비게이션 목적지를 도출한다.
      final String? target;
      if (next.isAuthenticated) {
        // 인증 완료 → 메인 화면
        target = AppRouter.main;
      } else if (next.passwordChangeRequired) {
        // 비밀번호 변경 필요 → 비밀번호 변경 화면
        target = AppRouter.changePassword;
      } else if (next.requiresGpsConsent && next.user != null) {
        // GPS 동의 필요 → GPS 동의 화면
        target = AppRouter.gpsConsent;
      } else if (next.user == null && next.errorMessage == null) {
        // 미인증 상태(로그아웃·자동로그인 실패·토큰만료) → 로그인 화면.
        target = AppRouter.login;
      } else {
        // 로그인 에러(errorMessage != null)는 이미 로그인 화면에서 발생한 것이므로
        // 재네비게이션하지 않는다(에러는 화면 내 오버레이로 즉시 표시됨).
        target = null;
      }

      // 동일 목적지로의 중복 push 차단.
      // LoginScreen 진입 후 savedEmployeeNumber 로드 등으로 authState 가 다시 emit 되어도
      // 목적지가 변하지 않으면 재 push 하지 않는다 — 로그인 화면이 무한히 쌓이는 루프 방지.
      if (target == null || target == _lastAuthRoute) return;
      _lastAuthRoute = target;
      navigator.pushNamedAndRemoveUntil(target, (route) => false);
    });

    return MaterialApp(
      title: '오뚜기 임직원 영업관리',
      theme: AppTheme.light,
      navigatorKey: navigatorKey,
      initialRoute:
          widget.startAtLogin ? AppRouter.login : AppRouter.initialRoute,
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
