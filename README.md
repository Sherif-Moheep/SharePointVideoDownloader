<div align="center">
  <img src="https://github.com/user-attachments/assets/1607d4f7-8504-4e2f-9e79-92c9f25556e9" alt="App Icon" width="128">
  <h1>SharePoint Video Downloader</h1>
  <p>A seamless, one-click desktop integration tool to download view-only videos directly from Microsoft SharePoint and Microsoft Stream.</p>
</div>

---

<div align="center">
  <img src="https://github.com/user-attachments/assets/a111e8d4-b507-4797-b1c8-04ef7f0bf95d" alt="Demo GIF" width="800">
</div>

---

### ✨ Why use this tool?
* **📥 1-Click Downloads:** Grab view-only SharePoint & Stream videos instantly with a single click.
* **🤖 Fully Automated:** No manual token extraction, inspecting elements, or hunting through network tabs.
* **🙌 No Technical Skills Needed:** You don't need to be a developer to use this. The app securely handles all authentication and background bridging for you.
* **⚡ Zero Setup Required:** Batteries included! `yt-dlp`, `FFmpeg`, and a local Java Runtime are silently bundled inside the app. 
* **🖥️ Modern Dashboard:** Track your download progress and video history in a sleek, native desktop UI.

---

### 🎥 Full Video Tutorial
If you are more of a visual learner, watch this full tutorial on how to install the app, load the Chrome extension, and download your first video!

<div align="center">
  <a href="https://youtu.be/lTo5-BwJ3vg">
    <img width="800" alt="Tutorial Video Snapshot" src="https://github.com/user-attachments/assets/537e3266-5dac-43b3-9ecc-6669c1ec7c33" />
  </a>
  <br>
  <i>(Note: Click the image above to watch the tutorial!)</i>
</div>

---

### 🚀 How to Use

1. **Pin the Extension:** Once installed, pin the extension to your Chrome toolbar. The icon will be greyed out by default.
2. **Open a Video:** Navigate to any view-only SharePoint or Stream video. When a downloadable video is detected, the extension icon will light up in full color!
3. **Click to Download:** Click the colored icon. The desktop application will automatically launch and queue your video.
4. **Start & Save:** Click "Start" in the desktop app. Your video will be merged and downloaded directly to your `Downloads/SharePoint Videos` folder.

---

### ⚙️ Step-by-Step Installation Guide

Because this tool bridges a desktop app with your browser, installation happens in two quick phases. You only have to do this once!

#### Phase 1: Install the Desktop App (The Engine)
This application handles the actual downloading and bundling of your videos.

1. **Download:** Go to the Releases tab on the right side of this page and download the latest `SharePointVideoDownloader_Setup.exe` file.
2. **Install:** Double-click the installer and follow the prompts (No Admin rights required!).
3. ⚠️ **CRITICAL - Run it Once:** After installation, the app will launch automatically. **It must be opened at least once!** Running it for the first time tells Windows and Chrome how to securely talk to each other in the background. Once it opens, you can minimize or close it.

#### Phase 2: Install the Chrome Extension (The Bridge)
The extension detects the videos in your browser and sends them to the desktop app.

1. **Download the Extension:** Download the `SharePointDownloader_Extension.zip` from the Releases tab and extract it to a folder on your computer.
2. **Open Extensions:** Open Google Chrome and type `chrome://extensions/` into the URL bar, then hit Enter.
3. **Enable Developer Mode:** In the top right corner of the screen, toggle the switch for **Developer mode** so it is turned ON.
4. **Load the Extension:** Click the **Load unpacked** button that appears in the top left. Select the folder you extracted in Step 1.
5. **Pin it:** Click the puzzle piece icon 🧩 in your Chrome toolbar and click the "Pin" icon next to the SharePoint Video Downloader. This ensures you can easily see when a video is ready to download!

🎉 **You're done!** Go open a SharePoint or Stream video and watch the icon light up.

---

### 🛠️ Architecture: How it Works
Because SharePoint requires strict authentication, a standalone desktop app cannot easily download videos on its own. This project solves that using a clever two-part architecture:

* **The Chrome Extension (The Scout):** Listens to network traffic via the `chrome.webRequest` API. It intercepts the video's manifest URL and the user's active Authorization (Bearer) token, storing them temporarily.
* **Native Messaging (The Bridge):** When clicked, the extension uses Chrome's Native Messaging API to wake up the Desktop App in the background, passing the tokens via a strict byte-length-prefixed JSON stream over `System.in`.
* **The Kotlin App (The Heavy Lifter):** The app receives the payload, spawns a background `yt-dlp` process, tracks progress in a local SQLite (Room) database, and updates the UI in real-time.

---

### 💻 Tech Stack
* **Desktop App:** Kotlin, Compose Multiplatform
* **Browser Extension:** JavaScript, Chrome Extension APIs
* **Communication:** Chrome Native Messaging API, Local Sockets
* **Local Storage:** SQLite (via Room Database)
* **Media Processing:** yt-dlp, FFmpeg
* **Installer & Deployment:** Inno Setup

---

### 🤝 Contributing
This project was built to make offline learning easier for everyone. Contributions are highly encouraged!

If you want to contribute, please fork the repository, create a feature branch, and submit a Pull Request!

### 📄 License
This project is licensed under the MIT License - see the `LICENSE` file for details.
