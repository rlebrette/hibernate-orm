/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.onetomany;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.NullPrecedence;
import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OrderByTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testOrderByOnIdClassProperties() throws Exception {
		Session s = openSession( );
		s.getTransaction().begin();
		Order o = new Order();
		o.setAcademicYear( 2000 );
		o.setSchoolId( "Supelec" );
		o.setSchoolIdSort( 1 );
		s.persist( o );
		OrderItem oi1 = new OrderItem();
		oi1.setAcademicYear( 2000 );
		oi1.setDayName( "Monday" );
		oi1.setSchoolId( "Supelec" );
		oi1.setOrder( o );
		oi1.setDayNo( 23 );
		s.persist( oi1 );
		OrderItem oi2 = new OrderItem();
		oi2.setAcademicYear( 2000 );
		oi2.setDayName( "Tuesday" );
		oi2.setSchoolId( "Supelec" );
		oi2.setOrder( o );
		oi2.setDayNo( 30 );
		s.persist( oi2 );
		s.flush();
		s.clear();

		OrderID oid = new OrderID();
		oid.setAcademicYear( 2000 );
		oid.setSchoolId( "Supelec" );
		o = (Order) s.get( Order.class, oid );
		assertEquals( 30, o.getItemList().get( 0 ).getDayNo().intValue() );

		s.getTransaction().rollback();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-465")
	@RequiresDialect(value = { H2Dialect.class, MySQLDialect.class },
			comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression. " +
					"For MySQL testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	public void testAnnotationNullsFirstLast() {
		Session session = openSession();

		// Populating database with test data.
		session.getTransaction().begin();
		Tiger tiger1 = new Tiger();
		tiger1.setName( null ); // Explicitly setting null value.
		Tiger tiger2 = new Tiger();
		tiger2.setName( "Max" );
		Monkey monkey1 = new Monkey();
		monkey1.setName( "Michael" );
		Monkey monkey2 = new Monkey();
		monkey2.setName( null );  // Explicitly setting null value.
		Zoo zoo = new Zoo( "Warsaw ZOO" );
		zoo.getTigers().add( tiger1 );
		zoo.getTigers().add( tiger2 );
		zoo.getMonkeys().add( monkey1 );
		zoo.getMonkeys().add( monkey2 );
		session.persist( zoo );
		session.persist( tiger1 );
		session.persist( tiger2 );
		session.persist( monkey1 );
		session.persist( monkey2 );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		zoo = (Zoo) session.get( Zoo.class, zoo.getId() );
		// Testing @org.hibernate.annotations.OrderBy.
		Iterator<Tiger> iterator1 = zoo.getTigers().iterator();
		Assert.assertEquals( tiger2.getName(), iterator1.next().getName() );
		Assert.assertNull( iterator1.next().getName() );
		// Testing @javax.persistence.OrderBy.
		Iterator<Monkey> iterator2 = zoo.getMonkeys().iterator();
		Assert.assertEquals( monkey1.getName(), iterator2.next().getName() );
		Assert.assertNull( iterator2.next().getName() );
		session.getTransaction().commit();

		session.clear();

		// Cleanup data.
		session.getTransaction().begin();
		session.delete( tiger1 );
		session.delete( tiger2 );
		session.delete( monkey1 );
		session.delete( monkey2 );
		session.delete( zoo );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-465")
	@RequiresDialect(value = { H2Dialect.class, MySQLDialect.class },
			comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression. " +
					"For MySQL testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	public void testCriteriaNullsFirstLast() {
		Session session = openSession();

		// Populating database with test data.
		session.getTransaction().begin();
		Zoo zoo1 = new Zoo( null );
		Zoo zoo2 = new Zoo( "Warsaw ZOO" );
		session.persist( zoo1 );
		session.persist( zoo2 );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		Criteria criteria = session.createCriteria( Zoo.class );
		criteria.addOrder( org.hibernate.criterion.Order.asc( "name" ).nulls( NullPrecedence.LAST ) );
		Iterator<Zoo> iterator = (Iterator<Zoo>) criteria.list().iterator();
		Assert.assertEquals( zoo2.getName(), iterator.next().getName() );
		Assert.assertNull( iterator.next().getName() );
		session.getTransaction().commit();

		session.clear();

		// Cleanup data.
		session.getTransaction().begin();
		session.delete( zoo1 );
		session.delete( zoo2 );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-465")
	@RequiresDialect(value = { H2Dialect.class, MySQLDialect.class },
			comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression. " +
					"For MySQL testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	public void testNullsFirstLastSpawnMultipleColumns() {
		Session session = openSession();

		// Populating database with test data.
		session.getTransaction().begin();
		Zoo zoo = new Zoo();
		zoo.setName( "Berlin ZOO" );
		Visitor visitor1 = new Visitor( null, null );
		Visitor visitor2 = new Visitor( null, "Antoniak" );
		Visitor visitor3 = new Visitor( "Lukasz", "Antoniak" );
		zoo.getVisitors().add( visitor1 );
		zoo.getVisitors().add( visitor2 );
		zoo.getVisitors().add( visitor3 );
		session.save( zoo );
		session.save( visitor1 );
		session.save( visitor2 );
		session.save( visitor3 );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		zoo = (Zoo) session.get( Zoo.class, zoo.getId() );
		Iterator<Visitor> iterator = zoo.getVisitors().iterator();
		Assert.assertEquals( 3, zoo.getVisitors().size() );
		Assert.assertEquals( visitor3, iterator.next() );
		Assert.assertEquals( visitor2, iterator.next() );
		Assert.assertEquals( visitor1, iterator.next() );
		session.getTransaction().commit();

		session.clear();

		// Cleanup data.
		session.getTransaction().begin();
		session.delete( visitor1 );
		session.delete( visitor2 );
		session.delete( visitor3 );
		session.delete( zoo );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-465")
	@RequiresDialect(value = { H2Dialect.class, MySQLDialect.class },
			comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression. " +
					"For MySQL testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	public void testHqlNullsFirstLast() {
		Session session = openSession();

		// Populating database with test data.
		session.getTransaction().begin();
		Zoo zoo1 = new Zoo();
		zoo1.setName( null );
		Zoo zoo2 = new Zoo();
		zoo2.setName( "Warsaw ZOO" );
		session.persist( zoo1 );
		session.persist( zoo2 );
		session.getTransaction().commit();

		session.getTransaction().begin();
		List<Zoo> orderedResults = (List<Zoo>) session.createQuery( "from Zoo z order by z.name nulls lAsT" ).list();
		Assert.assertEquals( Arrays.asList( zoo2, zoo1 ), orderedResults );
		session.getTransaction().commit();

		session.clear();

		// Cleanup data.
		session.getTransaction().begin();
		session.delete( zoo1 );
		session.delete( zoo2 );
		session.getTransaction().commit();

		session.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Order.class, OrderItem.class, Zoo.class, Tiger.class, Monkey.class, Visitor.class };
	}
}
