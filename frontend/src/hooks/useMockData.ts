/**
 * Mock 데이터 사용 여부를 확인하는 유틸리티
 */
export const useMockData = () => {
  const isEnabled = import.meta.env.VITE_USE_MOCK_DATA === "true";
  return isEnabled;
};
