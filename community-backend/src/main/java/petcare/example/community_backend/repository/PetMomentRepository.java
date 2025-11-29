package petcare.example.community_backend.repository;

import petcare.example.community_backend.model.PetMoment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PetMomentRepository extends JpaRepository<PetMoment, Long> {

    /**
     * 查找属于特定用户ID的所有动态，并按创建时间倒序排列。
     */
    List<PetMoment> findByUserIdOrderByCreatedAtDesc(Long userId);
}