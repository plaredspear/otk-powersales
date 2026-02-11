import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_category.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/domain/entities/claim_form.dart';
import 'package:mobile/domain/entities/claim_form_data.dart';
import 'package:mobile/domain/entities/claim_result.dart';
import 'package:mobile/domain/usecases/get_claim_form_data_usecase.dart';
import 'package:mobile/domain/usecases/register_claim_usecase.dart';
import 'package:mobile/presentation/pages/claim_register_page.dart';
import 'package:mobile/presentation/providers/claim_register_provider.dart';

// Mock UseCases
class MockGetClaimFormDataUseCase implements GetClaimFormDataUseCase {
  @override
  Future<ClaimFormData> call() async {
    return ClaimFormData(
      categories: [
        ClaimCategory(
          id: 1,
          name: '이물',
          subcategories: [
            const ClaimSubcategory(id: 101, name: '벌레'),
            const ClaimSubcategory(id: 102, name: '금속'),
          ],
        ),
        ClaimCategory(
          id: 2,
          name: '변질/변패',
          subcategories: [
            const ClaimSubcategory(id: 201, name: '맛 변질'),
          ],
        ),
      ],
      purchaseMethods: const [
        PurchaseMethod(code: 'PM01', name: '대형마트'),
        PurchaseMethod(code: 'PM02', name: '편의점'),
      ],
      requestTypes: const [
        ClaimRequestType(code: 'RT01', name: '제품 교환'),
        ClaimRequestType(code: 'RT02', name: '환불 요청'),
      ],
    );
  }
}

class MockRegisterClaimUseCase implements RegisterClaimUseCase {
  @override
  Future<ClaimRegisterResult> call(ClaimRegisterForm form) async {
    return ClaimRegisterResult(
      id: 1,
      storeName: form.storeName,
      storeId: form.storeId,
      productName: form.productName,
      productCode: form.productCode,
      createdAt: DateTime.now(),
    );
  }
}

void main() {
  group('ClaimRegisterPage', () {
    Widget buildTestWidget() {
      return ProviderScope(
        overrides: [
          claimRegisterProvider.overrideWith((ref) {
            return ClaimRegisterNotifier(
              registerClaimUseCase: MockRegisterClaimUseCase(),
              getClaimFormDataUseCase: MockGetClaimFormDataUseCase(),
            );
          }),
        ],
        child: const MaterialApp(
          home: ClaimRegisterPage(),
        ),
      );
    }

    testWidgets('페이지가 렌더링된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pump(); // Loading
      await tester.pumpAndSettle(); // Data loaded

      // Then
      expect(find.text('클레임 등록'), findsOneWidget);
    });

    testWidgets('AppBar에 뒤로가기 버튼이 있다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.byIcon(Icons.arrow_back), findsOneWidget);
    });

    testWidgets('필수 필드들이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then: 필수 필드 라벨들
      expect(find.text('거래처 *'), findsOneWidget);
      expect(find.text('제품 *'), findsOneWidget);
      expect(find.text('기한 *'), findsOneWidget);
      expect(find.text('클레임 종류1 *'), findsOneWidget);
      expect(find.text('클레임 종류2 *'), findsOneWidget);
      expect(find.text('불량 내역 *'), findsOneWidget);
      expect(find.text('불량 수량 *'), findsOneWidget);
      expect(find.text('불량 사진 *'), findsOneWidget);
      expect(find.text('일부인 사진 *'), findsOneWidget);
    });

    testWidgets('구매 정보 섹션이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.text('구매 정보 (선택)'), findsOneWidget);
      expect(find.text('구매 금액'), findsOneWidget);
      expect(find.text('구매 방법'), findsOneWidget);
      expect(find.text('구매 영수증 사진'), findsOneWidget);
    });

    testWidgets('요청사항 필드가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.text('요청사항'), findsOneWidget);
    });

    testWidgets('하단에 임시저장과 전송 버튼이 있다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.text('임시저장'), findsOneWidget);
      expect(find.text('전송'), findsOneWidget);
    });

    testWidgets('임시저장 버튼 탭 시 스낵바가 표시된다', (tester) async {
      // Given
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // When
      await tester.tap(find.text('임시저장'));
      await tester.pumpAndSettle();

      // Then
      expect(find.text('임시 저장되었습니다'), findsOneWidget);
    });

    testWidgets('제품 바코드 버튼이 동작한다', (tester) async {
      // Given
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // When
      await tester.tap(find.text('바코드'));
      await tester.pumpAndSettle();

      // Then
      expect(find.text('바코드 스캔 기능은 추후 구현 예정입니다'), findsOneWidget);
    });

    testWidgets('제품 선택 버튼이 동작한다', (tester) async {
      // Given
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // When
      await tester.tap(find.text('선택'));
      await tester.pumpAndSettle();

      // Then
      expect(find.text('제품 선택 기능은 추후 구현 예정입니다'), findsOneWidget);
    });
  });
}
