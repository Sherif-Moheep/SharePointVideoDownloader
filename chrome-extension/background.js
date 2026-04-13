const DEBUG = false;

if (DEBUG) console.log("SharePoint Downloader extension loaded. Waiting for user click...");

// ----------------------------------------------------------------------
// ICON & HOVER STATE MANAGEMENT (The Domain Checker)
// ----------------------------------------------------------------------

// Helper: Update icon and hover text based on domain
function updateIconState(tabId, url) {
    if (!url) return;
    
    // Check if the URL is a supported Microsoft media domain
    const isSupported = url.includes("sharepoint.com") || 
                        url.includes("svc.ms") || 
                        url.includes("microsoftstream.com");
    
    const mode = isSupported ? "color" : "gray";
    const hoverText = isSupported ? "Click to download video" : "No supported video detected";
    
    // Update the Icon color
    chrome.action.setIcon({
        tabId: tabId,
        path: {
            "16": `images/logo-16-${mode}.png`,
            "32": `images/logo-32-${mode}.png`,
            "48": `images/logo-48-${mode}.png`,
            "128": `images/logo-128-${mode}.png`
        }
    });

    // Update the Hover Text (Tooltip)
    chrome.action.setTitle({
        tabId: tabId,
        title: hoverText
    });

}

// Run when the user switches to a different tab
chrome.tabs.onActivated.addListener((activeInfo) => {
    chrome.tabs.get(activeInfo.tabId, (tab) => {
        if (!chrome.runtime.lastError && tab) {
            updateIconState(tab.id, tab.url);
        }
    });
});

// Run when the current tab's URL changes or refreshes
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    if (changeInfo.url) {
        updateIconState(tabId, changeInfo.url);
    } else if (changeInfo.status === 'complete' && tab.url) {
        updateIconState(tabId, tab.url);
    }
});


// ----------------------------------------------------------------------
// MAIN EXECUTION LOGIC (On-Demand Extraction)
// ----------------------------------------------------------------------

// Trigger: Handle user click on the extension icon
chrome.action.onClicked.addListener(async (tab) => {
    // Basic check to ensure we are on a relevant Microsoft media page
    if (!tab.url || (!tab.url.includes("sharepoint.com") && !tab.url.includes("svc.ms") && !tab.url.includes("microsoftstream.com"))) {
        chrome.scripting.executeScript({
            target: { tabId: tab.id },
            func: () => alert("❌ Please navigate to a SharePoint/Stream video page first.")
        });
        return;
    }

    try {
        if (DEBUG) console.log(`[Tab ${tab.id}] Injecting script to fetch size and URL...`);

        // Execute the async script inside the webpage's memory
        const injectionResults = await chrome.scripting.executeScript({
            target: { tabId: tab.id },
            world: "MAIN", // CRITICAL: This allows us to see window.g_fileInfo
            func: extractVideoData 
        });

        const extractedData = injectionResults[0]?.result;

        if (extractedData && extractedData.url) {
            if (DEBUG) console.log(`[Tab ${tab.id}] ✅ Success! Size: ${extractedData.size}`);

            const videoData = {
                url: extractedData.url,
                title: tab.title || "SharePoint Video",
                size: extractedData.size // Now contains the exact MB/GB from the server
            };

            // Send to Kotlin Native App via native messaging bridge
            chrome.runtime.sendNativeMessage('com.sharepoint.downloader', videoData, (response) => {
                if (chrome.runtime.lastError) {
                    if (DEBUG) console.error("Connection Error:", chrome.runtime.lastError.message);
                    chrome.scripting.executeScript({
                        target: { tabId: tab.id },
                        func: () => alert("❌ Could not connect to the SharePoint Video Downloader desktop app.\n\nPlease ensure your native host is installed and running.")
                    });
                } else {
                    if (DEBUG) console.log("Payload sent successfully to the Kotlin app!");
                }
            });

        } else {
            if (DEBUG) console.warn(`[Tab ${tab.id}] g_fileInfo not found on this page.`);
            chrome.scripting.executeScript({
                target: { tabId: tab.id },
                func: () => alert("❌ Could not extract the video data. The page might still be loading, or it is not a supported video player.")
            });
        }

    } catch (err) {
        if (DEBUG) console.error("Script Execution Error:", err);
    }
});

// ----------------------------------------------------------------------
// THE INJECTED SCRIPT (Runs in the webpage context)
// ----------------------------------------------------------------------
async function extractVideoData() {
    // Check if Microsoft's metadata object exists
    if (typeof window.g_fileInfo === 'undefined') return null;

    const transformUrl = window.g_fileInfo['.transformUrl'] || window.g_fileInfo['.providerCdnTransformUrl'];
    if (!transformUrl) return null;

    let finalUrl = null;
    let finalSize = "Unknown";

    // Internal helper function for formatting bytes
    function formatBytes(bytes) {
        if (bytes > 1024 * 1024 * 1024) {
            return (bytes / (1024 * 1024 * 1024)).toFixed(2) + " GB";
        } else {
            return (bytes / (1024 * 1024)).toFixed(0) + " MB";
        }
    }

    try {
        const urlObj = new URL(transformUrl);

        // 1. Fetch exact size from the Graph API using the docid
        const docid = urlObj.searchParams.get("docid");
        if (docid) {
            try {
                let apiUrl = decodeURIComponent(docid);
                if (apiUrl.includes("?")) apiUrl = apiUrl.split("?")[0];
                
                // Add ?$select=size to only request the byte count
                apiUrl += "?$select=size";

                // The browser automatically attaches your session cookies to this request
                const response = await fetch(apiUrl, {
                    method: "GET",
                    headers: { "Accept": "application/json" }
                });

                if (response.ok) {
                    const data = await response.json();
                    if (data.size) finalSize = formatBytes(data.size);
                }
            } catch (sizeError) {
                console.warn("Could not fetch file size, falling back to Unknown.");
            }
        }

        // 2. Build the yt-dlp DASH manifest URL
        // Replace the thumbnail endpoint with the videomanifest endpoint
        urlObj.pathname = urlObj.pathname.replace(/\/transform\/.*$/, '/transform/videomanifest');
        // Add yt-dlp DASH format requirements
        urlObj.searchParams.set('part', 'index');
        urlObj.searchParams.set('format', 'dash');
        
        finalUrl = urlObj.toString();

        return { 
            url: finalUrl, 
            size: finalSize 
        };

    } catch (e) {
        return null;
    }
}