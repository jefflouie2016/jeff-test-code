package com.proquest.interview.phonebook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import com.proquest.interview.util.DatabaseUtil;

public class PhoneBookImpl implements PhoneBook {

	// NOT SURE WHY YOU NEED MEMORY LIST HERE WHEREAS DATA IS STORE IN DATABASE, OR YOU WANT TO MANAGE AS CACHING LEVEL
	public static List<Person> people = new ArrayList<Person>();

	@Override
	public void addPerson(Person newPerson) {
		if (newPerson == null) throw new IllegalArgumentException("null");

		// Add into Memory list
		people.add(newPerson);

		// Add into database
		insertPeopleInDB(newPerson);
	}

	@Override
	public Person findPerson(String firstName, String lastName) {
		if (firstName == null || lastName == null) {
			throw new IllegalArgumentException("null");
			// the other option here is to return null, indicating not found
		}
		final String nameSought = firstName + " " + lastName;
		for (Person person : people) {
			if (nameSought.equals(person.getName())) return person;
		}
		return null;
	}

	@Override
	public Collection<Person> allPeople() {
		return Collections.unmodifiableList(people);
	}

	@Override
	public Collection<Person> allPeopleFromDB() {
		return Collections.unmodifiableList(getPeopleFromDB());
	}

	
	@Override
	public String toString() {
		return people.toString();
	}

	public static void main(String[] args) {
		DatabaseUtil.initDB(); // You should not remove this line, it creates the in-memory database

		PhoneBook phoneBook = PhoneBookFactory.createPhoneBook();

		/* TODO: create person objects and put them in the PhoneBook and database
		 * John Smith, (248) 123-4567, 1234 Sand Hill Dr, Royal Oak, MI
		 * Cynthia Smith, (824) 128-8758, 875 Main St, Ann Arbor, MI
		 */

		Person john = new Person.PersonBuilder()
				.name("John Smith")
				.phoneNumber("(248) 123-4567")
				.address("1234 Sand Hill Dr, Royal Oak, MI")
				.build();

		Person cynthia = new Person.PersonBuilder()
				.name("Cynthia Smith")
				.phoneNumber("(824) 128-8758")
				.address("875 Main St, Ann Arbor, MI")
				.build();

		// Add into PhoneBook Memory and Database
		phoneBook.addPerson(john);
		phoneBook.addPerson(cynthia);

		// TODO: print the phone book out to System.out
		// Print-out from LIST Memory
		System.out.println("************ Print-out from LIST Memory *************");
		for (Person person : phoneBook.allPeople()) {
			System.out.println(person);
		}
		System.out.println("\n");

		// Printout from database
		System.out.println("********** Print-out from Database *************");
		for (Person person : phoneBook.allPeopleFromDB()) {
			System.out.println(person);
		}
		System.out.println("\n");



		// TODO: find Cynthia Smith and print out just her entry
		System.out.println("******** find Cynthia Smith in database ***********");
		Person cynthiaSmith = phoneBook.findPerson("Cynthia", "Smith");
		System.out.println(cynthiaSmith);
		System.out.println("\n");



		// TODO: insert the new person objects into the database
		Person David = new Person.PersonBuilder()
				.name("David Smith")
				.phoneNumber("(248) 999-999")
				.address("999 Sand Hill Dr, Royal Oak, MI")
				.build();

		insertPeopleInDB(David);
		System.out.println(" ************ Print-out from Database again ************");
		for (Person person : phoneBook.allPeopleFromDB()) {
			System.out.println(person);
		}

	}


	public static List<Person> getPeopleFromDB() {
		return byCriteria("SELECT * FROM PHONEBOOK", new Object[] {});
	}

	public static List<Person> byName(String name) {
		return byCriteria("SELECT * FROM PHONEBOOK WHERE NAME = ?", name);
	}

	public static List<Person> byPhoneNumber(String phoneNumber) {
		return byCriteria("SELECT * FROM PHONEBOOK WHERE PHONENUMBER = ?", phoneNumber);
	}

	public static List<Person> byAddress(String address) {
		return byCriteria("SELECT * FROM PHONEBOOK WHERE ADDRESS = ?", address);
	}

	public static void insertPeopleInDB(Person people) {
		final String sql =
				"INSERT INTO PHONEBOOK (NAME, PHONENUMBER, ADDRESS) VALUES(?, ?, ?)";

		Connection con = null;
		PreparedStatement ps = null;

		try {

			System.setProperty("hsqldb.reconfig_logging", "false");

			con = getConnection();
			con.setAutoCommit(false);
			ps = con.prepareStatement(sql);
			QueryRunner q = new QueryRunner();
			q.fillStatementWithBean(ps, people, "name", "phoneNumber", "address");
			ps.addBatch();

			ps.executeBatch();
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(con);
		}
	}



	/************** Private Method ****************
	/**
	 *
	 * @return List<Person>
	 */
	private static ResultSetHandler<List<Person>> rowsToPeople() {
		return new ResultSetHandler<List<Person>>() {
			public List<Person> handle(ResultSet rs) throws SQLException {
				List<Person> people = new ArrayList<Person>();
				while (rs.next()) {
					people.add(new Person(rs.getString(1), rs.getString(2), rs.getString(3)));
				}
				return people;
			}
		};
	}

	/**
	 *
	 * @param sql
	 * @param criteria
	 * @return List<Person>
	 */
	private static List<Person> byCriteria(String sql, Object... criteria) {
		Connection con = null;
		try {
			con = getConnection();
			return new QueryRunner().query(con, sql, rowsToPeople(), criteria);
		} catch (SQLException e) {
			e.printStackTrace();
			// questionable; maybe we should throw some application-specific exception to indicate failure
			return new ArrayList<Person>();
		} finally {
			DbUtils.closeQuietly(con);
		}
	}

	/**
	 *
	 * @return Connection
	 * @throws SQLException
	 */
	private static Connection getConnection() throws SQLException {
		try {
			return DatabaseUtil.getConnection();
		} catch (ClassNotFoundException e) {
			// avoid dealing with CNFE; we aren't going to do anything different in that case anyway
			throw new SQLException(e);
		}
	}
}
