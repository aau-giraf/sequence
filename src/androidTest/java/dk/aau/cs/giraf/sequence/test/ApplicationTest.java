package dk.aau.cs.giraf.sequence.test;

import android.app.Application;
import android.test.ApplicationTestCase;

import junit.framework.Assert;

/*
 *
 * By convention, your test package name should follow the same name as the application package, suffixed with ".tests".
 * In the test package you created, add the Java class for your test case.
 * By convention, your test case name should also follow the same name as the Java or Android class that you want to test, but suffixed with “Test”.
 * Names of test methods must start with the word 'test'.
 */

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }
    /*
     * Test methods must be suffixed with the word test.
     */
    public void testExample() {
        Assert.assertEquals(true,true);
    }
}
