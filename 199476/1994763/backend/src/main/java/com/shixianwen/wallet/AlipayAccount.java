package com.shixianwen.wallet;

import com.shixianwen.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alipay_accounts")
public class AlipayAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "authorization_type", nullable = false, length = 20)
    private String authorizationType = "OAUTH";

    @Column(name = "identifier_type", length = 20)
    private String identifierType;

    @Column(name = "real_name", length = 80)
    private String realName;

    @Column(name = "account_ciphertext", nullable = false, length = 512)
    private String accountCiphertext;

    @Column(name = "alipay_user_id_hash", unique = true, length = 64)
    private String alipayUserIdHash;

    @Column(name = "account_masked", nullable = false, length = 120)
    private String accountMasked;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
