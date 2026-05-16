package com.ast.ut99;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Small Android-side data installer / preflight activity.
 *
 * Unreal Tournament itself is only launched once a readable data layout exists.
 * Accepted layouts are either:
 *   /Android/data/com.ast.ut99/files/UT99/System, Maps, Textures, Sounds, Music
 * or the older direct test layout:
 *   /Android/data/com.ast.ut99/files/System, Maps, Textures, Sounds, Music
 */
public class MainActivity extends Activity {
    private static final int REQ_SELECT_UT99_FOLDER = 3001;
    private static final int REQ_SELECT_UT99_ZIP = 3002;

    private File selectedRoot;
    private String lastImportMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        hideSystemUi();
        continueStartup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
    }

    private void continueStartup() {
        selectedRoot = UT99Paths.resolveDataRoot(this);
        UT99Paths.ensureSkeleton(UT99Paths.installRoot(this));

        if (UT99Paths.hasUsableGameData(selectedRoot)) {
            android.util.Log.i("UT99Installer", "data check OK root=" + selectedRoot.getAbsolutePath());
            launchGame(selectedRoot);
            return;
        }

        android.util.Log.w("UT99Installer", "data check failed root=" + selectedRoot.getAbsolutePath());
        showMissingDataScreen();
    }

    private void launchGame(File root) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(UT99Paths.EXTRA_DATA_ROOT, root.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private void hideSystemUi() {
        Window window = getWindow();
        if (window == null) return;
        View decor = window.getDecorView();
        if (decor == null) return;

        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private Locale currentLocale() {
        if (Build.VERSION.SDK_INT >= 24) {
            return getResources().getConfiguration().getLocales().get(0);
        }
        return getResources().getConfiguration().locale;
    }

    private boolean isGermanUi() {
        Locale locale = currentLocale();
        return locale != null && "de".equalsIgnoreCase(locale.getLanguage());
    }

    private String t(String de, String en) {
        return isGermanUi() ? de : en;
    }

    private Button button(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        return button;
    }

    private void showMissingDataScreen() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER);
        body.setPadding(48, 36, 48, 36);

        TextView title = new TextView(this);
        title.setText(t("Unreal Tournament-Daten fehlen", "Unreal Tournament data not found"));
        title.setTextSize(24.0f);
        title.setGravity(Gravity.CENTER);
        body.addView(title);

        String extra = "";
        if (lastImportMessage != null && lastImportMessage.length() > 0) {
            extra = t("\n\nLetzte Meldung:\n", "\n\nLast message:\n") + lastImportMessage;
        }

        TextView message = new TextView(this);
        message.setText(t(
                "Es wurde kein vollständiger UT99-Datenordner gefunden.\n\n" +
                        "Du kannst jetzt entweder den Unreal Tournament-Ordner auswählen oder eine ZIP-Datei importieren.\n\n" +
                        "Installationsziel:\n" + UT99Paths.installRoot(this).getAbsolutePath() + "\n\n" +
                        "Benötigt werden mindestens:\nSystem, Maps, Textures, Sounds, Music" + extra,
                "No complete UT99 data folder was found.\n\n" +
                        "Select the Unreal Tournament folder or import a ZIP file containing the game data.\n\n" +
                        "Install target:\n" + UT99Paths.installRoot(this).getAbsolutePath() + "\n\n" +
                        "Required folders:\nSystem, Maps, Textures, Sounds, Music" + extra));
        message.setTextSize(16.0f);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 24, 0, 24);
        body.addView(message);

        body.addView(button(t("UT99-Ordner auswählen", "Select UT99 folder"), v -> openFolderPicker()));
        body.addView(button(t("UT99-ZIP auswählen", "Select UT99 ZIP"), v -> openZipPicker()));
        body.addView(button(t("Erneut prüfen", "Check again"), v -> continueStartup()));

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(body);
        setContentView(scrollView);
        hideSystemUi();
    }

    private void showBusyScreen(String titleText, String messageText) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER);
        body.setPadding(48, 36, 48, 36);

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextSize(24.0f);
        title.setGravity(Gravity.CENTER);
        body.addView(title);

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        body.addView(progressBar);

        TextView message = new TextView(this);
        message.setText(messageText);
        message.setTextSize(16.0f);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 24, 0, 0);
        body.addView(message);

        setContentView(body);
        hideSystemUi();
    }

    private void openFolderPicker() {
        if (Build.VERSION.SDK_INT < 21) {
            lastImportMessage = t(
                    "Diese Android-Version hat keinen Ordnerauswahldialog. Kopiere die Daten manuell nach " + UT99Paths.installRoot(this).getAbsolutePath(),
                    "This Android version has no folder picker. Copy the data manually to " + UT99Paths.installRoot(this).getAbsolutePath());
            showMissingDataScreen();
            return;
        }

        try {
            Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(intent, REQ_SELECT_UT99_FOLDER);
        } catch (ActivityNotFoundException ex) {
            lastImportMessage = t("Kein kompatibler Ordnerauswahldialog gefunden: ",
                    "No compatible folder picker found: ") + ex.getMessage();
            showMissingDataScreen();
        }
    }

    private void openZipPicker() {
        if (Build.VERSION.SDK_INT < 19) {
            lastImportMessage = t(
                    "Diese Android-Version hat keinen Dateiauswahldialog. Entpacke die ZIP manuell nach " + UT99Paths.installRoot(this).getAbsolutePath(),
                    "This Android version has no file picker. Extract the ZIP manually to " + UT99Paths.installRoot(this).getAbsolutePath());
            showMissingDataScreen();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, t("UT99-ZIP auswählen", "Select UT99 ZIP")), REQ_SELECT_UT99_ZIP);
        } catch (ActivityNotFoundException ex) {
            lastImportMessage = t("Kein kompatibler Dateiauswahldialog gefunden: ",
                    "No compatible file picker found: ") + ex.getMessage();
            showMissingDataScreen();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            showMissingDataScreen();
            return;
        }

        Uri uri = data.getData();
        tryPersistPermission(data, uri);

        if (requestCode == REQ_SELECT_UT99_FOLDER) {
            installFromFolder(uri);
        } else if (requestCode == REQ_SELECT_UT99_ZIP) {
            installFromZip(uri);
        }
    }

    private void tryPersistPermission(Intent data, Uri uri) {
        if (Build.VERSION.SDK_INT < 19 || uri == null) return;
        try {
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
            // Some providers do not support persisted grants. The current one-shot import still works.
        }
    }

    private void installFromFolder(final Uri treeUri) {
        showBusyScreen(t("Installiere UT99-Daten", "Installing UT99 data"),
                t("Ordner wird kopiert …", "Copying folder …"));
        new Thread(() -> {
            final String result;
            try {
                InstallStats stats = importFolderTree(treeUri, UT99Paths.installRoot(this));
                result = t("Ordnerimport abgeschlossen: ", "Folder import complete: ") + stats.files + " files";
            } catch (Throwable ex) {
                android.util.Log.e("UT99Installer", "folder import failed", ex);
                runOnUiThread(() -> {
                    lastImportMessage = t("Ordnerimport fehlgeschlagen: ", "Folder import failed: ") + ex.getMessage();
                    showMissingDataScreen();
                });
                return;
            }
            runOnUiThread(() -> {
                lastImportMessage = result;
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                continueStartup();
            });
        }, "UT99FolderInstaller").start();
    }

    private void installFromZip(final Uri zipUri) {
        showBusyScreen(t("Installiere UT99-Daten", "Installing UT99 data"),
                t("ZIP wird entpackt …", "Extracting ZIP …"));
        new Thread(() -> {
            final String result;
            try {
                InstallStats stats = importZip(zipUri, UT99Paths.installRoot(this));
                result = t("ZIP-Import abgeschlossen: ", "ZIP import complete: ") + stats.files + " files";
            } catch (Throwable ex) {
                android.util.Log.e("UT99Installer", "zip import failed", ex);
                runOnUiThread(() -> {
                    lastImportMessage = t("ZIP-Import fehlgeschlagen: ", "ZIP import failed: ") + ex.getMessage();
                    showMissingDataScreen();
                });
                return;
            }
            runOnUiThread(() -> {
                lastImportMessage = result;
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                continueStartup();
            });
        }, "UT99ZipInstaller").start();
    }

    private InstallStats importFolderTree(Uri treeUri, File targetRoot) throws IOException {
        if (Build.VERSION.SDK_INT < 21) throw new IOException("Folder import requires Android 5.0 or newer.");
        UT99Paths.ensureSkeleton(targetRoot);

        Uri rootDocument = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
        Uri sourceDocument = findGameDataDocument(treeUri, rootDocument);
        if (sourceDocument == null) {
            throw new IOException("Selected folder does not contain System, Maps, Textures, Sounds and Music.");
        }

        InstallStats stats = new InstallStats();
        copyDocumentChildren(treeUri, sourceDocument, targetRoot, stats);
        if (!UT99Paths.hasUsableGameData(targetRoot)) {
            throw new IOException("Import finished, but required UT99 files were not found in " + targetRoot.getAbsolutePath());
        }
        return stats;
    }

    private Uri findGameDataDocument(Uri treeUri, Uri documentUri) throws IOException {
        if (documentHasRequiredFolders(treeUri, documentUri)) return documentUri;
        for (DocumentEntry child : listDocumentChildren(treeUri, documentUri)) {
            if (child.directory && documentHasRequiredFolders(treeUri, child.uri)) {
                return child.uri;
            }
        }
        return null;
    }

    private boolean documentHasRequiredFolders(Uri treeUri, Uri documentUri) throws IOException {
        Set<String> names = new HashSet<>();
        for (DocumentEntry child : listDocumentChildren(treeUri, documentUri)) {
            if (child.directory) names.add(child.name.toLowerCase(Locale.US));
        }
        return names.contains("system") && names.contains("maps") && names.contains("textures") &&
                names.contains("sounds") && names.contains("music");
    }

    private void copyDocumentChildren(Uri treeUri, Uri parentDocument, File targetDir, InstallStats stats) throws IOException {
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Cannot create " + targetDir.getAbsolutePath());
        }
        for (DocumentEntry child : listDocumentChildren(treeUri, parentDocument)) {
            String safeName = safeFileName(child.name);
            if (safeName.length() == 0) continue;
            File out = new File(targetDir, safeName);
            if (child.directory) {
                copyDocumentChildren(treeUri, child.uri, out, stats);
            } else {
                copyContentUriToFile(child.uri, out, stats);
            }
        }
    }

    private java.util.List<DocumentEntry> listDocumentChildren(Uri treeUri, Uri documentUri) throws IOException {
        java.util.ArrayList<DocumentEntry> entries = new java.util.ArrayList<>();
        if (Build.VERSION.SDK_INT < 21) return entries;

        String documentId = DocumentsContract.getDocumentId(documentUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(childrenUri,
                    new String[] {
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE
                    }, null, null, null);
            if (cursor == null) return entries;
            while (cursor.moveToNext()) {
                String childId = cursor.getString(0);
                String name = cursor.getString(1);
                String mime = cursor.getString(2);
                if (name == null || name.length() == 0) continue;
                Uri childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId);
                boolean directory = DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
                entries.add(new DocumentEntry(childUri, name, directory));
            }
        } catch (Exception ex) {
            throw new IOException("Could not read selected folder.", ex);
        } finally {
            if (cursor != null) cursor.close();
        }
        return entries;
    }

    private void copyContentUriToFile(Uri source, File out, InstallStats stats) throws IOException {
        File parent = out.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent.getAbsolutePath());
        }
        InputStream input = getContentResolver().openInputStream(source);
        if (input == null) throw new IOException("Cannot open " + source);
        try {
            FileOutputStream output = new FileOutputStream(out, false);
            try {
                stats.bytes += copy(input, output);
                stats.files++;
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }
    }

    private InstallStats importZip(Uri zipUri, File targetRoot) throws IOException {
        UT99Paths.ensureSkeleton(targetRoot);
        File tmp = File.createTempFile("ut99-import", ".zip", getCacheDir());
        try {
            InputStream in = getContentResolver().openInputStream(zipUri);
            if (in == null) throw new IOException("Cannot open selected ZIP.");
            try {
                FileOutputStream out = new FileOutputStream(tmp, false);
                try {
                    copy(in, out);
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }

            String prefix;
            ZipFile zipFile = new ZipFile(tmp);
            try {
                prefix = findZipGameDataPrefix(zipFile);
            } finally {
                zipFile.close();
            }
            if (prefix == null) {
                throw new IOException("ZIP does not contain System, Maps, Textures, Sounds and Music.");
            }

            InstallStats stats = new InstallStats();
            ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(new FileInputStream(tmp)));
            try {
                ZipEntry entry;
                while ((entry = zipInput.getNextEntry()) != null) {
                    String name = normalizeZipName(entry.getName());
                    if (name.length() == 0 || !name.startsWith(prefix)) continue;
                    String relative = name.substring(prefix.length());
                    if (relative.length() == 0 || shouldSkipZipEntry(relative)) continue;

                    File out = safeZipOutputFile(targetRoot, relative);
                    if (entry.isDirectory() || relative.endsWith("/")) {
                        if (!out.exists() && !out.mkdirs()) throw new IOException("Cannot create " + out.getAbsolutePath());
                    } else {
                        File parent = out.getParentFile();
                        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Cannot create " + parent.getAbsolutePath());
                        FileOutputStream fileOut = new FileOutputStream(out, false);
                        try {
                            stats.bytes += copy(zipInput, fileOut);
                            stats.files++;
                        } finally {
                            fileOut.close();
                        }
                    }
                    zipInput.closeEntry();
                }
            } finally {
                zipInput.close();
            }

            if (!UT99Paths.hasUsableGameData(targetRoot)) {
                throw new IOException("ZIP extracted, but required UT99 files were not found in " + targetRoot.getAbsolutePath());
            }
            return stats;
        } finally {
            if (!tmp.delete()) tmp.deleteOnExit();
        }
    }

    private String findZipGameDataPrefix(ZipFile zipFile) throws IOException {
        Set<String> lowerNames = new HashSet<>();
        java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            String name = normalizeZipName(entries.nextElement().getName()).toLowerCase(Locale.US);
            if (name.length() > 0) lowerNames.add(name);
        }

        Set<String> candidates = new HashSet<>();
        for (String name : lowerNames) {
            int idx = name.indexOf("system/");
            if (idx >= 0) candidates.add(name.substring(0, idx));
        }
        candidates.add("");

        for (String prefix : candidates) {
            if (zipHasPath(lowerNames, prefix + "system/") &&
                    zipHasPath(lowerNames, prefix + "maps/") &&
                    zipHasPath(lowerNames, prefix + "textures/") &&
                    zipHasPath(lowerNames, prefix + "sounds/") &&
                    zipHasPath(lowerNames, prefix + "music/")) {
                return prefix;
            }
        }
        return null;
    }

    private boolean zipHasPath(Set<String> names, String path) {
        for (String name : names) {
            if (name.equals(path) || name.startsWith(path)) return true;
        }
        return false;
    }

    private boolean shouldSkipZipEntry(String relative) {
        String lower = relative.toLowerCase(Locale.US);
        return lower.startsWith("__macosx/") || lower.endsWith("/.ds_store") || lower.equals(".ds_store");
    }

    private String normalizeZipName(String raw) {
        if (raw == null) return "";
        String name = raw.replace('\\', '/');
        while (name.startsWith("/")) name = name.substring(1);
        while (name.startsWith("./")) name = name.substring(2);
        return name;
    }

    private File safeZipOutputFile(File targetRoot, String relative) throws IOException {
        String normalized = normalizeZipName(relative);
        if (normalized.contains("../") || normalized.equals("..") || normalized.startsWith("../")) {
            throw new IOException("Unsafe ZIP entry: " + relative);
        }
        File out = new File(targetRoot, normalized);
        String rootPath = targetRoot.getCanonicalPath() + File.separator;
        String outPath = out.getCanonicalPath();
        if (!outPath.startsWith(rootPath)) {
            throw new IOException("Unsafe ZIP entry path: " + relative);
        }
        return out;
    }

    private String safeFileName(String name) {
        if (name == null) return "";
        String cleaned = name.replace('/', '_').replace('\\', '_').trim();
        if (cleaned.equals(".") || cleaned.equals("..")) return "";
        return cleaned;
    }

    private long copy(InputStream input, FileOutputStream output) throws IOException {
        byte[] buffer = new byte[128 * 1024];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
            total += read;
        }
        output.flush();
        return total;
    }

    private static final class DocumentEntry {
        final Uri uri;
        final String name;
        final boolean directory;

        DocumentEntry(Uri uri, String name, boolean directory) {
            this.uri = uri;
            this.name = name;
            this.directory = directory;
        }
    }

    private static final class InstallStats {
        int files;
        long bytes;
    }
}
