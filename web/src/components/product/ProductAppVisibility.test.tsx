import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import type { ProductAppVisibility as Visibility } from '@/api/product';
import { ProductAppVisibilityPanel, ProductAppVisibilityTag } from './ProductAppVisibility';

/**
 * 앱 노출 진단 표기 — 운영자가 "검색이 안 되는 것" 과 "주문만 막힌 것" 을 구분할 수 있어야 한다.
 *
 * 회귀 기준점: 제품 18010406 은 상태가 판매중이라 화면상 정상으로 보였고, 실제 원인(바코드 마스터 0건)이
 * 어디에도 드러나지 않아 개발자 문의로 넘어갔다. 사유와 조치처가 화면에 함께 나오는지 확인한다.
 */

const NORMAL: Visibility = { searchable: true, orderable: true, issues: [] };

const BARCODE_MISSING: Visibility = {
  searchable: false,
  orderable: true,
  issues: [
    {
      code: 'BARCODE_NOT_REGISTERED',
      scope: 'SEARCH',
      message: '제품 바코드가 등록되어 있지 않습니다 (바코드 마스터 0건).',
      action: 'SAP 바코드 마스터(ProductBarcode) 전송 여부를 확인하세요.',
    },
  ],
};

const ORDER_BLOCKED: Visibility = {
  searchable: true,
  orderable: false,
  issues: [
    {
      code: 'TASTING_GIFT',
      scope: 'ORDER',
      message: '시식·증정용 상품(TasteGift=x)이라 주문서에 담을 수 없습니다.',
      action: '정상 판매 제품이면 SAP 제품마스터의 증정/시식 구분을 확인하세요.',
    },
  ],
};

describe('ProductAppVisibilityTag', () => {
  it('정상이면 "정상" 배지', () => {
    render(<ProductAppVisibilityTag visibility={NORMAL} />);
    expect(screen.getByText('정상')).toBeInTheDocument();
  });

  it('검색에서 제외되면 "검색불가" 배지 — 주문 가능 여부보다 우선 표기', () => {
    render(<ProductAppVisibilityTag visibility={BARCODE_MISSING} />);
    expect(screen.getByText('검색불가')).toBeInTheDocument();
  });

  it('검색은 되고 주문만 막히면 "주문불가" 배지', () => {
    render(<ProductAppVisibilityTag visibility={ORDER_BLOCKED} />);
    expect(screen.getByText('주문불가')).toBeInTheDocument();
  });
});

describe('ProductAppVisibilityPanel', () => {
  it('정상이면 성공 안내만 노출하고 사유 목록은 없다', () => {
    render(<ProductAppVisibilityPanel visibility={NORMAL} />);
    expect(
      screen.getByText('모바일 앱에서 정상 노출되는 제품입니다 (검색 가능 / 주문 가능).'),
    ).toBeInTheDocument();
  });

  it('미노출이면 사유와 조치처를 함께 보여준다', () => {
    render(<ProductAppVisibilityPanel visibility={BARCODE_MISSING} />);

    expect(screen.getByText('제품검색 미노출')).toBeInTheDocument();
    expect(screen.getByText('주문담기 가능')).toBeInTheDocument();
    expect(
      screen.getByText('제품 바코드가 등록되어 있지 않습니다 (바코드 마스터 0건).'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('→ SAP 바코드 마스터(ProductBarcode) 전송 여부를 확인하세요.'),
    ).toBeInTheDocument();
  });

  it('주문만 차단된 경우 사유 범위를 "주문 차단" 으로 표기', () => {
    render(<ProductAppVisibilityPanel visibility={ORDER_BLOCKED} />);
    expect(screen.getByText('주문 차단')).toBeInTheDocument();
    expect(screen.getByText('제품검색 노출')).toBeInTheDocument();
  });
});
