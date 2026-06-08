import 'dart:io';

import 'package:flutter/material.dart';

import '../../../core/utils/image_picker_helper.dart';
import 'claim_form_row.dart';

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

    return ClaimFormRow(
      label: label,
      isRequired: isRequired,
      alignTrailingTop: true,
      trailing: _PhotoPickerButton(onPhotoSelected: onPhotoSelected),
      below: hasPhoto
          ? _PhotoPreview(
              photo: photo!,
              onRemove: onPhotoRemoved,
            )
          : const ClaimValueText(value: null, placeholder: '사진 선택'),
    );
  }
}

/// 사진 선택 버튼 (알약형)
class _PhotoPickerButton extends StatelessWidget {
  const _PhotoPickerButton({
    required this.onPhotoSelected,
  });

  final ValueChanged<File> onPhotoSelected;

  @override
  Widget build(BuildContext context) {
    return ClaimPillButton(
      icon: Icons.add,
      label: '사진 선택',
      onPressed: () => _showImageSourceSelector(context),
    );
  }

  Future<void> _showImageSourceSelector(BuildContext context) async {
    final file = await pickImageWithSourceSheet(context);
    if (file != null) {
      onPhotoSelected(file);
    }
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
          height: 160,
          decoration: BoxDecoration(
            border: Border.all(color: ClaimFormColors.divider),
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
