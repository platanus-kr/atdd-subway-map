package nextstep.subway.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.api.LineApi;
import nextstep.subway.api.StationApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("지하철 구간 관리 기능")
@Sql({"classpath:subway.init.sql"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SectionAcceptanceTest {

    @LocalServerPort
    int port;

    private long 강남역;
    private long 신논현역;
    private long 정자역;
    private long 판교역;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        강남역 = StationApi.createStationApi("강남역").jsonPath().getLong("id");
        신논현역 = StationApi.createStationApi("신논현역").jsonPath().getLong("id");
        정자역 = StationApi.createStationApi("정자역").jsonPath().getLong("id");
        판교역 = StationApi.createStationApi("판교역").jsonPath().getLong("id");
    }

    /**
     * Given 지하철 노선을 생성하고
     * When 지하철 구간을 추가하면
     * Then 지하철 노선 조회시 추가 된 지하철 역을 조회할 수 있다.
     */
    @DisplayName("지하철 구간 등록")
    @Test
    void addSection() {
        // given
        ExtractableResponse<Response> 신분당선 = LineApi.createLineApi("신분당선", "bg-red-600", 강남역, 신논현역, 10);

        // when
        long 신분당선_번호 = 신분당선.jsonPath().getLong("id");

        LineApi.addSectionApi(신분당선_번호, 신논현역, 정자역, 5);

        // then
        ExtractableResponse<Response> 신분당선_조회_응답 = LineApi.getLineByIdApi(신분당선_번호);
        List<String> stationNames = 신분당선_조회_응답.jsonPath().getList("stations.name", String.class);

        assertAll(
                () -> assertThat(stationNames).hasSize(3),
                () -> assertThat(stationNames).containsAnyOf("강남역", "신논현역", "정자역")
        );
    }

    /**
     * Given 지하철 노선을 생성하고
     * When 기존 노선의 종점역과 새로운 노선의 상행성이 일치하지 않도록 추가하면
     * Then 지하철 구간 등록은 예외를 발생시킨다.
     */
    @DisplayName("지하철 구간 등록 예외(하행역과 상행역)")
    @Test
    void addSectionExceptionUnmatchedException() {
        // given
        ExtractableResponse<Response> 신분당선 = LineApi.createLineApi("신분당선", "bg-red-600", 강남역, 신논현역, 10);

        // when
        long 신분당선_번호 = 신분당선.jsonPath().getLong("id");

        ExtractableResponse<Response> 구간_등록_응답 = LineApi.addSectionApi(신분당선_번호, 정자역, 판교역, 5);

        // then
        String exceptionMessage = 구간_등록_응답.jsonPath().getString("message");
        assertAll(
                () -> assertThat(구간_등록_응답.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(exceptionMessage).isEqualTo("기존 노선의 종점역과 신규 노선의 상행역이 일치하지 않습니다.")
        );
    }

    /**
     * Given 지하철 노선을 생성하고
     * When 새로운 노선이 기존 노선의 역에 등록되어 있을 때 추가하면
     * Then 지하철 노선은 정상적으로 추가되지 않는다.
     */
    @DisplayName("지하철 구간 등록 예외(이미 존재하는 역 등록)")
    @Test
    void addSectionExceptionAlreadyExistsStationException() {
        // given
        ExtractableResponse<Response> 신분당선 = LineApi.createLineApi("신분당선", "bg-red-600", 강남역, 신논현역, 10);

        // when
        long 신분당선_번호 = 신분당선.jsonPath().getLong("id");

        ExtractableResponse<Response> 구간_등록_응답 = LineApi.addSectionApi(신분당선_번호, 신논현역, 강남역, 5);

        // then
        String exceptionMessage = 구간_등록_응답.jsonPath().getString("message");
        assertAll(
                () -> assertThat(구간_등록_응답.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(exceptionMessage).isEqualTo("신규 구간의 하행역이 기존 노선의 역에 이미 등록되어 있습니다.")
        );
    }

    /**
     * Given 지하철 노선을 생성하고
     * Given 지하철 구간을 추가하고
     * When 노선의 마지막 구간을 제거하면
     * Then 지하철 노선 조회시 해당 구간 정보는 삭제된다.
     */
    @DisplayName("지하철 구간 삭제")
    @Test
    void deleteSection() {
        // given
        ExtractableResponse<Response> 신분당선 = LineApi.createLineApi("신분당선", "bg-red-600", 강남역, 신논현역, 10);

        // given
        long 신분당선_번호 = 신분당선.jsonPath().getLong("id");
        LineApi.addSectionApi(신분당선_번호, 신논현역, 정자역, 5);

        // when
        LineApi.deleteSectionApi(신분당선_번호, 정자역);

        // then
        ExtractableResponse<Response> 신분당선_조회_응답 = LineApi.getLineByIdApi(신분당선_번호);

        List<String> stationNames = 신분당선_조회_응답.jsonPath().getList("stations.name", String.class);

        assertAll(
                () -> assertThat(stationNames).hasSize(2),
                () -> assertThat(stationNames).containsAnyOf("강남역", "신논현역")
        );
    }

    /**
     * Given 지하철 노선을 생성하고
     * Given 지하철 구간을 추가하고
     * When 구간에 존재하지 않는 역을 삭제하면
     * Then 지하철 구간 삭제 오류가 발생한다.
     */
    @DisplayName("지하철 구간 삭제 예외(존재하지 않는 역 삭제)")
    @Test
    void deleteSectionWhenNotExistsStation() {
        // given
        ExtractableResponse<Response> 신분당선 = LineApi.createLineApi("신분당선", "bg-red-600", 강남역, 신논현역, 10);

        // given
        long 신분당선_번호 = 신분당선.jsonPath().getLong("id");
        LineApi.addSectionApi(신분당선_번호, 신논현역, 정자역, 5);

        // when
        ExtractableResponse<Response> 구간_삭제_응답 = LineApi.deleteSectionApi(신분당선_번호, 판교역);

        // then
        String exceptionMessage = 구간_삭제_응답.jsonPath().getString("message");

        assertAll(
                () -> assertThat(구간_삭제_응답.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(exceptionMessage).isEqualTo("삭제하려는 역이 노선에 등록되지 않은 역이거나, 마지막 구간의 역이 아닙니다.")
        );
    }

    /**
     * Given 지하철 노선을 생성하고
     * When 지하철 구간을 삭제하면
     * Then 지하철 구간 삭제 오류가 발생한다.
     */
    @DisplayName("지하철 구간 삭제 오류(구간이 1개인 노선)")
    @Test
    void deleteSectionOnlyOneSectionException() {
        // given
        ExtractableResponse<Response> 신분당선 = LineApi.createLineApi("신분당선", "bg-red-600", 강남역, 신논현역, 10);

        // when
        long 신분당선_번호 = 신분당선.jsonPath().getLong("id");
        ExtractableResponse<Response> 구간_삭제_응답 = LineApi.deleteSectionApi(신분당선_번호, 신논현역);

        // then
        String exceptionMessage = 구간_삭제_응답.jsonPath().getString("message");
        assertAll(
                () -> assertThat(구간_삭제_응답.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(exceptionMessage).isEqualTo("구간이 1개인 노선은 구간 삭제를 진행할 수 없습니다.")
        );
    }
}