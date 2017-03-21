package net.jards.core;

import net.jards.errors.JsonFormatException;

import java.util.*;

import static net.jards.core.Predicate.Operator.*;

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

            try {
                return value.equals(document.getPropertyValue(property));
            } catch (JsonFormatException e) {
                return false;
            }
        }

        public Object getValue() {
            return value;
        }
    }

    public enum Operator{
        SameAs,
        NotSameAs,
        Bigger,
        BiggerOrEquals,
        Smaller,
        SmallerOrEquals
    }

    public static final class Compare extends Predicate {

        private final String property;
        private final Operator operator;
        private final Object value;

        private Compare(String property, Operator operator, Object value) {
            this.property = property;
            this.operator = operator;
            this.value = value;
            if (value == null) {
                throw new NullPointerException("Value cannot be null.");
            }
        }

        public Compare(String property, Operator operator, String value) {
            this(property, operator, (Object) value);
        }

        public Compare(String property, Operator operator, Number value) {
            this(property, operator, (Object) value);
        }

        public Compare(String property, Operator operator, Boolean value) {
            this(property, operator, (Object) value);
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
            try {
                if (operator == SameAs){
                    return value.equals(document.getPropertyValue(property));
                } else if (operator == NotSameAs){
                    return !value.equals(document.getPropertyValue(property));
                } else if (operator == Bigger){
                    if (value instanceof String){
                        //property > value (compareTo gives >0), but we compare in other order
                        return ((String)value).compareTo((String)document.getPropertyValue(property)) < 0;
                    } else if (value instanceof Number){
                        return ((Number)value).doubleValue() < ((Number)document.getPropertyValue(property)).doubleValue();
                    } else return false;
                } else if (operator == BiggerOrEquals){
                    if (value instanceof String){
                        return ((String)value).compareTo((String)document.getPropertyValue(property)) <= 0;
                    } else if (value instanceof Number){
                        return ((Number)value).doubleValue() <= ((Number)document.getPropertyValue(property)).doubleValue();
                    } else return false;
                } else if (operator == Smaller){
                    if (value instanceof String){
                        return ((String)value).compareTo((String)document.getPropertyValue(property)) > 0;
                    } else if (value instanceof Number){
                        return ((Number)value).doubleValue() > ((Number)document.getPropertyValue(property)).doubleValue();
                    } else return false;
                } else if (operator == SmallerOrEquals){
                    if (value instanceof String){
                        //property > value (compareTo gives >0), but we compare in other order
                        return ((String)value).compareTo((String)document.getPropertyValue(property)) > 0;
                    } else if (value instanceof Number){
                        return ((Number)value).doubleValue() >= ((Number)document.getPropertyValue(property)).doubleValue();
                    } else return false;
                }
                return false;
            } catch (JsonFormatException e) {
                return false;
            }
        }

        public Object getValue() {
            return value;
        }

        public Operator getOperator() {
            return operator;
        }
    }

    public static final class EqualProperties extends Predicate {

        private final String property;
        private final String property2;

        public EqualProperties(String property, String property2) {
            this.property = property;
            this.property2 = property2;
        }

        @Override
        public Set<String> getProperties() {
            Set<String> result = new HashSet<>();
            result.add(property);
            result.add(property2);
            return result;
        }

        @Override
        public boolean match(Document document) {
            if (document == null || this.property2 == null) {
                return false;
            }

            try {
                return property2.equals(document.getPropertyValue(property));
            } catch (JsonFormatException e) {
                return false;
            }
        }
    }

    public static final class CompareProperties extends Predicate {

        private final String property;
        private final Operator operator;
        private final String property2;

        private CompareProperties(String property, Operator operator, String property2) {
            this.property = property;
            this.operator = operator;
            this.property2 = property2;
        }


        @Override
        public Set<String> getProperties() {
            Set<String> result = new HashSet<>();
            result.add(property);
            result.add(property2);
            return result;
        }

        @Override
        public boolean match(Document document) {
            if (document == null || this.property2 == null) {
                return false;
            }
            try {
                Object property2Object = document.getPropertyValue(property2);

                if (operator == SameAs){
                    return property2Object.equals(document.getPropertyValue(property));
                } else if (operator == NotSameAs){
                    return !property2Object.equals(document.getPropertyValue(property));
                } else if (operator == Bigger){
                    if (property2Object instanceof String){
                        //property > value (compareTo gives >0), but we compare in other order
                        return ((String)property2Object).compareTo((String)document.getPropertyValue(property)) < 0;
                    } else if (property2Object instanceof Number){
                        return ((Number)property2Object).doubleValue() < ((Number)document.getPropertyValue(property)).doubleValue();
                    } else return false;
                } else if (operator == BiggerOrEquals){
                    if (property2Object instanceof String){
                        return ((String)property2Object).compareTo((String)document.getPropertyValue(property)) <= 0;
                    } else if (property2Object instanceof Number){
                        return ((Number)property2Object).doubleValue() <= ((Number)document.getPropertyValue(property)).doubleValue();
                    } else return false;
                } else if (operator == Smaller){
                    if (property2Object instanceof String){
                        return ((String)property2Object).compareTo((String)document.getPropertyValue(property)) > 0;
                    } else if (property2Object instanceof Number){
                        return ((Number)property2Object).doubleValue() > ((Number)document.getPropertyValue(property)).doubleValue();
                    } else return false;
                } else if (operator == SmallerOrEquals){
                    if (property2Object instanceof String){
                        //property > value (compareTo gives >0), but we compare in other order
                        return ((String)property2Object).compareTo((String)document.getPropertyValue(property)) > 0;
                    } else if (property2Object instanceof Number){
                        return ((Number)property2Object).doubleValue() >= ((Number)document.getPropertyValue(property)).doubleValue();
                    } else return false;
                }
                return false;
            } catch (JsonFormatException e) {
                return false;
            }
        }

        public Operator getOperator() {
            return operator;
        }
    }

	//compare: bigger, less, bigger or equals, less or equals
    //equal properties , compare properties

}
