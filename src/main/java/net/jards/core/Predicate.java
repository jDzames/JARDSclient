package net.jards.core;

import java.util.*;

public abstract class Predicate {

	/**
	 * Returns set of name of document properties that are used by the
	 * predicate.
	 * 
	 * @return the set of names of document properties.
	 */
	public Set<String> getProperties() {
		return Collections.emptySet();
	}

	/**
	 * Matches the document against the predicated.
	 * 
	 * @param document
	 *            the document.
	 * @return true, if the document matches the predicates, false otherwise.
	 */
	public abstract boolean match(Document document);

	public static final class And extends Predicate {
		private final List<Predicate> predicates;

		public And(Predicate... predicates) {
			if (predicates.length < 2) {
				throw new IllegalArgumentException("At least two predicates must occur in a conjunction.");
			}

			for (Predicate p : predicates) {
				if (p == null) {
					throw new NullPointerException("Null value is not allowed.");
				}
			}

			this.predicates = new ArrayList<>(predicates.length);
			for (Predicate p : predicates) {
				if (p == null) {
					throw new NullPointerException("Null predicate is not allowed.");
				}

				this.predicates.add(p);
			}
		}

		@Override
		public Set<String> getProperties() {
			HashSet<String> result = new HashSet<>();
			for (Predicate p : predicates) {
				result.addAll(p.getProperties());
			}

			if (result.isEmpty()) {
				return Collections.emptySet();
			} else {
				return result;
			}
		}

		@Override
		public boolean match(Document document) {
			for (Predicate p : predicates) {
				if (!p.match(document)) {
					return false;
				}
			}

			return true;
		}

		public List<Predicate> getSubPredicates() {
			return Collections.unmodifiableList(predicates);
		}
	}

	public static final class Or extends Predicate {

		private final List<Predicate> predicates;

		public Or(Predicate... predicates) {
			if (predicates.length < 2) {
				throw new IllegalArgumentException("At least two predicates must occur in a disjunction.");
			}

			this.predicates = new ArrayList<>(predicates.length);
			for (Predicate p : predicates) {
				if (p == null) {
					throw new NullPointerException("Null predicate is not allowed.");
				}

				this.predicates.add(p);
			}
		}

		@Override
		public Set<String> getProperties() {
			HashSet<String> result = new HashSet<>();
			for (Predicate p : predicates) {
				result.addAll(p.getProperties());
			}

			if (result.isEmpty()) {
				return Collections.emptySet();
			} else {
				return result;
			}
		}

		@Override
		public boolean match(Document document) {
			for (Predicate p : predicates) {
				if (p.match(document)) {
					return true;
				}
			}

			return false;
		}

		public List<Predicate> getSubPredicates() {
			return Collections.unmodifiableList(predicates);
		}
	}

	public static final class Equals extends Predicate {

		private final String property;
		private final Object value;

		private Equals(String property, Object value) {
			this.property = property;
			this.value = value;
			if (value == null) {
				throw new NullPointerException("Value cannot be null.");
			}
		}

		public Equals(String property, String value) {
			this(property, (Object) value);
		}

		public Equals(String property, Number value) {
			this(property, (Object) value);
		}

		public Equals(String property, Boolean value) {
			this(property, (Object) value);
		}

		@Override
		public Set<String> getProperties() {
			return Collections.singleton(property);
		}

		@Override
		public boolean match(Document document) {
			if (document == null) {
				return false;
			}

			return value.equals(document.getPropertyValue(property));
		}
	}

	// Vymazat - len ako demo
	public static void main(String[] args) {
		Predicate p = new And(new Equals("$.name", "Janko"), new Equals("$.age", 20));
	}
}
