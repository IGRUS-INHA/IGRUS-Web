import type { PasswordSignupRequestJoinRoute } from '@/api/model/models/passwordSignupRequestJoinRoute';

export const JOIN_ROUTE_TITLE = 'IGRUS에 가입하게 된 경로가 어떻게 되나요?';

export const joinRouteOptions = [
  '에브리타임',
  '포스터 및 현수막',
  'OT',
  '지인 소개',
] as const;

export const joinRouteToEnum: Record<string, PasswordSignupRequestJoinRoute> = {
  '에브리타임': 'EVERYTIME',
  '포스터 및 현수막': 'POSTER',
  'OT': 'OT',
  '지인 소개': 'REFERRAL',
  '기타': 'OTHER',
};
