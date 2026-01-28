package igrus.web.common;

import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.repository.LoginAttemptRepository;
import igrus.web.security.auth.common.repository.LoginHistoryRepository;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.security.auth.password.repository.PasswordResetTokenRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserRoleHistoryRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 서비스 통합 테스트를 위한 기반 클래스.
 *
 * <p>고전파(Classical) 테스트 스타일을 위해 실제 H2 데이터베이스와
 * Spring Bean들을 사용합니다.</p>
 *
 * <p>주요 기능:
 * <ul>
 *     <li>테스트 전 데이터베이스 정리 (FK 순서 고려)</li>
 *     <li>공통 헬퍼 메서드 제공</li>
 *     <li>TransactionTemplate을 통한 트랜잭션 관리</li>
 * </ul>
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class ServiceIntegrationTestBase {

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected PrivacyConsentRepository privacyConsentRepository;

    @Autowired
    protected LoginAttemptRepository loginAttemptRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected EmailVerificationRepository emailVerificationRepository;

    @Autowired
    protected PasswordCredentialRepository passwordCredentialRepository;

    @Autowired
    protected PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    protected UserRoleHistoryRepository userRoleHistoryRepository;

    @Autowired
    protected LoginHistoryRepository loginHistoryRepository;

    protected TransactionTemplate transactionTemplate;

    /**
     * 테스트 전 초기화를 수행합니다.
     * 서브클래스에서 @BeforeEach 메서드에서 호출해야 합니다.
     */
    protected void setUpBase() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        cleanupDatabase();
    }

    /**
     * 데이터베이스를 정리합니다.
     * FK 제약 조건 순서를 고려하여 native query로 삭제합니다.
     *
     * <p>Repository.deleteAll() 대신 native query를 사용하는 이유:
     * <ul>
     *     <li>@SQLRestriction으로 인해 soft-deleted 레코드가 미삭제되는 문제 방지</li>
     *     <li>모든 테이블을 명시적으로 정리하여 테스트 간 데이터 격리 보장</li>
     * </ul>
     * </p>
     */
    protected void cleanupDatabase() {
        transactionTemplate.execute(status -> {
            // Phase 1: Inquiry 계층 (자식 먼저)
            entityManager.createNativeQuery("DELETE FROM inquiry_memos").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM inquiry_replies").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM inquiry_attachments").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM guest_inquiries").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM member_inquiries").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM inquiries").executeUpdate();

            // Phase 2: Board 계층 (자식 먼저)
            entityManager.createNativeQuery("DELETE FROM post_views").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM post_images").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM posts").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM board_permissions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM boards").executeUpdate();

            // Phase 3: User 종속 테이블
            entityManager.createNativeQuery("DELETE FROM login_histories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM refresh_tokens").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM password_reset_tokens").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM password_credentials").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM privacy_consents").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_role_histories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_suspensions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_positions").executeUpdate();

            // Phase 3: 부모 테이블
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM positions").executeUpdate();

            // Phase 4: 독립 테이블
            entityManager.createNativeQuery("DELETE FROM email_verifications").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM login_attempts").executeUpdate();

            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    /**
     * 테스트용 사용자를 생성합니다.
     *
     * @param studentId 학번
     * @param email 이메일
     * @param role 역할
     * @return 생성된 사용자
     */
    protected User createUser(String studentId, String email, UserRole role) {
        User user = User.create(
                studentId,
                "테스트유저",
                email,
                "010-" + studentId,
                "컴퓨터공학과",
                "테스트 동기"
        );
        user.changeRole(role);
        user.verifyEmail(); // PENDING_VERIFICATION -> ACTIVE (테스트에서 기본적으로 ACTIVE 상태 사용)
        return user;
    }

    /**
     * 테스트용 사용자를 생성하고 저장합니다.
     *
     * @param studentId 학번
     * @param email 이메일
     * @param role 역할
     * @return 저장된 사용자
     */
    protected User createAndSaveUser(String studentId, String email, UserRole role) {
        User user = createUser(studentId, email, role);
        return userRepository.save(user);
    }

    /**
     * 기본 설정의 테스트용 사용자를 생성하고 저장합니다.
     *
     * @return 저장된 사용자
     */
    protected User createAndSaveDefaultUser() {
        return createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
    }

    /**
     * 사용자에게 ID를 수동으로 설정합니다 (리플렉션 사용).
     *
     * @param user 사용자
     * @param id 설정할 ID
     */
    protected void setUserId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
    }

    /**
     * 엔티티의 필드를 수동으로 설정합니다 (리플렉션 사용).
     *
     * @param target 대상 객체
     * @param fieldName 필드명
     * @param value 설정할 값
     */
    protected void setField(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }
}
