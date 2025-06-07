package test;

import com.google.common.collect.Lists;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mineskin.*;
import org.mineskin.data.CodeAndMessage;
import org.mineskin.data.Visibility;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.response.SkinResponse;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class GetTest {

    static {
        MineSkinClientImpl.LOGGER.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        MineSkinClientImpl.LOGGER.addHandler(handler);
    }

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

    private static Stream<Arguments> clients() {
        return Stream.of(
                Arguments.of(APACHE),
                Arguments.of(JSOUP),
                Arguments.of(JAVA11)
        );
    }

    private static Stream<Arguments> clientsAndSkinIds() {
        List<MineSkinClient> clients = List.of(APACHE, JSOUP, JAVA11);
        List<String> skinIds = List.of(
                "c1a1982831874868a37f5be375d38d5b",
                "9c6409112aae4c7fb4f5d026a60dcdaf",
                "7117a52ab80d447d887179609a4bb00c"
        );
        List<List<Object>> lists = Lists.cartesianProduct(clients, skinIds);
        return lists.stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("clientsAndSkinIds")
    public void getUuid(List<?> args) {
        MineSkinClient client = (MineSkinClient) args.get(0);
        String skinId = (String) args.get(1);
        SkinResponse res = client.skins().get(skinId).join();
        System.out.println(res);
        assertTrue(res.isSuccess());
        assertEquals(200, res.getStatus());
        assertNotNull(res.getSkin());
        assertEquals(skinId, res.getSkin().uuid());
        assertEquals(Visibility.UNLISTED, res.getSkin().visibility());
    }

    //
    @ParameterizedTest
    @MethodSource("clients")
    public void getUuidNotFound(MineSkinClient client) {
        CompletionException root = assertThrows(CompletionException.class, () -> client.skins().get("8cadf501765e412fbdfa1a3fa9a87711").join());
        assertInstanceOf(MineSkinRequestException.class, root.getCause());
        MineSkinRequestException exception = (MineSkinRequestException) root.getCause();
        assertEquals("Skin not found", exception.getMessage());
        assertNotNull(exception.getResponse());
        assertFalse(exception.getResponse().isSuccess());
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals("Skin not found", exception.getResponse().getFirstError().map(CodeAndMessage::message).orElse(null));
    }


}
