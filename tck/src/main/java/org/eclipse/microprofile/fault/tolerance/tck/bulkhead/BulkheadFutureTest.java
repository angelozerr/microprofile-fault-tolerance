/*
 *******************************************************************************
 * Copyright (c) 2017-2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Checker;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.FutureChecker;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;

/**
 * This set of tests will test correct operation on the relevant methods of the
 * Future object that is returned from the business method of a Asynchronous
 * Method or Class.
 *
 * @author Gordon Hutchison
 * @author carlosdlr
 */
public class BulkheadFutureTest extends Arquillian {

    private static final int SHORT_TIME = 100;
    @Inject
    private BulkheadMethodAsynchronousDefaultBean bhBeanMethodAsynchronousDefault;
    @Inject
    private BulkheadClassAsynchronousDefaultBean bhBeanClassAsynchronousDefault;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadFutureTest.jar")
                .addPackage(FutureChecker.class.getPackage())
                .addClass(Utils.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
        return ShrinkWrap.create(WebArchive.class, "ftBulkheadTest.war").addAsLibrary(testJar);
    }

    @BeforeTest
    public void beforeTest(final ITestContext testContext) {
        Utils.log("Testmethod: " + testContext.getName());
    }

    /**
     * Tests that the Future that is returned from an asynchronous bulkhead
     * method can be queried for Done OK before and after a goodpath .get()
     */
    @Test
    public void testBulkheadMethodAsynchFutureDoneAfterGet() {
        Checker fc = new FutureChecker(SHORT_TIME);
        Future<String> result = null;

        try {
            result = bhBeanMethodAsynchronousDefault.test(fc);
        }
        catch (InterruptedException e1) {
            fail("Unexpected interruption", e1);
        }

        assertFalse(result.isDone(), "Future reporting Done when not");
        try {
            assertEquals(result.get(), "RESULT");
            assertEquals(result.get(1, TimeUnit.SECONDS), "RESULT");
        }
        catch (Throwable t) {
            fail("Unexpected exception", t);
        }
        assertTrue(result.isDone(), "Future done not reporting true");
    }

    /**
     * Tests that the Future that is returned from a asynchronous bulkhead
     * method can be queried for Done OK even if the user never calls get() to
     * drive the backend (i.e. the method is called non-lazily)
     */
    @Test
    public void testBulkheadMethodAsynchFutureDoneWithoutGet() {
        Checker fc = new FutureChecker(SHORT_TIME);
        try {
            final Future<String> result = bhBeanMethodAsynchronousDefault.test(fc);
            assertFalse(result.isDone(), "Future reporting Done when not");
            await().atMost(SHORT_TIME * 100, MILLISECONDS).untilAsserted(()-> assertTrue(result.isDone()));
        }
        catch (InterruptedException e1) {
            fail("Unexpected interruption", e1);
        }
    }

    /**
     * Tests that the Future that is returned from a asynchronous bulkhead can
     * be queried for Done OK after a goodpath get with timeout and also
     * multiple gets can be called ok. This test is for the annotation at a
     * Class level.
     */
    @Test
    public void testBulkheadClassAsynchFutureDoneAfterGet() {
        Checker fc = new FutureChecker(SHORT_TIME);
        Future<String> result = null;

        try {
            result = bhBeanClassAsynchronousDefault.test(fc);
        }
        catch (InterruptedException e1) {
            fail("Unexpected interruption", e1);
        }

        assertFalse(result.isDone(), "Future reporting Done when not");
        try {
            assertEquals(result.get(1, TimeUnit.SECONDS), "RESULT");
            assertEquals(result.get(), "RESULT");

        }
        catch (Throwable t) {
            fail("Unexpected exception", t);
        }
        assertTrue(result.isDone(), "Future done not reporting true");
    }

    /**
     * Tests that the Future that is returned from a asynchronous bulkhead can
     * be queried for Done OK when get() is not called. This test is for the
     * annotation at a Class level.
     */
    @Test
    public void testBulkheadClassAsynchFutureDoneWithoutGet() {
        Checker fc = new FutureChecker(SHORT_TIME);
        try {
            final Future<String> result = bhBeanClassAsynchronousDefault.test(fc);
            assertFalse(result.isDone(), "Future reporting Done when not");
            await().atMost(SHORT_TIME * 100, MILLISECONDS).untilAsserted(()-> assertTrue(result.isDone()));
        }
        catch (InterruptedException e1) {
            fail("Unexpected interruption", e1);
        }
    }
}
