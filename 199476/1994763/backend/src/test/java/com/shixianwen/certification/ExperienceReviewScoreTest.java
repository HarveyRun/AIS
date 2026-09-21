package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExperienceReviewScoreTest {

    @Test
    void calculatesEqualWeightReferenceIndex() {
        ExperienceReviewScore score = ExperienceReviewScore.of(6, 8, 7, 9, 8, 7);

        assertEquals(76, score.referenceIndex());
    }

    @Test
    void supportsFullZeroToOneHundredRange() {
        assertEquals(0, ExperienceReviewScore.of(0, 0, 0, 0, 0, 6).referenceIndex());
        assertEquals(100, ExperienceReviewScore.of(10, 10, 10, 10, 10, 6).referenceIndex());
    }

    @Test
    void derivesApprovalOnlyFromInformationSpecificity() {
        assertEquals(false, ExperienceReviewScore.of(10, 10, 10, 10, 10, 5).approved());
        assertEquals(true, ExperienceReviewScore.of(0, 0, 0, 0, 0, 6).approved());
    }

    @Test
    void requiresEveryDimensionAndRejectsOutOfRangeValues() {
        assertThrows(
            BusinessException.class,
            () -> ExperienceReviewScore.of(null, 5, 5, 5, 5, 6)
        );
        assertThrows(
            BusinessException.class,
            () -> ExperienceReviewScore.of(11, 5, 5, 5, 5, 6)
        );
        assertThrows(
            BusinessException.class,
            () -> ExperienceReviewScore.of(5, 5, -1, 5, 5, 6)
        );
        assertThrows(
            BusinessException.class,
            () -> ExperienceReviewScore.of(5, 5, 5, 5, 5, null)
        );
    }
}
