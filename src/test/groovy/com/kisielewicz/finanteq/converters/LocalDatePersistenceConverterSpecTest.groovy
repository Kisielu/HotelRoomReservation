package com.kisielewicz.finanteq.converters

import spock.lang.Specification

import java.sql.Date
import java.time.LocalDate

class LocalDatePersistenceConverterSpecTest extends Specification {

    LocalDatePersistenceConverter localDatePersistenceConverter = new LocalDatePersistenceConverter()

    def "should convert SQL date to LocalDate"() {
        given:
        Date date = new Date(Calendar.getInstance().getTimeInMillis())
        when:
        LocalDate result = localDatePersistenceConverter.convertToEntityAttribute(date)
        then:
        result.getYear() == LocalDate.now().getYear()
        result.getMonthValue() == LocalDate.now().getMonthValue()
        result.getDayOfMonth() == LocalDate.now().getDayOfMonth()
    }

    def "should convert LocalDate to SQL date"() {
        given:
        LocalDate localDate = LocalDate.now()
        when:
        Date result = localDatePersistenceConverter.convertToDatabaseColumn(localDate)
        then:
        result.toLocalDate().getYear() == LocalDate.now().getYear()
        result.toLocalDate().getMonthValue() == LocalDate.now().getMonthValue()
        result.toLocalDate().getDayOfMonth() == LocalDate.now().getDayOfMonth()
    }

    def "should throw NPE while converting null SQL date to LocalDate"() {
        given:
        Date date = null
        when:
        localDatePersistenceConverter.convertToEntityAttribute(date)
        then:
        NullPointerException e = thrown()
    }

    def "should throw NPE while converting LocalDate to SQL date"() {
        given:
        LocalDate localDate = null
        when:
        localDatePersistenceConverter.convertToDatabaseColumn(localDate)
        then:
        NullPointerException e = thrown()
    }
}
