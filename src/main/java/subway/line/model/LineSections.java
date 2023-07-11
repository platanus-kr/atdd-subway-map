package subway.line.model;

import lombok.NoArgsConstructor;
import subway.exception.SubwayBadRequestException;
import subway.line.constant.LineMessage;
import subway.station.model.Station;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Embeddable
@NoArgsConstructor
public class LineSections {

    private static final long MINIMAL_SECTION_SIZE = 2L;

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Section> sections = new ArrayList<>();

    public void add(Section section, Line line) {
        if (line.getLineSections().sections.size() > 1) {
            validUpStationInNewSectionIsDownStationInExistLine(section, line);
            validDownStationInNewSectionIsNotDuplicatedInExistLine(section, line);
        }
        section.setLine(line);
        this.sections.add(section);
    }

    public List<Station> getDownStations() {
        return this.sections.stream().flatMap(section -> Stream.of(section.getUpStation(), section.getDownStation())).collect(Collectors.toList());
    }

    public int getStationsCount() {
        return this.sections.size();
    }

    public Section deleteSectionByStation(Station targetStation) {
        vaildStationsCountIsOverMinimalSectionSize();
        validRemoveStationIsDownStationInExistLine(targetStation);

        Section lastSection = getLastSection();
        this.remove(lastSection);

        return lastSection;
    }

    public Section get(int index) {
        return sections.get(index);
    }

    public void remove(Section section) {
        sections.remove(section);
    }

    private void validUpStationInNewSectionIsDownStationInExistLine(Section section, Line line) {
        if (!line.getDownStation().equals(section.getUpStation())) {
            throw new SubwayBadRequestException(LineMessage.DOWN_STATION_NOT_MATCH_WITH_UP_STATION.getCode(),
                    LineMessage.DOWN_STATION_NOT_MATCH_WITH_UP_STATION.getMessage());
        }
    }

    private void validDownStationInNewSectionIsNotDuplicatedInExistLine(Section section, Line line) {
        List<Station> stationsInLine = line.getStationsInSections();
        stationsInLine.stream()
                .filter(s -> s.equals(section.getDownStation()))
                .findAny()
                .ifPresent(e -> {
                    throw new SubwayBadRequestException(LineMessage.ADD_SECTION_STATION_DUPLICATION_VALID_MESSAGE.getCode(),
                            LineMessage.ADD_SECTION_STATION_DUPLICATION_VALID_MESSAGE.getMessage());
                });
    }

    private void vaildStationsCountIsOverMinimalSectionSize() {
        if (this.getStationsCount() < MINIMAL_SECTION_SIZE) {
            throw new SubwayBadRequestException(LineMessage.DOWN_STATION_MINIMAL_VALID_MESSAGE.getCode(),
                    LineMessage.DOWN_STATION_MINIMAL_VALID_MESSAGE.getFormatMessage(MINIMAL_SECTION_SIZE));
        }
    }

    private void validRemoveStationIsDownStationInExistLine(Station targetStation, Station downStation) {
        if (!downStation.equals(targetStation)) {
            throw new SubwayBadRequestException(LineMessage.SECTION_DELETE_LAST_STATION_VALID_MESSAGE.getCode(),
                    LineMessage.SECTION_DELETE_LAST_STATION_VALID_MESSAGE.getMessage());
        }
    }

    private void validRemoveStationIsDownStationInExistLine(Station targetStation) {
        Section lastSection = getLastSection();
        Station downStation = lastSection.getDownStation();
        if (!downStation.equals(targetStation)) {
            throw new SubwayBadRequestException(LineMessage.SECTION_DELETE_LAST_STATION_VALID_MESSAGE.getCode(),
                    LineMessage.SECTION_DELETE_LAST_STATION_VALID_MESSAGE.getMessage());
        }
    }

    private Section getLastSection() {
        int lastSectionIndex = this.sections.size() - 1;
        return this.sections.get(lastSectionIndex);
    }
}
