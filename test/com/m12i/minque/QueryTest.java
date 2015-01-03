package com.m12i.minque;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.m12i.minque.Accessor;
import com.m12i.minque.Query;
import com.m12i.minque.QueryFactory;

public class QueryTest {

	private static final QueryFactory<HashMap<String,String>> factory = new QueryFactory<HashMap<String,String>>(new Accessor<HashMap<String,String>>() {
		@Override
		public String accsess(HashMap<String, String> elem, String prop) {
			return elem.get(prop);
		}
	});
	
	private static final List<HashMap<String, String>> list0 = new ArrayList<HashMap<String,String>>();
	
	private static final List<HashMap<String, String>> list1 = new ArrayList<HashMap<String,String>>();
	
	private static Query<HashMap<String,String>> create(String expr) {
		try {
			return factory.create(expr);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
	}
	
	private static HashMap<String, String> makeMap(String id, String... args) {
		final HashMap<String, String> result = new HashMap<String, String>();
		result.put("id", id);
		for (int i = 0; i < args.length; i ++) {
			result.put("key" + i, args[i]);
		}
		return result;
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		list1.add(makeMap("map0", "foo", "bar", "baz"));
		list1.add(makeMap("map1", "foo", "bar", "bax"));
		list1.add(makeMap("map2", "hello", "world"));
		list1.add(makeMap("map3", "0000", "1111", "2222", "3333"));
	}

	@Test
	public void selectOneTest00() {
		assertNull(create("key0 == foo").selectOneFrom(list0));
	}

	@Test
	public void selectOneTest10() {
		assertThat(create("key0 == foo").selectOneFrom(list1).get("id"), is("map0"));
	}

	@Test
	public void selectAllTest00() {
		assertThat(create("key0 == foo").selectFrom(list0).size(), is(0));
	}

	@Test
	public void selectAllTest10() {
		final List<HashMap<String, String>> res = create("key0 == foo").selectFrom(list1);
		assertThat(res.size(), is(2));
		assertThat(res.get(0).get("id"), is("map0"));
		assertThat(res.get(1).get("id"), is("map1"));
	}

	@Test
	public void selectAllTest11() {
		final List<HashMap<String, String>> res = create("key0 != foo").selectFrom(list1);
		assertThat(res.size(), is(2));
		assertThat(res.get(0).get("id"), is("map2"));
		assertThat(res.get(1).get("id"), is("map3"));
	}

	@Test
	public void selectAllTest12() {
		assertThat(create("key0 == hello").selectFrom(list1).get(0).get("id"), is("map2"));
	}

	@Test
	public void selectAllTest13() {
		final List<HashMap<String, String>> res = create("key0 == foo or key1 == world").selectFrom(list1);
		assertThat(res.size(), is(3));
	}

	@Test
	public void selectAllTest14() {
		final List<HashMap<String, String>> res = create("(key0 == foo and key1 == bar) or key1 == world").selectFrom(list1);
		assertThat(res.size(), is(3));
	}

	@Test
	public void selectAllTest15() {
		final List<HashMap<String, String>> res = create("key0 == foo and (key1 == bar or key1 == world)").selectFrom(list1);
		assertThat(res.size(), is(2));
	}

	@Test
	public void selectAllTest16() {
		final List<HashMap<String, String>> res = create("key0 == foo and ((key1 == bar) or key1 == world)").selectFrom(list1);
		assertThat(res.size(), is(2));
	}

	@Test
	public void selectAllTest17() {
		final List<HashMap<String, String>> res = create("key0 == foo and (key1 == bar or (key1 == world))").selectFrom(list1);
		assertThat(res.size(), is(2));
	}

	@Test
	public void selectAllTest18() {
		final List<HashMap<String, String>> res = create("key0 ^= f").selectFrom(list1);
		assertThat(res.size(), is(2));
	}

	@Test
	public void selectAllTest19() {
		final List<HashMap<String, String>> res = create("key0 $= oo").selectFrom(list1);
		assertThat(res.size(), is(2));
	}

	@Test
	public void selectAllTest20() {
		final List<HashMap<String, String>> res = create("key0 *= oo").selectFrom(list1);
		assertThat(res.size(), is(2));
	}

	@Test
	public void selectAllTest21() {
		final List<HashMap<String, String>> res = create("key0 *= o").selectFrom(list1);
		assertThat(res.size(), is(3));
	}

	@Test
	public void selectAllTest30() {
		final Query<HashMap<String, String>> q0 = create("key0 == ?");
		final List<HashMap<String, String>> res0 = q0.selectFrom(list1, "foo");
		assertThat(res0.size(), is(2));
		
		final List<HashMap<String, String>> res1 = q0.selectFrom(list1, "hello");
		assertThat(res1.size(), is(1));

		final Query<HashMap<String, String>> q1 = create("key0 == ? or key1 == ?");
		assertThat(q1.selectFrom(list1, "foo", "world").size(), is(3));
	}

	@Test
	public void countTest00() {
		final Query<HashMap<String, String>> q0 = create("key0 == ?");
		final int res0 = q0.countIn(list1, "foo");
		assertThat(res0, is(2));
		
		final int res1 = q0.countIn(list1, "hello");
		assertThat(res1, is(1));
	}
	
	public static final class Person {
		private final int age;
		private final String firstName;
		private final String lastName;
		private final Date birthDate;
		public Person(final String firstName, final String lastName, final int age) {
			this.age = age;
			this.firstName = firstName;
			this.lastName = lastName;
			final Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, - age);
			this.birthDate = cal.getTime();
		}
		public int getAge() {
			return age;
		}
		public String getFirstName() {
			return firstName;
		}
		public String getLastName() {
			return lastName;
		}
		public Date getBirthDate() {
			return birthDate;
		}
	}
	
