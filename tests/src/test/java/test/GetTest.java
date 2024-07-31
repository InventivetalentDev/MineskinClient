package test;

import com.google.gson.Gson;
import org.junit.Test;
import org.mineskin.JsoupRequestHandler;
import org.mineskin.MineSkinRequestException;
import org.mineskin.MineSkinClient;
import org.mineskin.response.GetSkinResponse;

import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class GetTest {

    private final MineSkinClient client = new MineSkinClient(new JsoupRequestHandler("MineskinJavaClient-Test", null, 3000, new Gson()), Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor());

    @Test
    public void getUuid() {
        GetSkinResponse res = client.getSkinByUuid("8cadf501765e412fbdfa1a3fa9a87710").join();
        assertTrue(res.isSuccess());
        assertEquals(200, res.getStatus());
        assertNotNull(res.getBody());
        assertEquals("8cadf501765e412fbdfa1a3fa9a87710",res.getBody().uuid());
    }

    @Test
    public void getUuidNotFound() {
        CompletionException root = assertThrows(CompletionException.class, () -> client.getSkinByUuid("8cadf501765e412fbdfa1a3fa9a87711").join());
        System.out.println(root.getCause());
        assertTrue(root.getCause() instanceof MineSkinRequestException);
        MineSkinRequestException exception = (MineSkinRequestException) root.getCause();
        assertEquals("Skin not found", exception.getMessage());
        assertNotNull(exception.getResponse());
        assertFalse(exception.getResponse().isSuccess());
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals("Skin not found", exception.getResponse().getError().orElse(null));
    }


}
