package iot.challenge.jura.firma.service.provider.transfer;

public class Dated<T> implements Comparable<Dated<T>> {
	final Long time;
	final T element;

	public Dated(Long time, T element) {
		this.time = time;
		this.element = element;
	}

	public Long getTime() {
		return time;
	}

	public T getElement() {
		return element;
	}

	@Override
	public int compareTo(Dated<T> other) {
		return time.compareTo(other.time);
	}
}
