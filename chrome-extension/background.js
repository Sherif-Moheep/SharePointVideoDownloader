// using Maps to store data separately for every tab.
// let videoMap = {}; // { tabId: { url, title, size } }
let timers = {}; // { tabId: timerId }

console.log("Extension started. Listening for SharePoint/Stream videos..."); // remove before production, just for testing

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
});

// Helper: Reset icon and clear data if user navigates away from video page
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    // If the URL changes and it's no longer a SharePoint/Stream video
    if (changeInfo.url && !changeInfo.url.includes("videomanifest")) {
        const storageKey = `video_${tabId}`;
        chrome.storage.session.remove(storageKey);
        
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

// Helper: Calculate approximate video size from metadata URLs
function grabVideoDetails(tabId, url){
        chrome.tabs.get(tabId, (tab) => {
            if (chrome.runtime.lastError || !tab) return;

            const videoTitle = tab.title || "Unknown Video";
            const estimatedSize = calculateVideoSize(url);
            const storageKey = `video_${tabId}`;

            const videoData = {
                url: url,
                title: videoTitle,
                size: estimatedSize
            };
            chrome.storage.session.set({ [storageKey]: videoData }, () => {
                console.log(`[Tab ${tabId}] Video data saved to session storage.`); // remove before production, just for testing
            });

            console.log(`[Tab ${tabId}] Video Captured!`); // remove before production, just for testing
            console.log(`Title: ${videoTitle}`); // remove before production, just for testing
            console.log(`Size:  ${estimatedSize}`); // remove before production, just for testing
            console.log(`URL:   ${url}`); // remove before production, just for testing 

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
            title: `Download: ${videoTitle} (~${estimatedSize})`, 
            tabId: tabId 
            });
        });

        delete timers[tabId];
}

// Helper function to calculate approx. video size from metadata URL
function calculateVideoSize(url) {
    try {
        const urlObj = new URL(url);
        // 1. Get the hidden metadata from the URL
        const metadataParam = urlObj.searchParams.get("altManifestMetadata");
        if (!metadataParam) return "Unknown";

        // 2. Decode Base64 to JSON
        // atob = ascii to binary, decodes base64 string to JSON object
        const data = JSON.parse(atob(metadataParam));

        let videoBitrate = data.Bitrate; 
        const durationNano = data.Duration100Nano; 
        
        if (!videoBitrate || !durationNano) return "Unknown";

        // 3. AUDIO FIX: Add 32kbps buffer for audio track
        const AUDIO_BITRATE_BUFFER = 32000; 
        const totalBitrate = videoBitrate + AUDIO_BITRATE_BUFFER;
        
        // 4. Convert Duration (100ns units -> Seconds)
        const durationSeconds = durationNano / 10000000;

        // 5. Calculate Size (Bits * Seconds / 8 = Bytes)
        const sizeInBytes = (totalBitrate * durationSeconds) / 8;

        // 6. Format Output
        if (sizeInBytes > 1024 * 1024 * 1024) {
            return (sizeInBytes / (1024 * 1024 * 1024)).toFixed(2) + " GB";
        } else {
            return (sizeInBytes / (1024 * 1024)).toFixed(0) + " MB";
        }
    } catch (e) {
        console.error("Calc Error:", e);
        return "Unknown";
    }
}