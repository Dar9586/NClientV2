package com.dar.nclientv2.api;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Ranges;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.SortType;
import com.dar.nclientv2.api.enums.SpecialTagIds;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Request;
import okhttp3.Response;

public class InspectorV3 extends Thread implements Parcelable {
    public static final Creator<InspectorV3> CREATOR = new Creator<InspectorV3>() {
        @Override
        public InspectorV3 createFromParcel(Parcel in) {
            return new InspectorV3(in);
        }

        @Override
        public InspectorV3[] newArray(int size) {
            return new InspectorV3[size];
        }
    };
    private SortType sortType;
    private boolean custom, forceStart = false;
    private int page, pageCount = -1, id;
    private String query, url;
    private ApiRequestType requestType;
    private Set<Tag> tags;
    private ArrayList<GenericGallery> galleries = null;
    private Ranges ranges = null;
    private InspectorResponse response;
    private WeakReference<Context> context;
    private Document htmlDocument;

    protected InspectorV3(Parcel in) {
        sortType = SortType.values()[in.readByte()];
        custom = in.readByte() != 0;
        page = in.readInt();
        pageCount = in.readInt();
        id = in.readInt();
        query = in.readString();
        url = in.readString();
        requestType = ApiRequestType.values[in.readByte()];
        ArrayList x = null;
        switch (GenericGallery.Type.values()[in.readByte()]) {
            case LOCAL:
                x = in.createTypedArrayList(LocalGallery.CREATOR);
                break;
            case SIMPLE:
                x = in.createTypedArrayList(SimpleGallery.CREATOR);
                break;
            case COMPLETE:
                x = in.createTypedArrayList(Gallery.CREATOR);
                break;
        }
        galleries = (ArrayList<GenericGallery>) x;
        tags = new HashSet<>(in.createTypedArrayList(Tag.CREATOR));
        ranges = in.readParcelable(Ranges.class.getClassLoader());
    }

    private InspectorV3(Context context, InspectorResponse response) {
        initialize(context, response);
    }

    /**
     * This method will not run, but a WebView inside MainActivity will do it in its place
     */
    public static InspectorV3 favoriteInspector(Context context, String query, int page, InspectorResponse response) {
        InspectorV3 inspector = new InspectorV3(context, response);
        inspector.page = page;
        inspector.pageCount = 0;
        inspector.query = query == null ? "" : query;
        inspector.requestType = ApiRequestType.FAVORITE;
        inspector.tags = new HashSet<>(1);
        inspector.createUrl();
        return inspector;
    }

    /**
     * @param favorite true if random online favorite, false for general random manga
     */
    public static InspectorV3 randomInspector(Context context, InspectorResponse response, boolean favorite) {
        InspectorV3 inspector = new InspectorV3(context, response);
        inspector.requestType = favorite ? ApiRequestType.RANDOM_FAVORITE : ApiRequestType.RANDOM;
        inspector.createUrl();
        return inspector;
    }

    public static InspectorV3 galleryInspector(Context context, int id, InspectorResponse response) {
        InspectorV3 inspector = new InspectorV3(context, response);
        inspector.id = id;
        inspector.requestType = ApiRequestType.BYSINGLE;
        inspector.createUrl();
        return inspector;
    }

    public static InspectorV3 basicInspector(Context context, int page, InspectorResponse response) {
        return searchInspector(context, null, null, page, Global.getSortType(), null, response);
    }

    public static InspectorV3 tagInspector(Context context, Tag tag, int page, SortType sortType, InspectorResponse response) {
        Collection<Tag> tags;
        if (!Global.isOnlyTag()) {
            tags = getDefaultTags();
            tags.add(tag);
        } else {
            tags = Collections.singleton(tag);
        }
        return searchInspector(context, null, tags, page, sortType, null, response);
    }

    public static InspectorV3 searchInspector(Context context, String query, Collection<Tag> tags, int page, SortType sortType, @Nullable Ranges ranges, InspectorResponse response) {
        InspectorV3 inspector = new InspectorV3(context, response);
        inspector.custom = tags != null;
        inspector.tags = inspector.custom ? new HashSet<>(tags) : getDefaultTags();
        inspector.tags.addAll(getLanguageTags(Global.getOnlyLanguage()));
        inspector.page = page;
        inspector.pageCount = 0;
        inspector.ranges = ranges;
        inspector.query = query == null ? "" : query;
        inspector.sortType = sortType;
        if (inspector.query.isEmpty() && (ranges == null || ranges.isDefault())) {
            switch (inspector.tags.size()) {
                case 0:
                    inspector.requestType = ApiRequestType.BYALL;
                    inspector.tryByAllPopular();
                    break;
                case 1:
                    inspector.requestType = ApiRequestType.BYTAG;
                    //else by search for the negative tag
                    if (inspector.getTag().getStatus() != TagStatus.AVOIDED)
                        break;
                default:
                    inspector.requestType = ApiRequestType.BYSEARCH;
                    break;
            }
        } else inspector.requestType = ApiRequestType.BYSEARCH;
        inspector.createUrl();
        return inspector;
    }

