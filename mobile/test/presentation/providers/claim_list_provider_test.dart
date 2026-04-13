import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_detail.dart';
import 'package:mobile/domain/entities/claim_list_item.dart';
import 'package:mobile/domain/entities/claim_photo.dart';
import 'package:mobile/domain/repositories/claim_repository.dart';
import 'package:mobile/domain/entities/claim_form.dart';
import 'package:mobile/domain/entities/claim_form_data.dart';
import 'package:mobile/domain/entities/claim_result.dart';
import 'package:mobile/domain/usecases/get_claims_usecase.dart';
import 'package:mobile/presentation/providers/claim_list_provider.dart';

void main() {
  group('ClaimListNotifier', () {
    late ClaimListNotifier notifier;
    late FakeClaimRepository fakeRepository;

    setUp(() {
      fakeRepository = FakeClaimRepository();
      final useCase = GetClaimsUseCase(fakeRepository);
      notifier = ClaimListNotifier(getClaims: useCase);
    });

    test('초기 상태가 올바르게 설정되어야 한다', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.items, isEmpty);
      expect(notifier.state.hasSearched, false);
    });

    group('loadClaims', () {
      test('조회 성공 시 items를 업데이트해야 한다', () async {
        fakeRepository.claimsToReturn = [_sampleItem1, _sampleItem2];
        await notifier.loadClaims();
        expect(notifier.state.items.length, 2);
        expect(notifier.state.hasSearched, true);
        expect(notifier.state.isLoading, false);
      });

      test('빈 결과 시 isEmpty가 true여야 한다', () async {
        fakeRepository.claimsToReturn = [];
        await notifier.loadClaims();
        expect(notifier.state.items, isEmpty);
        expect(notifier.state.isEmpty, true);
      });

      test('조회 실패 시 에러 메시지를 설정해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('네트워크 오류');
        await notifier.loadClaims();
        expect(notifier.state.errorMessage, '네트워크 오류');
        expect(notifier.state.isLoading, false);
      });
    });

    group('날짜 업데이트', () {
      test('시작일 변경', () {
        final newDate = DateTime(2026, 3, 1);
        notifier.updateStartDate(newDate);
        expect(notifier.state.startDate, newDate);
      });

      test('종료일 변경', () {
        final newDate = DateTime(2026, 3, 15);
        notifier.updateEndDate(newDate);
        expect(notifier.state.endDate, newDate);
      });
    });

    test('clearError로 에러 메시지를 초기화해야 한다', () async {
      fakeRepository.exceptionToThrow = Exception('에러');
      await notifier.loadClaims();
      expect(notifier.state.errorMessage, isNotNull);
      notifier.clearError();
      expect(notifier.state.errorMessage, isNull);
    });
  });
}

class FakeClaimRepository implements ClaimRepository {
  List<ClaimListItem> claimsToReturn = [];
  ClaimDetail? detailToReturn;
  Exception? exceptionToThrow;

  @override
  Future<List<ClaimListItem>> getClaims({
    String? startDate,
    String? endDate,
  }) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return claimsToReturn;
  }

  @override
  Future<ClaimDetail> getClaimDetail(int claimId) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return detailToReturn!;
  }

  @override
  Future<ClaimRegisterResult> registerClaim(ClaimRegisterForm form) async {
    throw UnimplementedError();
  }

  @override
  Future<ClaimFormData> getFormData() async {
    throw UnimplementedError();
  }
}

final _sampleItem1 = ClaimListItem(
  claimId: 1,
  accountName: '미광종합물류',
  productName: '진라면(매운맛)멀티',
  productCode: '12345678',
  categoryName: '포장불량',
  subcategoryName: '누수/누유',
  defectQuantity: 3,
  status: 'SUBMITTED',
  statusLabel: '접수',
  createdAt: DateTime(2026, 4, 8, 10, 30),
);

final _sampleItem2 = ClaimListItem(
  claimId: 2,
  accountName: '한양물류',
  productName: '맛있는부대찌개라양념',
  productCode: '87654321',
  categoryName: '이물혼입',
  subcategoryName: '플라스틱류',
  defectQuantity: 1,
  status: 'RESOLVED',
  statusLabel: '처리완료',
  createdAt: DateTime(2026, 4, 5, 14, 0),
);
