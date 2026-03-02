package ru.screenmon.storage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FrameAnalysisRepository extends JpaRepository<FrameAnalysis, Long> {

    List<FrameAnalysis> findTop3ByScreenIdOrderByTsDesc(Long screenId);

    /**
     * Берём больше истории, чтобы можно было применять "окно" из настроек (freezeFramesWindow / deadPixelsFramesPersist).
     * Spring Data требует константный TopN, поэтому используем запасом 20 и далее режем список.
     */
    List<FrameAnalysis> findTop20ByScreenIdOrderByTsDesc(Long screenId);

    FrameAnalysis findTop1ByScreenIdOrderByTsDesc(Long screenId);

    long countByScreenId(Long screenId);
}
