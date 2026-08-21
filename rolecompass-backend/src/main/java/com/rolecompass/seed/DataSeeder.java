package com.rolecompass.seed;

import com.rolecompass.entity.Question;
import com.rolecompass.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (questionRepository.count() == 0) {
            log.info("Seeding initial 4 Section 1 questions for RoleCompass demo...");

            List<Question> seedQuestions = List.of(
                    Question.builder()
                            .sectionId(1)
                            .text("I enjoy designing server-side architectures, REST APIs, and database schemas.")
                            .dimensionTags(new String[]{"backend", "system_design", "database"})
                            .triggerPredicate("{\"min_interest\": 3}")
                            .build(),
                    Question.builder()
                            .sectionId(1)
                            .text("I prefer managing CI/CD pipelines, container orchestration (Docker/K8s), and cloud infrastructure.")
                            .dimensionTags(new String[]{"devops", "cloud", "infrastructure"})
                            .triggerPredicate("{\"min_interest\": 3}")
                            .build(),
                    Question.builder()
                            .sectionId(1)
                            .text("I like building intuitive user interfaces, managing client state, and perfecting responsive designs.")
                            .dimensionTags(new String[]{"frontend", "ui_ux", "javascript"})
                            .triggerPredicate("{\"min_interest\": 3}")
                            .build(),
                    Question.builder()
                            .sectionId(1)
                            .text("I am fascinated by training machine learning models, statistical analysis, and data pipelines.")
                            .dimensionTags(new String[]{"data_science", "machine_learning", "python"})
                            .triggerPredicate("{\"min_interest\": 3}")
                            .build()
            );

            questionRepository.saveAll(seedQuestions);
            log.info("Successfully seeded {} questions.", seedQuestions.size());
        } else {
            log.info("Questions table already contains data. Skipping seed.");
        }
    }
}
