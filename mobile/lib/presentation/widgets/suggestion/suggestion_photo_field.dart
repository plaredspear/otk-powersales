import 'dart:io';

import 'package:flutter/material.dart';

/// 제안하기 사진 첨부 필드 (최대 2장)
class SuggestionPhotoField extends StatelessWidget {
  const SuggestionPhotoField({
    super.key,
    required this.photos,
    required this.onAddPhoto,
    required this.onRemovePhoto,
  });

  final List<File> photos;
  final VoidCallback onAddPhoto;
  final ValueChanged<int> onRemovePhoto;

  @override
  Widget build(BuildContext context) {
    final canAddPhoto = photos.length < 2;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '사진 (최대 2장)',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),

        // 사진 목록 + 추가 버튼
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            // 기존 사진들
            ...List.generate(photos.length, (index) {
              return _PhotoItem(
                photo: photos[index],
                onRemove: () => onRemovePhoto(index),
              );
            }),

            // 사진 추가 버튼
            if (canAddPhoto)
              _AddPhotoButton(
                onPressed: onAddPhoto,
              ),
          ],
        ),

        // 안내 문구
        if (photos.isEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              '사진을 첨부하면 제안 내용을 더 명확하게 전달할 수 있습니다',
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey.shade600,
              ),
            ),
          ),
      ],
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

/// 사진 추가 버튼
class _AddPhotoButton extends StatelessWidget {
  const _AddPhotoButton({
    required this.onPressed,
  });

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onPressed,
      borderRadius: BorderRadius.circular(4),
      child: Container(
        width: 100,
        height: 100,
        decoration: BoxDecoration(
          border: Border.all(
            color: Colors.grey.shade300,
            style: BorderStyle.solid,
          ),
          borderRadius: BorderRadius.circular(4),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.add_photo_alternate_outlined,
              size: 32,
              color: Colors.grey.shade600,
            ),
            const SizedBox(height: 4),
            Text(
              '사진 선택',
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey.shade600,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
