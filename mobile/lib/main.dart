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
import 'core/network/request_cancel_controller.dart';
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
    // 재생성 사유가 로그인(인증 복원)인지 로그아웃(로그인 화면)인지 구분한다.
    final startAuthenticated =
        generation > 0 && SessionResetController.instance.startAuthenticated;
    return ProviderScope(
      // key 가 바뀌면 ProviderScope 전체가 재생성되어 모든 Provider 가 폐기된다.
      key: ValueKey<int>(generation),
      child: OtokiApp(
        startAtLogin: generation > 0 && !startAuthenticated,
        startAuthenticated: startAuthenticated,
      ),
    );
  }
}

class OtokiApp extends ConsumerStatefulWidget {
  const OtokiApp({
    super.key,
    this.startAtLogin = false,
    this.startAuthenticated = false,
  });

  /// 로그아웃 재생성 세션 여부. true 면 스플래시 대신 로그인 화면에서 시작한다.
  final bool startAtLogin;

  /// 로그인 성공 재생성 세션 여부. true 면 스플래시/자동로그인 게이트를 건너뛰고
  /// 저장된 토큰으로 [AuthNotifier.restoreSession] 을 호출해 인증된 홈에서 시작한다.
  final bool startAuthenticated;

  @override
  ConsumerState<OtokiApp> createState() => _OtokiAppState();
}

