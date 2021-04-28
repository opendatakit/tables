package org.opendatakit.espresso;

import android.Manifest;
import android.webkit.WebView;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.web.model.Atom;
import androidx.test.espresso.web.model.ElementReference;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.UAUtils;

import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;

/**
 * Basic sample that shows the usage of Espresso web showcasing API.
 * <p/>
 * The sample has a simple layout which contains a single {@link WebView}. The HTML page displays
 * a form with an input tag and buttons to submit the form.
 */
@LargeTest
public class HTMLSelectTest {

    private Boolean initSuccess = null;
    private UiDevice mDevice;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<MainActivity>(
            MainActivity.class, false, true) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            if (initSuccess == null) {
                mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
                initSuccess = UAUtils.turnOnCustomHome(mDevice);
            }
        }

        @Override
        protected void afterActivityLaunched() {
            super.afterActivityLaunched();

            onWebView().forceJavascriptEnabled();
        }
    };

    // don't annotate used in chain rule
    private GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
    );

    @Rule
    public TestRule chainedRules = RuleChain
            .outerRule(grantPermissionRule)
            .around(mActivityRule);

    @Before
    public void setup() {
        UAUtils.assertInitSucess(initSuccess);
    }

    @Test
    public void infiniteTestToReplicateSigabrt() {
        if (true)
            return;

        // Run through the Tables app an infinite number of times to get a
        // crash
        int numOfTimesToRun = 400;
        int numOfMsToSleep = 0;
        for (int i = 0; i < numOfTimesToRun; i++) {
            boolean found = false;
            while (!found) {
                found = true;
                try {
                    Thread.sleep(numOfMsToSleep);
                    onWebView()
                            // Find the input element by tag name
                            .withElement(findElement(Locator.TAG_NAME, "select"))
                            // click on the select tag
                            .perform(webClick());
                } catch (RuntimeException e) {
                    //e.printStackTrace();
                    System.out.println("Failed to find the select tag");
                    found = false;
                } catch (InterruptedException ie) {
                    System.out.println("Error with thread sleep");
                }
            }

            Espresso.pressBack();

            System.out.println("Number of iterations = " + i);
        }
    }
}
