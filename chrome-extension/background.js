// using Maps to store data separately for every tab.
// let videoMap = {}; // { tabId: { url, title, size } }
let timers = {}; // { tabId: timerId }
let tokens = {}; // { tabId: bearerToken } - Stores intercepted tokens temporarily

console.log("Extension started. Listening for SharePoint/Stream videos..."); // remove before production, just for testing

// 1. LISTENER: Intercepts outgoing requests to grab the Bearer token
chrome.webRequest.onBeforeSendHeaders.addListener(
    function (details) {
        const tabId = details.tabId;
        if (tabId === -1) return;

        // Loop through headers looking for Authorization
        for (let header of details.requestHeaders) {
            if (header.name.toLowerCase() === 'authorization') {
                tokens[tabId] = header.value; 
                console.log(`[Tab ${tabId}] Bearer token intercepted!`); // remove before production
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

    // Debounve logic to get only the "final" request (which is the one we need)
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
            console.log(`No video found for Tab ${tab.id}`); // remove before production, just for testing
            chrome.scripting.executeScript({
                target: { tabId: tab.id },
                function: () => alert("❌ No video data found for this tab. Please make sure you're on a SharePoint/Stream video page and try again.")
            });
            return;
        }

        console.log(`READY TO DOWNLOAD:`, videoData.title); // remove before production, just for testing

        chrome.runtime.sendNativeMessage('com.sharepoint.downloader', videoData, (response) => {
            if (chrome.runtime.lastError) {
                console.error("Connection Error:", chrome.runtime.lastError.message); // remove before production, just for testing
            } else {
                console.log("Message sent successfully!"); // remove before production, just for testing
            }
        });
    });
});

// Helper: Process video details and update UI
chrome.tabs.onRemoved.addListener((tabId) => {
    const storageKey = `video_${tabId}`;
    chrome.storage.session.remove(storageKey, () => {
        console.log(`Cleared storage data for closed Tab ${tabId}`); // remove before production, just for testing
    });
    
    if (timers[tabId]) {
        clearTimeout(timers[tabId]);
        delete timers[tabId];
    }

    // ADD THIS: Clear the token when the tab is closed
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

        // ADD THIS: Clear the token if they leave the video page
        if (tokens[tabId]) {
            delete tokens[tabId];
        }
        
        chrome.action.setIcon({ 
            tabId: tabId, 
            path: {
                "16": "images/logo-16-grayv2.png",
                "32": "images/logo-32-grayv2.png",
                "48": "images/logo-48-grayv2.png",
                "128": "images/logo-128-grayv2.png"
            } 
        });

        chrome.action.setTitle({ 
            title: "No video detected", 
            tabId: tabId 
        });
    }
});

// Helper: Calculate approximate video size from metadata URLs
async function grabVideoDetails(tabId, url){
        chrome.tabs.get(tabId, async (tab) => {
            if (chrome.runtime.lastError || !tab) return;

            const videoTitle = tab.title || "Unknown Video";
            // const estimatedSize = calculateVideoSize(url);
            const currentToken = tokens[tabId] || null;
            let finalSize = "Unknown";
            // const storageKey = `video_${tabId}`;

            // Try to fetch the EXACT size using the token
            if (currentToken) {
                console.log(`[Tab ${tabId}] Token found. Fetching exact size from API...`); // remove before production
                finalSize = await fetchExactSize(url, currentToken);
            }

            // Fallback to estimation math if API request fails or token is missing
            if (!finalSize || finalSize === "Unknown") {
                console.log(`[Tab ${tabId}] API failed or no token. Falling back to estimated size.`); // remove before production
                // finalSize = calculateVideoSize(url);
                finalSize = "Unknown";
            }

            const storageKey = `video_${tabId}`;
            
            const videoData = {
                url: url,
                title: videoTitle,
                size: finalSize,
            };

            chrome.storage.session.set({ [storageKey]: videoData }, () => {
            console.log(`[Tab ${tabId}] Video data saved. Size: ${finalSize}`); 
            });

            console.log(`[Tab ${tabId}] Video Captured!`); // remove before production, just for testing
            console.log(`Title: ${videoTitle}`); // remove before production, just for testing
            console.log(`Size:  ${finalSize}`); // remove before production, just for testing
            console.log(`URL:   ${url}`); // remove before production, just for testing 

            chrome.action.setIcon({
                tabId: tabId,
                path: {
                    "16": "images/logo-16-colorv2.png",
                    "32": "images/logo-32-colorv2.png",
                    "48": "images/logo-48-colorv2.png",
                    "128": "images/logo-128-colorv2.png"
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
            console.warn("API request rejected. Status:", response.status);
        }
        return null;
        
    } catch (e) {
        console.error("Fetch Exact Size Error:", e);
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

// Helper function to calculate approx. video size from metadata URL
function calculateVideoSize(url) {
    try {
        const urlObj = new URL(url);
        const metadataParam = urlObj.searchParams.get("altManifestMetadata");
        if (!metadataParam) return "Unknown";

        const data = JSON.parse(atob(metadataParam));
        let videoBitrate = data.Bitrate; 
        const durationNano = data.Duration100Nano; 
        
        if (!videoBitrate || !durationNano) return "Unknown";

        const AUDIO_BITRATE_BUFFER = 32000; 
        const totalBitrate = videoBitrate + AUDIO_BITRATE_BUFFER;
        const durationSeconds = durationNano / 10000000;
        const sizeInBytes = (totalBitrate * durationSeconds) / 8;

        return formatBytes(sizeInBytes);
    } catch (e) {
        console.error("Calc Error:", e);
        return "Unknown";
    }
}