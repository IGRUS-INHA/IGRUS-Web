package igrus.web.user.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.AccountStatusChangeHistory;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.audit.AccountStatusChanged;
import igrus.web.user.repository.AccountStatusChangeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecordAccountStatusChangeService 통합 테스트")
class RecordAccountStatusChangeServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private RecordAccountStatusChangeService recordAccountStatusChangeService;

    @Autowired
    private AccountStatusChangeHistoryRepository accountStatusChangeHistoryRepository;

    private User targetUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            targetUser = createAndSaveUser("20220001", "target@inha.edu", UserRole.MEMBER);
            adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
            return null;
        });
    }

    @Test
    @DisplayName("APPROVAL 이벤트 수신 시 이력이 저장된다")
    void handleAccountStatusChange_Approval_SavesHistory() {
        transactionTemplate.execute(status -> {
            AccountStatusChanged event = new AccountStatusChanged(
                    targetUser.getId(), adminUser.getId(), AccountChangeType.APPROVAL,
                    "ASSOCIATE", "MEMBER", "관리자 승인"
            );

            recordAccountStatusChangeService.handleAccountStatusChange(event);
            return null;
        });

        List<AccountStatusChangeHistory> histories = accountStatusChangeHistoryRepository.findAll();
        assertThat(histories).hasSize(1);

        AccountStatusChangeHistory history = histories.get(0);
        assertThat(history.getChangeType()).isEqualTo(AccountChangeType.APPROVAL);
        assertThat(history.getPreviousValue()).isEqualTo("ASSOCIATE");
        assertThat(history.getNewValue()).isEqualTo("MEMBER");
        assertThat(history.getReason()).isEqualTo("관리자 승인");
        assertThat(history.getUserId()).isEqualTo(targetUser.getId());
        assertThat(history.getUserStudentId()).isEqualTo(targetUser.getStudentId());
        assertThat(history.getChangedByUserId()).isEqualTo(adminUser.getId());
        assertThat(history.getChangedByStudentId()).isEqualTo(adminUser.getStudentId());
    }

    @Test
    @DisplayName("ROLE_CHANGE 이벤트 수신 시 이력이 저장된다")
    void handleAccountStatusChange_RoleChange_SavesHistory() {
        transactionTemplate.execute(status -> {
            AccountStatusChanged event = new AccountStatusChanged(
                    targetUser.getId(), adminUser.getId(), AccountChangeType.ROLE_CHANGE,
                    "MEMBER", "OPERATOR", "관리자에 의한 역할 변경"
            );

            recordAccountStatusChangeService.handleAccountStatusChange(event);
            return null;
        });

        List<AccountStatusChangeHistory> histories = accountStatusChangeHistoryRepository.findAll();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getChangeType()).isEqualTo(AccountChangeType.ROLE_CHANGE);
        assertThat(histories.get(0).getPreviousValue()).isEqualTo("MEMBER");
        assertThat(histories.get(0).getNewValue()).isEqualTo("OPERATOR");
    }

    @Test
    @DisplayName("SUSPENSION 이벤트 수신 시 이력이 저장된다")
    void handleAccountStatusChange_Suspension_SavesHistory() {
        transactionTemplate.execute(status -> {
            AccountStatusChanged event = new AccountStatusChanged(
                    targetUser.getId(), adminUser.getId(), AccountChangeType.SUSPENSION,
                    "ACTIVE", "SUSPENDED", "규정 위반"
            );

            recordAccountStatusChangeService.handleAccountStatusChange(event);
            return null;
        });

        List<AccountStatusChangeHistory> histories = accountStatusChangeHistoryRepository.findAll();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getChangeType()).isEqualTo(AccountChangeType.SUSPENSION);
    }

    @Test
    @DisplayName("SUSPENSION_LIFT 이벤트 수신 시 이력이 저장된다")
    void handleAccountStatusChange_SuspensionLift_SavesHistory() {
        transactionTemplate.execute(status -> {
            AccountStatusChanged event = new AccountStatusChanged(
                    targetUser.getId(), adminUser.getId(), AccountChangeType.SUSPENSION_LIFT,
                    "SUSPENDED", "ACTIVE", null
            );

            recordAccountStatusChangeService.handleAccountStatusChange(event);
            return null;
        });

        List<AccountStatusChangeHistory> histories = accountStatusChangeHistoryRepository.findAll();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getChangeType()).isEqualTo(AccountChangeType.SUSPENSION_LIFT);
        assertThat(histories.get(0).getReason()).isNull();
    }

    @Test
    @DisplayName("WITHDRAWAL 이벤트 수신 시 이력이 저장된다")
    void handleAccountStatusChange_Withdrawal_SavesHistory() {
        transactionTemplate.execute(status -> {
            AccountStatusChanged event = new AccountStatusChanged(
                    targetUser.getId(), targetUser.getId(), AccountChangeType.WITHDRAWAL,
                    "ACTIVE", "WITHDRAWN", "개인 사유"
            );

            recordAccountStatusChangeService.handleAccountStatusChange(event);
            return null;
        });

        List<AccountStatusChangeHistory> histories = accountStatusChangeHistoryRepository.findAll();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getChangeType()).isEqualTo(AccountChangeType.WITHDRAWAL);
    }

    @Test
    @DisplayName("changedByUserId가 null인 경우에도 이력이 저장된다")
    void handleAccountStatusChange_NullChangedBy_SavesHistory() {
        transactionTemplate.execute(status -> {
            AccountStatusChanged event = new AccountStatusChanged(
                    targetUser.getId(), null, AccountChangeType.APPROVAL,
                    "PENDING_VERIFICATION", "ACTIVE", null
            );

            recordAccountStatusChangeService.handleAccountStatusChange(event);
            return null;
        });

        List<AccountStatusChangeHistory> histories = accountStatusChangeHistoryRepository.findAll();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getChangedByUserId()).isNull();
        assertThat(histories.get(0).getChangedByStudentId()).isNull();
    }
}
