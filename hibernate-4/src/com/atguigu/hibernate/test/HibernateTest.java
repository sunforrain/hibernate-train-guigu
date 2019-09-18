package com.atguigu.hibernate.test;

import com.atguigu.hibernate.entities.Department;
import com.atguigu.hibernate.entities.Employee;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class HibernateTest {

	private SessionFactory sessionFactory;
	private Session session;
	private Transaction transaction;
	
	@Before
	public void init(){
		Configuration configuration = new Configuration().configure();
		ServiceRegistry serviceRegistry = 
				new ServiceRegistryBuilder().applySettings(configuration.getProperties())
				                            .buildServiceRegistry();
		sessionFactory = configuration.buildSessionFactory(serviceRegistry);
		
		session = sessionFactory.openSession();
		transaction = session.beginTransaction();
	}
	
	@After
	public void destroy(){
		transaction.commit();
		session.close();
		sessionFactory.close();
	}

    @Test
    public void testHQLNamedParameter(){

        //1. 创建 Query 对象
        //基于命名参数.
        String hql = "FROM Employee e WHERE e.salary > :sal AND e.email LIKE :email";
        Query query = session.createQuery(hql);

        //2. 绑定参数
        query.setFloat("sal", 7000)
                .setString("email", "%A%");

        //3. 执行查询
        List<Employee> emps = query.list();
        System.out.println(emps.size());
    }

    @Test
    public void testHQL(){

        //1. 创建 Query 对象
        //基于位置的参数.
        String hql = "FROM Employee e WHERE e.salary > ? AND e.email LIKE ? AND e.dept = ? "
                + "ORDER BY e.salary";
        Query query = session.createQuery(hql);

        //2. 绑定参数
        //Query 对象调用 setXxx 方法支持方法链的编程风格.
        Department dept = new Department();
        dept.setId(1);
        query.setFloat(0, 1)
                .setString(1, "%@%")
                .setEntity(2, dept);

        //3. 执行查询
        List<Employee> emps = query.list();
        System.out.println(emps.size());
    }

}
