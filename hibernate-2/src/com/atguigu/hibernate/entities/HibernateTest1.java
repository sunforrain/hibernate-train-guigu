package com.atguigu.hibernate.entities;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.jdbc.Work;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HibernateTest1 {

	private SessionFactory sessionFactory;
	// 注意在生产中session和transaction是不能作为成员变量的,会有并发问题,这里测试没有问题
	private Session session;
	private Transaction transaction;

	// @Before junit的准备方法,会在所有@Test运行前运行
    // 在这里是用来准备session和trasaction了
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
    // @After junit的结束方法,会在所有@Test运行后运行
    // 在这里是用来关闭session和trasaction和sessionFactory了
	@After
	public void destroy(){
	    // 提交事务
		transaction.commit();
		// 关闭session
		session.close();
		sessionFactory.close();
	}
	
	@Test
	public void testComponent(){
		Worker worker = new Worker();
		Pay pay = new Pay();

		pay.setMonthlyPay(1000);
		pay.setYearPay(80000);
		pay.setVocationWithPay(5);

		worker.setName("ABCD");
		worker.setPay(pay);

		session.save(worker);
	}
	
	@Test
	public void testBlob() throws Exception{
	    // 保存图片的代码
//		News news = new News();
//		news.setAuthor("cc");
//		news.setContent("CONTENT");
//		news.setDate(new Date());
//		news.setDesc("DESC");
//		news.setTitle("CC");
//
//		InputStream stream = new FileInputStream("Hydrangeas.jpg");
//		// 这是个hibernate的工具类,用来制造Lob类型的数据,可以看到需要个输入流和输入流的的byte数
//		Blob image = Hibernate.getLobCreator(session)
//				              .createBlob(stream, stream.available());
//		news.setImage(image);
//
//		session.save(news);

		// 查询出图片的代码
		News news = (News) session.get(News.class, 1);
		Blob image = news.getImage();

		InputStream in = image.getBinaryStream();
		System.out.println(in.available());
	}
	
	@Test
	public void testPropertyUpdate(){
		News news = (News) session.get(News.class, 1);
		news.setTitle("aaaa"); 
		
		System.out.println(news.getDesc());
		System.out.println(news.getDate().getClass()); 
	}
	
	@Test
	public void testIdGenerator() throws InterruptedException, ParseException {
	    // 这里演示了date三种子类之一sql.Date的创建方式
		News news = new News("CC", "cc", new java.sql.Date(new Date().getTime()));
		session.save(news);
		// 测试increment进行主键生成的线程安全问题
//		Thread.sleep(5000); 
	}
	
	@Test
	public void testDynamicUpdate(){
		News news = (News) session.get(News.class, 1);
		news.setAuthor("AABCD");
		
	}
	// hibernate不能直接调用存储过程,而是要使用原生的jdbc的接口
	@Test
	public void testDoWork(){
		session.doWork(new Work() {
			
			@Override
			public void execute(Connection connection) throws SQLException {
				System.out.println(connection); 
				
				//调用存储过程.
//				String procedure = "{ call testProcedure() }";
//				CallableStatement callableStatement = connection.prepareCall(procedure);
//				callableStatement.executeUpdate();
			}
		});
	}
	
	/**
	 * evict: 从 session 缓存中把指定的持久化对象移除,但不会delete移除的对象
	 */
	@Test
	public void testEvict(){
		News news1 = (News) session.get(News.class, 1);
		News news2 = (News) session.get(News.class, 2);
		
		news1.setTitle("AA");
		news2.setTitle("BB");
		
		session.evict(news1); 
	}
	
	/**
	 * delete: 执行删除操作. 只要 OID 和数据表中一条记录对应, 就会准备执行 delete 操作
	 * 若 OID 在数据表中没有对应的记录, 则抛出异常
	 * 
	 * 可以通过设置 hibernate 配置文件 hibernate.use_identifier_rollback 为 true,
	 * 使删除对象后, 把其 OID 置为  null
	 */
	@Test
	public void testDelete(){
//		News news = new News();
//		news.setId(11);
		
		News news = (News) session.get(News.class, 163840);
		session.delete(news); 
		
		System.out.println(news);
	}
	
	/**
	 * 注意:
	 * 1. 若 OID 不为 null, 但数据表中还没有和其对应的记录. 会抛出一个异常. 
	 * 2. 了解: OID 值等于 id 的 unsaved-value 属性值(实体类的映射文件中设置)的对象, 也被认为是一个游离对象
	 */
	@Test
	public void testSaveOrUpdate(){
		News news = new News("FFF", "fff", new Date());
		news.setId(11);
		
		session.saveOrUpdate(news); 
	}
	
	/**
	 * update:
	 * 1. 若更新一个持久化对象, 不需要显示的调用 update 方法. 因为在调用 Transaction
	 * 的 commit() 方法时, 会先执行 session 的 flush 方法.
	 * 2. 更新一个游离对象, 需要显式的调用 session 的 update 方法. 可以把一个游离对象
	 * 变为持久化对象
	 * 
	 * 需要注意的:
	 * 1. 无论要更新的游离对象和数据表的记录是否一致, 都会发送 UPDATE 语句. 
	 *    如何能让 updat 方法不再盲目的出发 update 语句呢 ? 在 .hbm.xml 文件的 class 节点设置
	 *    select-before-update=true (默认为 false). 但通常不需要设置该属性. 
	 * 
	 * 2. 若数据表中没有对应的记录, 但还调用了 update 方法, 会抛出异常
	 * 
	 * 3. 当 update() 方法关联一个游离对象时, 
	 * 如果在 Session 的缓存中已经存在相同 OID 的持久化对象, 会抛出异常. 因为在 Session 缓存中
	 * 不能有两个 OID 相同的对象!
	 *    
	 */
	@Test
	public void testUpdate(){
		News news = (News) session.get(News.class, 1);
		
		transaction.commit();
		session.close(); // 这里session关闭后,后面的是另一个不同的session了,没有上面的news对象
		
//		news.setId(100);

		session = sessionFactory.openSession();
		transaction = session.beginTransaction();
//		session.update(news); // 这里虽然news没有改变,与数据库的一致,但是因为是新的session,还是游离态的,因而仍然会触发update语句
//		news.setAuthor("SUN"); 
		
		News news2 = (News) session.get(News.class, 1);
		session.update(news);// 这里会报错,因为OID上面已经有了
	}
	
	/**
	 * get VS load:
	 * 
	 * 1. 执行 get 方法: 会立即加载对象. 
	 *    执行 load 方法, 若不使用该对象(只查询但是不获取对象内任何变量的值), 则不会立即执行查询操作, 而返回一个代理对象
	 *    
	 *    get 是 立即检索, load 是延迟检索. 
	 * 
	 * 2. load 方法可能会抛出 LazyInitializationException 异常: 在需要初始化
	 * 代理对象之前已经关闭了 Session
	 * 
	 * 3. 若数据表中没有对应的记录, Session 也没有被关闭.  
	 *    get 返回 null
	 *    load 若不使用该对象的任何属性, 没问题; 若需要初始化了, 抛出异常.  
	 */
	@Test
	public void testLoad(){
		
		News news = (News) session.load(News.class, 10);
		System.out.println(news.getClass().getName()); 
		
//		session.close(); // 这里有问题,可能会抛出懒加载异常
//		System.out.println(news); 
	}
	
	@Test
	public void testGet(){
		News news = (News) session.get(News.class, 1);
//		session.close();// 在这里没问题,get不会报错
		System.out.println(news); 
	}
	
	/**
	 * persist(): 也会执行 INSERT 操作
	 * 
	 * 和 save() 的区别 : 
	 * 在调用 persist 方法之前, 若对象已经有 id 了, 则不会执行 INSERT, 而抛出异常
	 */
	@Test
	public void testPersist(){
		News news = new News();
		news.setTitle("EE");
		news.setAuthor("ee");
		news.setDate(new Date());
		news.setId(200); // 设置了ID会报错
		
		session.persist(news); 
	}
	
	/**
	 * 1. save() 方法
	 * 1). 使一个临时对象变为持久化对象
	 * 2). 为对象分配 ID.
	 * 3). 在 flush 缓存时会发送一条 INSERT 语句.
	 * 4). 在 save 方法之前的 id 是无效的
	 * 5). 持久化对象的 ID 是不能被修改的!
	 */
	@Test
	public void testSave(){
		News news = new News();
		news.setTitle("CC");
		news.setAuthor("cc");
		news.setDate(new Date());
		news.setId(100); 
		
		System.out.println(news);
		
		session.save(news);

		System.out.println(news);
//		news.setId(101); 
	}
	
	/**
	 * clear(): 清理缓存,clear后session被清空
	 */
	@Test
	public void testClear(){
		News news1 = (News) session.get(News.class, 1);
		
		session.clear();
		
		News news2 = (News) session.get(News.class, 1);
	}
	
	/**
	 * refresh(): 会强制发送 SELECT 语句, 以使 Session 缓存中对象的状态和数据表中对应的记录保持一致!
	 */
	@Test
	public void testRefresh(){
		News news = (News) session.get(News.class, 1);
		System.out.println(news);
		// 这里要设置hibernate访问mysql的默认事务隔离级别降一级,否则手动更改数据库记录后,为了保证可重复读,仍然是一样的值.
		session.refresh(news); 
		System.out.println(news); 
	}
	
	/**
	 * flush: 使数据表中的记录和 Session 缓存中的对象的状态保持一致.
     * 为了保持一致, 则可能会发送对应的 SQL 语句.将session中的数据update或insert等
	 * 1. 在 Transaction 的 commit() 方法中: 先调用 session 的 flush 方法, 再提交事务
	 * 2. flush() 方法会可能会发送 SQL 语句, 但不会提交事务. 
	 * 3. 注意: 在未提交事务或显式的调用 session.flush() 方法之前, 也有可能会进行 flush() 操作.
	 * 1). 执行 HQL 或 QBC 查询, 会先进行 flush() 操作, 以得到数据表的最新的记录
	 * 2). 这是个例外情况:若记录的 ID 是由底层数据库使用自增的方式生成的, 则在调用 save() 方法时, 就会立即发送 INSERT 语句,并提交事物.
	 *      因为 save 方法后, 必须保证对象的 ID 是存在的!
     *    如果是使用hibernate生成的主键ID,则不会立即发送INSERT语句,而是正常的先去查询新的ID,调用commit()
	 */
	@Test
	public void testSessionFlush2(){
		News news = new News("Java", "SUN", new Date());
		session.save(news);
	}
	
	@Test
	public void testSessionFlush(){
		News news = (News) session.get(News.class, 1);
		news.setAuthor("Oracle");
		
//		session.flush();
//		System.out.println("flush");
		// QBC 查询, 查询前会进行一次update以保证对象是最新的.
		News news2 = (News) session.createCriteria(News.class).uniqueResult();
		System.out.println(news2);
	}

    /**
     *
     * 这里两次查询只会发一条sql语句
     * 这与session缓存有关,只要 Session 实例没有结束生命周期, 且没有清理缓存，则存放在它缓存中的对象也不会结束生命周期
     */
    @Test
	public void testSessionCache(){
		
		News news = (News) session.get(News.class, 1);
		System.out.println(news); 
		
		News news2 = (News) session.get(News.class, 1);
		System.out.println(news2);
	}

}
