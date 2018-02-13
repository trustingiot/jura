package iot.challenge.jura.firma.service.provider.transferlocation;

import java.util.HashMap;
import java.util.Map;

public enum Rule {
	Default {
		@Override
		public <T> int compare(Dated<T> a, Dated<T> b) {
			return a.compareTo(b);
		}
	},
	Reverse {
		@Override
		public <T> int compare(Dated<T> a, Dated<T> b) {
			return a.compareTo(b) * -1;
		}
	};

	private static Map<Integer, Rule> rules;

	public static Map<Integer, Rule> getRules() {
		if (rules == null) {
			rules = new HashMap<>();
			rules.put(0, Rule.Default); // Never seen
			rules.put(1, Rule.Reverse); // Seen
		}
		return rules;
	}

	public static Rule get(int level) {
		return getRules().get(level);
	}

	public static <T> int compare(Rule r, Dated<T> a, Dated<T> b) {
		return r.compare(a, b);
	}

	public abstract <T> int compare(Dated<T> a, Dated<T> b);
}
