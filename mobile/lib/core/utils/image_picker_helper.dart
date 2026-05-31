import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

/// 카메라/갤러리 소스 선택 시트를 띄우고 이미지를 선택해 [File] 로 반환한다.
///
/// - 사용자가 소스 선택을 취소하거나 이미지를 고르지 않으면 `null`.
/// - 선택 실패(권한 거부 등) 시 [context] 에 SnackBar 를 노출하고 `null`.
///
/// [picker] 는 테스트 주입용 (미지정 시 기본 [ImagePicker]).
Future<File?> pickImageWithSourceSheet(
  BuildContext context, {
  ImagePicker? picker,
}) async {
  final source = await showModalBottomSheet<ImageSource>(
    context: context,
    builder: (sheetContext) => SafeArea(
      child: Wrap(
        children: [
          ListTile(
            leading: const Icon(Icons.camera_alt),
            title: const Text('카메라로 촬영'),
            onTap: () => Navigator.pop(sheetContext, ImageSource.camera),
          ),
          ListTile(
            leading: const Icon(Icons.photo_library),
            title: const Text('갤러리에서 선택'),
            onTap: () => Navigator.pop(sheetContext, ImageSource.gallery),
          ),
        ],
      ),
    ),
  );
  if (source == null) return null;

  try {
    final picked = await (picker ?? ImagePicker()).pickImage(
      source: source,
      maxWidth: 1920,
      maxHeight: 1920,
      imageQuality: 85,
    );
    return picked == null ? null : File(picked.path);
  } catch (_) {
    // 권한 거부/취소 등 실패
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('사진을 불러오지 못했습니다')),
      );
    }
    return null;
  }
}
