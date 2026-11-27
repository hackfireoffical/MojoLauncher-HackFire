package git.artdeell.mojo.fragments;

import android.os.Bundle;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.fragments.instance.InstanceEditorFragment;
import git.artdeell.mojo.fragments.modding.ModManagerFragment;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.modding.PlatformMetadata;
import git.artdeell.mojo.modding.modplatforms.models.ModrinthIndex;
import me.andreasmelone.basicmodinfoparser.platform.Platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Per-installation hub inspired by the version-management workflow in Zalith Launcher 2.
 * Existing Mojo screens remain the source of truth for editing and loader installation.
 */
public class VersionManagerFragment extends Fragment {
    public static final String TAG = "VersionManagerFragment";
    private Instance instance;
    private LinearLayout content;
    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/x-modrinth-modpack"), this::exportToUri);

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

        HorizontalScrollView navScroll = new HorizontalScrollView(requireContext());
        navScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout navigation = new LinearLayout(requireContext());
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navScroll.addView(navigation);
        root.addView(navScroll);
        addNavButton(navigation, "Overview", this::showOverview);
        addNavButton(navigation, "Configuration", () -> open(InstanceEditorFragment.class, InstanceEditorFragment.TAG));
        addNavButton(navigation, "Modify Version", () -> open(ModifyVersionFragment.class, ModifyVersionFragment.TAG));
        addNavButton(navigation, "Mods", () -> open(ModManagerFragment.class, ModManagerFragment.TAG));
        addNavButton(navigation, "Saves", () -> openFolder("saves"));
        addNavButton(navigation, "Resource Packs", () -> openFolder("resourcepacks"));
        addNavButton(navigation, "Shader Packs", () -> openFolder("shaderpacks"));
        addNavButton(navigation, "Screenshots", () -> openFolder("screenshots"));
        addNavButton(navigation, "Servers", this::openServers);

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
        String description = instance.description == null || instance.description.trim().isEmpty()
                ? "" : "\n" + instance.description;
        addText(content, instance.name + "\nMinecraft " + instance.versionId + description +
                (instance.moddingData == null ? "\nVanilla installation" : "\nModded installation · " + instance.moddingData.platform));
        addAction("Add card to Home screen · Set as selected profile", () -> {
            Instances.setSelectedInstance(instance);
            Toast.makeText(requireContext(), "Installation selected for Home", Toast.LENGTH_SHORT).show();
        });
        addAction("Custom Icon", () -> open(InstanceEditorFragment.class, InstanceEditorFragment.TAG));
        addAction("Edit Description", () -> open(InstanceEditorFragment.class, InstanceEditorFragment.TAG));
        addAction("Rename Version", () -> open(InstanceEditorFragment.class, InstanceEditorFragment.TAG));
        addAction("Modify Minecraft / Install Loader", () -> open(ModifyVersionFragment.class, ModifyVersionFragment.TAG));
        addAction("Install Modpack", () -> open(git.artdeell.mojo.fragments.modding.ModpackSearchFragment.class, git.artdeell.mojo.fragments.modding.ModpackSearchFragment.TAG));
        addAction("Export Modpack (.mrpack)", () -> exportLauncher.launch(instance.name.replaceAll("[^A-Za-z0-9._ -]", "_") + ".mrpack"));
        addAction("Delete Version", () -> open(InstanceEditorFragment.class, InstanceEditorFragment.TAG));
        addSection("Management");
        addAction("Manage Mods", () -> open(ModManagerFragment.class, ModManagerFragment.TAG));
        addAction("Running Log · Share latest log", () -> {
            File log = new File(instance.getGameDirectory(), "logs/latest.log");
            if (!log.exists()) log = new File(instance.getGameDirectory(), "latestlog.txt");
            if (log.exists()) Tools.openPath(requireContext(), log, true);
            else Toast.makeText(requireContext(), "No log file is available yet", Toast.LENGTH_SHORT).show();
        });
        addSection("Quick Actions · Installation Folders");
        addAction("Version Folder", () -> openFolder("."));
        addAction("Saves", () -> openFolder("saves"));
        addAction("Resource Packs", () -> openFolder("resourcepacks"));
        addAction("Shader Packs", () -> openFolder("shaderpacks"));
        addAction("Mods", () -> openFolder("mods"));
        addAction("Screenshots", () -> openFolder("screenshots"));
        addAction("Logs", () -> openFolder("logs"));
        addAction("Crash Reports", () -> openFolder("crash-reports"));
        addAction("Servers", this::openServers);
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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = dp(6);
        parent.addView(button, lp);
        button.setOnClickListener(v -> action.run());
    }


    private void exportToUri(Uri destination) {
        if (destination == null || instance == null) return;
        Instance exportInstance = instance;
        android.content.ContentResolver resolver = requireContext().getContentResolver();
        MojoApplication.sExecutorService.execute(() -> {
            try {
                OutputStream output = resolver.openOutputStream(destination);
                if (output == null) throw new IOException("Unable to open export destination");
                try (OutputStream stream = output; ZipOutputStream zip = new ZipOutputStream(stream)) {
                ModrinthIndex index = new ModrinthIndex();
                index.formatVersion = 1;
                index.game = "minecraft";
                index.name = exportInstance.name;
                index.versionId = exportInstance.versionId;
                index.summary = exportInstance.description == null ? "Exported from Pleiades / Mojo Launcher" : exportInstance.description;
                index.files = new ModrinthIndex.ModrinthIndexFile[0];
                Map<String, String> dependencies = new LinkedHashMap<>();
                dependencies.put("minecraft", exportInstance.moddingData == null ? exportInstance.versionId : exportInstance.moddingData.gameVersion);
                PlatformMetadata metadata = exportInstance.moddingData;
                if (metadata != null && metadata.platform != null && metadata.modloaderVersion != null) {
                    switch (metadata.platform) {
                        case FABRIC: dependencies.put("fabric-loader", metadata.modloaderVersion); break;
                        case QUILT: dependencies.put("quilt-loader", metadata.modloaderVersion); break;
                        case NEOFORGE: dependencies.put("neoforge", metadata.modloaderVersion); break;
                        case FORGE:
                        case FORGE_LEGACY: dependencies.put("forge", metadata.modloaderVersion); break;
                        default: break;
                    }
                }
                index.dependencies = dependencies;
                zip.putNextEntry(new ZipEntry("modrinth.index.json"));
                zip.write(Tools.GLOBAL_GSON.toJson(index).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                File gameDir = exportInstance.getGameDirectory();
                String[] folders = {"mods", "config", "resourcepacks", "shaderpacks", "defaultconfigs", "kubejs", "scripts"};
                for (String folder : folders) {
                    File source = new File(gameDir, folder);
                    if (source.exists()) addOverrideTree(zip, source, "overrides/" + folder);
                }
                File options = new File(gameDir, "options.txt");
                if (options.isFile()) addOverrideFile(zip, options, "overrides/options.txt");
                zip.finish();
                }
                Tools.runOnUiThread(() -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Modpack exported", Toast.LENGTH_LONG).show();
                });
            } catch (Exception error) {
                Tools.runOnUiThread(() -> Tools.showErrorRemote(error));
            }
        });
    }

    private static void addOverrideTree(ZipOutputStream zip, File root, String entryRoot) throws IOException {
        File[] children = root.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) addOverrideTree(zip, child, entryRoot + "/" + child.getName());
            else if (child.isFile() && !child.getName().endsWith(".disabled"))
                addOverrideFile(zip, child, entryRoot + "/" + child.getName());
        }
    }

    private static void addOverrideFile(ZipOutputStream zip, File file, String entryName) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) zip.write(buffer, 0, count);
        }
        zip.closeEntry();
    }

    private void openServers() {
        if (instance == null) return;
        File servers = new File(instance.getGameDirectory(), "servers.dat");
        if (servers.isFile()) Tools.openPath(requireContext(), servers, false);
        else Toast.makeText(requireContext(), "No saved servers in this installation yet", Toast.LENGTH_SHORT).show();
    }

    private void openFolder(String child) {
        if (instance == null) return;
        File folder = child.equals(".") ? instance.getInstanceDirectory() : new File(instance.getGameDirectory(), child);
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
