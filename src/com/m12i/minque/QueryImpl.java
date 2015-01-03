package com.m12i.minque;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
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

	/**
	 * 式と評価対象のオブジェクトを受け取って評価を行う.
	 * @param expr 式
	 * @param elem 評価対象オブジェクト
	 * @return 評価結果
	 * @throws IllegalArgumentException 未知の演算子や想定外の値が使用された場合
	 */
	@SuppressWarnings({ "rawtypes" })
	private boolean evaluate(Expression expr, E elem) {
		// 式の種類で処理分岐
		if (expr.isComparative()) {
			// 比較式の場合
			
			// 実際のプロパティ値やその他の情報を取得
			final Object actual = accessor.accsess(elem, expr.getProperty());
			final Operator op = expr.getOperator();
			final Object expected = expr.getValue();
			
			// 演算子の種類で処理分岐
			if (op.forNullable) {
				// nullチェック用演算子の場合
				return nullable(actual, op);
				
			} else if (actual != null) {
				// プロパティがnullでない場合
				
				if (op.forObject) {
					// Objectの等価性を比較するための演算子の場合
					return object(actual, expected, op);
					
				} else if (op.forString) {
					// 文字列の包含関係をチェックするための演算子の場合
					return string(actual, expected, op);
					
				} else if (op.forOrdered) {
					// Comparable同士を大小比較するための演算子の場合
					final Comparable[] pair = makeComparablePair(actual, expected);
					return pair[1] != null && ordered(pair[0], pair[1], op);
				}
			}
			
			// 上記条件のいずれにもマッチしないならとにかくfalse
			return false;
			
		} else {
			// 論理式の場合
			
			final Operator op = expr.getOperator();
			
			// 単項演算子かどうかで処理分岐
			if (! expr.hasLeft()) {
				// 単項演算子の場合
				if (op == Operator.NOT) {
					// オペランドの評価結果を反転して返す
					return ! evaluate(expr.getRight(), elem);
					
				} else {
					// 未知の演算子
					throw new RuntimeException("Unsupported logical expression.");
				}
			} else {
				// 二項演算子の場合
				
				// まず左辺オペランドを評価
				final boolean leftResult = evaluate(expr.getLeft(), elem);
				// 続いてその評価結果と演算子の種類で処理分岐
				if (leftResult && op == Operator.OR) {
					// 左辺がtrueで演算子がORなら 右辺を評価するまでもない
					return true;
				} else if (!leftResult && op == Operator.AND) {
					// 左辺がfalseで演算子がANDなら 右辺を評価するまでもない
					return false;
				} else {
					// それ以外の場合 右辺の評価結果をもって論理式の結果とする
					return evaluate(expr.getRight(), elem);
				}
			}
		}
	}
	
	/**
	 * オブジェクトの等価性比較のための演算子で評価を行う.
	 * 期待される値（左辺）が{@link String}インスタンスである場合、
	 * 実際の値は{@link Object#toString()}で文字列に変換された上で評価される。
	 * この不正確な評価を避けるにはバインド変数の使用が必要になる。
	 * @param actual 実際の値（左辺）
	 * @param expected 期待される値（右辺）
	 * @param op 演算子
	 * @return 評価結果
	 * @throws IllegalArgumentException 未知の演算子や想定外の値が使用された場合
	 */
	private boolean object(final Object actual, final Object expected, final Operator op) {
		final boolean asString = expected instanceof String;
		if (op == Operator.EQUALS) {
			return (asString ? actual.toString() : actual).equals(expected);
		} else if (op == Operator.NOT_EQUALS) {
			return !(asString ? actual.toString() : actual).equals(expected);
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * 文字列の包含関係をチェックするための演算子で評価を行う.
	 * @param actual 実際の値（左辺）
	 * @param expected 期待される値（右辺）
	 * @param op 演算子
	 * @return 評価結果
	 * @throws IllegalArgumentException 未知の演算子や想定外の値が使用された場合
	 */
	private boolean string(final Object actual, final Object expected, final Operator op) {
		final String s = actual.toString();
		final String s1 = expected.toString();
		if (op == Operator.CONTAINS) {
			return s.contains(s1);
		} else if (op == Operator.ENDS_WITH) {
			return s.endsWith(s1);
		} else if (op == Operator.STARTS_WITH) {
			return s.startsWith(s1);
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * {@code null}チェックのための演算子で評価を行う.
	 * @param actual 実際の値（左辺）
	 * @param op 演算子
	 * @return 評価結果
	 * @throws IllegalArgumentException 未知の演算子や想定外の値が使用された場合
	 */
	private boolean nullable(final Object actual, final Operator op) {
		if (op == Operator.IS_NULL) {
			return actual == null;
		} else if (op == Operator.IS_NULL) {
			return actual != null;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * {@link Comparable}同士を比較するための演算子で評価を行う.
	 * 評価には{@link Comparable#compareTo(Object)}を使用する。
	 * 評価に際して{@link ClassCastException}が発生した場合このメソッドは{@code false}を返す。
	 * @param actual 実際の値（左辺）
	 * @param expected 期待される値（右辺）
	 * @param op 演算子
	 * @return 評価結果
	 * @throws IllegalArgumentException 未知の演算子や想定外の値が使用された場合
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean ordered(final Comparable actual, final Comparable expected, final Operator op) {
		try {
			if (op == Operator.LESS_THAN) {
				return actual.compareTo(expected) < 0;
			} else if (op == Operator.LESS_THAN_EQUAL) {
				return actual.compareTo(expected) <= 0;
			} else if (op == Operator.GREATER_THAN) {
				return actual.compareTo(expected) > 0;
			} else if (op == Operator.GREATER_THAN_EQUAL) {
				return actual.compareTo(expected) >= 0;
			} else {
				throw new IllegalArgumentException();
			}
		} catch (final ClassCastException e) {
			return false;
		}
	}
	
	/**
	 * 第1引数と第2引数のそれぞれを{@link Comparable}にキャストする.
	 * キャスト結果は配列に格納して返す.
	 * 第1引数のキャスト結果は戻り値の配列の1つめの要素（添字は{@code 0}）、
	 * 第2引数のキャスト結果は戻り値の配列の2つめの要素（添字は{@code 1}）となる。
	 * 第1引数が{@link Number}のサブクラス（{@link BigDecimal}・{@link BigInteger}・
	 * {@link Byte}・{@link Double}・{@link Float}・
	 * {@link Integer}・{@link Long}・{@link Short}）である場合、
	 * 第2引数の値もそれらの値に変換される。
	 * 変換に失敗した場合は配列の要素は{@code null}になる。
	 * @param actual 実際の値（左辺）
	 * @param expected 期待される値（右辺）
	 * @return キャスト結果の格納された配列
	 */
	@SuppressWarnings("rawtypes")
	private Comparable[] makeComparablePair(Object actual, Object expected) {
		final String expectedString = expected.toString();
		final Comparable[] result = new Comparable[2];
		try {
			if (actual instanceof Integer) {
				result[0] = (Integer) actual;
				result[1] =  expected instanceof Integer ? (Comparable)expected : Integer.valueOf(expectedString);
			} else if (actual instanceof Long) {
				result[0] = (Long) actual;
				result[1] = expected instanceof Long ? (Comparable)expected : Long.valueOf(expectedString);
			} else if (actual instanceof Float) {
				result[0] = (Float) actual;
				result[1] = expected instanceof Float ? (Comparable)expected : Float.valueOf(expectedString);
			} else if (actual instanceof Double) {
				result[0] = (Double) actual;
				result[1] = expected instanceof Double ? (Comparable)expected : Double.valueOf(expectedString);
			} else if (actual instanceof Short) {
				result[0] = (Short) actual;
				result[1] = expected instanceof Short ? (Comparable)expected : Short.valueOf(expectedString);
			} else if (actual instanceof Byte) {
				result[0] = (Byte) actual;
				result[1] = expected instanceof Byte ? (Comparable)expected : Byte.valueOf(expectedString);
			} else if (actual instanceof BigDecimal) {
				result[0] = (BigDecimal) actual;
				result[1] = expected instanceof BigDecimal ? (Comparable)expected : new BigDecimal(expectedString);
			} else if (actual instanceof BigInteger) {
				result[0] = (BigInteger) actual;
				result[1] = expected instanceof BigInteger ? (Comparable)expected : new BigInteger(expectedString);
			} else if (actual instanceof String) {
				result[0] = actual.toString();
				result[1] = expectedString;
			} else if (actual instanceof Comparable 
					&& expected instanceof Comparable) {
				result[0] = (Comparable)actual;
				result[1] = (Comparable)expected;
			}
		} catch (final NumberFormatException e) {
			// Do nothing.
		}
		return result;
	}	
}
