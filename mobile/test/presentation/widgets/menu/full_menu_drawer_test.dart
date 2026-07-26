import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/constants/user_roles.dart';
import 'package:mobile/core/services/fcm_token_registrar.dart';
import 'package:mobile/data/datasources/auth_local_datasource.dart';
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/usecases/auto_login_usecase.dart';
import 'package:mobile/domain/usecases/change_password_usecase.dart';
import 'package:mobile/domain/usecases/login_usecase.dart';
import 'package:mobile/domain/usecases/logout_usecase.dart';
import 'package:mobile/presentation/providers/auth_provider.dart';
import 'package:mobile/presentation/providers/auth_state.dart';
import 'package:mobile/presentation/widgets/menu/full_menu_drawer.dart';
import 'package:package_info_plus/package_info_plus.dart';

/// 전체메뉴 드로워 권한별 그룹 노출 테스트
///
/// 핵심 회귀 대상: "여사원 관리" 그룹이 조장(LEADER) + 지점장(ADMIN) 모두에게 노출되고
/// 여사원(USER)·부서장(AccountViewAll)에게는 노출되지 않는다.
void main() {
  setUp(() {
    PackageInfo.setMockInitialValues(
      appName: 'mobile',
      packageName: 'com.otoki.mobile',
      version: '1.0.0',
      buildNumber: '1',
      buildSignature: '',
    );
  });

  Future<void> pumpDrawer(
    WidgetTester tester, {
    required String role,
    String? rawRole,
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authProvider.overrideWith(
            (ref) => _FakeAuthNotifier(role: role, rawRole: rawRole),
          ),
        ],
        child: MaterialApp(
          home: Scaffold(body: FullMenuDrawer()),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  group('FullMenuDrawer — 여사원 관리 그룹', () {
    testWidgets('조장(LEADER)에게 노출된다', (tester) async {
      await pumpDrawer(tester, role: UserRoles.leader, rawRole: '조장');

      expect(find.text('여사원 관리'), findsOneWidget);
    });

    testWidgets('지점장(ADMIN)에게도 조장과 동일하게 노출된다', (tester) async {
      await pumpDrawer(tester, role: UserRoles.branchManager, rawRole: '지점장');

      expect(find.text('여사원 관리'), findsOneWidget);
    });

    testWidgets('여사원(USER)에게는 노출되지 않는다', (tester) async {
      await pumpDrawer(tester, role: UserRoles.user, rawRole: '여사원');

      expect(find.text('여사원 관리'), findsNothing);
    });

    testWidgets('부서장(AccountViewAll)에게는 노출되지 않고 대리출근만 노출된다',
        (tester) async {
      await pumpDrawer(
        tester,
        role: UserRoles.user,
        rawRole: 'AccountViewAll',
      );

      expect(find.text('여사원 관리'), findsNothing);
      expect(find.text('대리출근'), findsOneWidget);
    });
  });

  group('FullMenuDrawer — 소비기한 메뉴', () {
    testWidgets('지점장(ADMIN)은 조장과 동일하게 "제품" 그룹이 비노출된다', (tester) async {
      await pumpDrawer(tester, role: UserRoles.branchManager, rawRole: '지점장');

      expect(find.text('제품'), findsNothing);
    });

    testWidgets('여사원(USER)에게는 "제품" 그룹이 노출된다', (tester) async {
      await pumpDrawer(tester, role: UserRoles.user, rawRole: '여사원');

      expect(find.text('제품'), findsOneWidget);
    });
  });
}

class _FakeAuthNotifier extends AuthNotifier {
  _FakeAuthNotifier({required String role, String? rawRole})
      : super(
          loginUseCase: _FakeLoginUseCase(),
          autoLoginUseCase: _FakeAutoLoginUseCase(),
          changePasswordUseCase: _FakeChangePasswordUseCase(),
          logoutUseCase: _FakeLogoutUseCase(),
          localDataSource: _FakeAuthLocalDataSource(),
          repository: _FakeAuthRepository(),
          fcmTokenRegistrar: _FakeFcmTokenRegistrar(),
        ) {
    state = AuthState(
      isLoading: false,
      isInitialized: true,
      user: User(
        id: 1,
        employeeCode: 'EMP-001',
        name: '테스트',
        orgName: '강남지점',
        role: role,
        rawRole: rawRole,
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
