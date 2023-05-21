package com.thoroldvix.g2gcalculator.item;

import com.thoroldvix.g2gcalculator.item.dto.ItemInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/wow-classic/v1/items")
@RequiredArgsConstructor
public class ItemsController {

    private final ItemService itemServiceImpl;

    @GetMapping
    public ResponseEntity<Set<ItemInfo>> getAllItems(Pageable pageable) {
        return ResponseEntity.ok(itemServiceImpl.getAllItems(pageable));
    }

    @GetMapping("search")
    public ResponseEntity<List<ItemInfo>> searchItems(@RequestParam("query") String query, Pageable pageable) {

        return ResponseEntity.ok(itemServiceImpl.searchItems(query, pageable));
    }

    @PostMapping
    public ResponseEntity<List<ItemInfo>> findItemsByIds(@RequestBody List<Integer> ids) {
        if (ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(itemServiceImpl.findItemsByIds(ids));
    }
}