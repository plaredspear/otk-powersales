import '../../../domain/entities/shelf_life_form.dart';
import '../../../domain/entities/shelf_life_item.dart';
import '../../../domain/repositories/shelf_life_repository.dart';

/// 유통기한 Mock Repository
///
/// Backend API 개발 전 Flutter-First 전략에 따라
/// 하드코딩된 Mock 데이터로 유통기한 관리 기능을 구현합니다.
class ShelfLifeMockRepository implements ShelfLifeRepository {
  /// Mock 데이터 (in-memory)
  final List<ShelfLifeItem> _items = List.from(_defaultItems);

  /// 다음 ID
  int _nextId = 100;

  /// 테스트용 예외
  Exception? exceptionToThrow;

  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 300));
  }

  @override
  Future<List<ShelfLifeItem>> getShelfLifeList(ShelfLifeFilter filter) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    return _items.where((item) {
      // 거래처 필터
      if (filter.storeId != null && item.storeId != filter.storeId) {
        return false;
      }
      // 날짜 범위 필터
      if (item.expiryDate.isBefore(filter.fromDate) ||
          item.expiryDate.isAfter(filter.toDate)) {
        return false;
      }
      return true;
    }).toList();
  }

  @override
  Future<ShelfLifeItem> registerShelfLife(ShelfLifeRegisterForm form) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final dDay = form.expiryDate.difference(today).inDays;

    final newItem = ShelfLifeItem(
      id: _nextId++,
      productCode: form.productCode,
      productName: _getProductName(form.productCode),
      storeName: _getStoreName(form.storeId),
      storeId: form.storeId,
      expiryDate: form.expiryDate,
      alertDate: form.alertDate,
      dDay: dDay,
      description: form.description,
      isExpired: dDay <= 0,
    );

    _items.add(newItem);
    return newItem;
  }

  @override
  Future<ShelfLifeItem> updateShelfLife(
    int id,
    ShelfLifeUpdateForm form,
  ) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    final index = _items.indexWhere((item) => item.id == id);
    if (index == -1) {
      throw Exception('유통기한 정보를 찾을 수 없습니다');
    }

    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final dDay = form.expiryDate.difference(today).inDays;

    final updated = ShelfLifeItem(
      id: _items[index].id,
      productCode: _items[index].productCode,
      productName: _items[index].productName,
      storeName: _items[index].storeName,
      storeId: _items[index].storeId,
      expiryDate: form.expiryDate,
      alertDate: form.alertDate,
      dDay: dDay,
      description: form.description,
      isExpired: dDay <= 0,
    );

    _items[index] = updated;
    return updated;
  }

  @override
  Future<void> deleteShelfLife(int id) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    _items.removeWhere((item) => item.id == id);
  }

  @override
  Future<int> deleteShelfLifeBatch(List<int> ids) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    final before = _items.length;
    _items.removeWhere((item) => ids.contains(item.id));
    return before - _items.length;
  }

  /// 제품코드 → 제품명 매핑 (Mock)
  String _getProductName(String productCode) {
    const productNames = {
      '30310009': '고등어김치&무조림(캔)280G',
      '11110015': '카레케찹280G',
      '11610028': '고깃집소스(트레이더스)830G',
      '18410022': '미니뿌셔_불고기맛(55GX4)',
      '10210005': '진라면(순한맛)5입',
      '10210006': '진라면(매운맛)5입',
      '11310012': '오뚜기카레약간매운맛100G',
      '12110008': '3분짜장',
    };
    return productNames[productCode] ?? '제품 ($productCode)';
  }

  /// 거래처ID → 거래처명 매핑 (Mock)
  String _getStoreName(int storeId) {
    const storeNames = {
      1025172: '(유)경산식품',
      1025829: '(유)공일구물류',
      1030456: '대성마트',
      1031789: '삼성유통',
      1032100: '한라식자재',
      1033201: '남도푸드',
    };
    return storeNames[storeId] ?? '거래처 ($storeId)';
  }

  /// 기본 Mock 유통기한 목록
  static final List<ShelfLifeItem> _defaultItems = [
    ShelfLifeItem(
      id: 1,
      productCode: '30310009',
      productName: '고등어김치&무조림(캔)280G',
      storeName: '(유)경산식품',
      storeId: 1025172,
      expiryDate: DateTime.now().subtract(const Duration(days: 3)),
      alertDate: DateTime.now().subtract(const Duration(days: 4)),
      dDay: -3,
      description: '',
      isExpired: true,
    ),
    ShelfLifeItem(
      id: 2,
      productCode: '11110015',
      productName: '카레케찹280G',
      storeName: '(유)경산식품',
      storeId: 1025172,
      expiryDate: DateTime.now().add(const Duration(days: 2)),
      alertDate: DateTime.now().add(const Duration(days: 1)),
      dDay: 2,
      description: '',
      isExpired: false,
    ),
    ShelfLifeItem(
      id: 3,
      productCode: '11610028',
      productName: '고깃집소스(트레이더스)830G',
      storeName: '(유)공일구물류',
      storeId: 1025829,
      expiryDate: DateTime.now().add(const Duration(days: 2)),
      alertDate: DateTime.now().add(const Duration(days: 1)),
      dDay: 2,
      description: '3층 선반',
      isExpired: false,
    ),
    ShelfLifeItem(
      id: 4,
      productCode: '18410022',
      productName: '미니뿌셔_불고기맛(55GX4)',
      storeName: '대성마트',
      storeId: 1030456,
      expiryDate: DateTime.now().add(const Duration(days: 5)),
      alertDate: DateTime.now().add(const Duration(days: 4)),
      dDay: 5,
      description: '',
      isExpired: false,
    ),
    ShelfLifeItem(
      id: 5,
      productCode: '10210005',
      productName: '진라면(순한맛)5입',
      storeName: '삼성유통',
      storeId: 1031789,
      expiryDate: DateTime.now(),
      alertDate: DateTime.now().subtract(const Duration(days: 1)),
      dDay: 0,
      description: '당일 만료',
      isExpired: true,
    ),
  ];
}
