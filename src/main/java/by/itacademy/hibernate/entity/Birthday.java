package by.itacademy.hibernate.entity;

import com.querydsl.core.annotations.PropertyType;
import com.querydsl.core.annotations.QueryType;
import com.sun.istack.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
public class Birthday implements Comparable<Birthday> {

    private final LocalDate birthDate;

    public Birthday(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public long getAge() {
        return ChronoUnit.YEARS.between(birthDate, LocalDate.now());
    }

    @Override
    public int compareTo(Birthday o) {
        return this.birthDate.compareTo(o.birthDate);
    }

}
