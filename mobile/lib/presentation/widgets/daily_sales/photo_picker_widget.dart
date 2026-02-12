import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

/// 일매출 등록 사진 선택 위젯
///
/// 카메라 촬영 또는 갤러리에서 사진을 선택하고 미리보기를 제공합니다.
/// 최대 1장의 사진만 선택 가능합니다.
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
            fontWeight: FontWeight.w600,
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
              onPressed: () => _showImageSourceDialog(context),
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
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // 카메라 버튼
              ElevatedButton.icon(
                onPressed: () => _pickImage(ImageSource.camera),
                icon: const Icon(Icons.camera_alt),
                label: const Text('촬영'),
              ),
              const SizedBox(width: 12),
              // 갤러리 버튼
              ElevatedButton.icon(
                onPressed: () => _pickImage(ImageSource.gallery),
                icon: const Icon(Icons.photo_library),
                label: const Text('갤러리'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// 이미지 소스 선택 다이얼로그 표시
  void _showImageSourceDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('사진 선택'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.camera_alt),
              title: const Text('카메라로 촬영'),
              onTap: () {
                Navigator.pop(context);
                _pickImage(ImageSource.camera);
              },
            ),
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('갤러리에서 선택'),
              onTap: () {
                Navigator.pop(context);
                _pickImage(ImageSource.gallery);
              },
            ),
          ],
        ),
      ),
    );
  }

  /// 이미지 선택 처리
  Future<void> _pickImage(ImageSource source) async {
    try {
      final picker = imagePicker ?? ImagePicker();
      final XFile? pickedFile = await picker.pickImage(
        source: source,
        maxWidth: 1920,
        maxHeight: 1920,
        imageQuality: 85,
      );

      if (pickedFile != null) {
        onPhotoChanged(File(pickedFile.path));
      }
    } catch (e) {
      // 에러 처리 (권한 거부, 취소 등)
      // TODO: 에러 메시지 표시 (SnackBar 등)
      debugPrint('Image picker error: $e');
    }
  }
}
