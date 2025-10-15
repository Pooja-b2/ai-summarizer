package com.pooja.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SummarizeResponse {
    private String summary;
    private String sentiment;  // positive/negative/neutral
}