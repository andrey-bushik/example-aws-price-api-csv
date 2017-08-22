package com.aws.pricing.csv.service;

import com.aws.pricing.csv.domain.AwsPriceApiProperties;
import com.aws.pricing.csv.domain.ProductPricing;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.Getter;
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
import java.util.List;
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
        }, response -> readResponseB(response.getBody(), pricingType));
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
            CsvMetaReader metaReader = new CsvMetaReader(reader).read();

            CsvSchema.Builder csvSchemaBuilder = new CsvSchema.Builder();
            metaReader.getHeaders().forEach(csvSchemaBuilder::addColumn);

            ObjectReader objectMapper = new CsvMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                    .readerFor(pricingType).with(csvSchemaBuilder.build());

            MappingIterator<T> mappingIterator = objectMapper.readValues(reader);
            List<T> prices = mappingIterator.readAll();

            List<String> meta = metaReader.getMeta();
            return ProductPricing.<T>builder().formatVersion(meta.get(0)).disclaimer(meta.get(1))
                    .publicationDate(meta.get(2)).version(meta.get(3)).offerCode(meta.get(4)).pricing(prices).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private class CsvMetaReader {
        static final int META_SIZE = 5;

        BufferedReader reader;
        ObjectReader objectReader;
        @Getter List<String> meta;
        @Getter List<String> headers;

        private CsvMetaReader(BufferedReader reader) {
            this.reader = reader;
            this.objectReader = new CsvMapper().reader().forType(String[].class);
        }

        private CsvMetaReader read() {
            //read meta
            meta = Stream.generate(() -> next()[1]).limit(META_SIZE).collect(Collectors.toList()); //get every second value
            //read headers
            headers = Arrays.stream(next()).map(StringUtils::trimAllWhitespace).collect(Collectors.toList());
            //reset buffered state to the price data
            reset();
            return this;
        }

        private String[] next() {
            try {
                return objectReader.readValue(reader.readLine());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void reset() {
            try {
                reader.mark(0);
                reader.reset();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
