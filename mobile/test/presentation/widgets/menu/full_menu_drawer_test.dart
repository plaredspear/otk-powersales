import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/constants/menu_constants.dart';
import 'package:mobile/domain/entities/menu_item.dart' as domain;
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/presentation/providers/auth_provider.dart';
import 'package:mobile/presentation/providers/auth_state.dart';
import 'package:mobile/presentation/widgets/menu/full_menu_drawer.dart';
import 'package:mobile/presentation/widgets/menu/menu_header.dart';
import 'package:mobile/presentation/widgets/menu/quick_action_bar.dart';
import 'package:mobile/presentation/widgets/menu/menu_group_widget.dart';

// Test User
const testUser = User(
  id: 1,
  employeeId: '20010585',
  name: '이점미',
  department: 'G마트A',
  branchName: '서울지점',
  role: 'USER',
);

// Test helper to create ProviderScope with auth state
Widget createWidgetWithAuthState(AuthState authState, Widget child) {
  return ProviderScope(
    overrides: [
      // Override the authProvider to return a notifier with the test state
      authProvider.overrideWith((ref) {
        final notifier = AuthNotifier(
          loginUseCase: ref.watch(loginUseCaseProvider),
          autoLoginUseCase: ref.watch(autoLoginUseCaseProvider),
          changePasswordUseCase: ref.watch(changePasswordUseCaseProvider),
          logoutUseCase: ref.watch(logoutUseCaseProvider),
          localDataSource: ref.watch(authLocalDataSourceProvider),
          repository: ref.watch(authRepositoryProvider),
        );
        // Set the initial state after construction
        notifier.state = authState;
        return notifier;
      }),
    ],
    child: MaterialApp(home: child),
  );
}

