import 'dart:io';

import 'package:flutter/material.dart';

/// 현장 점검 등록 - 사진 선택 위젯
///
/// 기능:
/// - 최대 2장의 사진 선택/표시
/// - 사진 추가 (카메라/갤러리 선택)
/// - 사진 삭제
class InspectionPhotoPicker extends StatelessWidget {
  /// 선택된 사진 목록
  final List<File> photos;

  /// 사진 추가 콜백
  final VoidCallback onAddPhoto;

  /// 사진 삭제 콜백 (index를 받음)
  final ValueChanged<int> onRemovePhoto;

  const InspectionPhotoPicker({
    super.key,
    required this.photos,
    required this.onAddPhoto,
    required this.onRemovePhoto,
  });

  /// 사진 추가 가능 여부 (최대 2장)
  bool get canAddPhoto => photos.length < 2;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 사진 섹션 헤더
        Padding(
          padding: const EdgeInsets.all(16),
          child: RichText(
            text: const TextSpan(
              children: [
                TextSpan(
                  text: '사진',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                TextSpan(
                  text: ' *',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.red,
                  ),
                ),
                TextSpan(
                  text: ' (최대 2장)',
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey,
                  ),
                ),
              ],
            ),
          ),
        ),

        // 사진 그리드
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              // 선택된 사진들
              ...List.generate(
                photos.length,
                (index) => _buildPhotoItem(
                  photos[index],
                  index,
                ),
              ),

              // 사진 추가 버튼
              if (canAddPhoto) _buildAddPhotoButton(),
            ],
          ),
        ),

        const SizedBox(height: 16),
      ],
    );
  }

  /// 사진 아이템 위젯
  Widget _buildPhotoItem(File photo, int index) {
    return Stack(
      children: [
        // 사진 썸네일
        Container(
          width: 120,
          height: 120,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey),
            borderRadius: BorderRadius.circular(8),
            image: DecorationImage(
              image: FileImage(photo),
              fit: BoxFit.cover,
            ),
          ),
        ),

        // 삭제 버튼
        Positioned(
          top: 4,
          right: 4,
          child: GestureDetector(
            onTap: () => onRemovePhoto(index),
            child: Container(
              padding: const EdgeInsets.all(4),
              decoration: const BoxDecoration(
                color: Colors.black54,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.close,
                color: Colors.white,
                size: 16,
              ),
            ),
          ),
        ),
      ],
    );
  }

  /// 사진 추가 버튼 위젯
  Widget _buildAddPhotoButton() {
    return GestureDetector(
      onTap: onAddPhoto,
      child: Container(
        width: 120,
        height: 120,
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey, width: 2),
          borderRadius: BorderRadius.circular(8),
          color: Colors.grey[100],
        ),
        child: const Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.add_a_photo,
              size: 40,
              color: Colors.grey,
            ),
            SizedBox(height: 8),
            Text(
              '사진 추가',
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
