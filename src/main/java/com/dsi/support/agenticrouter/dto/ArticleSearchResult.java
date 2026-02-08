package com.dsi.support.agenticrouter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSearchResult {

    private Long articleId;

    private String title;

    private Double similarityScore;

    private String category;

    private Integer priority;

    private String articleType;
}
