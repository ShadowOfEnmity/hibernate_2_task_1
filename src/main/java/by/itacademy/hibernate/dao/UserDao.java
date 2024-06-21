package by.itacademy.hibernate.dao;


import by.itacademy.hibernate.dto.BirthDayRangeFilter;
import by.itacademy.hibernate.entity.*;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.Session;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static by.itacademy.hibernate.entity.QCompany.company;
import static by.itacademy.hibernate.entity.QPayment.payment;
import static by.itacademy.hibernate.entity.QUser.user;
import static com.querydsl.core.types.dsl.Expressions.asDate;
import static com.querydsl.core.types.dsl.Expressions.dateTemplate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDao {

    private static final UserDao INSTANCE = new UserDao();

    /**
     * Возвращает всех сотрудников
     */
    public List<User> findAll(Session session) {
        return session.createQuery("from User", User.class).list();
    }

    /**
     * Возвращает всех сотрудников с указанным именем
     */
    public List<User> findAllByFirstName(Session session, String firstName) {
        return session.createQuery("from User u where u.personalInfo.firstname = :firstName", User.class)
                .setParameter("firstName", firstName)
                .list();
    }

    /**
     * Возвращает первые {limit} сотрудников, упорядоченных по дате рождения (в порядке возрастания)
     */
    public List<User> findLimitedUsersOrderedByBirthday(Session session, int limit) {
        return session.createQuery("from User order by personalInfo.birthDate asc", User.class)
                .setMaxResults(limit)
                .list();
    }

    /**
     * Возвращает всех сотрудников компании с указанным названием
     */
    public List<User> findAllByCompanyName(Session session, String companyName) {
        return session.createQuery("select u from User u inner join u.company c where c.name = :companyName", User.class)
                .setParameter("companyName", companyName)
                .list();
    }

    /**
     * Возвращает все выплаты, полученные сотрудниками компании с указанными именем,
     * упорядоченные по имени сотрудника, а затем по размеру выплаты
     */
    public List<Payment> findAllPaymentsByCompanyName(Session session, String companyName) {
        return session.createQuery("select p from Payment p inner join p.receiver u inner join u.company c where " +
                        "c.name = :companyName order by u.username asc, p.amount asc", Payment.class)
                .setParameter("companyName", companyName)
                .list();
    }

    /**
     * Возвращает среднюю зарплату сотрудника с указанными именем и фамилией
     */
    public Double findAveragePaymentAmountByFirstAndLastNames(Session session, String firstName, String lastName) {
        return session.createQuery("select avg(p.amount) from Payment p inner join p.receiver u " +
                        "where u.personalInfo.firstname = :firstName and u.personalInfo.lastname = :lastName", Double.class)
                .setParameter("firstName", firstName)
                .setParameter("lastName", lastName)
                .getResultStream().findFirst().orElseGet(() -> 0.0D);
    }

    /**
     * Возвращает для каждой компании: название, среднюю зарплату всех её сотрудников. Компании упорядочены по названию.
     */
    public List<Object[]> findCompanyNamesWithAvgUserPaymentsOrderedByCompanyName(Session session) {
        return session.createQuery("select c.name, avg(p.amount) from Company c inner join c.users u inner join u.payments p " +
                        "group by c.name order by c.name", Object[].class)
                .list();
    }

    /**
     * Возвращает список: сотрудник (объект User), средний размер выплат, но только для тех сотрудников, чей средний размер выплат
     * больше среднего размера выплат всех сотрудников
     * Упорядочить по имени сотрудника
     */
    public List<Object[]> isItPossible(Session session) {
        return session.createQuery("select u, avg(p.amount) from User u inner join u.payments p " +
                                "group by u having avg(p.amount)>(select avg(p2.amount) from Payment p2) order by u.username asc ",
                        Object[].class)
                .list();
    }
//////////////////////////////////QueryDSL/////////////////////////////

    /**
     * Найти всех сотрудников определенной компании с средней зарплатой не ниже чем заданная величина
     */
    public List<Tuple> findUsersByCompanyNameWithAvgPaymentsGreaterThanOrEqualToSetValue(Session session, String companyName, int amount) {
        return new JPAQueryFactory(session)
                .select(user, payment.amount.avg())
                .from(company)
                .join(company.users, user)
                .join(user.payments, payment)
                .where(company.name.eq(companyName))
                .orderBy(user.username.asc())
                .groupBy(user)
                .having(payment.amount.avg().goe(amount))
                .fetch();
    }

    /**
     * Найти сотрудников с днями рождения входящими в указанный интервал дат
     */
    public List<User> findUsersByBirthdayDateRange(Session session, BirthDayRangeFilter dateFilter) {

        Predicate predicate = QPredictate.builder()
                .add(dateFilter.getBegin(), user.personalInfo.birthDate::goe)
                .add(dateFilter.getEnd(), user.personalInfo.birthDate::loe)
                .buildAnd();

        return new JPAQueryFactory(session).
                select(user)
                .from(user)
                .where(predicate)
                .fetch();
    }

    /**
     * Получить максимальную зарплату по каждой организации и отсортировать по убыванию по названию организации
     */
    public List<Tuple> findMaxPaymentByEachCompanyOrderedByCompanyName(Session session) {
        return new JPAQueryFactory(session)
                .select(company.name, payment.amount.avg())
                .from(company)
                .join(company.users, user)
                .join(user.payments, payment)
                .groupBy(company)
                .orderBy(company.name.desc())
                .fetch();
    }

    /**
     * Получить количество сотрудников по каждой организации.Упорядочить по убыванию количества сотрудников и по возрастанию наименования организации
     */
    public List<Tuple> findUserCountByEachCompany(Session session) {
        return new JPAQueryFactory(session)
                .select(company.name, user.count())
                .from(company)
                .join(company.users, user)
                .groupBy(company)
                .orderBy(user.count().desc(), company.name.asc())
                .fetch();
    }

    /**
     * Найти отношение количества сотрудников каждой организации к общему количеству сотрудников
     */
    public List<Tuple> findCompaniesAndEmployeeCountRatio(Session session) {

        return new JPAQueryFactory(session)
                .select(company.name, user.count().castToNum(Double.class).divide(new JPAQuery<Long>(session).select(user.count()).from(user)))
                .from(company)
                .join(company.users, user)
                .groupBy(company)
                .orderBy(company.name.asc())
                .fetch();
    }

    public static UserDao getInstance() {
        return INSTANCE;
    }

}