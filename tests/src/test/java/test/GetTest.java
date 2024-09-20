package test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mineskin.*;
import org.mineskin.data.ExistingSkin;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.response.GetSkinResponse;

import java.util.concurrent.CompletionException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class GetTest {

    private static final MineSkinClient APACHE = MineSkinClient.builder()
            .requestHandler(ApacheRequestHandler::new)
            .userAgent("MineSkinClient-Apache/Tests")
            .apiKey(System.getenv("MINESKIN_API_KEY"))
            .build();
    private static final MineSkinClient JSOUP = MineSkinClient.builder()
            .requestHandler(JsoupRequestHandler::new)
            .userAgent("MineSkinClient-Jsoup/Tests")
            .apiKey(System.getenv("MINESKIN_API_KEY"))
            .build();
    private static final MineSkinClient JAVA11 = MineSkinClient.builder()
            .requestHandler(Java11RequestHandler::new)
            .userAgent("MineSkinClient-Java11/Tests")
            .apiKey(System.getenv("MINESKIN_API_KEY"))
            .build();
    static {
        MineSkinClientImpl.LOGGER.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        MineSkinClientImpl.LOGGER.addHandler(handler);
    }

    private static Stream<Arguments> clients() {
        return Stream.of(
                Arguments.of(APACHE),
                Arguments.of(JSOUP),
                Arguments.of(JAVA11)
        );
    }

    @ParameterizedTest
    @MethodSource("clients")
    public void getUuid(MineSkinClient client) {
        GetSkinResponse res = client.getSkinByUuid("8cadf501765e412fbdfa1a3fa9a87710").join();
        System.out.println(res);
        assertTrue(res.isSuccess());
        assertEquals(200, res.getStatus());
        assertNotNull(res.getSkin());
        assertInstanceOf(ExistingSkin.class, res.getSkin());
        assertEquals("8cadf501765e412fbdfa1a3fa9a87710",res.getBody().uuid());
    }

    @ParameterizedTest
    @MethodSource("clients")
    public void getUuidNotFound(MineSkinClient client) {
        CompletionException root = assertThrows(CompletionException.class, () -> client.getSkinByUuid("8cadf501765e412fbdfa1a3fa9a87711").join());
        assertInstanceOf(MineSkinRequestException.class, root.getCause());
        MineSkinRequestException exception = (MineSkinRequestException) root.getCause();
        assertEquals("Skin not found", exception.getMessage());
        assertNotNull(exception.getResponse());
        assertFalse(exception.getResponse().isSuccess());
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals("Skin not found", exception.getResponse().getError().orElse(null));
    }


}
