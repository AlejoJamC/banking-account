package com.waes.rabobank.bankingaccount.infrastructure.persistence.converter;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.YearMonth;
import static org.junit.jupiter.api.Assertions.*;

class YearMonthConverterTest {

    private final YearMonthConverter converter = new YearMonthConverter();

    @Test
    void shouldConvertYearMonthToLocalDateWhenStoringInDatabase() {
        YearMonth yearMonth = YearMonth.of(2027, 3);
        LocalDate expected = LocalDate.of(2027, 3, 1);
        assertEquals(expected, converter.convertToDatabaseColumn(yearMonth));
    }

    @Test
    void shouldReturnNullWhenConvertingNullYearMonthToDatabase() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void shouldConvertLocalDateToYearMonthWhenLoadingFromDatabase() {
        LocalDate date = LocalDate.of(2030, 9, 15);
        YearMonth expected = YearMonth.of(2030, 9);
        assertEquals(expected, converter.convertToEntityAttribute(date));
    }

    @Test
    void shouldReturnNullWhenConvertingNullLocalDateToEntity() {
        assertNull(converter.convertToEntityAttribute(null));
    }
}
