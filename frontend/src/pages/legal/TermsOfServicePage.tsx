import { useUIStore } from "@/stores";
import { cn } from "@/lib/utils";

export default function TermsOfServicePage() {
  const { theme } = useUIStore();
  const isDark = theme === "dark";

  return (
    <div className="py-s6 max-w-4xl mx-auto">
      <h1
        className={cn(
          "text-4xl font-bold mb-s6",
          isDark ? "text-white" : "text-black",
        )}
      >
        이용약관
      </h1>

      <div className="space-y-s6">
        {/* 섹션 1: 목적 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 1조 (목적)
          </h2>
          <p
            className={cn(
              "text-base leading-relaxed",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            본 약관은 IGRUS(이하 "동아리")가 제공하는 웹사이트 서비스(이하
            "서비스")의 이용과 관련하여 동아리와 회원 간의 권리, 의무 및
            책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.
          </p>
        </section>

        {/* 섹션 2: 정의 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 2조 (정의)
          </h2>
          <p
            className={cn(
              "text-base leading-relaxed",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            본 약관에서 사용하는 용어의 정의는 다음과 같습니다:
          </p>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>
              "서비스"란 동아리가 제공하는 웹사이트 및 관련 서비스를 의미합니다.
            </li>
            <li>
              "회원"이란 본 약관에 동의하고 동아리와 서비스 이용계약을 체결한
              자를 말합니다.
            </li>
            <li>
              "게시물"이란 회원이 서비스를 이용함에 있어 서비스에 게시한 문자,
              문서, 그림, 음성, 링크 등을 의미합니다.
            </li>
            <li>
              "운영진"이란 서비스의 전반적인 관리와 원활한 운영을 위하여
              동아리에서 선정한 자를 말합니다.
            </li>
          </ul>
        </section>

        {/* 섹션 3: 약관의 효력 및 변경 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 3조 (약관의 효력 및 변경)
          </h2>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>
              본 약관은 서비스를 이용하고자 하는 모든 회원에 대하여 그 효력을
              발생합니다.
            </li>
            <li>
              동아리는 합리적인 사유가 발생할 경우 관련 법령에 위배되지 않는
              범위 안에서 본 약관을 변경할 수 있습니다.
            </li>
            <li>
              약관이 변경되는 경우 동아리는 변경사항을 시행일자 7일 전부터
              공지사항을 통해 공지합니다.
            </li>
            <li>
              회원이 변경된 약관에 동의하지 않을 경우, 서비스 이용을 중단하고
              탈퇴할 수 있습니다.
            </li>
          </ul>
        </section>

        {/* 섹션 4: 이용계약의 성립 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 4조 (이용계약의 성립)
          </h2>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>
              이용계약은 회원이 되고자 하는 자(이하 "가입신청자")가 본 약관의
              내용에 대하여 동의를 한 다음 회원가입 신청을 하고, 동아리가 이러한
              신청에 대하여 승낙함으로써 체결됩니다.
            </li>
            <li>
              동아리는 다음 각 호에 해당하는 신청에 대해서는 승낙을 하지 않거나
              사후에 이용계약을 해지할 수 있습니다:
              <ul className="list-disc list-inside ml-6 mt-2 space-y-1">
                <li>타인의 명의를 이용하여 신청한 경우</li>
                <li>
                  허위의 정보를 기재하거나, 동아리가 제시하는 내용을 기재하지
                  않은 경우
                </li>
                <li>
                  관계 법령에 위배되거나 사회의 안녕과 질서, 미풍양속을 저해할
                  목적으로 신청한 경우
                </li>
              </ul>
            </li>
          </ul>
        </section>

        {/* 섹션 5: 회원정보의 변경 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 5조 (회원정보의 변경)
          </h2>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>
              회원은 마이페이지를 통하여 언제든지 본인의 개인정보를 열람하고
              수정할 수 있습니다.
            </li>
            <li>
              회원은 회원가입 신청 시 기재한 사항이 변경되었을 경우 온라인으로
              수정을 하거나 이메일 등의 방법으로 동아리에 대하여 그 변경사항을
              알려야 합니다.
            </li>
            <li>
              회원이 변경사항을 동아리에 알리지 않아 발생한 불이익에 대하여
              동아리는 책임지지 않습니다.
            </li>
          </ul>
        </section>

        {/* 섹션 6: 개인정보의 보호 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 6조 (개인정보의 보호)
          </h2>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>
              동아리는 회원의 개인정보를 보호하기 위하여 개인정보처리방침을
              수립하고 시행합니다.
            </li>
            <li>
              동아리의 개인정보처리방침은 관련 법령 및 동아리가 정하는 소정의
              절차에 의해 지속적으로 개선됩니다.
            </li>
            <li>
              동아리는 회원의 개인정보를 본인의 승낙 없이 타인에게 누설,
              배포하지 않습니다. 단, 관계법령에 의한 수사상의 목적으로
              관계기관으로부터 요구받은 경우는 예외로 합니다.
            </li>
          </ul>
        </section>

        {/* 섹션 7: 회원의 의무 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 7조 (회원의 의무)
          </h2>
          <p
            className={cn(
              "text-base leading-relaxed",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            회원은 다음 각 호의 행위를 하여서는 안 됩니다:
          </p>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>신청 또는 변경 시 허위내용의 등록</li>
            <li>타인의 정보도용</li>
            <li>동아리가 게시한 정보의 변경</li>
            <li>
              동아리가 정한 정보 이외의 정보(컴퓨터 프로그램 등) 등의 송신 또는
              게시
            </li>
            <li>동아리와 기타 제3자의 저작권 등 지적재산권에 대한 침해</li>
            <li>
              동아리 및 기타 제3자의 명예를 손상시키거나 업무를 방해하는 행위
            </li>
            <li>
              외설 또는 폭력적인 메시지, 화상, 음성, 기타 공서양속에 반하는
              정보를 서비스에 공개 또는 게시하는 행위
            </li>
            <li>기타 관계 법령에 위배되는 행위</li>
          </ul>
        </section>

        {/* 섹션 8: 게시물의 저작권 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 8조 (게시물의 저작권)
          </h2>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>
              회원이 서비스 내에 게시한 게시물의 저작권은 해당 게시물의
              저작자에게 귀속됩니다.
            </li>
            <li>
              회원이 서비스 내에 게시하는 게시물은 검색결과 내지 서비스 및 관련
              프로모션 등에 노출될 수 있으며, 해당 노출을 위해 필요한 범위
              내에서는 일부 수정, 복제, 편집되어 게시될 수 있습니다.
            </li>
            <li>
              동아리는 회원의 게시물을 소중히 보호하며, 회원의 동의 없이
              상업적으로 이용하지 않습니다.
            </li>
          </ul>
        </section>

        {/* 섹션 9: 게시물의 관리 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 9조 (게시물의 관리)
          </h2>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>
              회원의 게시물이 정보통신망법 및 저작권법 등 관련법에 위반되는
              내용을 포함하는 경우, 권리자는 관련법이 정한 절차에 따라 해당
              게시물의 게시중단 및 삭제 등을 요청할 수 있으며, 동아리는 관련법에
              따라 조치를 취하여야 합니다.
            </li>
            <li>
              동아리는 전항에 따른 권리자의 요청이 없는 경우라도 권리침해가
              인정될 만한 사유가 있거나 기타 동아리 정책 및 관련법에 위반되는
              경우에는 관련법에 따라 해당 게시물에 대해 임시조치 등을 취할 수
              있습니다.
            </li>
          </ul>
        </section>

        {/* 섹션 10: 서비스 이용의 제한 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 10조 (서비스 이용의 제한)
          </h2>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>
              동아리는 회원이 본 약관의 의무를 위반하거나 서비스의 정상적인
              운영을 방해한 경우, 경고, 일시정지, 영구이용정지 등으로 서비스
              이용을 단계적으로 제한할 수 있습니다.
            </li>
            <li>
              동아리는 전항에도 불구하고, 저작권법을 위반한 불법프로그램의 제공
              및 운영방해, 정보통신망법을 위반한 불법통신 및 해킹,
              악성프로그램의 배포, 접속권한 초과행위 등과 같이 관련법을 위반한
              경우에는 즉시 영구이용정지를 할 수 있습니다.
            </li>
          </ul>
        </section>

        {/* 섹션 11: 이용계약의 해지 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 11조 (이용계약의 해지)
          </h2>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>
              회원은 언제든지 마이페이지의 회원탈퇴 기능을 통해 이용계약 해지
              신청을 할 수 있으며, 동아리는 관련법 등이 정하는 바에 따라 이를
              즉시 처리하여야 합니다.
            </li>
            <li>
              회원이 계약을 해지할 경우, 관련법 및 개인정보처리방침에 따라
              동아리가 회원정보를 보유하는 경우를 제외하고는 해지 즉시 회원의
              모든 데이터는 소멸됩니다.
            </li>
          </ul>
        </section>

        {/* 섹션 12: 면책조항 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            제 12조 (면책조항)
          </h2>
          <ul
            className={cn(
              "list-decimal list-inside space-y-s2 ml-s4",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            <li>
              동아리는 천재지변 또는 이에 준하는 불가항력으로 인하여 서비스를
              제공할 수 없는 경우에는 서비스 제공에 관한 책임이 면제됩니다.
            </li>
            <li>
              동아리는 회원의 귀책사유로 인한 서비스 이용의 장애에 대하여는
              책임을 지지 않습니다.
            </li>
            <li>
              동아리는 회원이 서비스를 이용하여 기대하는 수익을 상실한 것에
              대하여 책임을 지지 않으며, 그 밖의 서비스를 통하여 얻은 자료로
              인한 손해에 관하여 책임을 지지 않습니다.
            </li>
          </ul>
        </section>

        {/* 부칙 */}
        <section className="space-y-s4">
          <h2
            className={cn(
              "text-2xl font-semibold",
              isDark ? "text-white" : "text-black",
            )}
          >
            부칙
          </h2>
          <p
            className={cn(
              "text-base leading-relaxed",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            본 약관은 {new Date().getFullYear()}년 {new Date().getMonth() + 1}월{" "}
            {new Date().getDate()}일부터 시행됩니다.
          </p>
        </section>
      </div>
    </div>
  );
}
