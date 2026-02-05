package igrus.web.event.exception;

/**
 * 잘못된 행사 정원 설정 시 발생하는 예외.
 */
public class InvalidEventCapacityException extends RuntimeException {

    private final Integer capacity;

    public InvalidEventCapacityException(Integer capacity) {
        super(String.format("행사 정원은 1명 이상이어야 합니다. 입력값: %d", capacity));
        this.capacity = capacity;
    }

    public Integer getCapacity() {
        return capacity;
    }
}
