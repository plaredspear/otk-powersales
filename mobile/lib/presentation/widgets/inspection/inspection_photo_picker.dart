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
        // 사진 라벨 행 — 우측에 "사진 선택" 알약버튼 (레거시 정합)
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: Row(
            children: [
              RichText(
                text: const TextSpan(
                  children: [
                    TextSpan(
                      text: '사진 (최대 2장)',
                      style: TextStyle(fontSize: 16, color: Colors.black87),
                    ),
                    TextSpan(
                      text: ' *',
                      style: TextStyle(fontSize: 16, color: Colors.red),
                    ),
                  ],
                ),
              ),
              const Spacer(),
              if (canAddPhoto)
                OutlinedButton.icon(
                  onPressed: onAddPhoto,
                  icon: const Icon(Icons.add, size: 18),
                  label: const Text('사진 선택'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.black87,
                    side: const BorderSide(color: Color(0xFFBDBDBD)),
                    shape: const StadiumBorder(),
                    minimumSize: Size.zero,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 8,
                    ),
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                ),
            ],
          ),
        ),

        const SizedBox(height: 8),

        // 사진 영역: 비어있으면 플레이스홀더, 있으면 썸네일 그리드
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
          child: photos.isEmpty
              ? const Text(
                  '사진 선택',
                  style: TextStyle(fontSize: 14, color: Colors.grey),
                )
              : Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: List.generate(
                    photos.length,
                    (index) => _buildPhotoItem(photos[index], index),
                  ),
                ),
        ),
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

}