class _OtokiAppState extends ConsumerState<OtokiApp>
    with WidgetsBindingObserver {
  StreamSubscription<String>? _tokenRefreshSub;

  /// 자동로그인 OFF 사용자의 "리줌(백그라운드 복귀) 타임아웃" 임계값.
  ///
  /// 레거시 Heroku 의 세션 비활동 만료(Spring Session Redis 기본 30분)에 정합한다.
  /// 자동로그인 ON 은 refresh token 으로 세션이 유지되므로 이 타임아웃을 적용하지 않는다.
  static const Duration _resumeReloginTimeout = Duration(minutes: 30);

  /// 앱이 백그라운드(paused/detached)로 전환된 시각. 복귀 시 경과 시간 계산용.
  DateTime? _pausedAt;

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
      // 전역 navigatorKey(GlobalKey)는 ProviderScope 재생성에도 보존되므로, 새 MaterialApp 의
      // initialRoute(login)가 무시되고 기존 Navigator 의 홈 스택이 그대로 재사용된다.
      // 그 결과 화면은 홈에 남고 Provider 만 비워져 영구 로딩에 갇힌다.
      // 재사용된 Navigator 를 명시적으로 로그인으로 리셋해 옛 홈 스택을 폐기한다.
      WidgetsBinding.instance.addPostFrameCallback((_) {
        navigatorKey.currentState?.pushNamedAndRemoveUntil(
          AppRouter.login,
          (route) => false,
        );
      });
    } else if (widget.startAuthenticated) {
      // 로그인 성공으로 재생성된 세션 — 스플래시/자동로그인 게이트를 건너뛰고 저장된
      // 토큰으로 세션을 복원한다. 복원이 인증 완료로 전이하면 authState 리스너가 홈으로
      // 전환하고, 실패하면 로그인 화면으로 떨어진다. 직전 화면은 로그인이므로 시작 라우트를
      // login 으로 맞춰 인증 완료 시 홈으로의 전환이 정상 발화하게 한다.
      _lastAuthRoute = AppRouter.login;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        ref.read(authProvider.notifier).restoreSession();
      });
    }
    WidgetsBinding.instance.addObserver(this);
    // 첫 프레임 직후 현재 authState 로 화면 전환을 1회 평가한다.
    // `ref.listen` 은 상태 "변화" 만 잡으므로, 자동 로그인이 첫 build 와 listen 등록
    // 사이의 좁은 창에서 종료 상태로 전이를 끝내버리면 그 전이를 영영 놓쳐 스플래시가
    // 멈춘다. 여기서 현재 상태를 직접 읽어 평가하면 그런 누락을 복구한다(상태 기반 전환).
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _applyNavigation();
    });
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
      _onResumed();
    } else if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.detached) {
      // 리줌 타임아웃 계산용으로 백그라운드 진입 시각을 기록.
      _pausedAt = DateTime.now();
      // 백그라운드/종료 전환 시 진행 중인 느린 외부 요청을 취소한다.
      // 취소하지 않으면 요청이 timeout 까지 매달려, 재개 시 stale 응답이 상태를
      // 뒤늦게 덮어쓸 수 있다. 토큰은 내부에서 새 것으로 교체되어 재개 후 요청은 정상.
      requestCancelController.cancelAll('app lifecycle: $state');
    }
  }

  /// 포그라운드 복귀 시: ① 리줌 타임아웃 적용 → ② GPS 동의 선제 확인.
  /// 타임아웃으로 로그아웃되면 GPS 확인은 생략한다(로그인 화면으로 전환되므로).
  Future<void> _onResumed() async {
    final loggedOut = await _enforceResumeReloginTimeout();
    if (loggedOut) return;
    await _checkGpsConsentOnResume();
  }

  /// 자동로그인 OFF 사용자가 [_resumeReloginTimeout] 이상 백그라운드에 머문 뒤
  /// 복귀하면 세션을 정리하고 로그인 화면으로 보낸다(레거시 세션 비활동 만료 정합).
  ///
  /// 자동로그인 ON 은 refresh token 으로 세션이 유지되므로 적용하지 않는다.
  /// 반환값: 타임아웃으로 로그아웃을 수행했으면 true.
  Future<bool> _enforceResumeReloginTimeout() async {
    final pausedAt = _pausedAt;
    _pausedAt = null;
    if (pausedAt == null) return false;

    // 보호할 활성 세션이 없으면(이미 로그인 화면 등) 무시.
    if (!ref.read(authProvider).isAuthenticated) return false;

    // 백그라운드 체류 시간이 임계값 미만이면 유지.
    if (DateTime.now().difference(pausedAt) < _resumeReloginTimeout) {
      return false;
    }

    // 자동로그인 ON 은 세션 유지(레거시: 앱이 자격증명 재제출로 무중단 재인증).
    final autoLogin =
        await ref.read(authLocalDataSourceProvider).isAutoLoginEnabled();
    if (!mounted || autoLogin) return false;

    // 세션 정리 + 루트 ProviderScope 재생성 → 로그인 화면(사유 1회 안내).
    await ref.read(authLocalDataSourceProvider).clearTokens();
    SessionResetController.instance
        .requestReset(reason: LogoutReason.inactivityTimeout);
    return true;
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

  /// authState 의 종료 상태로부터 네비게이션 목적지를 도출한다.
  ///
  /// 종료 상태가 아니거나(초기화 전·로딩 중) 재네비게이션 대상이 아니면 null.
  String? _navigationTargetFor(AuthState state) {
    // 초기화 완료 전에는 전환하지 않음.
    if (!state.isInitialized) return null;

    // 진행 중(로딩) 상태에는 화면 전환하지 않음 — 동일 화면 재진입(깜빡임) 방지.
    // 네비게이션 대상은 모두 종료 상태(authenticated/passwordChangeRequired/gpsConsent/login)이며,
    // 로딩은 항상 종료 상태로 이어지는 과도 상태이므로 건너뛰어도 안전하다.
    if (state.isLoading) return null;

    if (state.isAuthenticated) {
      // 인증 완료 → 메인 화면
      return AppRouter.main;
    } else if (state.passwordChangeRequired) {
      // 비밀번호 변경 필요 → 비밀번호 변경 화면
      return AppRouter.changePassword;
    } else if (state.requiresGpsConsent && state.user != null) {
      // GPS 동의 필요 → GPS 동의 화면
      return AppRouter.gpsConsent;
    } else if (state.user == null && state.errorMessage == null) {
      // 미인증 상태(로그아웃·자동로그인 실패·토큰만료) → 로그인 화면.
      return AppRouter.login;
    }
    // 로그인 에러(errorMessage != null)는 이미 로그인 화면에서 발생한 것이므로
    // 재네비게이션하지 않는다(에러는 화면 내 오버레이로 즉시 표시됨).
    return null;
  }

  /// 현재 authState 기준으로 화면 전환을 수행한다(상태 기반 — edge 가 아니라 현재 값).
  ///
  /// Navigator 가 아직 attach 되지 않았으면(앱 시작 직후 GlobalKey 미부착) 다음 프레임에
  /// 재시도한다. `ref.listen` 은 상태 "변화" 만 잡으므로, 종료 상태로의 전이가
  /// Navigator attach 보다 먼저 일어나 한 번 흘려보내면 재발화가 없어 스플래시가 영구
  /// 정지한다 — 이 메서드는 그 종료 상태를 현재 값으로 다시 평가해 복구한다.
  void _applyNavigation() {
    final navigator = navigatorKey.currentState;
    if (navigator == null) {
      // Navigator 미부착 — 다음 프레임에 현재 상태로 재평가(놓친 전이 복구).
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        _applyNavigation();
      });
      return;
    }

    final target = _navigationTargetFor(ref.read(authProvider));

    // 동일 목적지로의 중복 push 차단.
    // LoginScreen 진입 후 savedEmployeeNumber 로드 등으로 authState 가 다시 emit 되어도
    // 목적지가 변하지 않으면 재 push 하지 않는다 — 로그인 화면이 무한히 쌓이는 루프 방지.
    if (target == null || target == _lastAuthRoute) return;
    _lastAuthRoute = target;
    navigator.pushNamedAndRemoveUntil(target, (route) => false);
  }

  @override
  Widget build(BuildContext context) {
    // 인증 상태 변화 감지 → 현재 상태 기준으로 화면 전환(navigator 미부착 시 재시도).
    ref.listen<AuthState>(authProvider, (previous, next) {
      _applyNavigation();
    });

    return MaterialApp(
      title: '오뚜기 임직원 영업관리',
      theme: AppTheme.light,
      navigatorKey: navigatorKey,
      // 리셋 재생성 세션(로그아웃/로그인)은 스플래시/버전 게이트를 건너뛰고 로그인에서
      // 출발한다. 로그인 복원 세션은 restoreSession 이 인증 완료 후 홈으로 전환한다.
      initialRoute: (widget.startAtLogin || widget.startAuthenticated)
          ? AppRouter.login
          : AppRouter.initialRoute,
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
