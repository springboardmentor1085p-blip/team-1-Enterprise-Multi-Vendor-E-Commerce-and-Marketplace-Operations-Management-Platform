package com.shopstack.Model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vendor_approvals")
public class VendorApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long approvalId;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_user_id", nullable = false)
    private User reviewedByUser;

    @Column(name = "decision", nullable = false, length = 50)
    private String decision;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        reviewedAt = OffsetDateTime.now();
    }
}
