import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/services/fcm_token_registrar.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
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

    // 노란 헤더 영역 구조 검증 (commit bb48d9250 정합):
    //   ColoredBox(legacyYellow) > Column [
    //     SizedBox(height: topPadding)  ← SafeArea 상단 inset (디바이스별)
    //     _buildAppBar() → SizedBox(height: 116-55 = 61)  ← Transform 제거 후 축소된 헤더
    //     ScheduleCard ...
    //     SizedBox(height: homeCardProfileGap = 14)
    //   ]
    // 과거 "단일 노란 Container 높이 = top+116-55" 가설은 ColoredBox 전환 +
    // Transform 제거로 무효 → SafeArea inset + AppBar 축소 높이를 직접 검증한다.

    /// 노란 ColoredBox(자식이 Column 인 헤더 영역)의 첫 자식 SizedBox
    /// (=SafeArea 상단 inset) 높이를 찾는다.
    double findTopInsetHeight(WidgetTester tester) {
      final coloredBox = tester.widget<ColoredBox>(
        find
            .byWidgetPredicate(
              (w) =>
                  w is ColoredBox &&
                  w.color == AppColors.legacyYellow &&
                  w.child is Column,
            )
            .first,
      );
      final column = coloredBox.child! as Column;
      return (column.children.first as SizedBox).height!;
    }

    /// AppBar 영역 SizedBox 높이 = 레거시 헤더(116) − 카드 겹침(55) = 61.
    double expectedAppBarHeight() =>
        AppSpacing.homeHeaderHeight - AppSpacing.homeCardOverlap;

    for (final device in const [
      (name: 'iPhone 14', padding: EdgeInsets.only(top: 47, bottom: 34)),
      (name: 'iPhone SE', padding: EdgeInsets.only(top: 20, bottom: 0)),
      (name: 'Pixel', padding: EdgeInsets.only(top: 24, bottom: 48)),
    ]) {
      testWidgets(
          '${device.name} — SafeArea 상단 inset(${device.padding.top.toInt()}) + '
          'AppBar 축소 높이(61) 적용', (tester) async {
        await pumpHome(tester, padding: device.padding);

        // ① SafeArea 상단 inset 이 디바이스 top padding 만큼 정확히 반영된다.
        expect(
          findTopInsetHeight(tester),
          device.padding.top,
          reason: '${device.name}: 노란 영역 상단 inset = ${device.padding.top}',
        );

        // ② AppBar 영역이 Transform 제거 후 116-55=61 로 축소됐다 (회귀 방지).
        final appBarBox = tester.widget<SizedBox>(
          find
              .byWidgetPredicate((w) =>
                  w is SizedBox &&
                  w.height == expectedAppBarHeight() &&
                  w.child is Padding)
              .first,
        );
        expect(appBarBox.height, expectedAppBarHeight());
      });
    }
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
