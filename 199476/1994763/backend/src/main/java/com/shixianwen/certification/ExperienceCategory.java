package com.shixianwen.certification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "experience_categories")
@Getter
@Setter
@NoArgsConstructor
public class ExperienceCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ExperienceCategory parent;

    @Column(name = "recommendation_group", nullable = false)
    private boolean recommendationGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_category_id")
    private ExperienceCategory targetCategory;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
