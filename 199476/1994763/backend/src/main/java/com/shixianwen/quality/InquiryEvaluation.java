package com.shixianwen.quality;

import com.shixianwen.inquiry.Inquiry;
import com.shixianwen.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inquiry_evaluations")
public class InquiryEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false, unique = true)
    private Inquiry inquiry;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questioner_id", nullable = false)
    private User questioner;
    @Column(name = "answered_level", nullable = false, columnDefinition = "TINYINT")
    private int answeredLevel;
    @Column(name = "specific_level", nullable = false, columnDefinition = "TINYINT")
    private int specificLevel;
    @Column(name = "matched_level", nullable = false, columnDefinition = "TINYINT")
    private int matchedLevel;
    @Column(name = "useful_level", nullable = false, columnDefinition = "TINYINT")
    private int usefulLevel;
    @Column(name = "communication_level", nullable = false, columnDefinition = "TINYINT")
    private int communicationLevel;
    @Column(name = "ask_again_level", nullable = false, columnDefinition = "TINYINT")
    private int askAgainLevel;
    @Column(name = "negative_tags", columnDefinition = "json")
    private String negativeTags;
    @Column(length = 300)
    private String comment;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
