package com.aws.pricing.csv.domain;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
public class ProductPricing<T> {

    String formatVersion;
    String disclaimer;
    String publicationDate;
    String version;
    String offerCode;
    List<T> pricing;
}
