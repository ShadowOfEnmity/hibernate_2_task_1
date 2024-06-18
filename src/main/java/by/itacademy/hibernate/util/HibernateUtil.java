package by.itacademy.hibernate.util;

import by.itacademy.hibernate.convertor.BirthdayConvertor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    public static SessionFactory buildSessionFactory() {
        Configuration configuration = new Configuration();
        configuration.addAttributeConverter(new BirthdayConvertor());
//        configuration.configure();
        configuration.addAttributeConverter(new BirthdayConvertor());
//        configuration.registerTypeOverride(new JsonBinaryType());
//        configuration.addAnnotatedClass(User.class);
        configuration.setPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy());
        return configuration.configure().buildSessionFactory();
    }
}
