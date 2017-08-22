package com.aws.pricing.csv.domain;

import lombok.Data;

@Data
public class AmazonChimeProduct {

    String sku;
    String offerTermCode;
    String rateCode;
    String termType;
    String priceDescription;
    String effectiveDate;
    String startingRange;
    String endingRange;
    String unit;
    String pricePerUnit;
    String currency;
    String productFamily;
    String serviceCode;
    String location;
    String locationType;
    String usageType;
    String operation;
    String licenseType;

    //SKU,"OfferTermCode","RateCode","TermType","PriceDescription","EffectiveDate","StartingRange","EndingRange","Unit"
    //"PricePerUnit","Currency","Product Family","serviceCode","Location","Location Type","usageType","operation","License Type"
}
