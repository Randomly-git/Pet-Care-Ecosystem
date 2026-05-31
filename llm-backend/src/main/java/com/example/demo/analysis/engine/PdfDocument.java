package com.example.demo.analysis.engine;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PdfDocument {
    private String title;
    private String category;
    private String content;
}
