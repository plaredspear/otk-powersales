import { useState } from 'react';
import { Alert, Button, Card, Col, Descriptions, Image, Row, Skeleton, Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { ProductBarcodeItem } from '@/api/product';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useProductDetail } from '@/hooks/product/useProducts';
import { useProductInventorySearchStore } from '@/stores/productInventorySearchStore';
import InventorySearchModal from '@/components/product/InventorySearchModal';
import ResizableTable from '@/components/common/ResizableTable';

const STATUS_TAG: Record<string, string> = {
  판매중: 'green',
  단종: 'red',
  출고중지: 'red',
};

const BARCODE_COLUMNS: ColumnsType<ProductBarcodeItem> = [
  { title: '바코드', dataIndex: 'barcode', render: (v: string | null) => v ?? '-' },
  { title: '단위', dataIndex: 'unit', width: 120, render: (v: string | null) => v ?? '-' },
  { title: '시퀀스', dataIndex: 'sortOrder', width: 120, render: (v: string | null) => v ?? '-' },
];

/**
 * UC-02 제품 상세 페이지 + UC-04 Quick Action 진입점.
 *
 * entity 에 존재하는 필드만 표시 (사용자 결정 — entity 보강 없음).
 * 마케팅/알러지/이미지 필드는 entity 에 컬럼이 있는 항목만 렌더링.
 */
export default function ProductDetailPage() {
  const { productCode } = useParams<{ productCode: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  // 목록에서 넘어온 경우 직전 목록의 query string(page/필터)을 붙여 복귀 — "목록으로" 시 조건 초기화 방지.
  const listSearch = (location.state as { listSearch?: string } | null)?.listSearch ?? '';
  const listPath = `/product${listSearch}`;
  const [inventoryModalOpen, setInventoryModalOpen] = useState(false);
  const setInventoryTargets = useProductInventorySearchStore((s) => s.setTargets);

  const { data, isLoading, isError, error, refetch } = useProductDetail(productCode);

  const handleOpenInventory = () => {
    if (!data) return;
    setInventoryTargets([
      {
        productCode: data.productCode ?? '',
        name: data.name,
        category1: data.category1,
        category2: data.category2,
        unit: data.unit,
      },
    ]);
    setInventoryModalOpen(true);
  };

  if (isLoading) {
    return (
      <div style={{ padding: 24 }}>
        <Skeleton active />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="제품 상세를 불러오지 못했습니다"
          description={(error as Error)?.message}
          action={
            <Space>
              <Button onClick={() => refetch()}>재시도</Button>
              <Button onClick={() => navigate(listPath)}>목록으로</Button>
            </Space>
          }
        />
      </div>
    );
  }

  const shelfLifeText = data.shelfLife ? `${data.shelfLife}${data.shelfLifeUnit ?? ''}` : '-';
  const tasteGiftText =
    data.tasteGift === '1' ? '전용' : data.tasteGift === '2' ? '범용' : data.tasteGift ?? '-';

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate(listPath)}>← 목록으로</Button>
        <Button type="primary" onClick={handleOpenInventory}>
          재고조회
        </Button>
      </Space>

      <Card title={data.name ?? data.productCode ?? '제품 상세'} style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col span={8}>
            <Space direction="vertical">
              {data.imgRefPathFront ? (
                <Image src={data.imgRefPathFront} width={200} alt="전면 이미지" />
              ) : (
                <div
                  style={{
                    width: 200,
                    height: 200,
                    background: '#f5f5f5',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#999',
                  }}
                >
                  전면 이미지 없음
                </div>
              )}
              {data.imgRefPathBack ? (
                <Image src={data.imgRefPathBack} width={200} alt="후면 이미지" />
              ) : (
                <div
                  style={{
                    width: 200,
                    height: 200,
                    background: '#f5f5f5',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#999',
                  }}
                >
                  후면 이미지 없음
                </div>
              )}
            </Space>
          </Col>
          <Col span={16}>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="제품코드">{data.productCode ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="제품명">{data.name ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="바코드">{data.barcode ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="물류 바코드">{data.logisticsBarcode ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="단위">{data.unit ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="주문 단위">{data.orderingUnit ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="환산치수량">
                {data.conversionQuantity != null ? data.conversionQuantity.toLocaleString() : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="박스 입수량">
                {data.boxReceivingQuantity != null
                  ? Number(data.boxReceivingQuantity).toLocaleString()
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="표준 출고가(원)">
                {data.standardUnitPrice != null
                  ? Number(data.standardUnitPrice).toLocaleString()
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="부가세">
                {data.superTax != null ? Number(data.superTax).toLocaleString() : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="출시일">{data.launchDate ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="보관 방법">{data.storageCondition ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="유통 기한">{shelfLifeText}</Descriptions.Item>
              <Descriptions.Item label="제품 형태">{data.productType ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="제품 상태">
                {data.productStatus ? (
                  <Tag color={STATUS_TAG[data.productStatus] ?? undefined}>{data.productStatus}</Tag>
                ) : (
                  '-'
                )}
              </Descriptions.Item>
              <Descriptions.Item label="증정/시식 구분">{tasteGiftText}</Descriptions.Item>
              <Descriptions.Item label="팰릿">
                {data.pallet != null ? Number(data.pallet).toLocaleString() : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="제조사">{data.manufacture ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="제조사 상세">{data.manufactureDetail ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="클레임 관리">{data.claimManagement ?? '-'}</Descriptions.Item>
            </Descriptions>
          </Col>
        </Row>
      </Card>

      <Card title="분류 정보" style={{ marginBottom: 16 }}>
        <Descriptions column={3} bordered size="small">
          <Descriptions.Item label="대분류">{data.category1 ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="중분류">{data.category2 ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="소분류">{data.category3 ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="대분류 코드">{data.categoryCode1 ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="중분류 코드">{data.categoryCode2 ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="소분류 코드">{data.categoryCode3 ?? '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title={`제품 바코드 (${data.barcodes.length})`} style={{ marginBottom: 16 }}>
        <ResizableTable
          rowKey="id"
          columns={BARCODE_COLUMNS}
          dataSource={data.barcodes}
          pagination={false}
          size="small"
          locale={{ emptyText: '등록된 바코드가 없습니다' }}
        />
      </Card>

      <Card title="마케팅 정보" style={{ marginBottom: 16 }}>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="셀링 포인트">{data.sellingPoint ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="용도">{data.purpose ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="제품 특징">{data.productFeatures ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="타겟 거래처 유형">{data.targetAccountType ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="알러지 유발 물질">{data.allergen ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="교차 오염">{data.crossContamination ?? '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="감사 정보" size="small">
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="생성일시">
            {data.createdAt.replace('T', ' ').slice(0, 19)}
          </Descriptions.Item>
          <Descriptions.Item label="최종 수정일시">
            {data.lastModifiedAt.replace('T', ' ').slice(0, 19)}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <InventorySearchModal
        open={inventoryModalOpen}
        onClose={() => setInventoryModalOpen(false)}
      />
    </div>
  );
}
