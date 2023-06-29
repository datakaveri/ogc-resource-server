package ogc.rs.apiserver.exceptions;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class DxRuntimeExceptionTest {
    @Test
    @DisplayName("Test something during runtime")
    public void getStatusCode(VertxTestContext vertxTestContext){
        vertxTestContext.completeNow();
    }
}
