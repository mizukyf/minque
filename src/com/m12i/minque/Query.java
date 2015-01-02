package com.m12i.minque;

import java.util.Collection;
import java.util.List;

/**
 * 解析済みクエリを表わすオブジェクト.
 * コレクション要素を検索するためのAPIを提供します。
 * @param <E> 検索対象となるコレクションの要素型
 */
public interface Query<E> {
	/**
	 * クエリにマッチしたすべての要素を返す.
	 * 返却されるリストに含まれる要素の順序は、検索対象のコレクションの実装次第となります。
	 * @param source 検索対象
	 * @return クエリ内容にマッチしたすべての要素
	 * @throws IllegalArgumentException クエリ文字列にバインド変数のプレースホルダが含まれる場合
	 */
	List<E> selectFrom(Collection<E> source);
	/**
	 * クエリにマッチしたすべての要素を返す.
	 * 返却されるリストに含まれる要素の順序は、検索対象のコレクションの実装次第となります。
	 * @param source 検索対象
	 * @param vars バインド変数
	 * @return クエリ内容にマッチしたすべての要素
	 * @throws IllegalArgumentException クエリ文字列に含まれるプレースホルダの数とバインド変数の数が一致しない場合
	 */
	List<E> selectFrom(Collection<E> source, Object... vars);
	/**
	 * クエリにマッチした最初の要素を返す.
	 * クエリにマッチする要素が複数あった場合にいずれの要素が「最初の」要素とみなされるかは、
	 * 検索対象のコレクションの実装次第となります。
	 * マッチする要素がなかった場合は{@code null}を返します。
	 * @param source 検索対象
	 * @return クエリ内容にマッチした要素
	 * @throws IllegalArgumentException クエリ文字列にバインド変数のプレースホルダが含まれる場合
	 */
	E selectOneFrom(Collection<E> source);
	/**
	 * クエリにマッチした最初の要素を返す.
	 * クエリにマッチする要素が複数あった場合にいずれの要素が「最初の」要素とみなされるかは、
	 * 検索対象のコレクションの実装次第となります。
	 * マッチする要素がなかった場合は{@code null}を返します。
	 * @param source 検索対象
	 * @param vars バインド変数
	 * @return クエリ内容にマッチしたすべての要素
	 * @throws IllegalArgumentException クエリ文字列に含まれるプレースホルダの数とバインド変数の数が一致しない場合
	 */
	E selectOneFrom(Collection<E> source, Object... vars);
}
