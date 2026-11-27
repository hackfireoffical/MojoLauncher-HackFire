package git.artdeell.mojo.fragments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.fragments.instance.InstanceEditorFragment;
import git.artdeell.mojo.fragments.instance.InstanceTypeSelectFragment;
import git.artdeell.mojo.fragments.modding.ModManagerFragment;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;

import java.io.File;

/**
 * Per-installation hub inspired by the version-management workflow in Zalith Launcher 2.
 * Existing Mojo screens remain the source of truth for editing and loader installation.
 */
public class VersionManagerFragment extends Fragment {
    public static final String TAG = "VersionManagerFragment";
    private Instance instance;
    private LinearLayout content;

    public VersionManagerFragment() { super(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        instance = Instances.loadSelectedInstance();
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(getResources().getColor(R.color.background_app));
        scroll.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("Manage Version");
        title.setTextSize(24);
        title.setTextColor(getResources().getColor(R.color.primary_text));
        root.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText(instance == null ? "No installation selected" : instance.name + "  ·  " + instance.versionId);
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        subtitle.setTextColor(getResources().getColor(R.color.secondary_text));
        root.addView(subtitle);

        LinearLayout navigation = new LinearLayout(requireContext());
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(navigation);
        addNavButton(navigation, "Overview", this::showOverview);
        addNavButton(navigation, "Configuration", () -> open(InstanceEditorFragment.class, InstanceEditorFragment.TAG));
        addNavButton(navigation, "Add Loader", () -> open(InstanceTypeSelectFragment.class, InstanceTypeSelectFragment.TAG));
        addNavButton(navigation, "Mods", () -> open(ModManagerFragment.class, ModManagerFragment.TAG));

        content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(16), 0, 0);
        root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        showOverview();
        return scroll;
    }

    private void showOverview() {
        if (content == null) return;
        content.removeAllViews();
        if (instance == null) {
            addText(content, "Select an installation on the Home screen first.");
            return;
        }
        addSection("Installation");
        addText(content, instance.name + "\nMinecraft " + instance.versionId);
        addAction("Edit Description / Rename Version", () -> open(InstanceEditorFragment.class, InstanceEditorFragment.TAG));
        addAction("Add Loader / Modpack", () -> open(InstanceTypeSelectFragment.class, InstanceTypeSelectFragment.TAG));
        addSection("Management");
        addAction("Mods", () -> open(ModManagerFragment.class, ModManagerFragment.TAG));
        addAction("Saves", () -> openFolder("saves"));
        addAction("Resource Packs", () -> openFolder("resourcepacks"));
        addAction("Shader Packs", () -> openFolder("shaderpacks"));
        addAction("Screenshots", () -> openFolder("screenshots"));
        addAction("Servers", () -> openFolder("servers"));
        addSection("Quick Actions");
        addAction("Version Folder", () -> openFolder("."));
        addAction("Mods Folder", () -> openFolder("mods"));
        addAction("Logs", () -> openFolder("logs"));
        addAction("Crash Reports", () -> openFolder("crash-reports"));
    }

    private void addSection(String text) {
        TextView label = new TextView(requireContext());
        label.setText(text);
        label.setTextSize(18);
        label.setTextColor(getResources().getColor(R.color.primary_text));
        label.setPadding(0, dp(12), 0, dp(8));
        content.addView(label);
    }

    private void addText(LinearLayout parent, String text) {
        TextView label = new TextView(requireContext());
        label.setText(text);
        label.setTextSize(15);
        label.setTextColor(getResources().getColor(R.color.secondary_text));
        label.setPadding(dp(12), dp(12), dp(12), dp(12));
        parent.addView(label);
    }

    private void addAction(String text, Runnable action) {
        Button button = new Button(requireContext());
        button.setText(text);
        button.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(7);
        content.addView(button, lp);
        button.setOnClickListener(v -> action.run());
    }

    private void addNavButton(LinearLayout parent, String text, Runnable action) {
        Button button = new Button(requireContext());
        button.setText(text);
        button.setAllCaps(false);
        parent.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
        button.setOnClickListener(v -> action.run());
    }

    private void openFolder(String child) {
        if (instance == null) return;
        File folder = child.equals(".") ? instance.getGameDirectory() : new File(instance.getGameDirectory(), child);
        if (!folder.exists() && !folder.mkdirs()) {
            Toast.makeText(requireContext(), "Unable to create folder", Toast.LENGTH_SHORT).show();
            return;
        }
        Tools.openPath(requireContext(), folder, false);
    }

    private void open(Class<? extends Fragment> fragment, String tag) {
        Tools.swapFragment(requireActivity(), fragment, tag, new Bundle());
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
