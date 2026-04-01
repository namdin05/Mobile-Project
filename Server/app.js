import express from 'express';
import { execFile } from 'child_process';
import { resolve } from 'path';
import ytdl from '@distube/ytdl-core';

process.env.YTDL_NO_DEBUG_FILE = "1";

const app = express();
const publicDir = resolve("public");

app.use(express.static(publicDir));

app.get("/", (_req, res) => {
    res.redirect("/youtube-metadata");
});

app.get("/youtube-metadata", (_req, res) => {
    res.sendFile(resolve(publicDir, "json-formatter.html"));
});

app.get("/json-formatter", (_req, res) => {
    res.sendFile(resolve(publicDir, "json-formatter.html"));
});

const isValidYouTubeUrl = (value) => {
    if (!value || typeof value !== 'string') return false;

    try {
        const parsed = new URL(value);
        const host = parsed.hostname.toLowerCase();
        return host === "youtu.be" || host === "youtube.com" || host.endsWith(".youtube.com");
    } catch {
        return false;
    }
};

const toVideoUrl = (value) => {
    try {
        const videoId = ytdl.getVideoID(value);
        return `https://www.youtube.com/watch?v=${videoId}`;
    } catch {
        return null;
    }
};

app.get("/download", (req, res) => {
    const url = req.query.url;
    const file = "audio.mp3";

    if (!isValidYouTubeUrl(url)) {
        return res.status(400).json({ error: "Invalid or missing YouTube URL" });
    }

    const videoUrl = toVideoUrl(url);
    if (!videoUrl) {
        return res.status(400).json({ error: "URL is not a valid YouTube video link" });
    }

    execFile("yt-dlp", ["-x", "--audio-format", "mp3", "-o", file, videoUrl], (err) => {
        if (err) {
            if (err.code === "ENOENT") {
                return res.status(500).json({ error: "yt-dlp is not installed on server" });
            }

            return res.status(500).send("Download failed");
        }

        res.download(file);
    });
});

app.get("/metadata", async (req, res) => {
    const url = req.query.url;
    console.log(`Fetching metadata for URL: ${url}`);

    if (!isValidYouTubeUrl(url)) {
        return res.status(400).json({ error: "Invalid or missing YouTube URL" });
    }

    const videoUrl = toVideoUrl(url);
    if (!videoUrl) {
        return res.status(400).json({ error: "URL is not a valid YouTube video link" });
    }

    try {
        const info = await ytdl.getBasicInfo(videoUrl);
        const details = info.videoDetails;

        const metadata = {
            title: details.title,
            description: details.description,
            duration: details.lengthSeconds,
            channel: details.author?.name,
            thumbnail: details.thumbnails?.[details.thumbnails.length - 1]?.url,
            uploadDate: details.publishDate
        };

        console.log(metadata);
        res.json(metadata);
    } catch (err) {
        console.error("Failed to fetch metadata:", err);
        res.status(500).json({ error: "Failed to fetch metadata" });
    }
});


app.listen(3000, () => {
    console.log("Server running at http://localhost:3000/youtube-metadata");
});