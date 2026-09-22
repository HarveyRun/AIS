package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ExperienceCategoryService {
    private final ExperienceCategoryRepository repository;

    public ExperienceCategoryService(ExperienceCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> all() {
        return repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc().stream()
            .map(CategoryView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryView> available() {
        return repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc().stream()
            .filter(ExperienceCategory::isEnabled)
            .filter(item -> item.getParent() == null ||
                (item.getParent().isEnabled() && item.getParent().getDeletedAt() == null))
            .filter(item -> item.getParent() == null || !item.getParent().isRecommendationGroup()
                || selectableRealLeaf(item.getTargetCategory()))
            .map(CategoryView::from).toList();
    }

    @Transactional(readOnly = true)
    public ExperienceCategory requireSelectableLeaf(Long id) {
        if (id == null) return null;
        ExperienceCategory item = repository.findById(id)
            .orElseThrow(() -> BusinessException.badRequest("请选择有效的二级分类"));
        if (item.getParent() == null || !item.isEnabled() || item.getDeletedAt() != null ||
            !item.getParent().isEnabled() || item.getParent().getDeletedAt() != null) {
            throw BusinessException.badRequest("该分类已停用或不存在，请重新选择");
        }
        if (!item.getParent().isRecommendationGroup()) return item;
        ExperienceCategory target = item.getTargetCategory();
        if (!selectableRealLeaf(target)) {
            throw BusinessException.badRequest("该推荐分类已不可用，请重新选择");
        }
        return target;
    }

    @Transactional(readOnly = true)
    public List<Long> matchingLeafIds(Long id) {
        if (id == null) return List.of();
        ExperienceCategory item = active(id);
        if (!item.isEnabled()) throw BusinessException.badRequest("分类已停用，请重新选择");
        if (item.getParent() != null) return List.of(requireSelectableLeaf(id).getId());
        return repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc().stream()
            .filter(child -> child.getParent() != null && Objects.equals(child.getParent().getId(), id))
            .filter(ExperienceCategory::isEnabled)
            .map(child -> item.isRecommendationGroup() ? child.getTargetCategory() : child)
            .filter(this::selectableRealLeaf)
            .map(ExperienceCategory::getId)
            .distinct().toList();
    }

    private boolean selectableRealLeaf(ExperienceCategory item) {
        return item != null && item.getParent() != null && !item.getParent().isRecommendationGroup()
            && item.isEnabled() && item.getDeletedAt() == null
            && item.getParent().isEnabled() && item.getParent().getDeletedAt() == null;
    }

    @Transactional
    public CategoryView save(Long id, CategoryInput input) {
        if (input == null) throw BusinessException.badRequest("请填写分类信息");
        String name = input.name() == null ? "" : input.name().trim();
        ExperienceCategory item = id == null ? new ExperienceCategory() : active(id);
        ExperienceCategory parent = input.parentId() == null ? null : active(input.parentId());
        if (parent != null && parent.getParent() != null) {
            throw BusinessException.badRequest("最多只支持两级分类");
        }
        if (parent != null && id != null && parent.getId().equals(id)) {
            throw BusinessException.badRequest("不能把分类设为自己的子分类");
        }
        if (id != null && parent != null && repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc()
            .stream().anyMatch(other -> other.getParent() != null && id.equals(other.getParent().getId()))) {
            throw BusinessException.badRequest("已有二级分类的一级分类不能改为二级分类");
        }
        if (id != null && item.getParent() != null && parent != null &&
            item.getParent().isRecommendationGroup() != parent.isRecommendationGroup()) {
            throw BusinessException.badRequest("推荐分类和真实分类不能互相转换");
        }
        ExperienceCategory target = null;
        if (parent != null && parent.isRecommendationGroup()) {
            if (input.targetCategoryId() == null) throw BusinessException.badRequest("请选择要推荐的真实二级分类");
            target = requireSelectableLeaf(input.targetCategoryId());
            if (target.getParent().isRecommendationGroup()) throw BusinessException.badRequest("不能推荐推荐分类");
            name = target.getName();
        } else if (input.targetCategoryId() != null) {
            throw BusinessException.badRequest("仅推荐分类可以关联真实分类");
        }
        if (name.isEmpty() || name.length() > 40) {
            throw BusinessException.badRequest("分类名称须为1至40个字");
        }
        final String finalName = name;
        final ExperienceCategory finalTarget = target;
        boolean duplicate = repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc().stream()
            .anyMatch(other -> !other.getId().equals(id) &&
                (finalTarget == null ? other.getName().equals(finalName) :
                    other.getTargetCategory() != null &&
                    other.getTargetCategory().getId().equals(finalTarget.getId())) &&
                (other.getParent() == null ? parent == null :
                    parent != null && other.getParent().getId().equals(parent.getId())));
        if (duplicate) throw BusinessException.badRequest("同一级分类下名称不能重复");
        item.setParent(parent);
        item.setName(name);
        item.setTargetCategory(target);
        item.setSortOrder(input.sortOrder() == null ? 0 : input.sortOrder());
        item.setEnabled(input.enabled() == null || input.enabled());
        return CategoryView.from(repository.save(item));
    }

    @Transactional
    public CategoryView setEnabled(Long id, boolean enabled) {
        ExperienceCategory item = active(id);
        item.setEnabled(enabled);
        return CategoryView.from(repository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        ExperienceCategory item = active(id);
        LocalDateTime now = LocalDateTime.now();
        item.setDeletedAt(now);
        for (ExperienceCategory child : repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc()) {
            if (child.getParent() != null && id.equals(child.getParent().getId())) {
                child.setDeletedAt(now);
            }
        }
    }

    private ExperienceCategory active(Long id) {
        ExperienceCategory item = repository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("分类不存在"));
        if (item.getDeletedAt() != null) throw BusinessException.notFound("分类不存在");
        return item;
    }

    public record CategoryInput(Long parentId, String name, Integer sortOrder, Boolean enabled, Long targetCategoryId) {}
    public record CategoryView(Long id, Long parentId, String name, int sortOrder, boolean enabled,
                               boolean recommendationGroup, Long targetCategoryId) {
        static CategoryView from(ExperienceCategory item) {
            return new CategoryView(item.getId(),
                item.getParent() == null ? null : item.getParent().getId(),
                item.getTargetCategory() == null ? item.getName() : item.getTargetCategory().getName(),
                item.getSortOrder(), item.isEnabled(), item.isRecommendationGroup(),
                item.getTargetCategory() == null ? null : item.getTargetCategory().getId());
        }
    }
}
