package com.dar.nclientv2.async.downloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

import okhttp3.Request;
import okhttp3.Response;

public class GalleryDownloaderV2 {
    public static final String DUPLICATE_EXTENSION = ".DUP";
    public static final Pattern ID_FILE = Pattern.compile("^\\.\\d{1,6}$");
    private final Context context;
    private final int id;
    private final CopyOnWriteArraySet<DownloadObserver> observers = new CopyOnWriteArraySet<>();
    private final List<PageContainer> urls = new ArrayList<>();
    private Status status = Status.NOT_STARTED;
    private String title;
    private Uri thumbnail;
    private int start = -1, end = -1;
    private Gallery gallery;
    private File folder;
    private boolean initialized = false;

    public GalleryDownloaderV2(Context context, @Nullable String title, @Nullable Uri thumbnail, int id) {
        this.context = context;
        this.id = id;
        this.thumbnail = thumbnail;
        this.title = Gallery.getPathTitle(title, context.getString(R.string.download_gallery));
    }

    public GalleryDownloaderV2(Context context, Gallery gallery, int start, int end) {
        this(context, gallery.getTitle(), gallery.getCover(), gallery.getId());
        this.start = start;
        this.end = end;
        setGallery(gallery);
    }

    private static File findFolder(File downloadfolder, String pathTitle, int id) {
        File folder = new File(downloadfolder, pathTitle);
        if (usableFolder(folder, id)) return folder;
        int i = 1;
        do {
            folder = new File(downloadfolder, pathTitle + DUPLICATE_EXTENSION + (i++));
        } while (!usableFolder(folder, id));
        return folder;
    }

    private static boolean usableFolder(File file, int id) {
        if (!file.exists()) return true;//folder not exists
        if (new File(file, "." + id).exists()) return true;//same id
        File[] files = file.listFiles((dir, name) -> ID_FILE.matcher(name).matches());
        if (files != null && files.length > 0) return false;//has id but not equal
        LocalGallery localGallery = new LocalGallery(file);//read id from metadata
        return localGallery.getId() == id;
    }

    public boolean hasData() {
        return gallery != null;
    }

    public void removeObserver(DownloadObserver observer) {
        observers.remove(observer);
    }

    public File getFolder() {
        return folder;
    }

    public Gallery getGallery() {
        return gallery;
    }

    private void setGallery(Gallery gallery) {
        this.gallery = gallery;
        title = gallery.getPathTitle();
        thumbnail = gallery.getThumbnail();
        Queries.DownloadTable.addGallery(this);
        if (start == -1) start = 0;
        if (end == -1) end = gallery.getPageCount() - 1;
    }

    private int getTotalPage() {
        return Math.max(1, end - start + 1);
    }

    public int getPercentage() {
        if (gallery == null || urls.size() == 0) return 0;
        return ((getTotalPage() - urls.size()) * 100) / getTotalPage();
    }

    private void onStart() {
        setStatus(Status.DOWNLOADING);
        for (DownloadObserver observer : observers) observer.triggerStartDownload(this);
    }

    private void onEnd() {
        setStatus(Status.FINISHED);
        for (DownloadObserver observer : observers) observer.triggerEndDownload(this);
        LogUtility.d("Delete 75: " + id);
        Queries.DownloadTable.removeGallery(id);
    }

    private void onUpdate() {
        int total = getTotalPage();
        int reach = total - urls.size();
        for (DownloadObserver observer : observers)
            observer.triggerUpdateProgress(this, reach, total);
    }

    private void onCancel() {
        for (DownloadObserver observer : observers) observer.triggerCancelDownload(this);
    }

    private void onPause() {
        for (DownloadObserver observer : observers) observer.triggerPauseDownload(this);
    }

    public LocalGallery localGallery() {
        if (status != Status.FINISHED || folder == null) return null;
        return new LocalGallery(folder);
    }

    public String getTitle() {
        return title;
    }

    public void addObserver(DownloadObserver observer) {
        if (observer == null) return;
        observers.add(observer);
    }

