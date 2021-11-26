package nextstep.subway.line.application;

import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.line.domain.Section;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.SectionResponse;
import nextstep.subway.line.exception.DuplicateLineNameException;
import nextstep.subway.line.exception.LineNotFoundException;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.domain.StationRepository;
import nextstep.subway.station.exception.StationNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LineService {
    private final LineRepository lineRepository;
    private final StationRepository stationRepository;

    public LineService(LineRepository lineRepository, StationRepository stationRepository) {
        this.lineRepository = lineRepository;
        this.stationRepository = stationRepository;
    }

    public LineResponse saveLine(LineRequest request) {
        lineRepository.findByName(request.getName())
                .ifPresent(line -> { throw new DuplicateLineNameException(line); });
        Line line = lineRepository.save(request.toLine());
        if (request.hasSectionArguments()) {
            addSectionToLine(request, line);
        }
        return LineResponse.of(line);
    }

    private Section addSectionToLine(LineRequest request, Line line) {
        Station upStation = findStationById(request.getUpStationId());
        Station downStation = findStationById(request.getDownStationId());
        return line.addSection(upStation, downStation, request.getDistance());
    }

    public List<LineResponse> getLines() {
        return LineResponse.listOf(lineRepository.findAll());
    }

    public LineResponse getLine(Long id) {
        return lineRepository.findById(id)
                .map(LineResponse::of)
                .orElseThrow(() -> new LineNotFoundException(id));
    }

    public LineResponse modifyLine(Long id, LineRequest lineRequest) {
        return lineRepository.findById(id)
                .map(line -> line.update(lineRequest.toLine()))
                .map(LineResponse::of)
                .orElseThrow(() -> new LineNotFoundException(id));
    }

    public void deleteLine(Long id) {
        Line line = findLineById(id);
        lineRepository.delete(line);
    }

    public SectionResponse saveSection(Long lineId, LineRequest request) {
        Line line = findLineById(lineId);
        Section section = addSectionToLine(request, line);
        return SectionResponse.of(section);
    }

    public List<SectionResponse> getSections(Long lineId) {
        return lineRepository.findById(lineId)
                .map(Line::getSections)
                .map(SectionResponse::listOf)
                .orElseThrow(() -> new LineNotFoundException(lineId));
    }

    public void deleteSection(Long lineId, Long stationId) {
        Line line = findLineById(lineId);
        Station station = findStationById(stationId);
        line.deleteSectionByStation(station);
    }

    private Station findStationById(Long stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new StationNotFoundException(stationId));
    }

    private Line findLineById(Long lineId) {
        return lineRepository.findById(lineId)
                .orElseThrow(() -> new LineNotFoundException(lineId));
    }
}
