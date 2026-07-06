package cc.aoeiuv020.panovel.detail;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Checkable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.internal.CheckableImageButton;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityEventCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/**
 * @see CheckableImageButton
 */

@SuppressWarnings("ALL")
public class CheckableFloatingActionButton extends FloatingActionButton implements Checkable {

    private static final int[] DRAWABLE_STATE_CHECKED = new int[]{android.R.attr.state_checked};

    private boolean checked;

    public CheckableFloatingActionButton(Context context) {
        this(context, null);
    }

    public CheckableFloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckableFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onInitializeAccessibilityEvent(host, event);
                event.setChecked(isChecked());
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(View host,
                                                          AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setCheckable(true);
                info.setChecked(isChecked());
            }
        });
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            refreshDrawableState();
            sendAccessibilityEvent(
                    AccessibilityEventCompat.TYPE_WINDOW_CONTENT_CHANGED);
        }
    }

    @Override
    public void toggle() {
        setChecked(!this.checked);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (checked) {
            return mergeDrawableStates(
                    super.onCreateDrawableState(extraSpace + DRAWABLE_STATE_CHECKED.length),
                    DRAWABLE_STATE_CHECKED);
        } else {
            return super.onCreateDrawableState(extraSpace);
        }
    }
}
