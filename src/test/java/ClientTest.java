import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;

public class ClientTest {

    private final String _token = "xd9mXX4H-fy-OXUIsZ7WWgWQHmiKI3RNORaXj19bajYIAx9UA4Hps-Rs8gnP6TrdtSA9HDKV1FHtHGH1XLhKyQ==";
    private final String _bucket = "testBucket";
    private final String _org = "testOrg";
    private final String _influxDbUrl = "http://localhost:8086";
    private final InfluxDBClient _client = InfluxDBClientFactory.create(_influxDbUrl, _token.toCharArray());

    private String _hostname;

    @BeforeEach
    void setUp() throws UnknownHostException {
        _hostname = java.net.InetAddress.getLocalHost().getHostName();
    }

    /**
     * Query for InfluxDb:
     *
     * from(bucket: "testBucket")
     *   |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
     *   |> filter(fn: (r) => r["_measurement"] == "order")
     *   |> filter(fn: (r) => r["progress"] == "FINISHED_SUCCESSFULLY")
     *   |> aggregateWindow(every: 1h, fn: count)
     *
     * @param orderEvent
     */
    @ParameterizedTest
    @MethodSource("provideOrderEvents")
    void insertOrdersUsingPojo(OrderEvent orderEvent) {
        OrderPojo orderPojo = new OrderPojo();
        orderPojo.time = orderEvent.getEventTime();
        orderPojo.host = _hostname;
        orderPojo.orderId = orderEvent.getOrderId();
        orderPojo.progress = orderEvent.getOrderProgress();

        try (WriteApi writeApi = _client.getWriteApi()) {
            writeApi.writeMeasurement(_bucket, _org, WritePrecision.NS, orderPojo);
        }
    }

    private static Stream<OrderEvent> provideOrderEvents() {
        UUID id0 = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        return Stream.of(
                new OrderEvent(new Order(id0, Progress.NEW), Instant.now().minus(15, ChronoUnit.MINUTES)),
                new OrderEvent(new Order(id1, Progress.NEW), Instant.now().minus(15, ChronoUnit.MINUTES)),
                new OrderEvent(new Order(id2, Progress.NEW), Instant.now().minus(15, ChronoUnit.MINUTES)),

                new OrderEvent(new Order(id0, Progress.EXECUTING), Instant.now().minus(14, ChronoUnit.MINUTES)),
                new OrderEvent(new Order(id1, Progress.EXECUTING), Instant.now().minus(14, ChronoUnit.MINUTES)),
                new OrderEvent(new Order(id2, Progress.EXECUTING), Instant.now().minus(14, ChronoUnit.MINUTES)),

                new OrderEvent(new Order(id0, Progress.EXECUTING_REQUIRES_ATTENTION), Instant.now().minus(10, ChronoUnit.MINUTES)),
                new OrderEvent(new Order(id1, Progress.FINISHED_SUCCESSFULLY), Instant.now().minus(10, ChronoUnit.MINUTES)),
                new OrderEvent(new Order(id2, Progress.FINISHED_WITH_ERROR), Instant.now().minus(10, ChronoUnit.MINUTES)),

                new OrderEvent(new Order(id0, Progress.EXECUTING), Instant.now().minus(5, ChronoUnit.MINUTES)),

                new OrderEvent(new Order(id0, Progress.FINISHED_SUCCESSFULLY), Instant.now())
        );
    }

    private static class OrderEvent {
        private final Order _order;
        private final Instant _time;

        OrderEvent(Order order, Instant time) {
            _order = order;
            _time = time;
        }

        public UUID getOrderId() {
            return _order.getId();
        }

        public Progress getOrderProgress() {
            return _order.getProgress();
        }

        public Instant getEventTime() {
            return _time;
        }
    }

    @Measurement(name = "order")
    public static class OrderPojo {
        /**
         * Point in time of the event we're recording.
         */
        @Column(timestamp = true)
        Instant time;
        /**
         * Meta-info, so we can store and filter events from different AIVI instances.
         * Maybe we also want to add AIVI version as meta-info in the future?
         */
        @Column(tag = true)
        String host;
        /**
         * Progress of the order at the time of the data point.
         */
        @Column(tag = true)
        Progress progress;
        /**
         * Order ID, so we can group events of a single order.
         * This enables us to calculate stuff like average execution time of orders.
         * However, highly variable information like UUIDs should not be stored as tags (tags are all indexed, with
         * highly variable information, index memory requirements explode).
         */
        @Column()
        UUID orderId;
    }
}
