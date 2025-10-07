package test;

import org.junit.jupiter.api.Test;
import org.mineskin.Java11RequestHandler;
import org.mineskin.MineSkinClient;
import org.mineskin.MineSkinClientImpl;
import org.mineskin.data.User;
import org.mineskin.response.UserResponse;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class MiscTest {

    static {
        MineSkinClientImpl.LOGGER.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        MineSkinClientImpl.LOGGER.addHandler(handler);
    }

    private static final MineSkinClient JAVA11 = MineSkinClient.builder()
            .requestHandler(Java11RequestHandler::new)
            .userAgent("MineSkinClient-Java11/Tests")
            .apiKey(System.getenv("MINESKIN_API_KEY"))
            .build();

    @Test
    public void getUserTest() {
        MineSkinClient client = JAVA11;
        UserResponse userResponse = client.misc().getUser().join();
        System.out.println(userResponse);
        User user = userResponse.getUser();
        System.out.println(user);
        int concurrency = user.grants().concurrency().orElseThrow();
        assertTrue(concurrency > 5);
        int perMinute = user.grants().perMinute().orElseThrow();
        assertTrue(perMinute > 50);
    }

}
