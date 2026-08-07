package com.legacyloop.user.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A premium membership plan. Prices are held in paise, the unit Razorpay works in. */
@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 48)
    private String code; // BASIC_MONTHLY

    @Column(nullable = false, length = 120)
    private String name; // Pro membership

    @Column(length = 500)
    private String description;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @ElementCollection
    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @OrderColumn(name = "position")
    @Column(name = "feature", length = 200)
    @Builder.Default
    private List<String> features = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean recommended = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public BigDecimal amountRupees() {
        return BigDecimal.valueOf(amountPaise).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public String priceLabel() {
        return "₹" + amountRupees().stripTrailingZeros().toPlainString();
    }
}