void main() {
  group('MenuHeader', () {
    testWidgets('사용자명이 올바르게 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: MenuHeader(
              userName: '이점미',
              userInfo: 'G마트A / 서울지점',
              onClose: () {},
            ),
          ),
        ),
      );

      expect(find.text('이점미님'), findsOneWidget);
    });

    testWidgets('사용자 정보가 올바르게 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: MenuHeader(
              userName: '이점미',
              userInfo: 'G마트A / 서울지점',
              onClose: () {},
            ),
          ),
        ),
      );

      expect(find.text('G마트A / 서울지점'), findsOneWidget);
    });

    testWidgets('닫기 버튼 탭 시 onClose 콜백이 호출된다', (tester) async {
      bool closeCalled = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: MenuHeader(
              userName: '이점미',
              userInfo: 'G마트A / 서울지점',
              onClose: () {
                closeCalled = true;
              },
            ),
          ),
        ),
      );

      await tester.tap(find.byIcon(Icons.close));
      await tester.pumpAndSettle();

      expect(closeCalled, isTrue);
    });

    testWidgets('프로필 영역 탭 시 onProfileTap 콜백이 호출된다', (tester) async {
      bool profileTapped = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: MenuHeader(
              userName: '이점미',
              userInfo: 'G마트A / 서울지점',
              onClose: () {},
              onProfileTap: () {
                profileTapped = true;
              },
            ),
          ),
        ),
      );

      // 사용자명 영역 탭
      await tester.tap(find.text('이점미님'));
      await tester.pumpAndSettle();

      expect(profileTapped, isTrue);
    });
  });

  group('QuickActionBar', () {
    testWidgets('제품 검색 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: QuickActionBar(),
          ),
        ),
      );

      expect(find.text('제품 검색'), findsOneWidget);
      expect(find.byIcon(Icons.search), findsOneWidget);
    });

    testWidgets('활동 등록 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: QuickActionBar(),
          ),
        ),
      );

      expect(find.text('활동 등록'), findsOneWidget);
      expect(find.byIcon(Icons.add), findsOneWidget);
    });

    testWidgets('제품 검색 버튼 탭 시 콜백이 호출된다', (tester) async {
      bool productSearchTapped = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: QuickActionBar(
              onProductSearchTap: () {
                productSearchTapped = true;
              },
            ),
          ),
        ),
      );

      await tester.tap(find.text('제품 검색'));
      await tester.pumpAndSettle();

      expect(productSearchTapped, isTrue);
    });

    testWidgets('활동 등록 버튼 탭 시 콜백이 호출된다', (tester) async {
      bool activityRegisterTapped = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: QuickActionBar(
              onActivityRegisterTap: () {
                activityRegisterTapped = true;
              },
            ),
          ),
        ),
      );

      await tester.tap(find.text('활동 등록'));
      await tester.pumpAndSettle();

      expect(activityRegisterTapped, isTrue);
    });
  });

  group('MenuGroupWidget', () {
    const testGroup = domain.MenuGroup(
      id: 'test',
      icon: Icons.store,
      label: '거래처',
      items: [
        domain.MenuItem(id: 'my-stores', label: '내 거래처'),
        domain.MenuItem(id: 'store-search', label: '거래처 검색'),
      ],
    );

    testWidgets('그룹명이 표시된다', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: MenuGroupWidget(
              group: testGroup,
            ),
          ),
        ),
      );

      expect(find.text('거래처'), findsOneWidget);
      expect(find.byIcon(Icons.store), findsOneWidget);
    });

    testWidgets('메뉴 아이템들이 표시된다', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: MenuGroupWidget(
              group: testGroup,
            ),
          ),
        ),
      );

      expect(find.text('내 거래처'), findsOneWidget);
      expect(find.text('거래처 검색'), findsOneWidget);
    });

    testWidgets('아이템 탭 시 onItemTap 콜백이 호출된다', (tester) async {
      domain.MenuItem? tappedItem;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: MenuGroupWidget(
              group: testGroup,
              onItemTap: (item) {
                tappedItem = item;
              },
            ),
          ),
        ),
      );

      await tester.tap(find.text('내 거래처'));
      await tester.pumpAndSettle();

      expect(tappedItem, isNotNull);
      expect(tappedItem?.id, equals('my-stores'));
      expect(tappedItem?.label, equals('내 거래처'));
    });
  });

  group('FullMenuDrawer', () {
    testWidgets('사용자명이 헤더에 표시된다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      expect(find.text('이점미님'), findsOneWidget);
    });

    testWidgets('메뉴 그룹이 모두 표시된다 (7개 그룹명 확인)', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      // 7개 그룹명 확인
      expect(find.text('거래처'), findsOneWidget);
      expect(find.text('주문'), findsOneWidget);
      expect(find.text('제품'), findsOneWidget);
      expect(find.text('매출 현황'), findsOneWidget);
      expect(find.text('현장톡'), findsOneWidget);
      expect(find.text('커뮤니티'), findsOneWidget);
      expect(find.text('마이페이지'), findsOneWidget);

      // 총 7개 그룹 확인
      expect(MenuConstants.menuGroups.length, equals(7));
    });

    testWidgets('로그아웃 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      expect(find.text('로그아웃'), findsOneWidget);
    });

    testWidgets('빠른 액션 버튼들이 표시된다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      expect(find.text('제품 검색'), findsOneWidget);
      expect(find.text('활동 등록'), findsOneWidget);
    });

    testWidgets('Drawer 너비가 화면 너비의 85%이다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      final screenSize = tester.getSize(find.byType(MaterialApp));
      final drawerSize = tester.getSize(find.byType(Drawer));

      expect(drawerSize.width, closeTo(screenSize.width * 0.85, 0.1));
    });

    testWidgets('사용자 정보가 헤더에 올바르게 표시된다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      expect(find.text('이점미님'), findsOneWidget);
      expect(find.text('G마트A / 서울지점'), findsOneWidget);
    });

    testWidgets('메뉴 그룹 위젯이 모두 표시된다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      // MenuGroupWidget 개수 확인
      expect(find.byType(MenuGroupWidget), findsNWidgets(7));
    });

    testWidgets('QuickActionBar가 표시된다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      expect(find.byType(QuickActionBar), findsOneWidget);
    });

    testWidgets('MenuHeader가 표시된다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      expect(find.byType(MenuHeader), findsOneWidget);
    });

    testWidgets('메뉴 아이템 개수가 올바르다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      // MenuConstants에서 정의된 총 아이템 수 확인
      expect(MenuConstants.itemCount, equals(13));
    });

    testWidgets('사용자 정보가 없는 경우 기본값이 표시된다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial(),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      expect(find.text('사용자님'), findsOneWidget);
    });

    testWidgets('로그아웃 버튼 탭 시 확인 다이얼로그가 표시된다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      // 로그아웃 버튼 탭
      await tester.tap(find.text('로그아웃'));
      await tester.pumpAndSettle();

      // 다이얼로그 확인
      expect(find.byType(AlertDialog), findsOneWidget);
      expect(find.text('로그아웃 하시겠습니까?'), findsOneWidget);
      expect(find.text('취소'), findsOneWidget);
      expect(find.text('확인'), findsOneWidget);
    });

    testWidgets('로그아웃 다이얼로그에서 취소 버튼 탭 시 다이얼로그가 닫힌다', (tester) async {
      await tester.pumpWidget(
        createWidgetWithAuthState(
          AuthState.initial().toAuthenticated(testUser),
          const Scaffold(endDrawer: FullMenuDrawer()),
        ),
      );

      await tester.pumpAndSettle();

      // Drawer 열기
      final scaffoldState = tester.state<ScaffoldState>(find.byType(Scaffold));
      scaffoldState.openEndDrawer();
      await tester.pumpAndSettle();

      // 로그아웃 버튼 탭
      await tester.tap(find.text('로그아웃'));
      await tester.pumpAndSettle();

      // 취소 버튼 탭
      await tester.tap(find.text('취소'));
      await tester.pumpAndSettle();

      // 다이얼로그가 닫혔는지 확인
      expect(find.byType(AlertDialog), findsNothing);
    });
  });
}
