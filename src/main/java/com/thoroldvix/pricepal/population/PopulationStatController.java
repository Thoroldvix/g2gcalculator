package com.thoroldvix.pricepal.population;

import com.thoroldvix.pricepal.shared.TimeRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Populations Stats API", description = "API for retrieving population stats")
@RequiredArgsConstructor
@RequestMapping("/wow-classic/api/v1/servers/populations/stats")
public class PopulationStatController {

    private final PopulationStatsService populationStatsService;

    @Operation(summary = "Retrieves basic population statistics for all servers",
            description = "The statistics are based on all server population scans and the time range in days")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of statistics",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PopulationStatResponse.class))),
            @ApiResponse(responseCode = "400", description = "Time range less than 1 day", content = @Content),
            @ApiResponse(responseCode = "404", description = "No statistics found for all populations", content = @Content),
            @ApiResponse(responseCode = "500", description = "An unexpected exception occurred", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PopulationStatResponse> getStatsForAll(
            @Parameter(description = "Range of days to retrieve statistics for")
            @RequestParam(defaultValue = "7") int timeRange) {
        var statsForAll = populationStatsService.getForAll(new TimeRange(timeRange));
        return ResponseEntity.ok(statsForAll);
    }

    @Operation(summary = "Retrieves basic population statistics for a server identifier",
            description = "The statistics are based on the provided identifier and the time range in days")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of statistics",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PopulationStatResponse.class))),
            @ApiResponse(responseCode = "400", description = "Incorrect server identifier", content = @Content),
            @ApiResponse(responseCode = "404", description = "No statistics found for server identifier", content = @Content),
            @ApiResponse(responseCode = "500", description = "An unexpected exception occurred", content = @Content)
    })
    @GetMapping(value = "/{serverIdentifier}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PopulationStatResponse> getStatsForServer(
            @Parameter(description = "Identifier of the server in the format 'server-faction' (e.g. 'everlook-alliance') or server ID")
            @PathVariable String serverIdentifier,
            @Parameter(description = "Range of days to retrieve statistics for")
            @RequestParam(defaultValue = "7") int timeRange) {
        var statsForServer = populationStatsService.getForServer(serverIdentifier, new TimeRange(timeRange));
        return ResponseEntity.ok(statsForServer);
    }
    @GetMapping("/regions/{regionName}")
    public ResponseEntity<PopulationStatResponse> getStatsForRegion(@PathVariable String regionName,
                                                                               @Parameter(description = "Range of days to retrieve statistics for")
                                                                               @RequestParam(defaultValue = "7") int timeRange) {
        var statsForRegion = populationStatsService.getForRegion(regionName, new TimeRange(timeRange));
        return ResponseEntity.ok(statsForRegion);
    }

    @GetMapping("/factions/{factionName}")
    public ResponseEntity<PopulationStatResponse> getStatsForFaction(@PathVariable String factionName,
                                                                                @Parameter(description = "Range of days to retrieve statistics for")
                                                                                @RequestParam(defaultValue = "7") int timeRange) {
        var statsForFaction = populationStatsService.getForFaction(factionName, new TimeRange(timeRange));
        return ResponseEntity.ok(statsForFaction);
    }
}
