import { useState } from 'react';
import { Tag } from 'antd';
import { PictureOutlined } from '@ant-design/icons';
import type { ClaimListItem } from '@/api/claims';
import { STATUS_TAG, CATEGORY_COLOR, CATEGORY_FALLBACK_COLOR } from '../claimDisplay';

interface ClaimCardProps {
  claim: ClaimListItem;
  onClick: (claimId: number) => void;
}

/**
 * 클레임 목록 카드 — 대표 이미지를 배경으로, 하단에 어두운 그라데이션 오버레이 + 텍스트.
 * 이미지가 없거나 로드 실패 시 대분류 색상 플레이스홀더 + 아이콘으로 폴백.
 */
export default function ClaimCard({ claim, onClick }: ClaimCardProps) {
  const [imageFailed, setImageFailed] = useState(false);

  const hasImage = !!claim.representativeImageUrl && !imageFailed;
  const placeholderColor =
    CATEGORY_COLOR[claim.categoryValue ?? ''] ?? CATEGORY_FALLBACK_COLOR;
  const status = STATUS_TAG[claim.status];

  const categoryText = [claim.categoryLabel, claim.subcategoryLabel]
    .filter(Boolean)
    .join(' | ');

  return (
    <div
      onClick={() => onClick(claim.claimId)}
      style={{
        position: 'relative',
        aspectRatio: '4 / 3',
        borderRadius: 8,
        overflow: 'hidden',
        cursor: 'pointer',
        backgroundColor: placeholderColor,
        boxShadow: '0 1px 4px rgba(0,0,0,0.15)',
      }}
    >
      {hasImage ? (
        <img
          src={claim.representativeImageUrl!}
          alt={claim.productName ?? '클레임 이미지'}
          loading="lazy"
          onError={() => setImageFailed(true)}
          style={{
            position: 'absolute',
            inset: 0,
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
      ) : (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <PictureOutlined style={{ fontSize: 48, color: 'rgba(255,255,255,0.6)' }} />
        </div>
      )}

      {/* 어두운 그라데이션 오버레이 — 하단 텍스트 가독성 */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'linear-gradient(transparent 40%, rgba(0,0,0,0.65))',
        }}
      />

      {/* 우상단 상태 Tag */}
      <div style={{ position: 'absolute', top: 8, right: 8 }}>
        {status ? (
          <Tag color={status.color} style={{ marginInlineEnd: 0 }}>
            {status.label}
          </Tag>
        ) : (
          <Tag style={{ marginInlineEnd: 0 }}>{claim.status}</Tag>
        )}
      </div>

      {/* 하단 텍스트 레이어 */}
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          bottom: 0,
          padding: 12,
          color: '#fff',
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
        }}
      >
        <div
          style={{
            fontWeight: 600,
            fontSize: 15,
            overflow: 'hidden',
            whiteSpace: 'nowrap',
            textOverflow: 'ellipsis',
          }}
        >
          {claim.storeName ?? '-'}
        </div>
        <div
          style={{
            fontSize: 13,
            overflow: 'hidden',
            whiteSpace: 'nowrap',
            textOverflow: 'ellipsis',
          }}
        >
          {claim.productName ?? '-'}
        </div>
        {categoryText && (
          <div
            style={{
              fontSize: 12,
              opacity: 0.85,
              overflow: 'hidden',
              whiteSpace: 'nowrap',
              textOverflow: 'ellipsis',
            }}
          >
            {categoryText}
          </div>
        )}
        <div style={{ fontSize: 12, opacity: 0.85 }}>
          {claim.defectQuantity != null && `수량 ${claim.defectQuantity} · `}
          {claim.createdAt?.substring(0, 10)}
        </div>
      </div>
    </div>
  );
}
