import type { ThemeConfig } from 'antd';

/**
 * 모바일앱용 AntD 테마.
 *
 * 결정(트리아지): antd-mobile 전면전환 대신 AntD v5 유지 + 모바일 토큰 조정.
 * - controlHeight 상향(터치 타깃 ≥ 44px 지향)
 * - 폰트/라운드 모바일 가독성
 */
export const mobileTheme: ThemeConfig = {
  token: {
    colorPrimary: '#1677ff',
    fontSize: 15,
    controlHeight: 44,
    borderRadius: 10,
    sizeStep: 4,
  },
  components: {
    Card: { paddingLG: 16 },
    List: { itemPadding: '12px 0' },
    Button: { controlHeight: 46, fontSize: 16 },
    Input: { controlHeight: 46 },
  },
};
