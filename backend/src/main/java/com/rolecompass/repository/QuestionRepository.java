package com.rolecompass.repository;

import com.rolecompass.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySectionIdOrderByIdAsc(Integer sectionId);
    List<Question> findBySectionIdInOrderBySectionIdAscIdAsc(Collection<Integer> sectionIds);
    List<Question> findAllByOrderByIdAsc();
    long countByResponseTypeIsNull();
}
