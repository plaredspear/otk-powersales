import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/services/fcm_token_registrar.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/data/datasources/auth_local_datasource.dart';
import 'package:mobile/domain/entities/attendance_summary.dart';
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/repositories/home_repository.dart';
import 'package:mobile/domain/usecases/auto_login_usecase.dart';
import 'package:mobile/domain/usecases/change_password_usecase.dart';
import 'package:mobile/domain/usecases/get_home_data.dart';
import 'package:mobile/domain/usecases/login_usecase.dart';
import 'package:mobile/domain/usecases/logout_usecase.dart';
import 'package:mobile/presentation/pages/home_page.dart';
import 'package:mobile/presentation/providers/auth_provider.dart';
import 'package:mobile/presentation/providers/auth_state.dart';
import 'package:mobile/presentation/providers/home_provider.dart';

/// HomePage SafeArea 통합 테스트
///
/// 디바이스별 inset(MediaQuery.padding)을 주입하여 위젯 트리의 픽셀 위치를 검증한다.
void main() {
  HomeData buildHomeData() {
    return const HomeData(
      todaySchedules: [],
      attendanceSummary: AttendanceSummary(totalCount: 0, registeredCount: 0),
      attendanceApplicable: true,
      safetyCheckRequired: false,
      notices: [],
      currentDate: '2026-05-04',
    );
  }

  Future<void> pumpHome(
    WidgetTester tester, {
    required EdgeInsets padding,
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          homeProvider
              .overrideWith((ref) => _FakeHomeNotifier(buildHomeData())),
          authProvider.overrideWith((ref) => _FakeAuthNotifier()),
        ],
        child: MaterialApp(
          debugShowCheckedModeBanner: false,
          home: MediaQuery(
            data: MediaQueryData(
              size: const Size(393, 852),
              padding: padding,
            ),
            child: const Scaffold(
              endDrawer: Drawer(child: SizedBox.shrink()),
              body: HomePage(),
            ),
          ),
        ),
      ),
    );
    // pump 한 번 → postFrameCallback에서 fetchHomeData 호출 (fake는 noop)
    await tester.pump();
  }

  group('HomePage SafeArea / Status Bar', () {
    testWidgets('AnnotatedRegion이 dark icons + 노란 status bar로 적용된다',
        (tester) async {
      await pumpHome(
        tester,
        padding: const EdgeInsets.only(top: 47, bottom: 34),
      );

      final region = tester.widget<AnnotatedRegion<SystemUiOverlayStyle>>(
        find.byType(AnnotatedRegion<SystemUiOverlayStyle>).first,
      );
      expect(region.value.statusBarIconBrightness, Brightness.dark);
      expect(region.value.statusBarBrightness, Brightness.light);
      expect(region.value.systemNavigationBarColor,
          AppColors.homeBgGradientEnd);
    });

    testWidgets('iPhone 14 (top:47, bottom:34) — 노란 헤더 = 47+116-55 = 108',
        (tester) async {
      await pumpHome(
        tester,
        padding: const EdgeInsets.only(top: 47, bottom: 34),
      );

      final containers = tester
          .widgetList<Container>(find.byType(Container))
          .where((c) =>
              c.color == AppColors.legacyYellow &&
              c.constraints != null &&
              (c.constraints!.maxHeight - 108).abs() < 0.5)
          .toList();
      expect(containers, isNotEmpty,
          reason: 'iPhone 14 노란 영역 높이 = 47 + 116 - 55 = 108');
    });

    testWidgets('iPhone SE (top:20, bottom:0) — 노란 헤더 = 20+116-55 = 81',
        (tester) async {
      await pumpHome(
        tester,
        padding: const EdgeInsets.only(top: 20, bottom: 0),
      );

      final containers = tester
          .widgetList<Container>(find.byType(Container))
          .where((c) =>
              c.color == AppColors.legacyYellow &&
              c.constraints != null &&
              (c.constraints!.maxHeight - 81).abs() < 0.5)
          .toList();
      expect(containers, isNotEmpty,
          reason: 'iPhone SE 노란 영역 높이 = 20 + 116 - 55 = 81');
    });

    testWidgets('Pixel (top:24, bottom:48) — 노란 헤더 = 24+116-55 = 85',
        (tester) async {
      await pumpHome(
        tester,
        padding: const EdgeInsets.only(top: 24, bottom: 48),
      );

      final containers = tester
          .widgetList<Container>(find.byType(Container))
          .where((c) =>
              c.color == AppColors.legacyYellow &&
              c.constraints != null &&
              (c.constraints!.maxHeight - 85).abs() < 0.5)
          .toList();
      expect(containers, isNotEmpty,
          reason: 'Pixel 노란 영역 높이 = 24 + 116 - 55 = 85');
    });
  });
}

class _FakeHomeNotifier extends HomeNotifier {
  _FakeHomeNotifier(HomeData data) : super(_FakeGetHomeData()) {
    state = state.toData(data);
  }

  @override
  Future<void> fetchHomeData() async {}

  @override
  Future<void> refresh() async {}
}

class _FakeGetHomeData implements GetHomeData {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeAuthNotifier extends AuthNotifier {
  _FakeAuthNotifier()
      : super(
          loginUseCase: _FakeLoginUseCase(),
          autoLoginUseCase: _FakeAutoLoginUseCase(),
          changePasswordUseCase: _FakeChangePasswordUseCase(),
          logoutUseCase: _FakeLogoutUseCase(),
          localDataSource: _FakeAuthLocalDataSource(),
          repository: _FakeAuthRepository(),
          fcmTokenRegistrar: _FakeFcmTokenRegistrar(),
        ) {
    state = const AuthState(
      isLoading: false,
      isInitialized: true,
      user: User(
        id: 1,
        employeeCode: 'EMP-001',
        name: '테스트',
        orgName: '강남지점',
        role: 'USER',
      ),
    );
  }
}

class _FakeFcmTokenRegistrar implements FcmTokenRegistrar {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeLoginUseCase implements LoginUseCase {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeAutoLoginUseCase implements AutoLoginUseCase {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeChangePasswordUseCase implements ChangePasswordUseCase {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeLogoutUseCase implements LogoutUseCase {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeAuthLocalDataSource implements AuthLocalDataSource {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeAuthRepository implements AuthRepository {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
