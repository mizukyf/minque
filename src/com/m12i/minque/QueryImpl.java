package com.m12i.minque;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

final class QueryImpl<E> implements Query<E> {
	private final Expression expression;
	private final Accessor<E> accessor;
	private final Placeholders ph;
	private final boolean hasPlaceholders;
	public QueryImpl(Expression expression, final Placeholders ph, Accessor<E> accessor) {
		this.expression = expression;
		this.accessor = accessor;
		this.ph = ph;
		this.hasPlaceholders = ph.amount() > 0;
	}

	@Override
	public List<E> selectFrom(Iterable<E> source) {
		if (hasPlaceholders) {
			throw new IllegalArgumentException("Bind variables is required for this query.");
		}
		final List<E> result = new ArrayList<E>();
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				result.add(elem);
			}
		}
		return result;
	}

	@Override
	public List<E> selectFrom(Iterable<E> source, Object... vars) {
		ph.bind(vars);
		final List<E> result = new ArrayList<E>();
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				result.add(elem);
			}
		}
		return result;
	}

	@Override
	public int countFrom(Iterable<E> source) {
		if (hasPlaceholders) {
			throw new IllegalArgumentException("Bind variables is required for this query.");
		}
		int result = 0;
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				result ++;
			}
		}
		return result;
	}

	@Override
	public int countFrom(Iterable<E> source, Object... vars) {
		ph.bind(vars);
		int result = 0;
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				result ++;
			}
		}
		return result;
	}

	@Override
	public E selectOneFrom(Iterable<E> source) {
		if (hasPlaceholders) {
			throw new IllegalArgumentException("Bind variables is required for this query.");
		}
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				return elem;
			}
		}
		return null;
	}

	@Override
	public E selectOneFrom(Iterable<E> source, Object... vars) {
		ph.bind(vars);
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				return elem;
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean evaluate(Expression expr, E elem) {
		if (expr.isComparative()) {
			final Object actual = accessor.accsess(elem, expr.getProperty());
			final Operator op = expr.getOperator();
			final String expected = expr.getValue();
			if (op == Operator.IS_NOT_NULL) {
				return actual != null;
			} else if (op == Operator.IS_NULL) {
				return actual == null;
			} else if (actual != null) {
				if (op == Operator.CONTAINS) {
					return actual.toString().contains(expected);
				} else if (op == Operator.ENDS_WITH) {
					return actual.toString().endsWith(expected);
				} else if (op == Operator.EQUALS) {
					return expected.equals(actual.toString());
				} else if (op == Operator.NOT_EQUALS) {
					return !expected.equals(actual.toString());
				} else if (op == Operator.STARTS_WITH) {
					return actual.toString().startsWith(expected);
				} else if (actual instanceof Comparable) {
					try {
						final Comparable c0;
						final Comparable c1;

						if (actual instanceof Integer) {
							c0 = (Integer) actual;
							c1 = Integer.valueOf(expected);
						} else if (actual instanceof Long) {
							c0 = (Long) actual;
							c1 = Long.valueOf(expected);
						} else if (actual instanceof Float) {
							c0 = (Float) actual;
							c1 = Float.valueOf(expected);
						} else if (actual instanceof Double) {
							c0 = (Double) actual;
							c1 = Double.valueOf(expected);
						} else if (actual instanceof Short) {
							c0 = (Short) actual;
							c1 = Short.valueOf(expected);
						} else if (actual instanceof Byte) {
							c0 = (Byte) actual;
							c1 = Byte.valueOf(expected);
						} else if (actual instanceof Character && expected.length() == 1) {
							c0 = (Character) actual;
							c1 = expected.charAt(0);
						} else if (actual instanceof BigDecimal) {
							c0 = (BigDecimal) actual;
							c1 = new BigDecimal(expected);
						} else if (actual instanceof BigInteger) {
							c0 = (BigInteger) actual;
							c1 = new BigInteger(expected);
						} else if (actual instanceof String) {
							c0 = actual.toString();
							c1 = expected;
						} else {
							return false;
						}
						
						if (op == Operator.LESS_THAN) {
							return c0.compareTo(c1) < 0;
						} else if (op == Operator.LESS_THAN_EQUAL) {
							return c0.compareTo(c1) <= 0;
						} else if (op == Operator.GREATER_THAN) {
							return c0.compareTo(c1) > 0;
						} else if (op == Operator.GREATER_THAN_EQUAL) {
							return c0.compareTo(c1) >= 0;
						}
					} catch (final NumberFormatException e) {
						// Do nothing.
					}
				}
			}
			return false;
		} else {
			final Operator op = expr.getOperator();
			if (! expr.hasLeft()) {
				if (op == Operator.NOT) {
					return ! evaluate(expr.getRight(), elem);
				} else {
					throw new RuntimeException("Unsupported logical expression.");
				}
			} else {
				final boolean leftResult = evaluate(expr.getLeft(), elem);
				if (leftResult && op == Operator.OR) {
					return true;
				} else if (!leftResult && op == Operator.AND) {
					return false;
				} else {
					return evaluate(expr.getRight(), elem);
				}
			}
		}
	}
	
}
