package com.atguigu.hibernate.test;

import com.atguigu.hibernate.entities.Department;
import com.atguigu.hibernate.entities.Employee;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.*;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

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

    /**
     * HQL是支持DELETE的
     */
    @Test
    public void testHQLUpdate(){
        String hql = "DELETE FROM Department d WHERE d.id = :id";

        session.createQuery(hql).setInteger("id", 280)
                .executeUpdate();
    }

    /**
     * HQL不支持插入操作,一定要用sql的话使用本地sql操作
     */
    @Test
    public void testNativeSQL(){
        String sql = "INSERT INTO GG_DEPARTMENT VALUES(?, ?)";
        Query query = session.createSQLQuery(sql);

        query.setInteger(0, 280)
                .setString(1, "ATGUIGU")
                .executeUpdate();
    }

    /**
     * QBC的排序和翻页
     */
    @Test
    public void testQBC4(){
        Criteria criteria = session.createCriteria(Employee.class);

        //1. 添加排序
        criteria.addOrder(Order.asc("salary"));
        criteria.addOrder(Order.desc("email"));

        //2. 添加翻页方法
        int pageSize = 5;
        int pageNo = 1;
        criteria.setFirstResult((pageNo - 1) * pageSize)
                .setMaxResults(pageSize)
                .list();
    }

    /**
     * 统计查询,用Projection表示
     */
    @Test
    public void testQBC3(){
        Criteria criteria = session.createCriteria(Employee.class);

        //统计查询: 使用 Projection 来表示: 可以由 Projections 的静态方法得到
        criteria.setProjection(Projections.max("salary"));

        System.out.println(criteria.uniqueResult());
    }

    /**
     * 带and和or的QBC,拼出来的复杂语句
     */
    @Test
    public void testQBC2(){
        Criteria criteria = session.createCriteria(Employee.class);

        //1. AND: 使用 Conjunction 表示
        //Conjunction 本身就是一个 Criterion 对象
        //且其中还可以添加 Criterion 对象
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.like("name", "a", MatchMode.ANYWHERE));
        Department dept = new Department();
        dept.setId(1);
        conjunction.add(Restrictions.eq("dept", dept));
        System.out.println(conjunction);

        //2. OR: 使用 Disjunction 表示
        Disjunction disjunction = Restrictions.disjunction();
        // ge是大于等于
        disjunction.add(Restrictions.ge("salary", 6000F));
        disjunction.add(Restrictions.isNull("email"));

        // 按照顺序用and连接两种查询条件
        criteria.add(disjunction);
        criteria.add(conjunction);

        List<Employee> list = criteria.list();
        for(Employee employee: list) {
            System.out.println(employee);
        }
    }

    /**
     * 一个QBC的简单实例
     */
    @Test
    public void testQBC(){
        //1. 创建一个 Criteria 对象
        Criteria criteria = session.createCriteria(Employee.class);

        //2. 添加查询条件: 在 QBC 中查询条件使用 Criterion 来表示
        //Criterion 可以通过 Restrictions 的静态方法得到
        // equals方法
        criteria.add(Restrictions.eq("email", "2@B"));
        // greatThan方法
        criteria.add(Restrictions.gt("salary", 5000F));

        //3. 执行查询
        Employee employee = (Employee) criteria.uniqueResult();
        System.out.println(employee);
    }

    /**
     * 迫切内连接
     * 内连接和左连接的区别是不返回左表不符合条件的记录
     */
    @Test
    public void testInnerJoinFetch(){
//		String hql = "SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.emps";
        String hql = "FROM Department d INNER JOIN FETCH d.emps";
        Query query = session.createQuery(hql);

        List<Department> depts = query.list();
        depts = new ArrayList<>(new LinkedHashSet(depts));
        System.out.println(depts.size());

        for(Department dept: depts){
            System.out.println(dept.getName() + "-" + dept.getEmps().size());
        }
    }

    /**
     * 左外连接查询
     * 没有fetch的情况下,返回的是一个数组类型的集合,数组的元素是一个department和employee组成的数组
     * 同样也存在重复的问题,但是只能通过DISTINCT的方式去重,用LinkedHashSet去重的话因为是几个数组,没有效果
     * 多的一端虽然被查出来了,但是没有被初始化,根据配置文件来决定 Employee 集合的检索策略.
     * 如果希望 list() 方法返回的集合中仅包含 Department 对象, 可以在HQL 查询语句中使用 SELECT 关键字
     */
    @Test
    public void testLeftJoin(){
        // 不去重的情况下,返回的是数组
//        String hql = "FROM Department d LEFT JOIN d.emps";
//        Query query = session.createQuery(hql);
//
//        List<Object []> result = query.list();
//        System.out.println(result);
//
//        for(Object [] objs: result){
//            // 注意这里要打印,需要重写Employee和Department的toString方法
//            System.out.println(Arrays.asList(objs));
//        }

        // 去重的情况下,因为对应了Department了,返回的是对象
        String hql = "SELECT DISTINCT d FROM Department d LEFT JOIN d.emps";
        Query query = session.createQuery(hql);

        List<Department> depts = query.list();
        System.out.println(depts.size());

        for(Department dept: depts){
            // 从这里会发现employee会根据配置文件决定检索策略
            System.out.println(dept.getName() + ", " + dept.getEmps().size());
        }
    }

    /**
     * 如果是从多的一端查一的一端呢?
     */
    @Test
    public void testManyToOne2(){
        // 用左外连接会返回满足连接条件的记录和左表中不满足连接条件的记录,打印department就可能报空
//        String hql = "SELECT e FROM Employee e LEFT JOIN FETCH e.dept";
        // 用内连接是不会出现不符合连接条件的
//        String hql = "SELECT e FROM Employee e INNER JOIN FETCH e.dept";
        // 没有FETCH的话还是不能初始化department
        String hql = "SELECT e FROM Employee e INNER JOIN e.dept";
        Query query = session.createQuery(hql);

        List<Employee> emps = query.list();
        System.out.println(emps.size());

        for(Employee emp: emps){
            // 可能有空指针,有的员工没部门,用内连接就解决了
            System.out.println(emp.getName() + ", " + emp.getDept().getName());
        }
    }

    /**
     * 迫切左外连接查询
     * 注意左外连接会返回满足连接条件的记录和左表中不满足连接条件的记录
     * 在这个例子中,因为一个部门有两个员工,有四个部门,实际返回的是八个部门的list
     * 还有一个部门没有员工,也会返回,最终结果list长度为9
     * 但是实际上部门只有5个
     * 另外多的一端直接被初始化了,不受配置文件的影响
     */
    @Test
    public void testLeftJoinFetch(){
        // 不去重的语句
//		String hql = "FROM Department d LEFT JOIN FETCH d.emps";

        // 去重的方式1,使用DISTINCT关键字
//		String hql = "SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.emps";
//        Query query = session.createQuery(hql);
//
//        List<Department> depts = query.list();

        // 去重的方式2,使用一个LinkedHashSet包装一下,再用ArrayList包装回来
        String hql = "FROM Department d LEFT JOIN FETCH d.emps";
        Query query = session.createQuery(hql);

        List<Department> depts = query.list();
        depts = new ArrayList<>(new LinkedHashSet(depts));


        System.out.println(depts.size());

        for(Department dept: depts){
            System.out.println(dept.getName() + "-" + dept.getEmps().size());
        }
    }

    /**
     * 数据分组和统计的查询
     */
    @Test
    public void testGroupBy(){
        String hql = "SELECT min(e.salary), max(e.salary) "
                + "FROM Employee e "
                + "GROUP BY e.dept "
                + "HAVING min(salary) > :minSal";

        Query query = session.createQuery(hql)
                .setFloat("minSal", 1000);

        List<Object []> result = query.list();
        for(Object [] objs: result){
            System.out.println(Arrays.asList(objs));
        }
    }

    /**
     * 投影查询,
     * 使用实体类中的构造器情况下,数组的类型可以直接指定为实体类
     */
    @Test
    public void testFieldQuery2(){
        // 直接在HQL语句中引用构造器,注意实体类也要有无参构造器
        String hql = "SELECT new Employee(e.email, e.salary, e.dept) "
                + "FROM Employee e "
                + "WHERE e.dept = :dept";
        Query query = session.createQuery(hql);

        Department dept = new Department();
        dept.setId(1);
        List<Employee> result = query.setEntity("dept", dept)
                .list();

        for(Employee emp: result){
            System.out.println(emp.getId() + ", " + emp.getEmail()
                    + ", " + emp.getSalary() + ", " + emp.getDept());
        }
    }

    /**
     * 投影查询,
     * 不使用实体类中的构造器情况下,数组的类型为Object[]
     */
    @Test
    public void testFieldQuery(){
        String hql = "SELECT e.email, e.salary, e.dept FROM Employee e WHERE e.dept = :dept";
        Query query = session.createQuery(hql);

        Department dept = new Department();
        dept.setId(1);
        List<Object[]> result = query.setEntity("dept", dept)
                .list();

        for(Object [] objs: result){
            System.out.println(Arrays.asList(objs));
        }
    }

    /**
     * 命名查询,HQL语句是配置在配置文件中的
     */
    @Test
    public void testNamedQuery(){
        Query query = session.getNamedQuery("salaryEmps");
        // 绑定参数还是链式风格
        List<Employee> emps = query.setFloat("minSal", 5000)
                                    .setFloat("maxSal", 10000)
                                    .list();

        System.out.println(emps.size());
    }

    /**
     * 分页查询,会根据不同数据库生成不同的sql分页语句
     */
    @Test
    public void testPageQuery(){
        String hql = "FROM Employee";
        Query query = session.createQuery(hql);

        int pageNo = 1;
        int pageSize = 5;

        List<Employee> emps =
                            query.setFirstResult((pageNo - 1) * pageSize) // 设定从哪一个对象开始检索,索引位置的起始值为 0
                        .setMaxResults(pageSize) // 设定一次最多检索出的对象的数目
                        .list();
        System.out.println(emps);
    }

    @Test
    public void testHQLNamedParameter(){

        //1. 创建 Query 对象
        //基于命名参数.
        String hql = "FROM Employee e WHERE e.salary > :sal AND e.email LIKE :email";
        Query query = session.createQuery(hql);

        //2. 绑定参数:按参数名称
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

        //2. 绑定参数:按参数位置
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
