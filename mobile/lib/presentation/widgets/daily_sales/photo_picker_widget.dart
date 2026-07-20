import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/utils/image_picker_helper.dart';

/// 일매출 등록 사진 선택 위젯
///
/// 카메라 촬영 또는 갤러리에서 사진을 선택하고 미리보기를 제공합니다.
/// 최대 1장의 사진만 선택 가능합니다.
///
/// 선택/리사이즈(650px)/JPEG 재인코딩은 공용 [pickImageWithSourceSheet] 에 위임한다
/// (SF ContentVersion 규격 = 레거시 `ImageUtil.resizeImage(650, 650)` 정합).
class PhotoPickerWidget extends StatelessWidget {
  /// 선택된 사진 파일
  final File? photo;

  /// 사진 선택 시 호출되는 콜백
  final ValueChanged<File?> onPhotoChanged;

  /// ImagePicker 인스턴스 (테스트용 주입 가능)
  final ImagePicker? imagePicker;

  const PhotoPickerWidget({
    super.key,
    required this.photo,
    required this.onPhotoChanged,
    this.imagePicker,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 라벨
        const Text(
          '사진 첨부',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 12),

        // 사진 미리보기 또는 선택 버튼
        if (photo != null) _buildPhotoPreview(context) else _buildPickerButtons(context),
      ],
    );
  }

  /// 사진 미리보기 위젯
  Widget _buildPhotoPreview(BuildContext context) {
    return Container(
      width: double.infinity,
      height: 200,
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey.shade300),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Stack(
        fit: StackFit.expand,
        children: [
          // 이미지
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: Image.file(
              photo!,
              fit: BoxFit.cover,
            ),
          ),

          // 삭제 버튼
          Positioned(
            top: 8,
            right: 8,
            child: IconButton(
              onPressed: () => onPhotoChanged(null),
              icon: const Icon(Icons.close),
              style: IconButton.styleFrom(
                backgroundColor: Colors.black54,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.all(8),
              ),
            ),
          ),

          // 재선택 버튼
          Positioned(
            bottom: 8,
            right: 8,
            child: TextButton.icon(
              onPressed: () => _pickImage(context),
              icon: const Icon(Icons.edit),
              label: const Text('다시 선택'),
              style: TextButton.styleFrom(
                backgroundColor: Colors.black54,
                foregroundColor: Colors.white,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 사진 선택 버튼들
  Widget _buildPickerButtons(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey.shade300, width: 2),
        borderRadius: BorderRadius.circular(8),
        color: Colors.grey.shade50,
      ),
      child: Column(
        children: [
          const Icon(
            Icons.add_photo_alternate_outlined,
            size: 48,
            color: Colors.grey,
          ),
          const SizedBox(height: 16),
          const Text(
            '사진을 추가해주세요',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey,
            ),
          ),
          const SizedBox(height: 16),
          // 카메라/갤러리 선택은 공용 시트로 통일 (클레임/현장점검과 동일 UX)
          ElevatedButton.icon(
            onPressed: () => _pickImage(context),
            icon: const Icon(Icons.add_a_photo),
            label: const Text('사진 선택'),
            // Row 안 무한폭 제약 크래시 방지 (전역 테마 minimumSize 덮어쓰기).
            style: ElevatedButton.styleFrom(minimumSize: Size.zero),
          ),
        ],
      ),
    );
  }

  /// 이미지 선택 처리 — 소스 시트/리사이즈/JPEG 재인코딩은 공용 헬퍼가 담당.
  Future<void> _pickImage(BuildContext context) async {
    final file = await pickImageWithSourceSheet(context, picker: imagePicker);
    if (file != null) {
      onPhotoChanged(file);
    }
  }
}
