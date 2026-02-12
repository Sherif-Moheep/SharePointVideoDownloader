let debounceTimer = null;

console.log("Extensions started. Listening for video...");

chrome.webRequest.onBeforeRequest.addListener(
  function (details) {
    const url = details.url;
  
    if (details.method !== "GET") return;

    if (url.includes("videomanifest")) { 
        
        if (debounceTimer) {
            clearTimeout(debounceTimer);
        }

        debounceTimer = setTimeout(() => {
            
            const tabId = details.tabId;
            if (tabId && tabId !== -1) {
                chrome.tabs.get(tabId, function(tab) {
                    if (chrome.runtime.lastError || !tab) return;

                    const fullTitle = tab.title; 

                    console.log("-----------------------------------------");
                    console.log("✅ FINAL LINK CAPTURED (Debounced)");
                    console.log("🔗 URL:", url);
                    console.log("🏷️ NAME:", fullTitle);
                    console.log("-----------------------------------------");
                });
            }
            
        }, 2000); 
    }
  },
  { 
    urls: [
      "*://*.sharepoint.com/*videomanifest*",
      "*://*.svc.ms/*videomanifest*",
      "*://*.microsoftstream.com/*videomanifest*"
    ]
  }
);