    public int getId() {
        return id;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @NonNull
    public String getPathTitle() {
        return title;
    }

    @NonNull
    public String getTruePathTitle() {
        return title;
    }

    /**
     * @return true if the download has been completed, false otherwise
     */
    public boolean downloadGalleryData() {
        if (this.gallery != null) return true;
        InspectorV3 inspector = InspectorV3.galleryInspector(context, id, null);
        try {
            inspector.createDocument();
            inspector.parseDocument();
            if (inspector.getGalleries() == null || inspector.getGalleries().size() == 0)
                return false;
            Gallery g = (Gallery) inspector.getGalleries().get(0);
            if (g.isValid())
                setGallery(g);
            return g.isValid();
        } catch (Exception e) {
            LogUtility.e("Error while downloading", e);
            return false;
        }
    }

    public Uri getThumbnail() {
        return thumbnail;
    }

    public boolean canBeFetched() {
        return status != Status.FINISHED && status != Status.PAUSED;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        if (this.status == status) return;
        this.status = status;
        if (status == Status.CANCELED) {
            LogUtility.d("Delete 95: " + id);
            onCancel();
            Global.recursiveDelete(folder);
            Queries.DownloadTable.removeGallery(id);
        }
    }

    public void download() {
        initDownload();
        onStart();
        while (!urls.isEmpty()) {
            downloadPage(urls.get(0));
            Utility.threadSleep(50);
            if (status == Status.PAUSED) {
                onPause();
                return;
            }
            if (status == Status.CANCELED) {
                onCancel();
                return;
            }
        }
        onEnd();
    }

    private void downloadPage(PageContainer page) {
        if (savePage(page)) {
            urls.remove(page);
            onUpdate();
        }
    }

    private boolean isCorrupted(File file) {
        String path = file.getAbsolutePath();
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return Global.isJPEGCorrupted(path);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 256;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        boolean x = bitmap == null;
        if (!x) bitmap.recycle();
        bitmap = null;
        return x;
    }

    private boolean savePage(PageContainer page) {
        if (page == null) return true;
        File filePath = new File(folder, page.getPageName());
        LogUtility.d("Saving into: " + filePath + "," + page.url);
        if (filePath.exists() && !isCorrupted(filePath)) return true;
        try {
            Response r = Global.getClient(context).newCall(new Request.Builder().url(page.url).build()).execute();
            if (r.code() != 200) {
                r.close();
                return false;
            }
            assert r.body() != null;
            long expectedSize = Integer.parseInt(r.header("Content-Length", "-1"));
            long len = r.body().contentLength();
            if (len < 0 || expectedSize != len) {
                r.close();
                return false;
            }
            long written = Utility.writeStreamToFile(r.body().byteStream(), filePath);
            r.close();
            if (written != len) {
                filePath.delete();
                return false;
            }
            return true;
        } catch (IOException | NumberFormatException e) {
            LogUtility.e(e, e);
        }
        return false;
    }


    public void initDownload() {
        if (initialized) return;
        initialized = true;
        createFolder();
        createPages();
        checkPages();
    }

    private void checkPages() {
        File filePath;
        for (int i = 0; i < urls.size(); i++) {
            if(urls.get(i)==null){
                urls.remove(i--);
                continue;
            }
            filePath = new File(folder, urls.get(i).getPageName());
            if (filePath.exists() && !isCorrupted(filePath))
                urls.remove(i--);
        }
    }

    private void createPages() {
        for (int i = start; i <= end && i < gallery.getPageCount(); i++)
            urls.add(new PageContainer(i + 1, gallery.getHighPage(i).toString(), gallery.getPageExtension(i)));
    }

    private void createFolder() {
        folder = findFolder(Global.DOWNLOADFOLDER, title, id);
        folder.mkdirs();
        try {
            writeNoMedia();
            createIdFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createIdFile() throws IOException {
        File idFile = new File(folder, "." + id);
        idFile.createNewFile();
    }

    private void writeNoMedia() throws IOException {
        File nomedia = new File(folder, ".nomedia");
        LogUtility.d("NOMEDIA: " + nomedia + " for id " + id);
        FileWriter writer = new FileWriter(nomedia);
        gallery.jsonWrite(writer);
        writer.close();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GalleryDownloaderV2 that = (GalleryDownloaderV2) o;

        if (id != that.id) return false;
        return folder != null ? folder.equals(that.folder) : that.folder == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        return result;
    }

    public enum Status {NOT_STARTED, DOWNLOADING, PAUSED, FINISHED, CANCELED}

    public static class PageContainer {
        public final int page;
        public final String url, ext;

        public PageContainer(int page, String url, String ext) {
            this.page = page;
            this.url = url;
            this.ext = ext;
        }

        public String getPageName() {
            return String.format(Locale.US, "%03d.%s", page, ext);
        }
    }
}
