import { useGetBoardList, useGetBoardByCode } from '@/api/model/board/board';
import { useAuth } from './useAuth';
import { useParams } from 'react-router-dom';
import { canViewBoard, canWriteBoard } from '@/constants/permissions';
import type { BoardListResponse, BoardDetailResponse } from '@/api/model/models';
import type { BoardType } from '@/types/common';

// 클라이언트 타입 (필수 필드로 변환)
export interface Board {
  code: string;
  name: string;
  description: string;
  canRead: boolean;
  canWrite: boolean;
}

export interface BoardDetail extends Board {
  allowsAnonymous: boolean;
  allowsQuestionTag: boolean;
}

// 폴백 데이터
const FALLBACK_BOARDS: Board[] = [
  { code: 'notices', name: '공지사항', description: '', canRead: true, canWrite: false },
  { code: 'general', name: '자유게시판', description: '', canRead: false, canWrite: false },
  { code: 'insight', name: '정보공유', description: '', canRead: false, canWrite: false },
];

// 타입 변환 (null → undefined, optional → required)
function transformBoard(response: BoardListResponse): Board {
  return {
    code: response.code ?? '',
    name: response.name ?? '게시판',
    description: response.description ?? '',
    canRead: response.canRead ?? false,
    canWrite: response.canWrite ?? false,
  };
}

function transformBoardDetail(response: BoardDetailResponse): BoardDetail {
  return {
    code: response.code ?? '',
    name: response.name ?? '게시판',
    description: response.description ?? '',
    canRead: response.canRead ?? false,
    canWrite: response.canWrite ?? false,
    allowsAnonymous: response.allowsAnonymous ?? false,
    allowsQuestionTag: response.allowsQuestionTag ?? false,
  };
}

// 1. 전체 게시판 목록
export function useBoardList() {
  const { data, error, isLoading } = useGetBoardList();
  const { user } = useAuth();

  if (error || !data) {
    // 폴백: 클라이언트 권한 계산
    return {
      boards: FALLBACK_BOARDS.map(board => ({
        ...board,
        canRead: canViewBoard(user?.role, board.code as BoardType),
        canWrite: canWriteBoard(user?.role, board.code as BoardType),
      })),
      isLoading: false,
      error,
    };
  }

  return {
    boards: data.data?.map(transformBoard) ?? [],
    isLoading,
    error: undefined,
  };
}

// 2. 특정 게시판 조회
export function useBoardByCode(code: string) {
  const { data, error, isLoading } = useGetBoardByCode(code, {
    query: { enabled: !!code },
  });
  const { user } = useAuth();

  if (error || !data) {
    // 폴백
    return {
      board: {
        code,
        name: code === 'notices' ? '공지사항' : code === 'general' ? '자유게시판' : '정보공유',
        description: '',
        canRead: canViewBoard(user?.role, code as BoardType),
        canWrite: canWriteBoard(user?.role, code as BoardType),
        allowsAnonymous: code === 'general',
        allowsQuestionTag: code === 'general',
      } as BoardDetail,
      isLoading: false,
      error,
    };
  }

  return {
    board: transformBoardDetail(data.data),
    isLoading: false,
    error: undefined,
  };
}

// 3. 현재 게시판 (URL 기반)
export function useCurrentBoard() {
  const { boardType } = useParams<{ boardType: string }>();
  return useBoardByCode(boardType ?? '');
}
