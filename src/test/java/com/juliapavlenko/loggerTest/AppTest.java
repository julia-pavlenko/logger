package com.juliapavlenko.loggerTest;

import com.juliapavlenko.logger.App;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by julia on 14.01.18.
 */
public class AppTest {

    @Test
    public void calculateFactorialTest() {
        App app = new App();
        assertEquals("Factorial of 3 equals 6", 6, app.calculateFactorial(3));
    }
}
