import { useUIStore } from '@/stores';
import { cn } from '@/lib/utils';

export default function PrivacyPolicyPage() {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  return (
    <div className="py-s6 max-w-4xl mx-auto">
      <h1
        className={cn(
          'text-4xl font-bold mb-s6',
          isDark ? 'text-white' : 'text-black'
        )}
      >
        개인정보처리방침
      </h1>

      <div className="space-y-s6">
        {/* 섹션 1: 수집하는 개인정보 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              'text-2xl font-semibold',
              isDark ? 'text-white' : 'text-black'
            )}
          >
            1. 수집하는 개인정보
          </h2>
          <p className={cn('text-base leading-relaxed', isDark ? 'text-gray-400' : 'text-gray-600')}>
            IGRUS는 회원가입 및 서비스 제공을 위해 다음과 같은 개인정보를 수집합니다:
          </p>
          <ul className={cn('list-disc list-inside space-y-s2 ml-s4', isDark ? 'text-gray-400' : 'text-gray-600')}>
            <li>필수 항목: 학번, 이름, 이메일, 전화번호, 학과, 학년, 성별</li>
            <li>선택 항목: 동아리 가입 동기</li>
            <li>자동 수집 항목: 서비스 이용 기록, 접속 로그, 쿠키</li>
          </ul>
        </section>

        {/* 섹션 2: 개인정보의 수집 및 이용 목적 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              'text-2xl font-semibold',
              isDark ? 'text-white' : 'text-black'
            )}
          >
            2. 개인정보의 수집 및 이용 목적
          </h2>
          <p className={cn('text-base leading-relaxed', isDark ? 'text-gray-400' : 'text-gray-600')}>
            수집한 개인정보는 다음의 목적을 위해 활용됩니다:
          </p>
          <ul className={cn('list-disc list-inside space-y-s2 ml-s4', isDark ? 'text-gray-400' : 'text-gray-600')}>
            <li>회원 관리: 회원제 서비스 이용에 따른 본인확인, 개인 식별, 불량회원의 부정 이용 방지</li>
            <li>서비스 제공: 게시판, 행사 관리, 문의 사항 처리</li>
            <li>마케팅 및 광고: 동아리 행사 안내, 공지사항 전달</li>
          </ul>
        </section>

        {/* 섹션 3: 개인정보의 보유 및 이용 기간 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              'text-2xl font-semibold',
              isDark ? 'text-white' : 'text-black'
            )}
          >
            3. 개인정보의 보유 및 이용 기간
          </h2>
          <p className={cn('text-base leading-relaxed', isDark ? 'text-gray-400' : 'text-gray-600')}>
            회원의 개인정보는 원칙적으로 개인정보의 수집 및 이용목적이 달성되면 지체 없이 파기합니다.
            단, 관련 법령에 의하여 보존할 필요가 있는 경우 해당 법령에서 정한 기간 동안 보관합니다.
          </p>
          <ul className={cn('list-disc list-inside space-y-s2 ml-s4', isDark ? 'text-gray-400' : 'text-gray-600')}>
            <li>회원 탈퇴 시: 즉시 파기</li>
            <li>서비스 미이용 시: 1년 경과 후 파기</li>
          </ul>
        </section>

        {/* 섹션 4: 개인정보의 제3자 제공 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              'text-2xl font-semibold',
              isDark ? 'text-white' : 'text-black'
            )}
          >
            4. 개인정보의 제3자 제공
          </h2>
          <p className={cn('text-base leading-relaxed', isDark ? 'text-gray-400' : 'text-gray-600')}>
            IGRUS는 원칙적으로 이용자의 개인정보를 외부에 제공하지 않습니다.
            다만, 다음의 경우에는 예외로 합니다:
          </p>
          <ul className={cn('list-disc list-inside space-y-s2 ml-s4', isDark ? 'text-gray-400' : 'text-gray-600')}>
            <li>이용자가 사전에 동의한 경우</li>
            <li>법령의 규정에 의거하거나, 수사 목적으로 법령에 정해진 절차와 방법에 따라 수사기관의 요구가 있는 경우</li>
          </ul>
        </section>

        {/* 섹션 5: 개인정보의 파기 절차 및 방법 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              'text-2xl font-semibold',
              isDark ? 'text-white' : 'text-black'
            )}
          >
            5. 개인정보의 파기 절차 및 방법
          </h2>
          <p className={cn('text-base leading-relaxed', isDark ? 'text-gray-400' : 'text-gray-600')}>
            이용자의 개인정보는 원칙적으로 개인정보의 수집 및 이용목적이 달성되면 지체 없이 파기합니다.
          </p>
          <div className={cn('space-y-s2', isDark ? 'text-gray-400' : 'text-gray-600')}>
            <p className="font-semibold">파기 절차:</p>
            <p className="ml-s4">
              회원가입 등을 위해 입력한 정보는 목적이 달성된 후 별도의 DB로 옮겨져 내부 방침 및 기타 관련 법령에 의한 정보보호 사유에 따라
              일정 기간 저장된 후 파기됩니다.
            </p>
            <p className="font-semibold mt-s4">파기 방법:</p>
            <p className="ml-s4">
              전자적 파일 형태로 저장된 개인정보는 기록을 재생할 수 없는 기술적 방법을 사용하여 삭제합니다.
            </p>
          </div>
        </section>

        {/* 섹션 6: 이용자의 권리와 의무 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              'text-2xl font-semibold',
              isDark ? 'text-white' : 'text-black'
            )}
          >
            6. 이용자의 권리와 의무
          </h2>
          <p className={cn('text-base leading-relaxed', isDark ? 'text-gray-400' : 'text-gray-600')}>
            이용자는 언제든지 등록되어 있는 자신의 개인정보를 조회하거나 수정할 수 있으며, 가입 해지를 요청할 수도 있습니다.
            개인정보의 조회/수정 또는 가입해지를 하고자 할 경우에는 마이페이지를 통해 직접 처리하거나, 개인정보 관리책임자에게 문의하시면
            지체 없이 조치하겠습니다.
          </p>
        </section>

        {/* 섹션 7: 개인정보 보호책임자 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              'text-2xl font-semibold',
              isDark ? 'text-white' : 'text-black'
            )}
          >
            7. 개인정보 보호책임자
          </h2>
          <p className={cn('text-base leading-relaxed', isDark ? 'text-gray-400' : 'text-gray-600')}>
            IGRUS는 개인정보 처리에 관한 업무를 총괄해서 책임지고, 개인정보 처리와 관련한 정보주체의 불만처리 및 피해구제 등을 위하여
            아래와 같이 개인정보 보호책임자를 지정하고 있습니다.
          </p>
          <div className={cn('mt-4 p-4 rounded-lg', isDark ? 'bg-gray-800/50' : 'bg-gray-100')}>
            <p className={cn('font-semibold mb-s2', isDark ? 'text-white' : 'text-black')}>
              개인정보 보호책임자
            </p>
            <p className={cn(isDark ? 'text-gray-400' : 'text-gray-600')}>
              담당자: [담당자명]
            </p>
            <p className={cn(isDark ? 'text-gray-400' : 'text-gray-600')}>
              이메일: contact@igrus.club
            </p>
            <p className={cn(isDark ? 'text-gray-400' : 'text-gray-600')}>
              전화: 032-860-XXXX
            </p>
          </div>
        </section>

        {/* 섹션 8: 고지의 의무 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              'text-2xl font-semibold',
              isDark ? 'text-white' : 'text-black'
            )}
          >
            8. 고지의 의무
          </h2>
          <p className={cn('text-base leading-relaxed', isDark ? 'text-gray-400' : 'text-gray-600')}>
            현 개인정보처리방침 내용 추가, 삭제 및 수정이 있을 시에는 개정 최소 7일 전부터 홈페이지의 공지사항을 통해 고지할 것입니다.
          </p>
          <p className={cn('text-base leading-relaxed mt-s2', isDark ? 'text-gray-400' : 'text-gray-600')}>
            본 방침은 {new Date().getFullYear()}년 {new Date().getMonth() + 1}월 {new Date().getDate()}일부터 시행됩니다.
          </p>
        </section>
      </div>
    </div>
  );
}
