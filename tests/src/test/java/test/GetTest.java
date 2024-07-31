package test;

import org.junit.Test;
import org.mineskin.JsoupRequestHandler;
import org.mineskin.MineSkinClient;
import org.mineskin.MineSkinClientImpl;
import org.mineskin.data.ExistingSkin;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.response.GetSkinResponse;

import java.util.concurrent.CompletionException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import static org.junit.Assert.*;

public class GetTest {

    private static final MineSkinClient CLIENT = MineSkinClient.builder()
            .requestHandler(JsoupRequestHandler::new)
            .userAgent("MineSkinClient/Tests")
            .build();

    static {
        MineSkinClientImpl.LOGGER.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        MineSkinClientImpl.LOGGER.addHandler(handler);
    }

    @Test
    public void getUuid() {
        GetSkinResponse res = CLIENT.getSkinByUuid("8cadf501765e412fbdfa1a3fa9a87710").join();
        System.out.println(res);
        assertTrue(res.isSuccess());
        assertEquals(200, res.getStatus());
        assertNotNull(res.getSkin());
        assertTrue(res.getSkin() instanceof ExistingSkin);
        assertEquals("8cadf501765e412fbdfa1a3fa9a87710",res.getBody().uuid());
    }

    @Test
    public void getUuidNotFound() {
        CompletionException root = assertThrows(CompletionException.class, () -> CLIENT.getSkinByUuid("8cadf501765e412fbdfa1a3fa9a87711").join());
        assertTrue(root.getCause() instanceof MineSkinRequestException);
        MineSkinRequestException exception = (MineSkinRequestException) root.getCause();
        assertEquals("Skin not found", exception.getMessage());
        assertNotNull(exception.getResponse());
        assertFalse(exception.getResponse().isSuccess());
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals("Skin not found", exception.getResponse().getError().orElse(null));
    }


}
