# Minque

## プロジェクトの概要

Minqueはクエリ文字列を使って`Iterable<E>`の要素を検索するためのAPIです。
クエリ文字列は比較演算と論理演算からなるシンプルな構文を持ちます。

```java
// ここにPersonオブジェクトのリストがあると仮定
final List<Person> target = ...;

// QueryFactoryを初期化
final QueryFactory factory = QueryFactory.createBeanQueryFactory(Person.class);

// firstNameプロパティを条件に検索するクエリを作成
final Query query0 = factory.create("firstName == 'foo'");
// 条件にマッチするものすべてを取得
final List<Person> result0 = query0.selectFrom(target);

// firstNameが'f'ではじまり'o'で終わるかlastNameが"bar"である要素を検索するクエリを作成
final Query query1 = factory.create("(firstName ^= 'f' and firstName $= 'o') or lastName == 'bar'");
// 条件にマッチする1件だけを取得
final Person result1 = query1.selectOneFrom(target);

// firstNameがバインド変数で指定された値である要素を検索するクエリを作成
final Query query2 = factory.create("firstName == ?");
final int result2 = query2.countIn(target, "baz");
```

### QueryFactory

`QueryFactory`は文字列として表現されたクエリをパースして解析済みクエリを生成するファクトリ・オブジェクトです。
このオブジェクトにはあらかじめ2つの実装が用意されています。

1つは`MapQueryFactory`で`Map<String,Object>`のコレクションを検索するためのもの、
もう1つは`BeanQueryFactory`でJava Beansプロパティを持つオブジェクトのコレクションを検索するためのものです。
加えて後述の`Accessor`インターフェースを実装することで任意のオブジェクトを検索対象とすることもできます。

### Query

解析済みクエリを表わすオブジェクトです。コレクション要素を検索するためのAPIを提供します。
ファクトリによって解析されたコードは、`Query`内部にJavaオブジェクト・グラフとして格納され、コレクションの要素を検索する時に使用されます。

### Accessor

クエリの条件式で指定されたプロパティを要素から取得するためのアクセサのインターフェースです。
このインターフェースの実装オブジェクトをパラメータとして`QueryFactory`を初期化することができます。

### 検索対象コレクションとAccessorオブジェクト

`Accessor`インターフェースは、MinqueのAPIが提供する機能の抽象化のかなめです。
このインターフェースを適切に実装することで、ライブラリのユーザはさまざまなコレクションを検索対象とするクエリを作成できます。

前述のとおり`QueryFactory`には定義済みの`Accessor`実装で初期化されたインスタンスも用意されています。
例えば、`QueryFactory.createMapQueryFactory()`が返すファクトリ・インスタンスは、
`Map<String,Object>`を処理する`Accessor`で初期化されています。

また、`QueryFactory.createBeanQueryFactory(Class)`が返すインスタンスは、
リフレクションによりプロパティにアクセスする汎用的な`Accessor`で初期化されています。
このファクトリから生成されるクエリではプロパティ（比較式の左辺）はそのままJava Beansプロパティに対応します。
したがって`"foo == 1"`というクエリは、`getFoo()`（もしくは`foo()`）が`"1"`を返すBeanにマッチします。
`BeanQueryFactory`のユーザはリフレクションに伴うパフォーマンス上のコストを支払うのと引き換えに、
いちいち`Accessor`を実装することから生じる面倒──開発・保守上のコストやリスクから開放されます。

## クエリの構文

使用できるクエリは論理演算子と比較演算子からなる比較的シンプルなものです。
式（expression）は比較演算子を使った単純な式から、そうした式同士を論理演算子で組み合わせた複雑な式までさまざまに指定できます。

### 評価順序

演算子間に優先順位はとくにないため、クエリ内に複数の式がある場合の評価順序は基本的に「左から右へ」です。
丸括弧で囲うことで論理演算の結合方法を指定できます。
つまり `a == 1 and b == 2 or c == 3` は `(a == 1 and b == 2) or c == 3` と同義であるということです。

### 論理演算子

論理演算子には二項演算子（logical_binary_operator）と単項演算子（logical_unary_operator）が存在します。
`!`、`&&`、`||`、`and`、`or`といった演算子はいずれもご想像通りの動作をするはずです。

### 比較演算子

比較演算子にも二項演算子（comparative_binary_operator）と単項演算子（comparative_unary_operator）が存在します。

二項演算子の `==` と `!=` はJavaの `Object#equals(Object other)` による等価性比較を行います
（右辺が`String`の場合は例外で、左辺を`Object#toString()`で文字列に変換したうえで比較します）。

`^=` は左辺で指定されたプロパティが右辺で指定された値で始まることを（前方一致）、
`*=` は左辺で指定されたプロパティが右辺で指定された値を含むことを（中間一致）、
`$=` は左辺で指定されたプロパティが右辺で指定された値で終わることを（後方一致）それぞれ表します。

単項演算子の `is null` と `is not null` はこれもご想像通りの動作をするはずです。

大小比較のための`<`・`<=`・`>`・`>=`は左辺と右辺が`Comparable<T>`である場合にのみ有効です。
ただし左辺が`String`の場合は例外で、右辺が`Comparable<T>`であるかどうかにかかわらず
右辺を`Object#toString()`で変換したうえで比較します。

### バインド変数

クエリの構文上、比較演算の右辺に直接記述できるのは文字列のみです。
左辺が文字列でない場合はまず文字列に変換された上で比較が行われます。
この制約はバインド変数`?`を使うことで回避できます。
等価性比較（`==`・`!=`）と大小比較（`<`・`<=`・`>`・`>=`）の際にはバインド変数の使用を検討してください。

```java
// バインド変数のプレースホルダは'?'
final Query query0 = factory.create("value == ?");
// クエリを実行するときにバインド変数を指定する
final int result0 = query0.countIn(..., 123);
```

### ショートカット

クエリ内で演算子を伴わずプロパティ名だけを記述した場合、当該プロパティは`boolean`もしくは`Boolean`型であるものと仮定され、
`... == true`という演算子と右辺が続くのとほぼ同義にみなされます。
これは真偽値プロパティを扱う場合に可能となるショートカットであり、糖衣構文です
（実際には`value == true`という記述はあくまでも文字列比較を行うものです。
より厳密にショートカットしたのと同義の比較を行うにはバインド変数を使用する必要があります）。

```java
// 以下の3つのオペレーションはほぼ同義（3つめのみ厳密性に欠ける）

// ショートカットを使う方法
final Query query0 = factory.create("value");
final int result0 = query0.countIn(...);

// バインド変数で明示する方法
final Query query1 = factory.create("value == ?");
final int result1 = query0.countIn(..., true);

// 文字列として比較する方法
final Query query2 = factory.create("value == true");
final int result2 = query0.countIn(...);
```

### BNFチックな構文定義

```bnf
<query> ::= <expression>

<expression> ::= "(" expression ")" 
	| <expression> <logical_binary_operator> <expression>
	| <logical_unary_operator> <expression>
	| <property> <comparative_binary_operator> <value>
	| <property> <comparative_unary_operator>

<logical_unary_operator> ::= "!"

<logical_binary_operator> ::= "&&"
	| "||"
	| "and"
	| "or"

<comparative_unary_operator> ::= "is null"
	| "is not null"

<comparative_binary_operator> ::= "=="
	| "!="
	| "^="
	| "*="
	| "$="
	| "<"
	| "<="
	| ">"
	| ">="

<property> ::= '"' string '"'
	| "'" string "'"
	| string

<value> ::= '"' string '"'
	| "'" string "'"
	| string
```



