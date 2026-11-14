package com.appnova.novatube.adblock

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Brave Shields-inspired 3-Layer AdBlock Engine for YouTube:
 * 1. Network-level request blocker (EasyList / Peter Lowe / Brave ad rules)
 * 2. JSON-Prune Scriptlet (strips adPlacements, playerAds, adSlots, adBreakHeartbeatParams
 *    from JSON.parse, fetch Response.json(), XHR, and window.ytInitialPlayerResponse
 *    BEFORE the YouTube player can schedule any pre-roll or mid-roll ad)
 * 3. Cosmetic DOM filter & fallback video ad skipper
 */
object BraveShieldsEngine {

    private val blockedDomains = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.",
        "google-analytics.com",
        "googletagmanager.com",
        "2mdn.net",
        "moatads.com",
        "innovid.com",
        "fwmrm.net",
        "imasdk.googleapis.com",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",
        "s0.2mdn.net",
        "static.doubleclick.net"
    )

    private val blockedYouTubePatterns = listOf(
        "/pagead/",
        "/ptracking",
        "/api/stats/ads",
        "/api/stats/qoe?adformat=",
        "/get_midroll_info",
        "/youtubei/v1/player/ad_break",
        "&adformat=",
        "&ad_type=",
        "?adformat="
    )

    fun shouldBlockRequest(request: WebResourceRequest): Boolean {
        if (request.isForMainFrame) return false
        val url = request.url.toString().lowercase()
        val host = request.url.host?.lowercase().orEmpty()

        if (blockedDomains.any { host == it || host.endsWith(".$it") || host.contains(it) }) {
            return true
        }
        if ((host.contains("youtube.com") || host.contains("googlevideo.com")) &&
            blockedYouTubePatterns.any { url.contains(it) }
        ) {
            return true
        }
        return false
    }

    fun emptyResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain",
        "UTF-8",
        204,
        "No Content",
        mapOf("Access-Control-Allow-Origin" to "*"),
        ByteArrayInputStream(ByteArray(0))
    )

    /**
     * Brave Shields-style json-prune scriptlet + cosmetic rules.
     * Hooks JSON.parse, Response.prototype.json, and window.ytInitialPlayerResponse
     * to prune adPlacements, playerAds, adSlots, and adBreakHeartbeatParams.
     */
    const val BRAVE_YOUTUBE_SCRIPTLET_JS = """
        (function() {
            if (window.__braveShieldsActive) return;
            window.__braveShieldsActive = true;

            const AD_KEYS = [
                'adPlacements',
                'playerAds',
                'adSlots',
                'adBreakHeartbeatParams'
            ];

            function pruneYouTubeAds(obj) {
                if (!obj || typeof obj !== 'object') return obj;
                for (let i = 0; i < AD_KEYS.length; i++) {
                    const k = AD_KEYS[i];
                    if (k in obj) {
                        delete obj[k];
                    }
                }
                if (obj.playerResponse && typeof obj.playerResponse === 'object') {
                    for (let i = 0; i < AD_KEYS.length; i++) {
                        delete obj.playerResponse[AD_KEYS[i]];
                    }
                }
                return obj;
            }

            // 1. Hook JSON.parse (prunes XHR & internal JSON parses)
            const origJsonParse = JSON.parse;
            JSON.parse = function(text, reviver) {
                const data = origJsonParse.call(this, text, reviver);
                try {
                    if (data && typeof data === 'object') {
                        pruneYouTubeAds(data);
                    }
                } catch (e) {}
                return data;
            };

            // 2. Hook Fetch Response.prototype.json
            if (window.Response && Response.prototype.json) {
                const origResJson = Response.prototype.json;
                Response.prototype.json = function() {
                    return origResJson.call(this).then(data => {
                        try { pruneYouTubeAds(data); } catch (e) {}
                        return data;
                    });
                };
            }

            // 3. Intercept window.ytInitialPlayerResponse
            try {
                let currentPlayerRes = window.ytInitialPlayerResponse;
                if (currentPlayerRes) pruneYouTubeAds(currentPlayerRes);
                Object.defineProperty(window, 'ytInitialPlayerResponse', {
                    configurable: true,
                    get: function() { return currentPlayerRes; },
                    set: function(val) {
                        currentPlayerRes = pruneYouTubeAds(val);
                    }
                });
            } catch (e) {}
        })();
    """

    const val BRAVE_COSMETIC_CSS = """
        ytm-promoted-sparkles-web-renderer,
        ytm-promoted-video-renderer,
        ytm-companion-ad-renderer,
        ytm-statement-banner-renderer,
        ytd-ad-slot-renderer,
        ytm-ad-slot-renderer,
        ad-slot-renderer,
        ytm-in-feed-ad-layout-renderer,
        ytm-brand-video-singleton-renderer,
        ytm-rich-item-renderer:has(ytm-ad-slot-renderer),
        ytm-rich-item-renderer:has(ad-slot-renderer),
        .ytp-ad-module,
        .ytp-ad-overlay-container,
        .ytp-ad-message-container,
        [data-style="AD_BADGE"] {
            display: none !important;
        }
    """
}
