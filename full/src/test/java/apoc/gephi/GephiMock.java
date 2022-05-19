package apoc.gephi;

import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.RegexBody;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.model.RegexBody.regex;

public record GephiMock(ClientAndServer server) {

    public static GephiMock createAndStart() {
        return new GephiMock(ClientAndServer.startClientAndServer(8080));
    }

    public void clearAllExpectations() {
        this.server.reset();
    }

    public void shutdown() {
        this.server.stop();
    }

    public void mockSuccess(String workspace, GephiEntity... entities) {
        new MockServerClient("localhost", 8080)
            .when(request()
                .withMethod("POST")
                .withPath("/" + workspace)
                .withQueryStringParameter(param("operation", "updateGraph"))
                .withHeader(header("Content-Type", "application/json; charset=utf-8"))
                .withBody(bodyMatcher(entities))
            ).respond(response()
                .withStatusCode(201)
            );
    }

    /**
     * https://5-2.mock-server.com/mock_server/creating_expectations_request_matchers.html
     * We had to chose to go with the RegEx body matcher rather than the String or JSON or JSONSchema matchers because:
     * - the JSON that Gephi accepts is not valid JSON (JSON or JSONSchema doesn't work)
     * - the JSON that we send to Gephi contains random values which change from run to run (String doesn't work)
     */
    private RegexBody bodyMatcher(GephiEntity... entities) {
        return regex(
                Arrays.stream(entities)
                .map(GephiEntity::toRegexPattern)
                .collect(Collectors.joining("\r\n")));
    }

    sealed interface GephiEntity {
        String toRegexPattern();

        default String numberPattern() {
            return "-?[\\d\\.]+";
        }
    }

    record Node(int id, String label) implements GephiEntity {
        @Override
        public String toRegexPattern() {
            return String.format(
                "\\{\"an\":\\{\"%d\":\\{\"label\":\"%s\",\"TYPE\":\"%s\",\"size\":10,\"x\":%s,\"y\":%s,\"r\":%s,\"g\":%s,\"b\":%s\\}\\}\\}",
                id, label, label, numberPattern(), numberPattern(), numberPattern(), numberPattern(), numberPattern());
        }

        public static Node node(int id, String label) {
            return new Node(id, label);
        }
    }

    record Relationship(int id, String label, int source, int target, String weight, Set<String> properties) implements GephiEntity {
        private String propertiesPattern() {
            final var joined = properties.stream()
                .map(property -> String.format("\"%s\":\"%s\"", property, property))
                .collect(Collectors.joining(","));

            return properties.isEmpty() ? "" : String.format(",%s", joined);
        }

        @Override
        public String toRegexPattern() {
            return String.format(
                "\\{\"ae\":\\{\"%d\":\\{\"label\":\"%s\",\"TYPE\":\"%s\",\"source\":\"%d\",\"target\":\"%d\",\"directed\":true,\"weight\":%s,\"r\":%s,\"g\":%s,\"b\":%s%s\\}\\}\\}",
                id, label, label, source, target, weight, numberPattern(), numberPattern(), numberPattern(), propertiesPattern());
        }

        public static Relationship relationship(int id, String label, int source, int target, String weight, Set<String> properties) {
            return new Relationship(id, label, source, target, weight, properties);
        }

        public static Relationship relationship(int id, String label, int source, int target, String weight) {
            return relationship(id, label, source, target, weight, new HashSet<>());
        }

        public static Relationship relationship(int id, String label, int source, int target) {
            return relationship(id, label, source, target, "1.0");
        }
    }
}
