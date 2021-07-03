import java.util.UUID;

public class Order {

    private final UUID _id;
    private final Progress _progress;

    public Order(UUID id, Progress progress) {
        _id = id;
        _progress = progress;
    }

    public UUID getId() {
        return _id;
    }

    public Progress getProgress() {
        return _progress;
    }
}
