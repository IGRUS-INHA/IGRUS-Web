import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { inquiriesApi } from '@/api/inquiries';

export const inquiryKeys = {
  all: ['inquiries'],
  lists: () => [...inquiryKeys.all, 'list'],
  list: (filters) => [...inquiryKeys.lists(), filters],
  details: () => [...inquiryKeys.all, 'detail'],
  detail: (id) => [...inquiryKeys.details(), id],
};

// 내 문의 목록 조회
export function useMyInquiries() {
  return useQuery({
    queryKey: inquiryKeys.list({ my: true }),
    queryFn: () => inquiriesApi.getMyList(),
  });
}

// 문의 상세 조회
export function useInquiry(inquiryId) {
  return useQuery({
    queryKey: inquiryKeys.detail(inquiryId),
    queryFn: () => inquiriesApi.getDetail(inquiryId),
    enabled: !!inquiryId,
  });
}

// 문의 작성
export function useCreateInquiry() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data) => inquiriesApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: inquiryKeys.lists() });
    },
  });
}
