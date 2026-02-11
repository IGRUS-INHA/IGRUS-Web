import { useState, useCallback } from 'react';
import {
  useCheckStudentIdDuplicate,
  useCheckEmailDuplicate,
} from '@/api/model/password-authentication/password-authentication';
import { hasErrorCode, getErrorMessage } from '@/utils/error';

interface DuplicateCheckStatus {
  isChecking: boolean;
  isAvailable: boolean;
  isDuplicate: boolean;
  message: string | undefined;
  isChecked: boolean;
}

interface UseSignupDuplicateCheckReturn {
  studentId: DuplicateCheckStatus;
  email: DuplicateCheckStatus;
  checkStudentId: (studentId: string) => void;
  checkEmail: (email: string) => void;
  resetStudentId: () => void;
  resetEmail: () => void;
}

const INITIAL_STATUS: DuplicateCheckStatus = {
  isChecking: false,
  isAvailable: false,
  isDuplicate: false,
  message: undefined,
  isChecked: false,
};

export function useSignupDuplicateCheck(): UseSignupDuplicateCheckReturn {
  const [studentIdToCheck, setStudentIdToCheck] = useState('');
  const [emailToCheck, setEmailToCheck] = useState('');

  const studentIdQuery = useCheckStudentIdDuplicate(
    { studentId: studentIdToCheck },
    {
      query: {
        enabled: /^\d{8}$/.test(studentIdToCheck),
        retry: false,
        staleTime: 0,
        gcTime: 0,
      },
    },
  );

  const emailQuery = useCheckEmailDuplicate(
    { email: emailToCheck },
    {
      query: {
        enabled: emailToCheck.length > 0 && emailToCheck.includes('@'),
        retry: false,
        staleTime: 0,
        gcTime: 0,
      },
    },
  );

  const studentIdEnabled = /^\d{8}$/.test(studentIdToCheck);
  const emailEnabled = emailToCheck.length > 0 && emailToCheck.includes('@');

  const studentIdStatus: DuplicateCheckStatus = !studentIdEnabled
    ? INITIAL_STATUS
    : {
        isChecking: studentIdQuery.isFetching,
        isAvailable:
          studentIdQuery.isSuccess && studentIdQuery.data?.data?.available === true,
        isDuplicate:
          hasErrorCode(studentIdQuery.error, 'DUPLICATE_STUDENT_ID') ||
          hasErrorCode(studentIdQuery.error, 'INVALID_STUDENT_ID'),
        message: studentIdQuery.isSuccess
          ? '사용 가능한 학번입니다.'
          : studentIdQuery.error
            ? getErrorMessage(studentIdQuery.error)
            : undefined,
        isChecked: studentIdQuery.isSuccess || studentIdQuery.isError,
      };

  const emailStatus: DuplicateCheckStatus = !emailEnabled
    ? INITIAL_STATUS
    : {
        isChecking: emailQuery.isFetching,
        isAvailable:
          emailQuery.isSuccess && emailQuery.data?.data?.available === true,
        isDuplicate:
          hasErrorCode(emailQuery.error, 'DUPLICATE_EMAIL') ||
          hasErrorCode(emailQuery.error, 'INVALID_EMAIL_FORMAT'),
        message: emailQuery.isSuccess
          ? '사용 가능한 이메일입니다.'
          : emailQuery.error
            ? getErrorMessage(emailQuery.error)
            : undefined,
        isChecked: emailQuery.isSuccess || emailQuery.isError,
      };

  const checkStudentId = useCallback((value: string) => {
    setStudentIdToCheck(value);
  }, []);

  const checkEmail = useCallback((value: string) => {
    setEmailToCheck(value);
  }, []);

  const resetStudentId = useCallback(() => {
    setStudentIdToCheck('');
  }, []);

  const resetEmail = useCallback(() => {
    setEmailToCheck('');
  }, []);

  return {
    studentId: studentIdStatus,
    email: emailStatus,
    checkStudentId,
    checkEmail,
    resetStudentId,
    resetEmail,
  };
}
