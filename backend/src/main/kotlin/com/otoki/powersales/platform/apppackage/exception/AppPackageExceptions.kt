package com.otoki.powersales.platform.apppackage.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

class AppPackageFileRequiredException : BusinessException(
    errorCode = "APP_PACKAGE_FILE_REQUIRED",
    message = "패키지 파일이 필요합니다",
    httpStatus = HttpStatus.BAD_REQUEST,
)

class AppPackageInvalidExtensionException(expected: String) : BusinessException(
    errorCode = "APP_PACKAGE_INVALID_EXTENSION",
    message = "플랫폼과 파일 확장자가 일치하지 않습니다 (필요: $expected)",
    httpStatus = HttpStatus.BAD_REQUEST,
)

class AppPackageDuplicateVersionException(versionCode: Long) : BusinessException(
    errorCode = "APP_PACKAGE_DUPLICATE_VERSION",
    message = "동일 플랫폼에 이미 등록된 버전입니다 (versionCode=$versionCode)",
    httpStatus = HttpStatus.CONFLICT,
)

class AppPackageNotFoundException : BusinessException(
    errorCode = "APP_PACKAGE_NOT_FOUND",
    message = "앱 패키지를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND,
)

class AppPackageCannotDeleteLatestException : BusinessException(
    errorCode = "APP_PACKAGE_CANNOT_DELETE_LATEST",
    message = "최신으로 지정된 버전은 삭제할 수 없습니다. 다른 버전을 최신으로 지정한 뒤 삭제하세요",
    httpStatus = HttpStatus.CONFLICT,
)

class AppPackageBundleIdentifierRequiredException : BusinessException(
    errorCode = "APP_PACKAGE_BUNDLE_IDENTIFIER_REQUIRED",
    message = "유효한 iOS 패키지(.ipa)가 아닙니다. Info.plist 에서 bundle identifier 를 읽을 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST,
)

class AppPackageVersionRequiredException(field: String) : BusinessException(
    errorCode = "APP_PACKAGE_VERSION_REQUIRED",
    message = "$field 값이 필요합니다. 패키지에서 자동 추출되지 않았으므로 직접 입력하세요",
    httpStatus = HttpStatus.BAD_REQUEST,
)

/**
 * 서버 환경(stage) 과 업로드된 패키지의 빌드 환경(개발/운영)이 어긋날 때.
 * 식별자(applicationId / bundleIdentifier)의 `.dev` 접미사 유무로 판별한다.
 * 운영 서버에 개발 패키지를, 개발 서버에 운영 패키지를 올리는 사고를 차단한다.
 */
class AppPackageEnvironmentMismatchException(serverStage: String, packageEnv: String) : BusinessException(
    errorCode = "APP_PACKAGE_ENVIRONMENT_MISMATCH",
    message = "서버 환경($serverStage)과 패키지 환경($packageEnv)이 일치하지 않아 등록할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST,
)
