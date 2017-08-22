package com.aws.pricing.csv;

import com.aws.pricing.csv.domain.AmazonChimeProduct;
import com.aws.pricing.csv.domain.AmazonCloudSearch;
import com.aws.pricing.csv.domain.AmazonConnect;
import com.aws.pricing.csv.domain.AwsPriceApiProperties;
import com.aws.pricing.csv.service.AwsPricingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@EnableConfigurationProperties(AwsPriceApiProperties.class)
public class PricingConfiguration {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean
    public CommandLineRunner commandLineRunner(AwsPricingService awsPricingService) {
        return strings -> {
            log.info("\n\n{}\n", awsPricingService.getProductPrices("AmazonChime", AmazonChimeProduct.class));
            log.info("\n\n{}\n", awsPricingService.getProductPrices("AmazonCloudSearch", AmazonCloudSearch.class));
            log.info("\n\n{}\n", awsPricingService.getProductPrices("AmazonConnect", AmazonConnect.class));
        };
    }
}
