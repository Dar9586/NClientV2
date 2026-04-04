# Local fork vs NClientV2 3.0.5 (official zip)

Baseline: [GitHub tag 3.0.5](https://github.com/Dar9586/NClientV2/releases/tag/3.0.5). Full diff: [docs/vs-upstream-3.0.5.patch](docs/vs-upstream-3.0.5.patch) (generated April 2026, no build dirs or `.idea`).

## Build

- `minSdk` 16 (bump from 14) force the use of project butter.
- Glide annotation processor 5.0.5
- Fastscroll 1.3.0.

-`net.opacapp:multiline-collapsingtoolbar` was hosted on JCenter and the upstream project got archived at some point; 
- switched to the JitPack fork `com.github.jhavatar:multiline-collapsingtoolbar:27.1.3` 

## WebP and page JSON

- `ImageExt` includes WebP!
- `Page` handles `width`/`height`, reads `path` to infer extension, and has saner `Parcel` fallbacks.
- `GalleryData` supports the newer shape with `cover`, `thumbnail`, and `pages`.
- `SimpleGallery`: fewer nasty nulls when serializing and a sensible default when thumbnails are WebP.

## Gallery parser (`InspectorV3`)

The site keeps changing markup. There are now more ways to pull gallery JSON out of the page (inline scripts, Next `__NEXT_DATA__`, etc.), and if HTML parsing dies completely it falls back to the HTTP API (`api/v2/gallery/…` and `api/gallery/…`). Logging is there for the next time something breaks.

## Networking
30s connect/read/write timeouts on the `OkHttpClient.Builder` instances in `Global` and `OkHttpUrlLoader` so requests don’t hang forever.

## Regenerating the patch

```bash
diff -ruN \
  --exclude=build --exclude=.gradle --exclude=.idea \
  --exclude='*.iml' --exclude=local.properties \
  --exclude=.git --exclude=.cursor --exclude=docs \
  /path/to/upstream/NClientV2-3.0.5 \
  /path/to/this/project \
  | sed -E 's/^diff -ruN .*(app\/[^ ]+) (app\/[^ ]+)$/diff -ruN \1 \2/' \
  > docs/vs-upstream-3.0.5.patch
```

Exit code 1 from `diff` just means “there were differences”—that’s expected.
