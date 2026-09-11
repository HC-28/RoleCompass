package com.rolecompass.repository;

import com.rolecompass.entity.Answer;
import com.rolecompass.entity.AnswerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, AnswerId> {
    List<Answer> findByIdSessionId(UUID sessionId);
}
