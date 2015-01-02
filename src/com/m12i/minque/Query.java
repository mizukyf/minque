package com.m12i.minque;

import java.util.List;

/**
 * 解析済みクエリを表わすオブジェクト.
 * {@link Iterable}を引数にとって条件にマッチする要素を取得するためのAPIを提供します。
 * @param <E> 検索対象の{@link Iterable}実装の要素型
 */
public interface Query<E> {
	/**
	 * クエリにマッチしたすべての要素を返す.
	 * 返却されるリストに含まれる要素の順序は、検索対象の{@link Iterable}実装の実装次第となります。
	 * @param target 検索対象
	 * @return クエリ内容にマッチしたすべての要素
	 * @throws IllegalArgumentException クエリ文字列にバインド変数のプレースホルダが含まれる場合
	 */
	List<E> selectFrom(Iterable<E> target);
	/**
	 * クエリにマッチしたすべての要素を返す.
	 * 返却されるリストに含まれる要素の順序は、検索対象の{@link Iterable}実装の実装次第となります。
	 * @param target 検索対象
	 * @param vars バインド変数
	 * @return クエリ内容にマッチしたすべての要素
	 * @throws IllegalArgumentException クエリ文字列に含まれるプレースホルダの数とバインド変数の数が一致しない場合
	 */
	List<E> selectFrom(Iterable<E> target, Object... vars);
	/**
	 * クエリにマッチした最初の要素を返す.
	 * クエリにマッチする要素が複数あった場合にいずれの要素が「最初の」要素とみなされるかは、
	 * 検索対象の{@link Iterable}実装の実装次第となります。
	 * マッチする要素がなかった場合は{@code null}を返します。
	 * @param target 検索対象
	 * @return クエリ内容にマッチした要素
	 * @throws IllegalArgumentException クエリ文字列にバインド変数のプレースホルダが含まれる場合
	 */
	E selectOneFrom(Iterable<E> target);
	/**
	 * クエリにマッチした最初の要素を返す.
	 * クエリにマッチする要素が複数あった場合にいずれの要素が「最初の」要素とみなされるかは、
	 * 検索対象の{@link Iterable}実装の実装次第となります。
	 * マッチする要素がなかった場合は{@code null}を返します。
	 * @param target 検索対象
	 * @param vars バインド変数
	 * @return クエリ内容にマッチしたすべての要素
	 * @throws IllegalArgumentException クエリ文字列に含まれるプレースホルダの数とバインド変数の数が一致しない場合
	 */
	E selectOneFrom(Iterable<E> target, Object... vars);
	/**
	 * クエリにマッチした要素の数を返す.
	 * @param target 検索対象
	 * @return クエリ内容にマッチした要素の数
	 * @throws IllegalArgumentException クエリ文字列にバインド変数のプレースホルダが含まれる場合
	 */
	int countFrom(Iterable<E> target);
	/**
	 * クエリにマッチした要素の数を返す.
	 * @param target 検索対象
	 * @param vars バインド変数
	 * @return クエリ内容にマッチした要素の数
	 * @throws IllegalArgumentException クエリ文字列に含まれるプレースホルダの数とバインド変数の数が一致しない場合
	 */
	int countFrom(Iterable<E> target, Object... vars);
}
