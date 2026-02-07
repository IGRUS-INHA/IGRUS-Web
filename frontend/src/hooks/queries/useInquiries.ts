import {
  useQuery,
  useMutation,
  useQueryClient,
  type UseQueryResult,
  type UseMutationResult,
} from '@tanstack/react-query';
import { inquiriesApi } from '@/api/inquiries';
import type { Inquiry, InquiryDetail } from '@/types/entities';
import type { CreateInquiryRequest } from '@/types/api';

// 쿼리 키 상수
export const inquiryKeys = {
  all: ['inquiries'] as const,
  lists: () => [...inquiryKeys.all, 'list'] as const,
  list: (filters: Record<string, unknown>) =>
    [...inquiryKeys.lists(), filters] as const,
  details: () => [...inquiryKeys.all, 'detail'] as const,
  detail: (id: string) => [...inquiryKeys.details(), id] as const,
};

// 내 문의 목록 조회
export function useMyInquiries(): UseQueryResult<Inquiry[]> {
  return useQuery({
    queryKey: inquiryKeys.list({ my: true }),
    queryFn: async (): Promise<Inquiry[]> => {
      const response = await inquiriesApi.getMyList();
      return response.data;
    },
  });
}

// 문의 상세 조회
export function useInquiry(inquiryId: string): UseQueryResult<InquiryDetail> {
  return useQuery({
    queryKey: inquiryKeys.detail(inquiryId),
    queryFn: async (): Promise<InquiryDetail> => {
      const response = await inquiriesApi.getMyDetail(inquiryId);
      return response.data;
    },
    enabled: !!inquiryId,
  });
}

// 문의 작성
export function useCreateInquiry(): UseMutationResult<
  Inquiry,
  Error,
  CreateInquiryRequest
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateInquiryRequest): Promise<Inquiry> => {
      const response = await inquiriesApi.create(data);
      return response.data;
    },
    onSuccess: (): void => {
      void queryClient.invalidateQueries({ queryKey: inquiryKeys.lists() });
    },
  });
}
