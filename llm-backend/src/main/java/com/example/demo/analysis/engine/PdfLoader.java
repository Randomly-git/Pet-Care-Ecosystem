package com.example.demo.analysis.engine;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PdfLoader {

    public List<PdfDocument> load(String classpathDir) {
        List<PdfDocument> docs = new ArrayList<>();
        try {
            File dir = new ClassPathResource(classpathDir).getFile();
            if (!dir.isDirectory()) {
                log.warn("{} 不是目录", classpathDir);
                return docs;
            }
            File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".pdf"));
            if (files == null || files.length == 0) {
                log.warn("{} 下没有PDF文件", classpathDir);
                return docs;
            }
            for (File file : files) {
                loadFile(file, "general", docs);
            }
            log.info("PDF加载完成: {} 页来自 {} 个文件", docs.size(), files.length);
        } catch (IOException e) {
            log.warn("加载PDF失败(路径可能不存在): {}", e.getMessage());
        }
        return docs;
    }

    private void loadFile(File file, String category, List<PdfDocument> docs) {
        String title = file.getName().replace(".pdf", "").replace(".PDF", "");
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = doc.getNumberOfPages();
            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(doc).trim();
                if (!text.isEmpty()) {
                    docs.add(new PdfDocument(title + "_p" + i, category, text));
                }
            }
        } catch (IOException e) {
            log.error("解析PDF失败: {}", file.getName(), e);
        }
    }
}
