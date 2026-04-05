const DEBUG = false; // debug flag

let timers = {}; // { tabId: timerId }
let tokens = {}; // { tabId: bearerToken } - Stores intercepted tokens temporarily

if (DEBUG) console.log("Extension started. Listening for SharePoint/Stream videos...");

// 1. LISTENER: Intercepts outgoing requests to grab the Bearer token
chrome.webRequest.onBeforeSendHeaders.addListener(
    function (details) {
        const tabId = details.tabId;
        if (tabId === -1) return;

        for (let header of details.requestHeaders) {
            if (header.name.toLowerCase() === 'authorization') {
                tokens[tabId] = header.value; 
                if (DEBUG) console.log(`[Tab ${tabId}] Bearer token intercepted!`);
                break; 
            }
        }
        return { requestHeaders: details.requestHeaders };
    },
    { 
        urls: [
            "*://*.sharepoint.com/*",
            "*://*.svc.ms/*",
            "*://*.microsoftstream.com/*"
        ] 
    },
    ["requestHeaders", "extraHeaders"] 
);

// Listener: Intercepts SharePoint/Stream video manifest requests
chrome.webRequest.onBeforeRequest.addListener(
  function (details) {
    if (details.method !== "GET") return;
    
    const tabId = details.tabId;
    const url = details.url;

    if (tabId === -1) return;

    // Debounce logic to get only the "final" request
    if (timers[tabId]) clearTimeout(timers[tabId]);

    timers[tabId] = setTimeout(() => grabVideoDetails(tabId, url), 2000); 
  },
  { 
    urls: [
      "*://*.sharepoint.com/*videomanifest*",
      "*://*.svc.ms/*videomanifest*",
      "*://*.microsoftstream.com/*videomanifest*"
    ] 
  }
);

// Trigger: Handle user click on extension icon
chrome.action.onClicked.addListener((tab) => {
    const storageKey = `video_${tab.id}`;
    
    chrome.storage.session.get([storageKey], (result) => {
        const videoData = result[storageKey];

        if (!videoData) {
            chrome.scripting.executeScript({
                target: { tabId: tab.id },
                function: () => alert("❌ No video data found for this tab. Please make sure you're on a SharePoint/Stream video page and try again.")
            });
            return;
        }

        chrome.runtime.sendNativeMessage('com.sharepoint.downloader', videoData, (response) => {
            if (chrome.runtime.lastError) {
                if (DEBUG) console.error("Connection Error:", chrome.runtime.lastError.message);

                chrome.scripting.executeScript({
                    target: { tabId: tab.id },
                    function: () => alert("❌ Could not connect to the SharePoint Video Downloader desktop app.\n\nPlease ensure you have installed the app and opened it at least once.")
                });
            } else {
                if (DEBUG) console.log("Message sent successfully!");
            }
        });
    });
});

// Helper: Process video details and update UI
chrome.tabs.onRemoved.addListener((tabId) => {
    const storageKey = `video_${tabId}`;
    chrome.storage.session.remove(storageKey, () => {
        if (DEBUG) console.log(`Cleared storage data for closed Tab ${tabId}`);
    });
    
    if (timers[tabId]) {
        clearTimeout(timers[tabId]);
        delete timers[tabId];
    }

    if (tokens[tabId]) {
        delete tokens[tabId];
    }
});

// Helper: Reset icon and clear data if user navigates away from video page
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    // If the URL changes and it's no longer a SharePoint/Stream video
    if (changeInfo.url && !changeInfo.url.includes("videomanifest")) {
        const storageKey = `video_${tabId}`;
        chrome.storage.session.remove(storageKey);

        // Clear the token if they leave the video page
        if (tokens[tabId]) {
            delete tokens[tabId];
        }
        
        chrome.action.setIcon({ 
            tabId: tabId, 
            path: {
                "16": "images/logo-16-gray.png",
                "32": "images/logo-32-gray.png",
                "48": "images/logo-48-gray.png",
                "128": "images/logo-128-gray.png"
            } 
        });

        chrome.action.setTitle({ 
            title: "No video detected", 
            tabId: tabId 
        });
    }
});

// Helper: Prepare video details and update UI
async function grabVideoDetails(tabId, url){
    chrome.tabs.get(tabId, async (tab) => {
        if (chrome.runtime.lastError || !tab) return;

        const videoTitle = tab.title || "Unknown Video";
        const currentToken = tokens[tabId] || null;
        let finalSize = "Unknown";

        // Try to fetch the EXACT size using the token
        if (currentToken) {
            const exactSize = await fetchExactSize(url, currentToken);
            if (exactSize) {
                finalSize = exactSize;
            }
        }

        const storageKey = `video_${tabId}`;
        const videoData = {
            url: url,
            title: videoTitle,
            size: finalSize,
        };

        chrome.storage.session.set({ [storageKey]: videoData }, () => {
            if (DEBUG) console.log(`[Tab ${tabId}] Video data saved. Size: ${finalSize}`); 
        });

        chrome.action.setIcon({
            tabId: tabId,
            path: {
                "16": "images/logo-16-color.png",
                "32": "images/logo-32-color.png",
                "48": "images/logo-48-color.png",
                "128": "images/logo-128-color.png"
            }
        });

        chrome.action.setTitle({ 
            title: `Download: ${videoTitle} (${finalSize})`, 
            tabId: tabId 
        });
    });

    delete timers[tabId];
}

// 7. HELPER: Parses the API URL from the manifest and fetches the size
async function fetchExactSize(manifestUrl, bearerToken) {
    try {
        const urlObj = new URL(manifestUrl);
        
        // The docid parameter contains the encoded Graph API URL
        const docid = urlObj.searchParams.get("docid");
        if (!docid) return null;

        let apiUrl = decodeURIComponent(docid);

        if (apiUrl.includes("?")) {
            apiUrl = apiUrl.split("?")[0];
        }

        // Add ?$select=size to only request the byte count
        apiUrl += "?$select=size";

        const response = await fetch(apiUrl, {
            method: "GET",
            headers: {
                "Authorization": bearerToken,
                "Accept": "application/json"
            }
        });

        if (response.ok) {
            const data = await response.json();
            if (data.size) {
                return formatBytes(data.size);
            }
        } else {
            if (DEBUG) console.warn("API request rejected. Status:", response.status);
        }
        return null;
        
    } catch (e) {
        if (DEBUG) console.error("Fetch Exact Size Error:", e);
        return null;
    }
}

// 8. HELPER: Converts raw bytes into a clean MB/GB string
function formatBytes(bytes) {
    if (bytes > 1024 * 1024 * 1024) {
        return (bytes / (1024 * 1024 * 1024)).toFixed(2) + " GB";
    } else {
        return (bytes / (1024 * 1024)).toFixed(0) + " MB";
    }
}