package ogc.rs.dummy;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DummyServiceImpl implements DummyService {

    private static final Logger LOGGER = LogManager.getLogger(DummyServiceImpl.class);

    @Override
    public Future<JsonObject> dummyTokenIntrospect(JsonObject request, JsonObject authenticationInfo) {
        LOGGER.debug("Inside DummyImpl");

        int n1=0,n2=1,n3=0,i,count=100;
        System.out.print("n1 "+n1+" n2 "+n2);//printing 0 and 1

        for(i=2;i<count;++i)//loop starts from 2 because 0 and 1 are already printed
        {
            n3=n1+n2;
            System.out.print(" n3 "+n3);
            n1=n2;
            n2=n3;
        }
        LOGGER.debug("FIBBO "+n3);

        return Future.succeededFuture(new JsonObject());
    }
}
