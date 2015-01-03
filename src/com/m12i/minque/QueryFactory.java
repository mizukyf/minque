package com.m12i.minque;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.m12i.minque.ExpressionParser.ExpressionAndPlaceholders;

/**
 * 文字列として表現されたクエリをパースして解析済みクエリを生成するファクトリ・オブジェクト.
 * @param <E> 解析済みクエリによる検索対象となるコレクションの要素型
 */
public final class QueryFactory<E> {
	/**
	 * {@link Map}のコレクションのためのクエリ・ファクトリを生成する.
	 * @return ファクトリ・オブジェクト
	 */
	public static QueryFactory<Map<String, Object>> createMapQueryFactory() { 
		return new QueryFactory<Map<String,Object>>(new Accessor<Map<String, Object>>() {
				@Override
				public Object accsess(Map<String, Object> elem, String prop) {
					return elem.containsKey(prop) ? elem.get(prop) : null;
				}
			});
	}
	/**
	 * Java Beansのコレクションのためのクエリ・ファクトリを生成する.
	 * @param elemType 検索対象コレクションの要素型
	 * @return ファクトリ・オブジェクト
	 */
	public static<T> QueryFactory<T> createBeanQueryFactory(final Class<T> elemType) { 
		// getterっぽいメソッドを格納しておくマップ
		final HashMap<String, Method> methods = new HashMap<String, Method>();
		try {
			// 引数で指定されたクラスのpublicメソッドについて繰り返し処理
			for (final Method m : elemType.getMethods()) {
				// メソッドのシグネチャをチェック
				if (m.getParameterTypes().length == 0 && m.getReturnType() != Void.class) {
					// 「引数の数がゼロであり かつ 戻り値がvoidでない」場合はgetterとみなす
					methods.put(m.getName(), m);
				}
			}
		} catch(Exception e) {
			// Do nothing.
		}
		// ファクトリを初期化して返す
		return new QueryFactory<T>(new Accessor<T>() {
			// getterをコールする際に使用するダミーの引数リスト
			private final Object[] args = new Object[0];
			// プロパティ名にマッチするgetterメソッドを返す
			private Method getGetter(String prop) {
				// JavaBeans規約に則ったgetter名を構成
				final String getterName = "get" +
						prop.substring(0, 1).toUpperCase() + prop.substring(1);
				final String checkerName = "is" +
						prop.substring(0, 1).toUpperCase() + prop.substring(1);
				
				// getterっぽいメソッドを格納したマップの要素キーと照合
				if (methods.containsKey(getterName)) {
					// JavaBeans規約に則ったgetterが存在すればそれを返す
					return methods.get(getterName);
					
				} else if (methods.containsKey(checkerName)) {
					// JavaBeans規約に則ったgetterが存在すればそれを返す
					return methods.get(getterName);
					
				} else if (methods.containsKey(prop)) {
					// プロパティと同名のメソッドが存在すればそれを返す
					return methods.get(prop);
					
				} else {
					// いずれにも該当しなければnullを返す
					return null;
				}
			}
			@Override
			public Object accsess(T elem, String prop) {
				try {
					// プロパティ名にマッチするgetterメソッドを探す
					final Method m = getGetter(prop);
					// nullチェックを実施
					if (m == null) return null;
					// メソッドをダミーの引数リストに適用する
					final Object o = m.invoke(elem, args);
					// 再びnullチェックを実施
					if (o == null) return null;
					// getterの呼び出し結果を文字列化して返す
					return o;
					
				} catch (Exception e) {
					// 何らかの理由でメソッドコールが失敗したらともかくnullを返す
					return null;
				}
			}
		});
	}
	private static final ExpressionParser p = new ExpressionParser();
	private final Accessor<E> a;
	/**
	 * ファクトリ・オブジェクトのコンストラクタ.
	 * アクセサ・オブジェクト──クエリの条件式で指定されたプロパティを要素から取得するためのオブジェクト──をパラメータとして受け取り、
	 * ファクトリ・オブジェクトを初期化します。
	 * @param accessor アクセサ・オブジェクト
	 */
	public QueryFactory(Accessor<E> accessor) {
		if (accessor == null) {
			throw new IllegalArgumentException();
		}
		this.a = accessor;
	}
	/**
	 * 文字列として表現されたクエリをパースして解析済みクエリを生成する.
	 * @param query クエリ文字列
	 * @return 解析済みクエリ
	 * @throws QueryParseException クエリのパースに失敗した場合
	 */
	public Query<E> create(String query) throws QueryParseException {
		try {
			final ExpressionAndPlaceholders r = p.parse(query);
			return new QueryImpl<E>(r.expression, r.placeholders, a);
		} catch (final ParseException e) {
			throw new QueryParseException(e);
		}
	}
}
