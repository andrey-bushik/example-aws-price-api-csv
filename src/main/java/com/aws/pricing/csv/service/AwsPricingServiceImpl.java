package com.aws.pricing.csv.service;

import com.aws.pricing.csv.domain.AwsPriceApiProperties;
import com.aws.pricing.csv.domain.ProductPricing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class AwsPricingServiceImpl implements AwsPricingService {

    private RestTemplate restTemplate;
    private AwsPriceApiProperties awsPriceApiProperties;

    @Autowired
    public AwsPricingServiceImpl(RestTemplate restTemplate, AwsPriceApiProperties awsPriceApiProperties) {
        this.restTemplate = restTemplate;
        this.awsPriceApiProperties = awsPriceApiProperties;
    }

    @Override
    public <T> ProductPricing<T> getProductPrices(String name, Class<T> pricingType) {
        String priceUrl = String.format(awsPriceApiProperties.getUrl(), name);
        return restTemplate.execute(priceUrl, HttpMethod.GET, clientHttpRequest -> {
            clientHttpRequest.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        }, response -> readResponseA(response.getBody(), pricingType));
    }

    /**
     * approach A
     */
    private <T> ProductPricing<T> readResponseA(InputStream inputStream, Class<T> pricingType) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            ObjectReader metaReader = new CsvMapper().enable(CsvParser.Feature.WRAP_AS_ARRAY).readerFor(String[].class);
            MappingIterator<String[]> mappingIterator = metaReader.readValues(reader);

            ProductPricing<T> pricing = ProductPricing.<T>builder().formatVersion(readMeta(mappingIterator)).disclaimer(readMeta(mappingIterator))
                    .publicationDate(readMeta(mappingIterator)).version(readMeta(mappingIterator)).offerCode(readMeta(mappingIterator)).build();

            CsvSchema csvSchema = readSchema(mappingIterator);

            ObjectMapper objectMapper = new CsvMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
            ObjectReader objectReader = objectMapper.readerFor(pricingType).with(csvSchema);
            ObjectWriter objectWriter = objectMapper.writer(csvSchema);
            Stream<String[]> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(mappingIterator, 0), false);
            pricing.setPricing(stream.map((Function<String[], T>) strings -> readValue(objectReader, objectWriter, strings)).collect(Collectors.toList()));
            return pricing;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private String readMeta(MappingIterator<String[]> iterator) {
        return iterator.next()[1];
    }

    private CsvSchema readSchema(MappingIterator<String[]> iterator) {
        CsvSchema.Builder schema = new CsvSchema.Builder();
        for (String value : iterator.next()) {
            schema.addColumn(StringUtils.trimAllWhitespace(value));
        }
        return schema.build();
    }

    private <T> T readValue(ObjectReader objectReader, ObjectWriter objectWriter, String[] source) {
        try {
            return objectReader.readValue(objectWriter.writeValueAsString(source));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  approach B
     */
    private <T> ProductPricing<T> readResponseB(InputStream inputStream, Class<T> pricingType) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            ProductPricing<T> productPricing = ProductPricing.<T>builder().formatVersion(getMeta(reader)).disclaimer(getMeta(reader))
                    .publicationDate(getMeta(reader)).version(getMeta(reader)).offerCode(getMeta(reader)).build();

            CsvSchema csvSchema = getSchema(reader);

            reader.mark(0);
            reader.reset();

            ObjectReader objectMapper = new CsvMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES).readerFor(pricingType).with(csvSchema);
            MappingIterator<T> mappingIterator = objectMapper.readValues(reader);
            productPricing.setPricing(mappingIterator.readAll());
            return productPricing;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private String getMeta(BufferedReader reader) throws IOException {
        return readRow(reader)[1];
    }

    private CsvSchema getSchema(BufferedReader reader) throws IOException {
        CsvSchema.Builder schema = new CsvSchema.Builder();
        for (String value : readRow(reader)) {
            schema.addColumn(StringUtils.trimAllWhitespace(value));
        }
        return schema.build();
    }

    private String[] readRow(BufferedReader reader) throws IOException {
        return reader.readLine().replace("\"", "").split(",");
    }
}
