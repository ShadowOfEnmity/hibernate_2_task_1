package by.itacademy.hibernate.dto;

import by.itacademy.hibernate.entity.Birthday;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BirthDayRangeFilter {
    Birthday begin;
    Birthday end;
}
