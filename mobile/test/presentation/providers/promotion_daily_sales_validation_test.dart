import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_sales_form.dart';
import 'package:mobile/presentation/providers/promotion_daily_sales_provider.dart';

/// 일 매출 등록 검증 (레거시 `promotion/event/write.jsp` `#send` 핸들러 정합).
void main() {
  DailySalesForm form({
    bool attendanceRegistered = true,
    String? imageUrl = 'https://s3/photo.jpg',
    String? promotionType = '시식',
    String? promotionName = '상온(오뚜기카레_매운맛100G)',
  }) {
    return DailySalesForm(
      promotionEmployeeId: 1,
      promotionId: 10,
      promotionType: promotionType,
      promotionName: promotionName,
      isClosed: false,
      editable: true,
      attendanceRegistered: attendanceRegistered,
      hasDraft: false,
      imageUrl: imageUrl,
    );
  }

  PromotionDailySalesState state({
    int? mainQuantity,
    int? mainAmount,
    String? subName,
    int? subQuantity,
    int? subAmount,
    bool attendanceRegistered = true,
    String? imageUrl = 'https://s3/photo.jpg',
  }) {
    return PromotionDailySalesState(
      form: form(
        attendanceRegistered: attendanceRegistered,
        imageUrl: imageUrl,
      ),
      mainQuantity: mainQuantity,
      mainAmount: mainAmount,
      subName: subName,
      subQuantity: subQuantity,
      subAmount: subAmount,
    );
  }

  group('validationError - 레거시 검사 순서/문구', () {
    test('아무것도 입력하지 않으면 대표/기타 중 하나 필수 안내', () {
      expect(
        state().validationError,
        '대표 제품 및 기타 제품 중 한 개는 필수로 입력해야 합니다.',
      );
    });

    test('대표제품 수량만 입력하면 총 판매 금액 요구', () {
      expect(
        state(mainQuantity: 10).validationError,
        '총 판매 금액을 입력하세요.',
      );
    });

    test('대표제품 금액만 입력하면 판매 수량 요구', () {
      expect(
        state(mainAmount: 10000).validationError,
        '판매 수량을 입력하세요.',
      );
    });

    test('기타제품 부분 입력은 대표제품이 완성되어 있어도 차단', () {
      final s = state(mainQuantity: 10, mainAmount: 10000, subQuantity: 5);
      expect(s.validationError, '행사 대체 제품을 입력하세요.');
    });

    // 레거시 1차 검사는 수량/금액 4필드만 본다 → 대체제품명만 입력한 상태는
    // "기타제품 판매 수량" 이 아니라 1차 안내 문구가 나온다.
    test('기타제품 대체제품명만 입력하면 1차 안내 문구', () {
      expect(
        state(subName: '진라면').validationError,
        '대표 제품 및 기타 제품 중 한 개는 필수로 입력해야 합니다.',
      );
    });

    test('대표제품 완비 + 기타 대체제품명만 있으면 기타 수량 요구', () {
      expect(
        state(mainQuantity: 10, mainAmount: 10000, subName: '진라면')
            .validationError,
        '기타제품 판매 수량을 입력하세요.',
      );
    });

    test('기타제품 대체제품명 + 수량이면 총 판매 금액 요구', () {
      expect(
        state(subName: '진라면', subQuantity: 5).validationError,
        '총 판매 금액을 입력하세요.',
      );
    });

    test('사진이 없으면 사진 첨부 요구 (상품 검증 통과 후)', () {
      expect(
        state(mainQuantity: 10, mainAmount: 10000, imageUrl: null)
            .validationError,
        '사진을 첨부해 주세요.',
      );
    });

    test('출근 미등록이면 마지막에 출근등록 요구', () {
      expect(
        state(
          mainQuantity: 10,
          mainAmount: 10000,
          attendanceRegistered: false,
        ).validationError,
        '출근등록을 완료해주세요.',
      );
    });

    test('대표제품만 완비하면 통과', () {
      expect(state(mainQuantity: 10, mainAmount: 10000).validationError, isNull);
    });

    test('기타제품만 완비해도 통과', () {
      expect(
        state(subName: '진라면', subQuantity: 5, subAmount: 5000).validationError,
        isNull,
      );
    });
  });

  group('promotionLabel', () {
    test('`[행사유형]행사명` 으로 조합', () {
      expect(form().promotionLabel, '[시식]상온(오뚜기카레_매운맛100G)');
    });

    test('행사유형이 없으면 행사명만', () {
      expect(form(promotionType: null).promotionLabel, '상온(오뚜기카레_매운맛100G)');
    });

    test('둘 다 없으면 null', () {
      expect(
        form(promotionType: null, promotionName: null).promotionLabel,
        isNull,
      );
    });
  });
}
