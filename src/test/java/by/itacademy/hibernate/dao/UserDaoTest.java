package by.itacademy.hibernate.dao;


import by.itacademy.hibernate.dto.BirthDayRangeFilter;
import by.itacademy.hibernate.entity.Birthday;
import by.itacademy.hibernate.utils.TestDataImporter;
import by.itacademy.hibernate.entity.Payment;
import by.itacademy.hibernate.entity.User;
import by.itacademy.hibernate.util.HibernateUtil;
import com.querydsl.core.Tuple;
import lombok.Cleanup;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
class UserDaoTest {

    private final SessionFactory sessionFactory = HibernateUtil.buildSessionFactory();
    private final UserDao userDao = UserDao.getInstance();

    @BeforeAll
    public void initDb() {
        TestDataImporter.importData(sessionFactory);
    }

    @AfterAll
    public void finish() {
        sessionFactory.close();
    }

    @Test
    void findAll() {
        @Cleanup Session session = sessionFactory.openSession();
        session.beginTransaction();

        List<User> results = userDao.findAll(session);
        assertThat(results).hasSize(5);

        List<String> fullNames = results.stream().map(User::fullName).collect(toList());
        assertThat(fullNames).containsExactlyInAnyOrder("Bill Gates", "Steve Jobs", "Sergey Brin", "Tim Cook", "Diane Greene");

        session.getTransaction().commit();
    }

