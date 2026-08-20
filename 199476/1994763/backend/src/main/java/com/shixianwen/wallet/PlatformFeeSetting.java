package com.shixianwen.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "platform_fee_settings")
public class PlatformFeeSetting {
    @Id
    private Long id;

    @Column(name = "client_platform", nullable = false, unique = true, length = 20)
    private String clientPlatform;

    @Column(name = "service_fee_rate", nullable = false, precision = 7, scale = 6)
    private BigDecimal serviceFeeRate;

    @Column(name = "updated_by_admin_id")
    private Long updatedByAdminId;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
