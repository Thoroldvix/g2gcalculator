package com.thoroldvix.economatic.itemprice;

import com.thoroldvix.economatic.item.ItemService;
import com.thoroldvix.economatic.server.Faction;
import com.thoroldvix.economatic.server.Region;
import com.thoroldvix.economatic.server.ServerService;
import com.thoroldvix.economatic.shared.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


import static com.thoroldvix.economatic.server.ServerErrorMessages.*;
import static com.thoroldvix.economatic.shared.ErrorMessages.*;
import static com.thoroldvix.economatic.shared.ValidationUtils.validateCollectionNotEmpty;


@Service
@Validated
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemPriceService {

    public static final String ITEM_IDENTIFIER_CANNOT_BE_NULL_OR_EMPTY = "Item identifier cannot be null or empty";
    private final ItemService itemService;
    private final ServerService serverService;
    private final ItemPriceRepository itemPriceRepository;
    private final ItemPriceMapper itemPriceMapper;
    private final AuctionHouseMapper auctionHouseMapper;
    private final SearchSpecification<ItemPrice> searchSpecification;
    private final JdbcTemplate jdbcTemplate;

    @Cacheable("item-price-cache")
    public PagedAuctionHouseInfo getRecentForServer(
            @NotEmpty(message = SERVER_IDENTIFIER_CANNOT_BE_NULL_OR_EMPTY)
            String serverIdentifier,
            @NotNull(message = PAGEABLE_CANNOT_BE_NULL)
            Pageable pageable) {
        Page<ItemPrice> page = findRecentForServer(serverIdentifier, pageable);
        validateCollectionNotEmpty(page.getContent(),
                () -> new ItemPriceNotFoundException("No recent item prices found for server identifier " + serverIdentifier));

        return auctionHouseMapper.toPagedWithServer(page);
    }


    public AuctionHouseInfo getRecentForRegion(
            @NotEmpty(message = REGION_NAME_CANNOT_BE_NULL_OR_EMPTY)
            String regionName,
            @NotEmpty(message = ITEM_IDENTIFIER_CANNOT_BE_NULL_OR_EMPTY)
            String itemIdentifier) {
        List<ItemPrice> itemPrices = findRecentForRegionAndItem(regionName, itemIdentifier);
        validateCollectionNotEmpty(itemPrices,
                () -> new ItemPriceNotFoundException("No item prices found for region and item identifier " + regionName + " " + itemIdentifier));

        return auctionHouseMapper.toInfoWithRegionAndItem(itemPrices);
    }


    public AuctionHouseInfo getRecentForFaction(
            @NotEmpty(message = FACTION_NAME_CANNOT_BE_NULL_OR_EMPTY)
            String factionName,
            @NotEmpty(message = ITEM_IDENTIFIER_CANNOT_BE_NULL_OR_EMPTY)
            String itemIdentifier) {
        List<ItemPrice> itemPrices = findRecentForFactionAndItem(factionName, itemIdentifier);
        validateCollectionNotEmpty(itemPrices,
                () -> new ItemPriceNotFoundException("No item prices found for faction and item identifier " + factionName + " " + itemIdentifier));

        return auctionHouseMapper.toInfoWithFactionAndItem(itemPrices);
    }


    public AuctionHouseInfo getRecentForServer(
            @NotEmpty(message = SERVER_IDENTIFIER_CANNOT_BE_NULL_OR_EMPTY)
            String serverIdentifier,
            @NotEmpty(message = ITEM_IDENTIFIER_CANNOT_BE_NULL_OR_EMPTY)
            String itemIdentifier) {
        List<ItemPrice> itemPrices = findRecentForServerAndItem(serverIdentifier, itemIdentifier);
        validateCollectionNotEmpty(itemPrices,
                () -> new ItemPriceNotFoundException(String.format("No item prices found for server identifier %s and item identifier %s", serverIdentifier, itemIdentifier)));

        return auctionHouseMapper.toInfoWithServerAndItem(itemPrices);
    }

    @Cacheable("item-price-cache")
    public ItemPricePagedResponse search(@Valid SearchRequest searchRequest,
                                         @NotNull(message = PAGEABLE_CANNOT_BE_NULL)
                                         Pageable pageable) {
        Page<ItemPrice> page = findAllForSearch(searchRequest, pageable);
        validateCollectionNotEmpty(page.getContent(), () -> new ItemPriceNotFoundException("No item prices found for search request"));

        return itemPriceMapper.toPagedResponse(page);
    }

    @Cacheable("item-price-cache")
    public PagedAuctionHouseInfo getForServer(
            @NotEmpty(message = SERVER_IDENTIFIER_CANNOT_BE_NULL_OR_EMPTY)
            String serverIdentifier,
            @NotEmpty(message = ITEM_IDENTIFIER_CANNOT_BE_NULL_OR_EMPTY)
            String itemIdentifier,
            @NotNull(message = TIME_RANGE_CANNOT_BE_NULL)
            TimeRange timeRange,
            @NotNull(message = PAGE_CANNOT_BE_NULL)
            Pageable pageable) {
        Page<ItemPrice> page = findForServerAndTimeRange(serverIdentifier, itemIdentifier, timeRange, pageable);
        validateCollectionNotEmpty(page.getContent(),
                () -> new ItemPriceNotFoundException("No item prices found for time range %s for server identifier %s and item identifier %s"
                        .formatted(timeRange, serverIdentifier, itemIdentifier)));

        return auctionHouseMapper.toPagedWithServerAndItem(page);
    }
@Cacheable("item-price-cache")
    public ItemPricePagedResponse getRecentForItemListAndServers(
            @Valid ItemPriceRequest request,
            @NotNull(message = PAGEABLE_CANNOT_BE_NULL)
            Pageable pageable) {

        Page<ItemPrice> page = findRecentForRequest(request, pageable);

        validateCollectionNotEmpty(page.getContent(),
                () -> new ItemPriceNotFoundException("No recent prices found for item list"));

        return itemPriceMapper.toPagedResponse(page);
    }

    @Transactional
    public void saveAll(
            @NotNull(message = "Item prices cannot be null")
            List<ItemPrice> itemPricesToSave) {
        itemPriceRepository.saveAll(itemPricesToSave, jdbcTemplate);
    }

    private Page<ItemPrice> findAllForSearch(SearchRequest searchRequest, Pageable pageable) {
        Specification<ItemPrice> specification = searchSpecification.create(searchRequest.globalOperator(), searchRequest.searchCriteria());
        return itemPriceRepository.findAll(specification, pageable);
    }


    private Page<ItemPrice> findRecentForServer(String serverIdentifier, Pageable pageable) {
        int serverId = serverService.getServer(serverIdentifier).id();
        return itemPriceRepository.findRecentForServer(serverId, pageable);
    }


    private Page<ItemPrice> findForServerAndTimeRange(String serverIdentifier,
                                                      String itemIdentifier,
                                                      TimeRange timeRange,
                                                      Pageable pageable) {
        int itemId = itemService.getItem(itemIdentifier).id();
        int serverId = serverService.getServer(serverIdentifier).id();
        return itemPriceRepository.findForServerAndTimeRange(serverId, itemId, timeRange.start(), timeRange.end(), pageable);
    }


    private List<ItemPrice> findRecentForServerAndItem(String serverIdentifier, String itemIdentifier) {
        int itemId = itemService.getItem(itemIdentifier).id();
        int serverId = serverService.getServer(serverIdentifier).id();
        return itemPriceRepository.findRecentForServerAndItem(serverId, itemId);
    }


    private List<ItemPrice> findRecentForRegionAndItem(String regionName, String itemIdentifier) {
        Region region = StringEnumConverter.fromString(regionName, Region.class);
        int itemId = itemService.getItem(itemIdentifier).id();
        return itemPriceRepository.findRecentForRegionAndItem(region.ordinal(), itemId);
    }


    private List<ItemPrice> findRecentForFactionAndItem(String factionName, String itemIdentifier) {
        Faction faction = StringEnumConverter.fromString(factionName, Faction.class);
        int itemId = itemService.getItem(itemIdentifier).id();
        return itemPriceRepository.findRecentForFactionAndItem(faction.ordinal(), itemId);
    }


    private Page<ItemPrice> findRecentForRequest(ItemPriceRequest request, Pageable pageable) {
        Set<Integer> itemIds = getItemIds(request.itemList());
        if (request.serverList() == null || request.serverList().isEmpty()) {
            return itemPriceRepository.findRecentForItemList(itemIds, pageable);
        }
        Set<Integer> serverIds = getServerIds(request.serverList());
        return itemPriceRepository.findRecentForItemListAndServers(itemIds, serverIds, pageable);
    }

    private Set<Integer> getServerIds(Set<String> serverList) {
        return serverList.parallelStream()
                .map(server -> serverService.getServer(server).id())
                .collect(Collectors.toSet());
    }

    private Set<Integer> getItemIds(Set<String> itemList) {
        return itemList.parallelStream()
                .map(item -> itemService.getItem(item).id())
                .collect(Collectors.toSet());
    }
}
