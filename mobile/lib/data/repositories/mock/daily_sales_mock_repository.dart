import 'dart:io';

import '../../../domain/entities/daily_sales.dart';
import '../../../domain/repositories/daily_sales_repository.dart';
import '../../mock/daily_sales_mock_data.dart';

/// 일매출 Mock Repository
///
/// Backend API 개발 전 프론트엔드 개발을 위한 Mock 데이터 제공.
/// Flutter-First 전략에 따라 하드코딩된 데이터로 UI/UX 검증.
class DailySalesMockRepository implements DailySalesRepository {
  /// 등록된 일매출 목록 (메모리에 저장)
  final List<DailySales> _registeredSales = [];

  /// 자동 증가 ID
  int _nextId = 1;

  DailySalesMockRepository() {
    // Mock 데이터 초기화
    _registeredSales.addAll(DailySalesMockData.data);
    _nextId = _registeredSales.length + 1;
  }

  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 300));
  }

  @override
  Future<DailySales> registerDailySales({
    required String eventId,
    required DateTime salesDate,
    int? mainProductPrice,
    int? mainProductQuantity,
    int? mainProductAmount,
    String? subProductCode,
    String? subProductName,
    int? subProductQuantity,
    int? subProductAmount,
    required File photo,
  }) async {
    await _simulateDelay();

    // 비즈니스 로직 검증
    final hasMainProduct = mainProductPrice != null &&
        mainProductQuantity != null &&
        mainProductAmount != null;
    final hasSubProduct = subProductCode != null &&
        subProductName != null &&
        subProductQuantity != null &&
        subProductAmount != null;

    if (!hasMainProduct && !hasSubProduct) {
      throw Exception('대표 제품 또는 기타 제품 중 최소 하나를 입력해주세요');
    }

    // 중복 등록 체크 (같은 행사, 같은 날짜에 이미 등록된 경우)
    final existingRegistered = _registeredSales.where((sales) {
      return sales.eventId == eventId &&
          sales.salesDate.year == salesDate.year &&
          sales.salesDate.month == salesDate.month &&
          sales.salesDate.day == salesDate.day &&
          sales.status == DailySalesStatus.registered;
    }).toList();

    if (existingRegistered.isNotEmpty) {
      throw Exception('오늘 매출이 이미 등록되었습니다');
    }

    // 새로운 일매출 생성
    final newSales = DailySales(
      id: 'ds-${_nextId.toString().padLeft(3, '0')}',
      eventId: eventId,
      salesDate: salesDate,
      mainProductPrice: mainProductPrice,
      mainProductQuantity: mainProductQuantity,
      mainProductAmount: mainProductAmount,
      subProductCode: subProductCode,
      subProductName: subProductName,
      subProductQuantity: subProductQuantity,
      subProductAmount: subProductAmount,
      photoUrl: 'https://example.com/photos/${photo.path.split('/').last}',
      status: DailySalesStatus.registered,
      registeredAt: DateTime.now(),
    );

    _registeredSales.add(newSales);
    _nextId++;

    return newSales;
  }

  @override
  Future<DailySales> saveDraft({
    required String eventId,
    required DateTime salesDate,
    int? mainProductPrice,
    int? mainProductQuantity,
    int? mainProductAmount,
    String? subProductCode,
    String? subProductName,
    int? subProductQuantity,
    int? subProductAmount,
    File? photo,
  }) async {
    await _simulateDelay();

    // 임시저장은 유효성 검증 없이 저장
    final newSales = DailySales(
      id: 'ds-draft-${_nextId.toString().padLeft(3, '0')}',
      eventId: eventId,
      salesDate: salesDate,
      mainProductPrice: mainProductPrice,
      mainProductQuantity: mainProductQuantity,
      mainProductAmount: mainProductAmount,
      subProductCode: subProductCode,
      subProductName: subProductName,
      subProductQuantity: subProductQuantity,
      subProductAmount: subProductAmount,
      photoUrl: photo != null
          ? 'https://example.com/photos/${photo.path.split('/').last}'
          : null,
      status: DailySalesStatus.draft,
      registeredAt: null,
    );

    _registeredSales.add(newSales);
    _nextId++;

    return newSales;
  }

  /// 특정 행사의 일매출 목록 조회 (테스트용)
  List<DailySales> getByEventId(String eventId) {
    return _registeredSales
        .where((sales) => sales.eventId == eventId)
        .toList();
  }

  /// 전체 일매출 목록 조회 (테스트용)
  List<DailySales> getAll() {
    return List.unmodifiable(_registeredSales);
  }

  /// Mock 데이터 초기화 (테스트용)
  void reset() {
    _registeredSales.clear();
    _registeredSales.addAll(DailySalesMockData.data);
    _nextId = _registeredSales.length + 1;
  }
}
