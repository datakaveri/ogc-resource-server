package ogc.rs.authenticator;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class JwtAuthServiceTest {
    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext)
    {
        // TODO: add setup
    }
}
