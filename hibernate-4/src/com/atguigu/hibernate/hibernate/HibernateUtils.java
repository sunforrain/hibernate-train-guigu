package com.atguigu.hibernate.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

/**
 * 获取session的工具类,这里是获取当前thread的session
 * 那么这个工具类还需要是单例类
 */
public class HibernateUtils {

    private HibernateUtils () {}

    private static HibernateUtils instance = new HibernateUtils();

    public static HibernateUtils getInstance() {
        return instance;
    }

    private SessionFactory sessionFactory;

    public SessionFactory getSessionFactory () {
        if (sessionFactory == null) {
            Configuration configuration = new Configuration().configure();
            ServiceRegistry serviceRegistry =
                    new ServiceRegistryBuilder().applySettings(configuration.getProperties())
                            .buildServiceRegistry();
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        }
        return sessionFactory;
    }

    /**
     * 获取当前线程的session
     * @return
     */
    public Session getSession () {
        return getSessionFactory().getCurrentSession();
    }
}
