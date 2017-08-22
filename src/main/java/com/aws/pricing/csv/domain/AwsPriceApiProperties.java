package com.aws.pricing.csv.domain;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aws.price.api")
public class AwsPriceApiProperties {

    String url;
}