    @NonNull
    private static HashSet<Tag> getDefaultTags() {
        HashSet<Tag> tags = new HashSet<>(Queries.TagTable.getAllStatus(TagStatus.ACCEPTED));
        tags.addAll(getLanguageTags(Global.getOnlyLanguage()));
        if (Global.removeAvoidedGalleries())
            tags.addAll(Queries.TagTable.getAllStatus(TagStatus.AVOIDED));
        tags.addAll(Queries.TagTable.getAllOnlineBlacklisted());
        return tags;
    }

    private static Set<Tag> getLanguageTags(Language onlyLanguage) {
        Set<Tag> tags = new HashSet<>();
        if (onlyLanguage == null) return tags;
        switch (onlyLanguage) {
            case ENGLISH:
                tags.add(Queries.TagTable.getTagById(SpecialTagIds.LANGUAGE_ENGLISH));
                break;
            case JAPANESE:
                tags.add(Queries.TagTable.getTagById(SpecialTagIds.LANGUAGE_JAPANESE));
                break;
            case CHINESE:
                tags.add(Queries.TagTable.getTagById(SpecialTagIds.LANGUAGE_CHINESE));
                break;
        }
        return tags;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (sortType != null)
            dest.writeByte((byte) (sortType.ordinal()));
        else dest.writeByte((byte) SortType.RECENT_ALL_TIME.ordinal());
        dest.writeByte((byte) (custom ? 1 : 0));
        dest.writeInt(page);
        dest.writeInt(pageCount);
        dest.writeInt(id);
        dest.writeString(query);
        dest.writeString(url);
        dest.writeByte(requestType.ordinal());
        if (galleries == null || galleries.size() == 0)
            dest.writeByte((byte) GenericGallery.Type.SIMPLE.ordinal());
        else dest.writeByte((byte) galleries.get(0).getType().ordinal());
        dest.writeTypedList(galleries);
        dest.writeTypedList(new ArrayList<>(tags));
        dest.writeParcelable(ranges, flags);
    }

    public String getSearchTitle() {
        //triggered only when in searchMode
        if (query.length() > 0) return query;
        return url.replace(Utility.getBaseUrl() + "search/?q=", "").replace('+', ' ');
    }

    public void initialize(Context context, InspectorResponse response) {
        this.response = response;
        this.context = new WeakReference<>(context);
    }

    public InspectorResponse getResponse() {
        return response;
    }

    public InspectorV3 cloneInspector(Context context, InspectorResponse response) {
        InspectorV3 inspectorV3 = new InspectorV3(context, response);
        inspectorV3.query = query;
        inspectorV3.url = url;
        inspectorV3.tags = tags;
        inspectorV3.requestType = requestType;
        inspectorV3.sortType = sortType;
        inspectorV3.pageCount = pageCount;
        inspectorV3.page = page;
        inspectorV3.id = id;
        inspectorV3.custom = custom;
        inspectorV3.ranges = ranges;
        return inspectorV3;
    }


