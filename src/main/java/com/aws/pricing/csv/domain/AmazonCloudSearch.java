package com.aws.pricing.csv.domain;

import lombok.Data;

@Data
public class AmazonCloudSearch {

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
    String instanceType;
    String group;
    String groupDescription;
    String transferType;
    String fromLocation;
    String fromLocationType;
    String toLocation;
    String toLocationType;
    String usageType;
    String operation;
    String cloudSearchVersion;

    //SKU,"OfferTermCode","RateCode","TermType","PriceDescription","EffectiveDate","StartingRange","EndingRange","Unit"
    //"PricePerUnit","Currency","Product Family","serviceCode","Location","Location Type","Instance Type","Group"
    //"Group Description","Transfer Type","From Location","From Location Type","To Location","To Location Type"
    //"usageType","operation","Cloud Search Version"
}
