package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

@Entity
@Table(name = "article_template")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleTemplate extends BaseEntity {

    private static final int TEMPLATE_PREVIEW_LENGTH = 120;

    @Column(nullable = false, length = 100)
    private String name;

    @Lob
    @Column(nullable = false)
    private String templateContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> variables = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "article_template_applicable_categories",
        joinColumns = @JoinColumn(name = "article_template_id")
    )
    @Column(name = "category", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<TicketCategory> applicableCategories = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "article_template_applicable_priorities",
        joinColumns = @JoinColumn(name = "article_template_id")
    )
    @Column(name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<TicketPriority> applicablePriorities = new LinkedHashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    private static String preview(
        String content
    ) {
        String safe = StringUtils.defaultString(content);
        return StringUtils.abbreviate(safe, TEMPLATE_PREVIEW_LENGTH);
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    private void normalizeCollections() {
        variables = Objects.requireNonNullElseGet(variables, ArrayList::new);
        applicableCategories = Objects.requireNonNullElseGet(applicableCategories, LinkedHashSet::new);
        applicablePriorities = Objects.requireNonNullElseGet(applicablePriorities, LinkedHashSet::new);
        active = Objects.requireNonNullElse(active, true);
    }

    @Override
    public String toString() {
        return "ArticleTemplate{" +
               "id=" + getId() +
               ", name='" + name + '\'' +
               ", templateContentPreview='" + preview(templateContent) + '\'' +
               ", variables=" + variables +
               ", applicableCategories=" + applicableCategories +
               ", applicablePriorities=" + applicablePriorities +
               ", active=" + active +
               '}';
    }
}
