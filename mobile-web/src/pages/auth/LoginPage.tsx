import { useEffect, useState } from 'react';
import { Button, Checkbox, ConfigProvider, Form, Input, App as AntdApp } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { LEGACY } from '@/theme/mobileTheme';
import logo from '@/assets/logo-powersales.png';

/**
 * 로그인 — Heroku 레거시(login.jsp) 화면 정합.
 * 파워세일즈 로고 + 사번/비밀번호 입력 + 아이디 기억하기/자동로그인 + 남색(btn_blue) 버튼 + 카피라이트.
 *
 * - "아이디 기억하기": 레거시 쿠키(key) 대응 — 사번을 localStorage 에 보관해 재방문 시 자동 채움.
 * - "자동로그인": 레거시 isAutoLogin 대응 — 현재 백엔드 자동로그인 미연동이라 선호값만 보관(디바이스 쉘 단계에서 연결).
 * 단말 UUID/토큰 흐름은 디바이스 쉘 단계(Wave 3)에서 보강.
 */
const REMEMBER_ID_KEY = 'mw_rememberedId';
const AUTO_LOGIN_KEY = 'mw_autoLogin';

export default function LoginPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const login = useAuthStore((s) => s.login);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  // 저장된 사번/자동로그인 선호값 복원
  useEffect(() => {
    const savedId = localStorage.getItem(REMEMBER_ID_KEY);
    form.setFieldsValue({
      employeeCode: savedId ?? '',
      rememberId: !!savedId,
      autoLogin: localStorage.getItem(AUTO_LOGIN_KEY) === 'Y',
    });
  }, [form]);

  const onFinish = async (values: {
    employeeCode: string;
    password: string;
    rememberId?: boolean;
    autoLogin?: boolean;
  }) => {
    const employeeCode = values.employeeCode.trim();
    setLoading(true);
    try {
      await login(employeeCode, values.password);

      if (values.rememberId) localStorage.setItem(REMEMBER_ID_KEY, employeeCode);
      else localStorage.removeItem(REMEMBER_ID_KEY);
      localStorage.setItem(AUTO_LOGIN_KEY, values.autoLogin ? 'Y' : 'N');

      // 강제 비밀번호 변경 필요 시 변경 화면으로(레거시 resetPwd 흐름). AuthGuard 도 동일 가드.
      // GPS 동의(requiresGpsConsent) 가드는 Wave 3(디바이스) 범위.
      const dest = useAuthStore.getState().passwordChangeRequired ? '/password/change' : '/';
      navigate(dest, { replace: true });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '로그인에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        background: '#fff',
        display: 'flex',
        flexDirection: 'column',
        padding: '0 16px',
        paddingTop: 'calc(var(--mw-safe-top) + 24px)',
        paddingBottom: 'calc(var(--mw-safe-bottom) + 16px)',
      }}
    >
      {/* 로고 */}
      <div style={{ textAlign: 'center', marginTop: '22vh', marginBottom: 40 }}>
        <img src={logo} alt="오뚜기 파워세일즈" style={{ height: 32, maxWidth: '80%', objectFit: 'contain' }} />
      </div>

      {/* 입력 폼 */}
      <Form form={form} onFinish={onFinish} requiredMark={false} size="large">
        <Form.Item name="employeeCode" rules={[{ required: true, message: '아이디를 입력하세요' }]}>
          <Input placeholder="아이디 입력 (사번)" inputMode="numeric" style={{ height: 52, borderRadius: 8 }} />
        </Form.Item>
        <Form.Item name="password" rules={[{ required: true, message: '비밀번호를 입력하세요' }]} style={{ marginTop: 8 }}>
          <Input.Password
            placeholder="비밀번호 입력"
            visibilityToggle={false}
            style={{ height: 52, borderRadius: 8 }}
          />
        </Form.Item>

        {/* 아이디 기억하기 / 자동로그인 — 레거시 ul.input_box.default (각 50% 폭) */}
        <div style={{ display: 'flex', margin: '4px 2px 16px' }}>
          <div style={{ width: '50%' }}>
            <Form.Item name="rememberId" valuePropName="checked" noStyle>
              <Checkbox>아이디 기억하기</Checkbox>
            </Form.Item>
          </div>
          <div style={{ width: '50%' }}>
            <Form.Item name="autoLogin" valuePropName="checked" noStyle>
              <Checkbox>자동로그인</Checkbox>
            </Form.Item>
          </div>
        </div>

        {/* 로그인 버튼 — 레거시 btn_blue (남색) */}
        <ConfigProvider theme={{ token: { colorPrimary: LEGACY.navy } }}>
          <Button
            type="primary"
            htmlType="submit"
            block
            loading={loading}
            style={{ height: 52, borderRadius: 8, fontWeight: 800 }}
          >
            로그인
          </Button>
        </ConfigProvider>
      </Form>

      {/* 카피라이트 */}
      <p
        style={{
          marginTop: 'auto',
          paddingTop: 32,
          textAlign: 'center',
          color: '#8c8c8c',
          fontSize: 13,
          fontWeight: 400,
        }}
      >
        Copyright © otoki co.Ltd All Rights Reserved.
      </p>
    </div>
  );
}
