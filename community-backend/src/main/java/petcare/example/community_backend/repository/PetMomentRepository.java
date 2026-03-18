package petcare.example.community_backend.repository;

import petcare.example.community_backend.model.PetMoment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PetMomentRepository extends JpaRepository<PetMoment, Long> {

    /**
     * 查找属于特定用户ID的所有动态，并按创建时间倒序排列。
     */
    List<PetMoment> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 获取所有用户的动态，按创建时间倒序排列，支持分页。
     */
    Page<PetMoment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根据动态ID获取作者用户ID
     */
    @Query("SELECT m.userId FROM PetMoment m WHERE m.id = :momentId")
    Optional<Long> findUserIdById(@Param("momentId") Long momentId);
}