import 'dart:io';

import 'package:flutter/material.dart';

/// 클레임 사진 첨부 필드
class ClaimPhotoField extends StatelessWidget {
  const ClaimPhotoField({
    super.key,
    required this.label,
    required this.photo,
    required this.onPhotoSelected,
    required this.onPhotoRemoved,
    this.isRequired = false,
  });

  final String label;
  final File? photo;
  final ValueChanged<File> onPhotoSelected;
  final VoidCallback onPhotoRemoved;
  final bool isRequired;

  @override
  Widget build(BuildContext context) {
    final hasPhoto = photo != null && photo!.path.isNotEmpty;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 라벨
        Text(
          isRequired ? '$label *' : label,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),

        // 사진 영역
        if (hasPhoto)
          _PhotoPreview(
            photo: photo!,
            onRemove: onPhotoRemoved,
          )
        else
          _PhotoPickerButton(
            onPhotoSelected: onPhotoSelected,
          ),
      ],
    );
  }
}

/// 사진 선택 버튼
class _PhotoPickerButton extends StatelessWidget {
  const _PhotoPickerButton({
    required this.onPhotoSelected,
  });

  final ValueChanged<File> onPhotoSelected;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: () => _showImageSourceSelector(context),
      icon: const Icon(Icons.add_photo_alternate),
      label: const Text('사진 선택'),
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(double.infinity, 48),
      ),
    );
  }

  Future<void> _showImageSourceSelector(BuildContext context) async {
    await showModalBottomSheet<String>(
      context: context,
      builder: (context) => const _ImageSourceSheet(),
    );

    // TODO: Implement actual image picker when image_picker package is added
    // For now, this is just UI structure
  }
}

/// 사진 미리보기
class _PhotoPreview extends StatelessWidget {
  const _PhotoPreview({
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
          width: double.infinity,
          height: 200,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey.shade300),
            borderRadius: BorderRadius.circular(4),
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: Image.file(
              photo,
              fit: BoxFit.cover,
            ),
          ),
        ),

        // 삭제 버튼
        Positioned(
          top: 8,
          right: 8,
          child: IconButton(
            onPressed: onRemove,
            icon: const Icon(Icons.close),
            style: IconButton.styleFrom(
              backgroundColor: Colors.black54,
              foregroundColor: Colors.white,
            ),
          ),
        ),
      ],
    );
  }
}

/// 이미지 소스 선택 바텀시트 (카메라/갤러리)
class _ImageSourceSheet extends StatelessWidget {
  const _ImageSourceSheet();

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Wrap(
        children: [
          ListTile(
            leading: const Icon(Icons.camera_alt),
            title: const Text('카메라로 촬영'),
            onTap: () => Navigator.pop(context, 'camera'),
          ),
          ListTile(
            leading: const Icon(Icons.photo_library),
            title: const Text('갤러리에서 선택'),
            onTap: () => Navigator.pop(context, 'gallery'),
          ),
        ],
      ),
    );
  }
}
