package com.louise.udacity.mydict;

import org.joda.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testJodaDate() {
        LocalDate date = LocalDate.now();
        System.out.println("current date is " + date.toString());
        LocalDate later = date.plusDays(5);
        System.out.println("future date is " + later.toString());


    }
}