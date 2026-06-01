import { Card, Col, Row, Typography } from 'antd';
import { ReadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import DetailHeader from '@/components/DetailHeader';
import { EDUCATION_CATEGORIES } from '@/api/education';

/** 교육 카테고리 선택 (레거시 edu/main.jsp). */
export default function EducationMainPage() {
  const navigate = useNavigate();
  return (
    <>
      <DetailHeader title="교육자료" />
      <Typography.Paragraph type="secondary">카테고리를 선택하세요</Typography.Paragraph>
      <Row gutter={[12, 12]}>
        {EDUCATION_CATEGORIES.map((cat) => (
          <Col span={12} key={cat.code}>
            <Card
              hoverable
              styles={{ body: { padding: 20, textAlign: 'center' } }}
              onClick={() => navigate(`/education/list/${cat.code}`)}
            >
              <ReadOutlined style={{ fontSize: 28, color: '#52c41a' }} />
              <div style={{ marginTop: 10, fontWeight: 600 }}>{cat.name}</div>
            </Card>
          </Col>
        ))}
      </Row>
    </>
  );
}
