package com.thoroldvix.pricepal.goldprice;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.thoroldvix.pricepal.server.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class GoldPriceUpdateService {

    private final ServerRepository serverRepository;
    private final G2GPriceClient g2gPriceClient;
    private final ServerService serverServiceImpl;
    private final GoldPriceService goldPriceServiceImpl;


    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    private void update() {
        log.info("Updating gold prices");
        Instant start = Instant.now();
        String goldPricesJson = g2gPriceClient.getAllPrices();
        List<GoldPrice> pricesToSave = retrieveGoldPrices(goldPricesJson);
        goldPriceServiceImpl.saveAll(pricesToSave);
        log.info("Finished updating gold prices in {} ms", Duration.between(start, Instant.now()).toMillis());
    }

    private List<GoldPrice> retrieveGoldPrices(String goldPricesJson) {
        List<GoldPriceResponse> priceResponses = extractFromJson(goldPricesJson);
        List<ServerResponse> allServers = serverServiceImpl.getAll();
        return getPriceList(allServers, priceResponses);
    }


    private List<GoldPrice> getPriceList(List<ServerResponse> servers, List<GoldPriceResponse> prices) {
        List<GoldPrice> pricesToSave = new ArrayList<>();
        servers.forEach(server -> {
            GoldPrice price = getForServer(server, prices);
            pricesToSave.add(price);
        });
        return pricesToSave;
    }

    private GoldPrice getForServer(ServerResponse server, List<GoldPriceResponse> prices) {
        String uniqueServerName = server.uniqueName();
        BigDecimal price = findPrice(prices, uniqueServerName);
        Server serverEntity = serverRepository.getReferenceById(server.id());
        return GoldPrice.builder()
                .price(price)
                .server(serverEntity)
                .build();
    }

     private List<GoldPriceResponse> extractFromJson(String goldPricesJson) {
        G2GPriceListDeserializer deserializer = new G2GPriceListDeserializer();
        ObjectMapper mapper = new ObjectMapper();
        DeserializationContext context = mapper.getDeserializationContext();
        JsonParser parser;
        try {
            parser = mapper.getFactory().createParser(goldPricesJson);
            return deserializer.deserialize(parser, context);
        } catch (IOException e) {
            throw new GoldPriceParsingException("Error while parsing gold price json", e);
        }
    }

    private BigDecimal findPrice(List<GoldPriceResponse> prices, String uniqueServerName) {
        return prices.stream().filter(result -> result.serverName().equals(uniqueServerName))
                .findFirst()
                .map(GoldPriceResponse::price)
                .orElseThrow(() -> new G2GPriceNotFoundException("Price not found for server: " + uniqueServerName));
    }
}