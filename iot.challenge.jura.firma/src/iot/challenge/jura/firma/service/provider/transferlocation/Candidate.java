package iot.challenge.jura.firma.service.provider.transferlocation;

public class Candidate<T> implements Comparable<Candidate<T>> {
	final Integer level;
	final Dated<T> dated;

	public Candidate(Integer level, Dated<T> dated) {
		this.level = level;
		this.dated = dated;
	}

	@Override
	public int compareTo(Candidate<T> other) {
		if (level < other.level)
			return 1;

		if (level > other.level)
			return -1;

		return Rule.get(level).compare(this.dated, other.dated);
	}
}
