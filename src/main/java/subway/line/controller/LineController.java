package subway.line.controller;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import subway.line.dto.LineCreateRequest;
import subway.line.dto.LineModifyRequest;
import subway.line.dto.LineResponse;
import subway.line.service.LineService;
import subway.station.model.Station;
import subway.station.service.StationService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/lines")
@RequiredArgsConstructor
public class LineController {

    private final LineService lineService;
    private final StationService stationService;

    @PostMapping
    public ResponseEntity<LineResponse> createLine(@RequestBody LineCreateRequest lineRequest) {
        Station upStation = stationService.findEntityById(lineRequest.getUpStationId());
        Station downStations = stationService.findEntityById(lineRequest.getDownStationId());
        LineResponse line = lineService.saveLine(lineRequest, upStation, downStations);
        return ResponseEntity.created(URI.create("/lines/" + line.getId())).body(line);
    }

    @GetMapping
    public ResponseEntity<List<LineResponse>> retrieveLines() {
        return ResponseEntity.ok().body(lineService.findAllLines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LineResponse> retrieveLine(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok().body(lineService.findLineResponseById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> modifyLine(@PathVariable(name = "id") Long id,
                                           @RequestBody LineModifyRequest request) {
        lineService.updateLine(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLine(@PathVariable(name = "id") Long id) {
        lineService.deleteLineById(id);
        return ResponseEntity.noContent().build();
    }
}