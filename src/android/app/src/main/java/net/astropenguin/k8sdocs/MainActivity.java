package net.astropenguin.k8sdocs;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String LOCAL_HOST = "k8s.local";
    private static final String UPSTREAM_HOST = "kubernetes.io";
    private static final String ASSET_ROOT = "site";

    private WebView webView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        WebView.setWebContentsDebuggingEnabled(true);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDatabaseEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);

        webView.setWebViewClient(new OfflineWebViewClient(getAssets()));
        webView.loadUrl("https://" + LOCAL_HOST + "/docs/home/");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    private final class OfflineWebViewClient extends WebViewClient {
        private final AssetManager assets;

        OfflineWebViewClient(AssetManager assets) {
            this.assets = assets;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String host = uri.getHost();

            if (LOCAL_HOST.equalsIgnoreCase(host) || UPSTREAM_HOST.equalsIgnoreCase(host)) {
                return responseFor(uri);
            }

            return null;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String host = uri.getHost();

            if (LOCAL_HOST.equalsIgnoreCase(host) || UPSTREAM_HOST.equalsIgnoreCase(host)) {
                if (UPSTREAM_HOST.equalsIgnoreCase(host)) {
                    Uri local = uri.buildUpon().authority(LOCAL_HOST).build();
                    view.loadUrl(local.toString());
                    return true;
                }
                return false;
            }

            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            }

            return false;
        }

        private WebResourceResponse responseFor(Uri uri) {
            String path = uri.getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) {
                path = "/index.html";
            }

            while (path.startsWith("/")) {
                path = path.substring(1);
            }

            String assetPath = ASSET_ROOT + "/" + path;

            InputStream stream = openAsset(assetPath);
            if (stream == null && !hasExtension(path)) {
                if (!assetPath.endsWith("/")) {
                    assetPath += "/";
                }
                assetPath += "index.html";
                stream = openAsset(assetPath);
            }

            if (stream == null) {
                byte[] body = (
                    "<!doctype html><meta charset=utf-8>" +
                    "<title>Not found</title>" +
                    "<h1>Offline page not found</h1>" +
                    "<p>" + escapeHtml(uri.toString()) + "</p>"
                ).getBytes(java.nio.charset.StandardCharsets.UTF_8);

                return new WebResourceResponse(
                    "text/html",
                    "utf-8",
                    404,
                    "Not Found",
                    java.util.Collections.singletonMap("Cache-Control", "no-store"),
                    new ByteArrayInputStream(body)
                );
            }

            String mime = mimeType(assetPath);
            String encoding = isTextMime(mime) ? "utf-8" : null;

            android.util.Log.d("K8sDocs", mime + ": " + assetPath);

            if ("text/html".equals(mime)) {
                try {
                    stream = patchHtml(stream);
                } catch (IOException e) {
                    return null;
                }
            } else if ("text/css".equals(mime)) {
                try {
                    stream = patchCss(stream);
                } catch (IOException e) {
                    return null;
                }
            }

            return new WebResourceResponse(
                mime,
                encoding,
                200,
                "OK",
                java.util.Collections.singletonMap("Cache-Control", "public, max-age=31536000"),
                stream
            );
        }

        private InputStream openAsset(String path) {
            try {
                return assets.open(path, AssetManager.ACCESS_STREAMING);
            } catch (IOException ignored) {
                return null;
            }
        }

        private InputStream patchHtml(InputStream input) throws IOException {
            String html = new String(
                    input.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8
                    );

            html = html.replaceFirst(
                "(?i)<meta\\s+name=(?:[\"']viewport[\"']|viewport)\\s+content=[\"'][^\"']*[\"']\\s*/?>",
                "<meta name=\"viewport\" content=\"width=1280\">"
            )
            .replace(
                "https://code.jquery.com/jquery-3.7.1.min.js",
                "/offline/vendor/jquery/jquery-3.7.1.min.js"
            )
            .replace(
                "https://cdn.jsdelivr.net/npm/mermaid@9.3.0/dist/mermaid.min.js",
                "/offline/vendor/mermaid/mermaid-9.3.0.min.js"
            )
            .replace(
                "https://cdnjs.cloudflare.com/ajax/libs/iframe-resizer/4.3.9/iframeResizer.min.js",
                "/offline/vendor/iframe-resizer/iframeResizer-4.3.9.min.js"
            )
            .replace(
                "https://cdn.jsdelivr.net/npm/katex@0.16.10/dist/katex.min.js",
                "/offline/vendor/katex-0.16.10/katex.min.js"
            )
            .replace(
                "https://cdn.jsdelivr.net/npm/katex@0.16.10/dist/contrib/auto-render.min.js",
                "/offline/vendor/katex-0.16.10/contrib/auto-render.min.js"
            )
            .replace(
                "https://cdn.jsdelivr.net/npm/katex@0.16.10/dist/katex.min.css",
                "/offline/vendor/katex-0.16.10/katex.min.css"
            )
            .replace(
                "https://fonts.googleapis.com/css?family=Open+Sans:300,300i,400,400i,700,700i&display=swap",
                "/offline/vendor/google-fonts/open-sans.css"
            )
            .replace(
                "https://fonts.googleapis.com/css?family=Open+Sans",
                "/offline/vendor/google-fonts/open-sans.css"
            )
            .replaceAll(
                "(?i)\\s+integrity=(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)",
                ""
            )
            .replaceAll(
                "(?i)\\s+crossorigin(?:=(?:\"[^\"]*\"|'[^']*'|[^\\s>]+))?",
                ""
            )
            .replaceAll(
                "(?i)<script[^>]+src=[\"']?https://platform\\.twitter\\.com/widgets\\.js[\"']?[^>]*></script>",
                ""
            );

            return new ByteArrayInputStream(
                html.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
        }

        private InputStream patchCss(InputStream input) throws IOException {
            String css = new String(
                input.readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8
            );

            css = css
                .replace(
                    "https://fonts.googleapis.com/css?family=Open+Sans:300,300i,400,400i,700,700i&display=swap",
                    "/offline/vendor/google-fonts/open-sans.css"
                )
                .replace(
                    "https://fonts.googleapis.com/css?family=Open+Sans",
                    "/offline/vendor/google-fonts/open-sans.css"
                );

            return new ByteArrayInputStream(
                css.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
        }
    }

    private static boolean hasExtension(String path) {
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        return dot > slash;
    }

    private static String mimeType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "text/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".otf")) return "font/otf";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".txt")) return "text/plain";

        String guessed = URLConnection.guessContentTypeFromName(path);
        return guessed != null ? guessed : "application/octet-stream";
    }

    private static boolean isTextMime(String mime) {
        return mime.startsWith("text/")
            || "application/json".equals(mime)
            || "application/xml".equals(mime)
            || "image/svg+xml".equals(mime)
            || "text/javascript".equals(mime);
    }

    private static String escapeHtml(String s) {
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
