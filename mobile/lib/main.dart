import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app_router.dart';
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

class _OtokiAppState extends ConsumerState<OtokiApp> {
  final _navigatorKey = GlobalKey<NavigatorState>();

  @override
  void initState() {
    super.initState();
    // 앱 시작 시 인증 초기화 (저장된 사번 로드 + 자동 로그인 시도)
    Future.microtask(() {
      ref.read(authProvider.notifier).initialize();
    });
  }

  @override
  Widget build(BuildContext context) {
    // 인증 상태 변화 감지 → 화면 전환
    ref.listen<AuthState>(authProvider, (previous, next) {
      final navigator = _navigatorKey.currentState;
      if (navigator == null) return;

      // 초기화 완료 전에는 무시
      if (!next.isInitialized) return;

      if (next.isAuthenticated) {
        // 인증 완료 → 메인 화면
        navigator.pushNamedAndRemoveUntil(
          AppRouter.main,
          (route) => false,
        );
      } else if (next.requiresPasswordChange) {
        // 비밀번호 변경 필요 → 비밀번호 변경 화면
        navigator.pushNamedAndRemoveUntil(
          AppRouter.changePassword,
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
      navigatorKey: _navigatorKey,
      initialRoute: AppRouter.initialRoute,
      routes: AppRouter.routes,
      onUnknownRoute: AppRouter.onUnknownRoute,
      debugShowCheckedModeBanner: false,
    );
  }
}
