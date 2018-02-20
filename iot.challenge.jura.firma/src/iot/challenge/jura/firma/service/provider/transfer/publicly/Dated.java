package iot.challenge.jura.firma.service.provider.transfer.publicly;

public class Dated<T> implements Comparable<Dated<T>> {
	final Long time;
	final T element;

	public Dated(Long time, T element) {
		this.time = time;
		this.element = element;
	}

	@Override
	public int compareTo(Dated<T> other) {
		return time.compareTo(other.time);
	}
}
