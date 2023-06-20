package com.thoroldvix.pricepal.item;

import com.thoroldvix.pricepal.shared.SearchRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemService {
    ItemPagedResponse search(SearchRequest searchRequest, Pageable pageable);

    ItemPagedResponse getAll(Pageable pageable);

    List<ItemResponse> getAll();

    ItemResponse getItem(String itemIdentifier);

    ItemSummaryResponse getSummary();

    ItemResponse addItem(ItemRequest itemRequest);

    ItemResponse deleteItem(String itemIdentifier);
}