	public static final QueryFactory<Person> personQueryFactory =
			QueryFactory.createBeanQueryFactory(Person.class);
	
	@Test
	public void selectFromTest40() throws QueryParseException {
		final List<Person> list = new ArrayList<Person>();
		list.add(new Person("foo", "bar", 20));
		list.add(new Person("foo", "baz", 40));
		list.add(new Person("far", "boo", 60));
		
		final Query<Person> ageQuery = personQueryFactory.create("age < ?");
		assertThat(ageQuery.countIn(list, "foo"), is(0));
		assertThat(ageQuery.countIn(list, "20"), is(0));
		assertThat(ageQuery.countIn(list, 20), is(0));
		assertThat(ageQuery.countIn(list, 21), is(1));
		assertThat(ageQuery.countIn(list, "21"), is(1));
		assertThat(ageQuery.countIn(list, 40), is(1));
		assertThat(ageQuery.countIn(list, 41), is(2));
		
		final Query<Person> firstNameQuery = personQueryFactory.create("firstName > ?");
		assertThat(firstNameQuery.countIn(list, "foo"), is(0));
		assertThat(firstNameQuery.countIn(list, "fop"), is(0));
		assertThat(firstNameQuery.countIn(list, "fas"), is(2));
		assertThat(firstNameQuery.countIn(list, "fa"), is(3));
		
		final Query<Person> birthDateQuery = personQueryFactory.create("birthDate < ?");
		assertThat(birthDateQuery.countIn(list, "foo"), is(0));
		assertThat(birthDateQuery.countIn(list, new Date()), is(3));
	}
	
	public static final class TrueOrFalse {
		private final boolean value;
		public TrueOrFalse(final boolean value) {
			this.value = value;
		}
		public boolean getValue() {
			return value;
		}
	}
	
	@Test
	public void selectFromTest50() throws QueryParseException {
		final List<TrueOrFalse> list = new ArrayList<TrueOrFalse>();
		list.add(new TrueOrFalse(true));
		list.add(new TrueOrFalse(true));
		list.add(new TrueOrFalse(false));
		final QueryFactory<TrueOrFalse> factory = QueryFactory.createBeanQueryFactory(TrueOrFalse.class);
		final Query<TrueOrFalse> query0 = factory.create("value");
		final Query<TrueOrFalse> query1 = factory.create("(value)");
		final Query<TrueOrFalse> query2 = factory.create("!value");
		final Query<TrueOrFalse> query3 = factory.create("value == ?");
		
		assertThat(query0.selectFrom(list).size(), is(2));
		assertThat(query1.selectFrom(list).size(), is(2));
		assertThat(query2.selectFrom(list).size(), is(1));
		assertThat(query3.selectFrom(list, true).size(), is(2));
		assertThat(query3.selectFrom(list, false).size(), is(1));
	}
}
