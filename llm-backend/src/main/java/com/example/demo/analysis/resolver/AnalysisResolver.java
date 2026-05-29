package com.example.demo.analysis.resolver;

import com.example.demo.analysis.dto.AnalysisResult;
import com.example.demo.analysis.service.HealthAnalysisService;
import graphql.kickstart.tools.GraphQLQueryResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnalysisResolver implements GraphQLQueryResolver {

    private final HealthAnalysisService healthAnalysisService;

    public AnalysisResolver(HealthAnalysisService healthAnalysisService) {
        this.healthAnalysisService = healthAnalysisService;
    }

    public AnalysisResult activityHealthAnalysis(String petId, Integer days, String userRequirement) {
        log.info("活动健康分析: petId={}, days={}", petId, days);
        return healthAnalysisService.analyze(petId, days, userRequirement);
    }
}
