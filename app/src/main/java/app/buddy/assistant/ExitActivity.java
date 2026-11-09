package app.buddy.assistant;

import android.app.Activity;
import android.os.Bundle;

/** A notification activity, not a receiver trampoline: latch Exit before opening Android's App info. */
public final class ExitActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppLifecycle.exit(this, true);
        finish();
    }
}
