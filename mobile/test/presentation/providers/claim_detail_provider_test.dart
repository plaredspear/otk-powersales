import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_detail.dart';
import 'package:mobile/domain/entities/claim_list_item.dart';
import 'package:mobile/domain/entities/claim_photo.dart';
import 'package:mobile/domain/repositories/claim_repository.dart';
import 'package:mobile/domain/entities/claim_form.dart';
import 'package:mobile/domain/entities/claim_form_entry.dart';
import 'package:mobile/domain/entities/claim_result.dart';
import 'package:mobile/domain/usecases/get_claim_detail_usecase.dart';
import 'package:mobile/presentation/providers/claim_detail_provider.dart';

void main() {
  group('ClaimDetailNotifier', () {
    late ClaimDetailNotifier notifier;
    late FakeClaimRepository fakeRepository;

    setUp(() {
      fakeRepository = FakeClaimRepository();
      final useCase = GetClaimDetailUseCase(fakeRepository);
      notifier = ClaimDetailNotifier(getClaimDetail: useCase);
    });

    test('초기 상태가 올바르게 설정되어야 한다', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.detail, isNull);
      expect(notifier.state.errorMessage, isNull);
    });

    group('loadDetail', () {
      test('조회 성공 시 detail을 업데이트해야 한다', () async {
        fakeRepository.detailToReturn = _sampleDetail;
        await notifier.loadDetail(1);
        expect(notifier.state.detail, isNotNull);
        expect(notifier.state.detail!.claimId, 1);
        expect(notifier.state.detail!.status, 'SUBMITTED');
        expect(notifier.state.detail!.photos.length, 1);
        expect(notifier.state.isLoading, false);
      });

      test('조회 실패 시 에러 메시지를 설정해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('클레임을 찾을 수 없습니다');
        await notifier.loadDetail(999);
        expect(notifier.state.errorMessage, '클레임을 찾을 수 없습니다');
        expect(notifier.state.detail, isNull);
        expect(notifier.state.isLoading, false);
      });
    });

    test('clearError로 에러 메시지를 초기화해야 한다', () async {
      fakeRepository.exceptionToThrow = Exception('에러');
      await notifier.loadDetail(1);
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
  Future<ClaimFormEntry> getForm() async => throw UnimplementedError();

  @override
  Future<void> saveDraft(ClaimRegisterForm? form) async =>
      throw UnimplementedError();

  @override
  Future<void> deleteDraft() async => throw UnimplementedError();
}

final _sampleDetail = ClaimDetail(
  claimId: 1,
  accountName: '미광종합물류',
  productName: '진라면(매운맛)멀티',
  productCode: '12345678',
  dateType: 'EXPIRY_DATE',
  dateTypeLabel: '유통기한',
  date: DateTime(2026, 8, 19),
  categoryName: '포장불량',
  subcategoryName: '누수/누유',
  defectDescription: '포장 파손으로 내용물 누유 확인',
  defectQuantity: 3,
  purchaseAmount: 3500,
  purchaseMethodName: '개인카드',
  requestTypeName: '교환',
  status: 'SUBMITTED',
  statusLabel: '접수',
  createdAt: DateTime(2026, 4, 8, 10, 30),
  photos: [
    const ClaimPhoto(
      photoId: 1,
      photoType: 'DEFECT',
      photoTypeLabel: '불량 사진',
      url: 'https://cdn.example.com/claims/1/defect.jpg',
      originalFileName: 'IMG_001.jpg',
    ),
  ],
);
