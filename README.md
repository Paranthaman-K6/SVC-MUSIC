# Voice Chat Music

Client-side Fabric mod for Minecraft 26.3 that plays songs from YouTube (via `yt-dlp`) into
[Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat), so the people you are talking to
hear the music **at the same time** as your voice.

No server mod, no bot account, no extra port. Everyone else just needs Simple Voice Chat.

## How it works

Simple Voice Chat sends one audio frame per 20 ms from your microphone. This mod decodes the song with
`ffmpeg` to 48 kHz mono PCM, and for every microphone frame it mixes one music frame into the outgoing
frame through the client-side `MergeClientSoundEvent`. Whatever SVC does with your voice (proximity,
groups, whispers) it does with the mixed audio too.

SVC never plays your own audio back to you, so the same frame is also played through a local
`ClientStaticAudioChannel` for your own ears. Both copies are gain-controlled; while you speak the music
drops to 35 % instead of stopping, so your voice stays on top and the song never cuts out.

## Requirements

- Minecraft 26.3, Fabric Loader 0.20.1+
- [Simple Voice Chat](https://modrinth.com/mod/simple-voice-chat) 2.6.27+ (client and server)
- `yt-dlp` and `ffmpeg` on your `PATH`
- A working microphone selected in Simple Voice Chat

## Install

1. Put `voicechat-music-client-0.1.0.jar` and Simple Voice Chat in `.minecraft/mods`.
2. Install the tools: `pipx install yt-dlp` and your distro's `ffmpeg` package.
3. Start the game and join a server with Simple Voice Chat.

## Use

Press **K** to open the panel (`M` is Simple Voice Chat's mute key, so it is not used).

| Panel | |
| --- | --- |
| Search box | song name, YouTube song link or playlist link |
| Results / Saved | click a song to play it (or queue it if something is playing) |
| Save | saves the selected song, the song playing, or the link typed in the box |
| Remove | deletes the selected saved song |
| Add all | queues the whole result list (a pasted playlist) or your saved songs |
| Volume | music level in your voice stream |
| Monitor | your own local copy only |
| Duck while speaking | on/off |
| Pause / Next / Stop | queue control |

Every control has a hover tooltip. Hover a song for its full title and duration. Song rows and the
now-playing card show YouTube thumbnail art when it is available.

The same thing from chat:

```
/music play <query>     search and play the first match
/music pause | resume | skip | stop
/music vol <0-100>
/music save             save what is playing
/music list             list saved songs
/music panel            open the panel
```

Settings live in `config/vcmusic/config.json`, saved songs in `config/vcmusic/library.json`.
`ytdlp` and `ffmpeg` in the config can be absolute paths if they are not on `PATH`.

## Limitations

- Music travels through your microphone stream. If you have no microphone, are muted in Simple Voice
  Chat, or have voice chat disabled, the music is not sent at all. The panel says
  `Enable voice chat to play music` when it sees no microphone frames.
- The talking indicator stays lit while music plays. There is no way to separate it from your mic audio.
- Everyone who can hear your voice hears the music, with the normal proximity and group rules.
  A server admin can set `codec=AUDIO` in the SVC server config for better music quality; the default
  `VOIP` codec is tuned for speech.
- The first seconds of a track are silent while `yt-dlp` resolves the stream. The panel shows
  `Buffering...` until audio arrives.
- No seeking, and the queue is not saved between sessions.

## Build

JDK 25 is required.

```
./gradlew build      # jar in build/libs
./gradlew runClient  # dev client with Simple Voice Chat
```

`./gradlew test` covers the PCM decoding, frame splitting and ducking math.
