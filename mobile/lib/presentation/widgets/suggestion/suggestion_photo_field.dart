import 'dart:io';

import 'package:flutter/material.dart';

import 'suggestion_logistics_claim_fields.dart';

/// 제안하기 사진 첨부 필드 (최대 2장, 레거시 suggestWrite.jsp 정합)
///
/// 라벨 우측에 "+ 사진 선택" pill 버튼을 두고, 미첨부 시 "사진 선택" 플레이스홀더,
/// 첨부 시 썸네일 목록을 평면 행으로 노출한다.
class SuggestionPhotoField extends StatelessWidget {
  const SuggestionPhotoField({
    super.key,
    required this.photos,
    required this.onAddPhoto,
    required this.onRemovePhoto,
    this.required = false,
  });

  final List<File> photos;
  final VoidCallback onAddPhoto;
  final ValueChanged<int> onRemovePhoto;

  /// 물류 클레임 등 사진 필수 분류 여부 — 라벨에 `*` 표기
  final bool required;

  @override
  Widget build(BuildContext context) {
    final canAddPhoto = photos.length < 2;

    return SuggestionFieldRow(
      label: '사진 (최대 2장)',
      required: required,
      trailing: SuggestionPillButton(
        icon: Icons.add,
        label: '사진 선택',
        onPressed: canAddPhoto ? onAddPhoto : null,
      ),
      child: photos.isEmpty
          ? const Text(
              '사진 선택',
              style: TextStyle(
                fontSize: 15,
                color: kSuggestionPlaceholderColor,
              ),
            )
          : Wrap(
              spacing: 8,
              runSpacing: 8,
              children: List.generate(photos.length, (index) {
                return _PhotoItem(
                  photo: photos[index],
                  onRemove: () => onRemovePhoto(index),
                );
              }),
            ),
    );
  }
}

/// 사진 아이템
class _PhotoItem extends StatelessWidget {
  const _PhotoItem({
    required this.photo,
    required this.onRemove,
  });

  final File photo;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        // 사진 썸네일
        Container(
          width: 100,
          height: 100,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey.shade300),
            borderRadius: BorderRadius.circular(4),
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
            onTap: onRemove,
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
