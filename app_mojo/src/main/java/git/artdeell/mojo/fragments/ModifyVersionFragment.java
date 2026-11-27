package git.artdeell.mojo.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.fragments.instance.InstanceEditorFragment;
import git.artdeell.mojo.fragments.modding.instance.FabricInstallFragment;
import git.artdeell.mojo.fragments.modding.instance.ForgeInstallFragment;
import git.artdeell.mojo.fragments.modding.instance.NeoforgeInstallFragment;
import git.artdeell.mojo.fragments.modding.instance.OptiFineInstallFragment;
import git.artdeell.mojo.fragments.modding.instance.QuiltInstallFragment;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;
import me.andreasmelone.basicmodinfoparser.platform.Platform;

/** ZL2-style component picker that modifies the currently selected installation. */
public class ModifyVersionFragment extends Fragment {
    public static final String TAG = "ModifyVersionFragment";

    public ModifyVersionFragment() { super(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Instance instance = Instances.loadSelectedInstance();
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        root.setPadding(padding, padding, padding, padding);
        root.setBackgroundColor(getResources().getColor(R.color.background_app));
        scroll.addView(root);

        TextView heading = new TextView(requireContext());
        heading.setText("Modify Version");
        heading.setTextSize(23);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(getResources().getColor(R.color.primary_text));
        root.addView(heading);

        TextView description = new TextView(requireContext());
        description.setText(instance == null ? "No installation selected" : instance.name + " · Minecraft " + instance.versionId);
        description.setTextSize(14);
        description.setTextColor(getResources().getColor(R.color.secondary_text));
        description.setPadding(0, dp(5), 0, dp(16));
        root.addView(description);

        if (instance == null) return scroll;
        addCard(root, "Minecraft", "Change the base Minecraft version", true,
                () -> open(InstanceEditorFragment.class, InstanceEditorFragment.TAG));
        addCard(root, "OptiFine", "Install or update OptiFine", instance.versionId != null && instance.versionId.toLowerCase().contains("optifine"),
                () -> open(OptiFineInstallFragment.class, OptiFineInstallFragment.TAG));
        addCard(root, "Forge", "Install or update Minecraft Forge", isInstalled(instance, Platform.FORGE) || isInstalled(instance, Platform.FORGE_LEGACY),
                () -> open(ForgeInstallFragment.class, ForgeInstallFragment.TAG));
        addCard(root, "NeoForge", "Install or update NeoForge", isInstalled(instance, Platform.NEOFORGE),
                () -> open(NeoforgeInstallFragment.class, NeoforgeInstallFragment.TAG));
        addCard(root, "Fabric", "Install or update Fabric Loader", isInstalled(instance, Platform.FABRIC),
                () -> open(FabricInstallFragment.class, FabricInstallFragment.TAG));
        addCard(root, "Quilt", "Install or update Quilt Loader", isInstalled(instance, Platform.QUILT),
                () -> open(QuiltInstallFragment.class, QuiltInstallFragment.TAG));
        return scroll;
    }

    private boolean isInstalled(Instance instance, Platform platform) {
        return instance.moddingData != null && instance.moddingData.platform == platform;
    }

    private void addCard(LinearLayout parent, String title, String subtitle, boolean installed, Runnable action) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(13), dp(16), dp(13));
        card.setBackgroundResource(R.drawable.background_line);
        card.setClickable(true);
        card.setFocusable(true);
        TextView name = new TextView(requireContext());
        name.setText(title);
        name.setTextSize(17);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setTextColor(getResources().getColor(R.color.primary_text));
        card.addView(name);
        TextView detail = new TextView(requireContext());
        detail.setText(subtitle + (installed ? " · Installed" : " · Not installed"));
        detail.setTextSize(13);
        detail.setTextColor(getResources().getColor(R.color.secondary_text));
        detail.setPadding(0, dp(3), 0, 0);
        card.addView(detail);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(9);
        parent.addView(card, lp);
        card.setOnClickListener(v -> action.run());
    }

    private void open(Class<? extends Fragment> fragment, String tag) {
        Tools.swapFragment(requireActivity(), fragment, tag, new Bundle());
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
