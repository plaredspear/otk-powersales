import '../entities/inspection_list_item.dart';
import '../entities/inspection_detail.dart';
import '../entities/inspection_form.dart';
import '../entities/inspection_theme.dart';
import '../entities/inspection_field_type.dart';

/// 현장 점검 Repository 인터페이스
///
/// 현장 점검 CRUD 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class InspectionRepository {
  /// 현장 점검 목록 조회
  ///
  /// [filter]: 검색 필터 (거래처, 분류, 기간)
  /// Returns: 현장 점검 목록 항목 목록
  Future<List<InspectionListItem>> getInspectionList(InspectionFilter filter);

  /// 현장 점검 상세 조회
  ///
  /// [id]: 점검 ID
  /// Returns: 현장 점검 상세 정보
  Future<InspectionDetail> getInspectionDetail(int id);

  /// 현장 점검 등록
  ///
  /// [form]: 등록 폼 데이터
  /// Returns: 등록된 현장 점검 항목
  Future<InspectionListItem> registerInspection(InspectionRegisterForm form);

  /// 테마 목록 조회
  ///
  /// Returns: 오늘 기준 기간 포함 테마 목록
  Future<List<InspectionTheme>> getThemes();

  /// 현장 유형 목록 조회
  ///
  /// Returns: 현장 유형 코드 목록
  Future<List<InspectionFieldType>> getFieldTypes();
}
