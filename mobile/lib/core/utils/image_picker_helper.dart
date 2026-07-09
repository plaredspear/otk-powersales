import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';
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
      // 레거시 정합: Heroku ImageUtil.resizeImage(650, 650) 와 동일하게 가로/세로 각 650px 상한.
      // SF ContentVersion 저장 이미지 규격을 레거시와 맞춘다 (클레임/현장점검 공유 헬퍼).
      maxWidth: 650,
      maxHeight: 650,
      imageQuality: 85,
    );
    if (picked == null) return null;
    return await _reencodeToJpeg(File(picked.path));
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

/// 선택된 이미지를 JPEG 로 재인코딩한다.
///
/// image_picker 는 리사이즈는 하지만 iOS 원본 포맷(HEIC)을 유지하는 경우가 있어, 파일명은 .jpg
/// 인데 실바이트는 HEIC 인 불일치가 생긴다. 이 파일이 SF `ContentVersion` 으로 올라가면
/// `FileExtensionGuard`/포맷 검증에서 거부된다(`허용되지 않는 파일 확장자입니다`). 네이티브
/// 인코더로 확실히 JPEG 바이트 + .jpg 확장자로 통일한다. 재인코딩 실패 시 원본을 그대로 반환한다.
Future<File> _reencodeToJpeg(File source) async {
  try {
    final dir = source.parent.path;
    final stem = source.uri.pathSegments.last.split('.').first;
    final target = '$dir/${stem}_jpeg.jpg';
    final result = await FlutterImageCompress.compressAndGetFile(
      source.absolute.path,
      target,
      format: CompressFormat.jpeg,
      quality: 85,
      // image_picker 에서 이미 650px 로 축소됨 — 여기서는 포맷 변환이 목적이라 축소를 반복하지 않도록
      // 넉넉한 상한을 둔다 (레거시 650 상한은 pickImage 단계에서 이미 적용).
      minWidth: 650,
      minHeight: 650,
    );
    return result == null ? source : File(result.path);
  } catch (_) {
    return source;
  }
}
