package subway.section;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import subway.exception.SectionAlreadyCreateStationException;
import subway.exception.SectionBadRequestException;
import subway.exception.SectionUpStationNotMatchException;
import subway.line.Line;
import subway.line.LineService;
import subway.station.Station;
import subway.station.StationService;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SectionService {
    private final LineService lineService;
    private final StationService stationService;
    private final SectionRepository sectionRepository;

    public SectionService(LineService lineService, SectionRepository sectionRepository, StationService stationService) {
        this.lineService = lineService;
        this.sectionRepository = sectionRepository;
        this.stationService = stationService;
    }

    @Transactional
    public SectionResponse createSection(long line_id, SectionCreateRequest request) {
        Line line = lineService.findOneById(line_id);
        Station upStation = stationService.findStation(request.getUpStationId());
        Station downStation = stationService.findStation(request.getDownStationId());

        Section section = new Section(upStation, downStation, request.getDistance(), line);
        line.addSection(section);
        section = sectionRepository.save(section);
        return new SectionResponse(section.getId(), section.getUpStation().getId(), section.getDownStation().getId(), section.getDistance());
    }

    @Transactional
    public void deleteSection(long lineId, long stationId) {
        Line line = lineService.findOneById(lineId);
        Station requestStation = stationService.findStation(stationId);
        Station lineDownStation = line.getDownStation();
        if (requestStation != lineDownStation || line.hasMinimumStations()) {
            throw new SectionBadRequestException();
        }
        Section section = line.removeLastSection();
        sectionRepository.delete(section);
    }
}
