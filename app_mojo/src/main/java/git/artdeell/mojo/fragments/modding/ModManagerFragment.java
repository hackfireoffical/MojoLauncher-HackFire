package git.artdeell.mojo.fragments.modding;

import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.contracts.OpenDocumentWithExtension;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.modding.modmanager.DependencyScanner;
import git.artdeell.mojo.modding.modmanager.ModAdapter;
import git.artdeell.mojo.modding.modmanager.ModInfo;
import git.artdeell.mojo.modding.modmanager.ModScanner;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import git.artdeell.mojo.R;
import me.andreasmelone.basicmodinfoparser.platform.Platform;

public class ModManagerFragment extends Fragment {
    public static final String TAG = "ModManagerFragment";
    private RecyclerView mModRecyclerView;
    private ProgressBar mProgressBar;
    private EditText mSearchEditText;
    private File mModsFolder;

    private Instance mInstance;
    private final ActivityResultLauncher<Object> mModImportLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), uri -> {
                if (uri == null || getContext() == null || mInstance == null) return;
                Context context = requireContext();
                String fileName = Tools.getFileName(context, uri);
                MojoApplication.sExecutorService.execute(() -> importMod(context, uri, fileName));
            });

    private void importMod(Context context, Uri uri, String fileName) {
        try {
            if (!mModsFolder.exists() && !mModsFolder.mkdirs()) throw new IOException("Unable to create mods folder");
            File target = new File(mModsFolder, fileName);
            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 OutputStream output = new FileOutputStream(target)) {
                if (input == null) throw new IOException("Unable to read selected file");
                IOUtils.copy(input, output);
            }
            Tools.runOnUiThread(() -> {
                if (!isAdded()) return;
                android.widget.Toast.makeText(requireContext(), "Mod imported", android.widget.Toast.LENGTH_SHORT).show();
                refreshModsList();
            });
        } catch (IOException error) {
            Tools.runOnUiThread(() -> Tools.showErrorRemote(error));
        }
    }

    public ModManagerFragment() {
        super(R.layout.fragment_mod_manager);
    }

    private void refreshModsList() {
        mProgressBar.setVisibility(View.VISIBLE);
        MojoApplication.sExecutorService.execute(()->{
            Instance instance = Instances.loadSelectedInstance();
            if(instance == null  || instance.moddingData == null || instance.moddingData.platform == null) {
                Tools.runOnUiThread(() -> mProgressBar.setVisibility(View.GONE));
                return;
            }
            Platform modPlatform = instance.moddingData.platform;
            List<ModInfo> modInfoList = ModScanner.findMods(mModsFolder, modPlatform);
            DependencyScanner scanner = new DependencyScanner(modPlatform, modInfoList);
            Tools.runOnUiThread(()->{
                if(isRemoving() || isDetached()) return;
                ModAdapter modAdapter = new ModAdapter(modInfoList, modPlatform, scanner);
                mModRecyclerView.setAdapter(modAdapter);
                modAdapter.performSearch(m->true, mProgressBar);
            });
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mInstance = Instances.loadSelectedInstance();
        if (mInstance == null) {
            Tools.backToMainMenu(requireActivity());
            return;
        }
        mModsFolder = new File(mInstance.getGameDirectory(), "mods");
        mModRecyclerView = view.findViewById(R.id.mod_manager_recyclerview);
        mProgressBar = view.findViewById(R.id.search_mod_progressbar);
        View mOverlay = view.findViewById(R.id.search_mod_overlay);
        Button mImportButton = view.findViewById(R.id.mod_manager_import_button);
        Button mDownloadButton = view.findViewById(R.id.mod_manager_download_button);
        mImportButton.setOnClickListener(v -> mModImportLauncher.launch(null));
        mDownloadButton.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), ModSearchFragment.class, ModSearchFragment.TAG, new Bundle()));
        mSearchEditText = view.findViewById(R.id.search_mod_edittext);
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                ModAdapter adapter = (ModAdapter) mModRecyclerView.getAdapter();
                if (adapter != null) {
                    String query = s.toString().trim().toLowerCase();
                    adapter.performSearch(mod -> query.isEmpty() || mod.searchTerms.contains(query), mProgressBar);
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        mModRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        Tools.setupOverlayView(mModRecyclerView, mOverlay);
    }

    @Override
    public void onResume(){
        super.onResume();
        refreshModsList();
    }
}