    @Test
    void findAllByFirstName() {
        @Cleanup Session session = sessionFactory.openSession();
        session.beginTransaction();

        List<User> results = userDao.findAllByFirstName(session, "Bill");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).fullName()).isEqualTo("Bill Gates");

        session.getTransaction().commit();
    }

    @Test
    void findLimitedUsersOrderedByBirthday() {
        @Cleanup Session session = sessionFactory.openSession();
        session.beginTransaction();

        int limit = 3;
        List<User> results = userDao.findLimitedUsersOrderedByBirthday(session, limit);
        assertThat(results).hasSize(limit);

        List<String> fullNames = results.stream().map(User::fullName).collect(toList());
        assertThat(fullNames).contains("Diane Greene", "Steve Jobs", "Bill Gates");

        session.getTransaction().commit();
    }

    @Test
    void findAllByCompanyName() {
        @Cleanup Session session = sessionFactory.openSession();
        session.beginTransaction();

        List<User> results = userDao.findAllByCompanyName(session, "Google");
        assertThat(results).hasSize(2);

        List<String> fullNames = results.stream().map(User::fullName).collect(toList());
        assertThat(fullNames).containsExactlyInAnyOrder("Sergey Brin", "Diane Greene");

        session.getTransaction().commit();
    }

    @Test
    void findAllPaymentsByCompanyName() {
        @Cleanup Session session = sessionFactory.openSession();
        session.beginTransaction();

        List<Payment> applePayments = userDao.findAllPaymentsByCompanyName(session, "Apple");
        assertThat(applePayments).hasSize(5);

        List<Integer> amounts = applePayments.stream().map(Payment::getAmount).collect(toList());
        assertThat(amounts).contains(250, 500, 600, 300, 400);

        session.getTransaction().commit();
    }

    @Test
    void findAveragePaymentAmountByFirstAndLastNames() {
        @Cleanup Session session = sessionFactory.openSession();
        session.beginTransaction();

        Double averagePaymentAmount = userDao.findAveragePaymentAmountByFirstAndLastNames(session, "Bill", "Gates");
        assertThat(averagePaymentAmount).isEqualTo(300.0);

        session.getTransaction().commit();
    }

    @Test
    void findCompanyNamesWithAvgUserPaymentsOrderedByCompanyName() {
        @Cleanup Session session = sessionFactory.openSession();
        session.beginTransaction();

        List<Object[]> results = userDao.findCompanyNamesWithAvgUserPaymentsOrderedByCompanyName(session);
        assertThat(results).hasSize(3);

        List<String> orgNames = results.stream().map(a -> (String) a[0]).collect(toList());
        assertThat(orgNames).contains("Apple", "Google", "Microsoft");

        List<Double> orgAvgPayments = results.stream().map(a -> (Double) a[1]).collect(toList());
        assertThat(orgAvgPayments).contains(410.0, 400.0, 300.0);

        session.getTransaction().commit();
    }

    @Test
    void isItPossible() {
        @Cleanup Session session = sessionFactory.openSession();
        session.beginTransaction();

        List<Object[]> results = userDao.isItPossible(session);
        assertThat(results).hasSize(2);

        List<String> names = results.stream().map(r -> ((User) r[0]).fullName()).collect(toList());
        assertThat(names).contains("Sergey Brin", "Steve Jobs");

        List<Double> averagePayments = results.stream().map(r -> (Double) r[1]).collect(toList());
        assertThat(averagePayments).contains(500.0, 450.0);

        session.getTransaction().commit();
    }


    @Nested
    @DisplayName("Testing queries written using query dsl")
    class QueryDsl {

        @Test
        void findUsersByCompanyNameWithPaymentsGreaterThanOrEqualToSetValue() {
            @Cleanup Session session = sessionFactory.openSession();
            session.beginTransaction();

            List<Tuple> results = userDao.findUsersByCompanyNameWithAvgPaymentsGreaterThanOrEqualToSetValue(session, "Google", 300);
            assertThat(results).hasSize(2);

            List<String> names = results.stream().map(tuple -> Objects.requireNonNull(tuple.get(0, User.class)).fullName()).toList();
            assertThat(names).containsExactly("Diane Greene", "Sergey Brin");

            List<Double> averagePayments = results.stream().map(tuple -> Objects.requireNonNull(tuple.get(1, Double.class))).toList();
            assertThat(averagePayments).containsExactly(300.0D, 500.0D);

            session.getTransaction().commit();
        }

        @Test
        void findUsersByBirthdayDateRange() {
            @Cleanup Session session = sessionFactory.openSession();
            session.beginTransaction();

            BirthDayRangeFilter dateFilter = BirthDayRangeFilter.builder()
                    .begin(new Birthday(LocalDate.of(1955, 1, 1)))
                    .end(new Birthday(LocalDate.of(1955, 2, 24)))
                    .build();

            List<User> results = userDao.findUsersByBirthdayDateRange(session, dateFilter);

            assertThat(results).hasSize(2);

            List<String> names = results.stream().map(User::fullName).collect(toList());
            assertThat(names).contains("Steve Jobs", "Diane Greene");

            session.getTransaction().commit();
        }

        @Test
        void findMaxPaymentByEachCompanyOrderedByCompanyName() {
            @Cleanup Session session = sessionFactory.openSession();
            session.beginTransaction();

            List<Tuple> paymentsByOrganizations = userDao.findMaxPaymentByEachCompanyOrderedByCompanyName(session);

            assertThat(paymentsByOrganizations).hasSize(3);

            List<String> names = paymentsByOrganizations.stream().map(tuple -> tuple.get(0, String.class)).collect(toList());
            assertThat(names).containsExactly("Microsoft", "Google", "Apple");


            List<Double> averagePayments = paymentsByOrganizations.stream().map(tuple -> Objects.requireNonNull(tuple.get(1, Double.class))).toList();
            assertThat(averagePayments).containsExactly(300.0D, 400.0D, 410.0D);

            session.getTransaction().commit();
        }

        @Test
        void findUserCountByEachCompany() {
            @Cleanup Session session = sessionFactory.openSession();
            session.beginTransaction();

            List<Tuple> results = userDao.findUserCountByEachCompany(session);
            assertThat(results).hasSize(3);

            List<String> names = results.stream().map(tuple -> tuple.get(0, String.class)).collect(toList());
            assertThat(names).containsExactly("Apple", "Google", "Microsoft");

            List<Long> counts = results.stream().map(tuple -> tuple.get(1, Long.class)).collect(toList());

            assertThat(counts).containsExactly(2L, 2L, 1L);
            session.getTransaction().commit();
        }

        @Test
        void findCompaniesAndEmployeeCountRatio() {
            @Cleanup Session session = sessionFactory.openSession();
            session.beginTransaction();
            List<Tuple> results = userDao.findCompaniesAndEmployeeCountRatio(session);

            assertThat(results).hasSize(3);

            List<String> names = results.stream().map(tuple -> tuple.get(0, String.class)).collect(toList());
            assertThat(names).containsExactly("Apple", "Google", "Microsoft");

            List<Double> ratioList = results.stream().map(tuple -> tuple.get(1, Double.class)).toList();

            assertThat(ratioList).containsExactly(0.4D, 0.4D, 0.2D);

            session.getTransaction().commit();
        }
    }

}
