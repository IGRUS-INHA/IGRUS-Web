package igrus.web.user.repository;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // === 기본 조회 (soft delete 자동 필터링 by @SQLRestriction) ===

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByRole(UserRole role);

    Page<User> findByRole(UserRole role, Pageable pageable);

    long countByRole(UserRole role);

    // === 삭제된 데이터 포함 조회 (관리자용, native query로 @SQLRestriction 우회) ===

    @Query(value = "SELECT * FROM users u WHERE u.users_id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") Long id);

    @Query(value = "SELECT * FROM users u WHERE u.users_email = :email", nativeQuery = true)
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    @Query(value = "SELECT * FROM users u WHERE u.users_student_id = :studentId", nativeQuery = true)
    Optional<User> findByStudentIdIncludingDeleted(@Param("studentId") String studentId);
}
