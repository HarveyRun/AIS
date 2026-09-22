package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExperienceCategoryServiceTest {
    private final ExperienceCategoryRepository repository = mock(ExperienceCategoryRepository.class);
    private final ExperienceCategoryService service = new ExperienceCategoryService(repository);

    @Test
    void onlyEnabledLeafUnderEnabledParentCanBeSelected() {
        ExperienceCategory parent = category(1L, "住房家居", null);
        ExperienceCategory child = category(2L, "新房装修", parent);
        when(repository.findById(2L)).thenReturn(Optional.of(child));
        when(repository.findById(1L)).thenReturn(Optional.of(parent));
        assertSame(child, service.requireSelectableLeaf(2L));

        parent.setEnabled(false);
        assertThrows(BusinessException.class, () -> service.requireSelectableLeaf(2L));
        parent.setEnabled(true);
        child.setDeletedAt(LocalDateTime.now());
        assertThrows(BusinessException.class, () -> service.requireSelectableLeaf(2L));
        child.setDeletedAt(null);
        assertThrows(BusinessException.class, () -> service.requireSelectableLeaf(1L));
    }

    @Test
    void duplicateNamesAreScopedToTheSameParent() {
        ExperienceCategory first = category(1L, "住房家居", null);
        ExperienceCategory second = category(2L, "生活方式", null);
        ExperienceCategory existing = category(3L, "买房卖房", first);
        when(repository.findById(1L)).thenReturn(Optional.of(first));
        when(repository.findById(2L)).thenReturn(Optional.of(second));
        when(repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc())
            .thenReturn(List.of(first, second, existing));
        when(repository.save(any(ExperienceCategory.class))).thenAnswer(call -> call.getArgument(0));

        assertThrows(BusinessException.class, () -> service.save(null,
            new ExperienceCategoryService.CategoryInput(1L, "买房卖房", 20, true, null)));
        assertDoesNotThrow(() -> service.save(null,
            new ExperienceCategoryService.CategoryInput(2L, "买房卖房", 20, true, null)));
    }

    @Test
    void deletingParentSoftDeletesItsChildren() {
        ExperienceCategory parent = category(1L, "住房家居", null);
        ExperienceCategory child = category(2L, "新房装修", parent);
        when(repository.findById(1L)).thenReturn(Optional.of(parent));
        when(repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc()).thenReturn(List.of(parent, child));

        service.delete(1L);

        assertNotNull(parent.getDeletedAt());
        assertEquals(parent.getDeletedAt(), child.getDeletedAt());
        verify(repository, never()).delete(any());
    }

    @Test
    void appOptionsExcludeDisabledBranchesButKeepConfiguredOrder() {
        ExperienceCategory first = category(1L, "住房家居", null);
        ExperienceCategory child = category(2L, "买房卖房", first);
        ExperienceCategory hidden = category(3L, "法律维权", null);
        ExperienceCategory hiddenChild = category(4L, "劳动仲裁", hidden);
        hidden.setEnabled(false);
        when(repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc())
            .thenReturn(List.of(first, child, hidden, hiddenChild));

        assertEquals(List.of("住房家居", "买房卖房"),
            service.available().stream().map(ExperienceCategoryService.CategoryView::name).toList());
    }

    @Test
    void recommendationResolvesToTheRealCategoryForPublishingAndFiltering() {
        ExperienceCategory recommendationGroup = category(1L, "常见推荐", null);
        recommendationGroup.setRecommendationGroup(true);
        ExperienceCategory housing = category(2L, "住房家居", null);
        ExperienceCategory real = category(3L, "买房卖房", housing);
        ExperienceCategory shortcut = category(4L, "买房卖房", recommendationGroup);
        shortcut.setTargetCategory(real);
        when(repository.findById(4L)).thenReturn(Optional.of(shortcut));
        when(repository.findById(1L)).thenReturn(Optional.of(recommendationGroup));
        when(repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc())
            .thenReturn(List.of(recommendationGroup, shortcut, housing, real));

        assertSame(real, service.requireSelectableLeaf(4L));
        assertEquals(List.of(3L), service.matchingLeafIds(1L));
        assertEquals(List.of(3L), service.matchingLeafIds(4L));
        assertEquals(3L, service.available().get(1).targetCategoryId());
    }

    private ExperienceCategory category(Long id, String name, ExperienceCategory parent) {
        ExperienceCategory result = new ExperienceCategory();
        result.setId(id);
        result.setName(name);
        result.setParent(parent);
        result.setEnabled(true);
        return result;
    }
}