    @Nullable
    private String trimScriptTag(String scriptHtml) {
        if (scriptHtml == null) return null;

        // Strategy 0 (SvelteKit response envelope):
        // {"status":200,"statusText":"OK","headers":{...},"body":"{\"id\":641740,...}"}
        // The gallery JSON is inside the "body" field as an escaped JSON string.
        // We detect it by looking for escaped gallery markers like \"media_id\"
        if (scriptHtml.contains("\"body\":\"") && scriptHtml.contains("\\\"media_id\\\"")) {
            String body = extractJsonStringValue(scriptHtml, "body");
            if (body != null && body.contains("\"id\"") && body.contains("\"media_id\"")) {
                LogUtility.d("[trimScriptTag] Extracted gallery JSON via SvelteKit body envelope");
                return body;
            }
        }

        // Strategy 1 (variable assignment): window._gallery = { ... };
        for (String varName : new String[]{"window._gallery", "window._n_gallery", "_gallery"}) {
            int varIdx = scriptHtml.indexOf(varName);
            if (varIdx < 0) continue;
            int eqIdx = scriptHtml.indexOf('=', varIdx + varName.length());
            if (eqIdx < 0) continue;
            int braceStart = scriptHtml.indexOf('{', eqIdx + 1);
            if (braceStart < 0) continue;
            int depth = 0;
            for (int i = braceStart; i < scriptHtml.length(); i++) {
                char c = scriptHtml.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        String json = scriptHtml.substring(braceStart, i + 1);
                        if (json.contains("\"id\"") || json.contains("\"media_id\"")) {
                            LogUtility.d("[trimScriptTag] Extracted gallery JSON via " + varName);
                            return json;
                        }
                        break;
                    }
                }
            }
        }

        // Strategy 2: look for JSON.parse(<quote>...<quote>) with any quote style
        int s = scriptHtml.indexOf("parse(");
        if (s >= 0) {
            for (char q : new char[]{'"', '\'', '`'}) {
                int startQuote = scriptHtml.indexOf(q, s);
                if (startQuote < 0) continue;
                int endQuote = scriptHtml.lastIndexOf(q);
                if (endQuote <= startQuote) continue;
                String json = scriptHtml.substring(startQuote + 1, endQuote);
                json = Utility.unescapeUnicodeString(json);
                if (!json.isEmpty() && json.contains("\"id\"")) return json;
            }
        }

        // Strategy 3: direct JSON object embedded in script (try both key orders)
        for (String marker : new String[]{"{'id':", "{\"id\":", "{\"media_id\":", "{'media_id':"}) {
            int startJson = scriptHtml.indexOf(marker);
            if (startJson < 0) continue;
            int endJson = scriptHtml.lastIndexOf('}');
            if (endJson <= startJson) continue;
            String json = scriptHtml.substring(startJson, endJson + 1);
            if (json.contains("\"id\"") || json.contains("'id'")) return json;
        }

        return null;
    }

    /**
     * Extract and unescape a JSON string value from a JSON document.
     * Finds "key":"..." and properly handles escape sequences within the value.
     *
     * @param json the raw JSON text
     * @param key  the key whose string value to extract
     * @return the unescaped string value, or null if not found
     */
    @Nullable
    private String extractJsonStringValue(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int idx = json.indexOf(needle);
        if (idx < 0) return null;
        int valueStart = idx + needle.length();
        StringBuilder sb = new StringBuilder();
        for (int i = valueStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '/': sb.append('/'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'u':
                        if (i + 5 < json.length()) {
                            try {
                                int codePoint = Integer.parseInt(json.substring(i + 2, i + 6), 16);
                                sb.append((char) codePoint);
                                i += 5;
                            } catch (NumberFormatException e) {
                                sb.append(c);
                            }
                        } else {
                            sb.append(c);
                        }
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            } else if (c == '"') {
                // Unescaped quote = end of the JSON string value
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void tryByAllPopular() {
        if (sortType != SortType.RECENT_ALL_TIME) {
            requestType = ApiRequestType.BYSEARCH;
            query = "-nclientv2";
        }
    }

    private void createUrl() {
        String query;
        try {
            query = this.query == null ? null : URLEncoder.encode(this.query, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
            query = this.query;
        }
        StringBuilder builder = new StringBuilder(Utility.getBaseUrl());
        if (requestType == ApiRequestType.BYALL) builder.append("?page=").append(page);
        else if (requestType == ApiRequestType.RANDOM) builder.append("random/");
        else if (requestType == ApiRequestType.RANDOM_FAVORITE) builder.append("favorites/random");
        else if (requestType == ApiRequestType.BYSINGLE) builder.append("g/").append(id);
        else if (requestType == ApiRequestType.FAVORITE) {
            builder.append("favorites/");
            if (query != null && query.length() > 0)
                builder.append("?q=").append(query).append('&');
            else builder.append('?');
            builder.append("page=").append(page);
        } else if (requestType == ApiRequestType.BYSEARCH || requestType == ApiRequestType.BYTAG) {
            builder.append("search/?q=").append(query);
            for (Tag tt : tags) {
                if (builder.toString().contains(tt.toQueryTag(TagStatus.ACCEPTED))) continue;
                builder.append('+').append(URLEncoder.encode(tt.toQueryTag()));
            }
            if (ranges != null)
                builder.append('+').append(ranges.toQuery());
            builder.append("&page=").append(page);
            if (sortType.getUrlAddition() != null) {
                builder.append("&sort=").append(sortType.getUrlAddition());
            }
        }
        url = builder.toString().replace(' ', '+');
        LogUtility.d("WWW: " + getBookmarkURL());
    }

    public void forceStart() {
        forceStart = true;
        start();
    }

    private String getBookmarkURL() {
        if (page < 2) return url;
        else return url.substring(0, url.lastIndexOf('=') + 1);
    }

    public boolean createDocument() throws IOException {
        if (htmlDocument != null) return true;
        Response response = Global.getClient(context.get()).newCall(new Request.Builder().url(url).build()).execute();
        setHtmlDocument(Jsoup.parse(response.body().byteStream(), "UTF-8", Utility.getBaseUrl()));
        response.close();
        return response.code() == HttpURLConnection.HTTP_OK;
    }

    public void parseDocument() throws IOException, InvalidResponseException {
        if (requestType.isSingle()) doSingle(htmlDocument.body());
        else doSearch(htmlDocument.body());
        htmlDocument = null;
    }

    public void setHtmlDocument(Document htmlDocument) {
        this.htmlDocument = htmlDocument;
    }

    public boolean canParseDocument() {
        return this.htmlDocument != null;
    }

    @Override
    public synchronized void start() {
        if (getState() != State.NEW) return;
        if (forceStart || response.shouldStart(this))
            super.start();
    }

    @Override
    public void run() {
        LogUtility.d("Starting download: " + url);
        if (response != null) response.onStart();
        try {
            if (!createDocument()) {
                throw new InvalidResponseException();
            }
            parseDocument();
            if (response != null) {
                response.onSuccess(galleries);
            }
        } catch (Exception e) {
            if (response != null) response.onFailure(e);
        }
        if (response != null) response.onEnd();
        LogUtility.d("Finished download: " + url);
    }

    private void filterDocumentTags() {
        if (galleries == null || tags == null) return;
        ArrayList<SimpleGallery> galleryTag = new ArrayList<>(galleries.size());
        for (GenericGallery gal : galleries) {
            assert gal instanceof SimpleGallery;
            SimpleGallery gallery = (SimpleGallery) gal;
            if (gallery.hasTags(tags)) {
                galleryTag.add(gallery);
            }
        }
        galleries.clear();
        galleries.addAll(galleryTag);
    }

    private void doSingle(Element document) throws IOException, InvalidResponseException {
        galleries = new ArrayList<>(1);
        Elements scripts = document.getElementsByTag("script");
        String json = null;

        // Strategy 1: search all inline script tags
        for (Element script : scripts) {
            String scriptHtml = script.html();
            if (!scriptHtml.isEmpty()) {
                // Log a truncated preview so logcat shows what the site embeds
                LogUtility.d("[doSingle] script tag (" + scriptHtml.length() + " chars): "
                        + scriptHtml.substring(0, Math.min(300, scriptHtml.length())));
            }
            json = trimScriptTag(scriptHtml);
            if (json != null) break;
        }

        // Strategy 2: Next.js __NEXT_DATA__ pattern (e.g. after a site migration)
        if (json == null) {
            Element nextData = document.getElementById("__NEXT_DATA__");
            if (nextData != null) {
                LogUtility.d("[doSingle] Found __NEXT_DATA__, trying to extract gallery JSON");
                json = extractGalleryJsonFromNextData(nextData.html());
            }
        }

        // Strategy 3: fall back to the JSON API endpoint (bypasses HTML entirely)
        if (json == null) {
            LogUtility.d("[doSingle] HTML parse exhausted, trying API endpoint for id=" + id);
            json = tryApiEndpoint();
        }

        if (json == null) {
            LogUtility.e("[doSingle] Unable to find gallery JSON for: " + url);
            throw new InvalidResponseException();
        }

        Element relContainer = document.getElementById("related-container");
        Elements rel;
        if (relContainer != null)
            rel = relContainer.getElementsByClass("gallery");
        else
            rel = new Elements();
        boolean isFavorite;
        try {
            isFavorite = document.getElementById("favorite").getElementsByTag("span").get(0).text().equals("Unfavorite");
        } catch (Exception e) {
            isFavorite = false;
        }
        LogUtility.d("is favorite? " + isFavorite);
        galleries.add(new Gallery(context.get(), json, rel, isFavorite));
    }

    /**
     * Attempt to pull the gallery JSON out of a Next.js __NEXT_DATA__ blob.
     * The gallery object is typically at props.pageProps.gallery (or similar).
     * We look for the first {"id": substring and take everything up to the
     * balanced closing brace so GalleryData can parse it normally.
     */
    @Nullable
    private String extractGalleryJsonFromNextData(String nextDataJson) {
        if (nextDataJson == null || nextDataJson.isEmpty()) return null;
        // Gallery object always starts with an "id" integer field
        int start = nextDataJson.indexOf("{\"id\":");
        if (start < 0) return null;
        // Walk forward counting braces to find the balanced closing brace
        int depth = 0;
        for (int i = start; i < nextDataJson.length(); i++) {
            char c = nextDataJson.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    String candidate = nextDataJson.substring(start, i + 1);
                    if (candidate.contains("\"media_id\"")) return candidate;
                    // wrong object, keep searching
                    start = nextDataJson.indexOf("{\"id\":", i + 1);
                    if (start < 0) return null;
                    i = start - 1;
                    depth = 0;
                }
            }
        }
        return null;
    }

    /**
     * Direct JSON API fallback: nhentai exposes /api/gallery/<id> which returns
     * the gallery JSON without any HTML wrapping.
     */
    @Nullable
    private String tryApiEndpoint() {
        // Try multiple known API URL patterns (the site may change endpoints)
        String[] apiUrls = {
            Utility.getBaseUrl() + "api/v2/gallery/" + id,
            Utility.getBaseUrl() + "api/gallery/" + id,
        };
        for (String apiUrl : apiUrls) {
            try {
                LogUtility.d("[doSingle] API call: " + apiUrl);
                Response apiResponse = Global.getClient(context.get())
                        .newCall(new Request.Builder().url(apiUrl).build()).execute();
                String body = apiResponse.body() != null ? apiResponse.body().string() : null;
                int code = apiResponse.code();
                apiResponse.close();
                LogUtility.d("[doSingle] API response code: " + code);
                if (code == HttpURLConnection.HTTP_OK && body != null && body.contains("\"id\"")) {
                    return body;
                }
                if (body != null) {
                    LogUtility.d("[doSingle] API body (truncated): "
                            + body.substring(0, Math.min(300, body.length())));
                }
            } catch (Exception e) {
                LogUtility.e("[doSingle] API fallback exception for " + apiUrl + ": " + e.getMessage(), e);
            }
        }
        return null;
    }


    private void doSearch(Element document) throws InvalidResponseException {
        Elements gal = document.getElementsByClass("gallery");
        galleries = new ArrayList<>(gal.size());
        for (Element e : gal) galleries.add(new SimpleGallery(context.get(), e));
        gal = document.getElementsByClass("last");
        pageCount = gal.size() == 0 ? Math.max(1, page) : findTotal(gal.last());
        if (document.getElementById("content") == null)
            throw new InvalidResponseException();
        if (Global.isExactTagMatch())
            filterDocumentTags();
    }

    private int findTotal(Element e) {
        String temp = e.attr("href");

        try {
            return Integer.parseInt(Uri.parse(temp).getQueryParameter("page"));
        } catch (Exception ignore) {
            return 1;
        }
    }

    public void setSortType(SortType sortType) {
        this.sortType = sortType;
        createUrl();
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
        createUrl();
    }

    public List<GenericGallery> getGalleries() {
        return galleries;
    }

    public String getUrl() {
        return url;
    }

    public ApiRequestType getRequestType() {
        return requestType;
    }

    public int getPageCount() {
        return pageCount;
    }

    public boolean isCustom() {
        return custom;
    }

    public String getQuery() {
        return query;
    }

    public Tag getTag() {
        Tag t = null;
        if (tags == null) return null;
        for (Tag tt : tags) {
            if (tt.getType() != TagType.LANGUAGE)
                return tt;
            t = tt;
        }
        return t;
    }

    public static class InvalidResponseException extends Exception {
        public InvalidResponseException() {
            super();
        }
    }

    public interface InspectorResponse {
        boolean shouldStart(InspectorV3 inspector);

        void onSuccess(List<GenericGallery> galleries);

        void onFailure(Exception e);

        void onStart();

        void onEnd();
    }

    public static abstract class DefaultInspectorResponse implements InspectorResponse {
        @Override
        public boolean shouldStart(InspectorV3 inspector) {
            return true;
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onEnd() {
        }

        @Override
        public void onSuccess(List<GenericGallery> galleries) {
        }

        @Override
        public void onFailure(Exception e) {
            LogUtility.e(e.getLocalizedMessage(), e);
        }
    }
}
