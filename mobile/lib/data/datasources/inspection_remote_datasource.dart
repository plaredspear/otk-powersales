import 'dart:io';

import '../models/inspection_detail_model.dart';
import '../models/inspection_field_type_model.dart';
import '../models/inspection_list_item_model.dart';
import '../models/inspection_register_request.dart';
import '../models/inspection_theme_model.dart';

/// 현장 점검 API DataSource 인터페이스
///
/// 현장 점검 관련 API 호출을 추상화합니다.
abstract class InspectionRemoteDataSource {
  /// GET /api/v1/inspections
  ///
  /// 현장 점검 목록을 조회합니다.
  ///
  /// [storeId] 거래처 ID (null이면 전체)
  /// [category] 분류 (OWN/COMPETITOR, null이면 전체)
  /// [fromDate] 점검일 시작 (YYYY-MM-DD)
  /// [toDate] 점검일 종료 (YYYY-MM-DD)
  Future<List<InspectionListItemModel>> getInspectionList({
    int? storeId,
    String? category,
    required String fromDate,
    required String toDate,
  });

  /// GET /api/v1/inspections/{inspectionId}
  ///
  /// 현장 점검 상세 정보를 조회합니다.
  Future<InspectionDetailModel> getInspectionDetail(int inspectionId);

  /// POST /api/v1/inspections
  ///
  /// 현장 점검을 등록합니다 (multipart/form-data).
  ///
  /// [request] 등록 요청 데이터 (폼 필드 + 사진 파일)
  Future<InspectionListItemModel> registerInspection(
    InspectionRegisterRequest request,
  );

  /// GET /api/v1/inspections/themes
  ///
  /// 테마 목록을 조회합니다 (오늘 기준 기간 포함 테마만).
  Future<List<InspectionThemeModel>> getThemes();

  /// GET /api/v1/inspections/field-types
  ///
  /// 현장 유형 코드 목록을 조회합니다.
  Future<List<InspectionFieldTypeModel>> getFieldTypes();
}
