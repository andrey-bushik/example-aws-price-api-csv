package com.aws.pricing.csv.service;

import com.aws.pricing.csv.domain.ProductPricing;

public interface AwsPricingService {

    <T> ProductPricing<T> getProductPrices(String name, Class<T> pricingType);
}
