import { useState, useCallback } from 'react';
import {
  useCheckStudentIdDuplicate,
  useCheckEmailDuplicate,
  useCheckPhoneNumberDuplicate,
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
  phoneNumber: DuplicateCheckStatus;
  checkStudentId: (studentId: string) => void;
  checkEmail: (email: string) => void;
  checkPhoneNumber: (phoneNumber: string) => void;
  resetStudentId: () => void;
  resetEmail: () => void;
  resetPhoneNumber: () => void;
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
  const [phoneNumberToCheck, setPhoneNumberToCheck] = useState('');

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

  const phoneNumberQuery = useCheckPhoneNumberDuplicate(
    { phoneNumber: phoneNumberToCheck },
    {
      query: {
        enabled: /^\d{3}-\d{4}-\d{4}$/.test(phoneNumberToCheck),
        retry: false,
        staleTime: 0,
        gcTime: 0,
      },
    },
  );

  const studentIdEnabled = /^\d{8}$/.test(studentIdToCheck);
  const emailEnabled = emailToCheck.length > 0 && emailToCheck.includes('@');
  const phoneNumberEnabled = /^\d{3}-\d{4}-\d{4}$/.test(phoneNumberToCheck);

  // Orval이 에러 타입을 void로 생성하므로 unknown으로 캐스팅
  const studentIdError = studentIdQuery.error as unknown;
  const emailError = emailQuery.error as unknown;
  const phoneNumberError = phoneNumberQuery.error as unknown;

  const studentIdStatus: DuplicateCheckStatus = !studentIdEnabled
    ? INITIAL_STATUS
    : {
        isChecking: studentIdQuery.isFetching,
        isAvailable:
          studentIdQuery.isSuccess && studentIdQuery.data?.data?.available === true,
        isDuplicate:
          hasErrorCode(studentIdError, 'DUPLICATE_STUDENT_ID') ||
          hasErrorCode(studentIdError, 'INVALID_STUDENT_ID'),
        message: studentIdQuery.isSuccess
          ? '사용 가능한 학번입니다.'
          : studentIdError
            ? getErrorMessage(studentIdError)
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
          hasErrorCode(emailError, 'DUPLICATE_EMAIL') ||
          hasErrorCode(emailError, 'INVALID_EMAIL_FORMAT'),
        message: emailQuery.isSuccess
          ? '사용 가능한 이메일입니다.'
          : emailError
            ? getErrorMessage(emailError)
            : undefined,
        isChecked: emailQuery.isSuccess || emailQuery.isError,
      };

  const phoneNumberStatus: DuplicateCheckStatus = !phoneNumberEnabled
    ? INITIAL_STATUS
    : {
        isChecking: phoneNumberQuery.isFetching,
        isAvailable:
          phoneNumberQuery.isSuccess && phoneNumberQuery.data?.data?.available === true,
        isDuplicate:
          hasErrorCode(phoneNumberError, 'DUPLICATE_PHONE_NUMBER') ||
          hasErrorCode(phoneNumberError, 'INVALID_PHONE_NUMBER_FORMAT'),
        message: phoneNumberQuery.isSuccess
          ? '사용 가능한 전화번호입니다.'
          : phoneNumberError
            ? getErrorMessage(phoneNumberError)
            : undefined,
        isChecked: phoneNumberQuery.isSuccess || phoneNumberQuery.isError,
      };

  const checkStudentId = useCallback((value: string) => {
    setStudentIdToCheck(value);
  }, []);

  const checkEmail = useCallback((value: string) => {
    setEmailToCheck(value);
  }, []);

  const checkPhoneNumber = useCallback((value: string) => {
    setPhoneNumberToCheck(value);
  }, []);

  const resetStudentId = useCallback(() => {
    setStudentIdToCheck('');
  }, []);

  const resetEmail = useCallback(() => {
    setEmailToCheck('');
  }, []);

  const resetPhoneNumber = useCallback(() => {
    setPhoneNumberToCheck('');
  }, []);

  return {
    studentId: studentIdStatus,
    email: emailStatus,
    phoneNumber: phoneNumberStatus,
    checkStudentId,
    checkEmail,
    checkPhoneNumber,
    resetStudentId,
    resetEmail,
    resetPhoneNumber,
  };
}
