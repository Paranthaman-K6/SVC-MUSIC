package dev.local.vcmusic;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent;

public final class MusicVoicechatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return "vcmusic";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MergeClientSoundEvent.class, MusicPlayer.INSTANCE::onMerge);
    }
}
