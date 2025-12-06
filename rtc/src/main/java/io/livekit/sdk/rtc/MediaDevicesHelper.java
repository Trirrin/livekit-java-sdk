package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.audio.AudioTrackSource;
import dev.onvoid.webrtc.media.video.VideoCaptureCapability;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for media device enumeration and track creation.
 * Provides methods to list available audio/video devices and create media tracks.
 */
public class MediaDevicesHelper {

    private final PeerConnectionFactory factory;
    private AudioDeviceModule audioDeviceModule;

    public MediaDevicesHelper(PeerConnectionFactory factory) {
        this.factory = factory;
    }

    /**
     * Get a list of all available audio input devices.
     */
    public List<MediaDeviceInfo> getAudioInputDevices() {
        List<MediaDeviceInfo> devices = new ArrayList<>();
        try {
            List<AudioDevice> audioDevices = MediaDevices.getAudioCaptureDevices();
            int index = 0;
            for (AudioDevice device : audioDevices) {
                devices.add(new MediaDeviceInfo(
                        String.valueOf(index++),
                        device.getName(),
                        MediaDeviceInfo.Kind.AUDIO_INPUT
                ));
            }
        } catch (Exception e) {
            // Device enumeration may fail on some platforms
        }
        return devices;
    }

    /**
     * Get a list of all available audio output devices.
     */
    public List<MediaDeviceInfo> getAudioOutputDevices() {
        List<MediaDeviceInfo> devices = new ArrayList<>();
        try {
            List<AudioDevice> audioDevices = MediaDevices.getAudioRenderDevices();
            int index = 0;
            for (AudioDevice device : audioDevices) {
                devices.add(new MediaDeviceInfo(
                        String.valueOf(index++),
                        device.getName(),
                        MediaDeviceInfo.Kind.AUDIO_OUTPUT
                ));
            }
        } catch (Exception e) {
            // Device enumeration may fail on some platforms
        }
        return devices;
    }

    /**
     * Get a list of all available video input devices.
     */
    public List<MediaDeviceInfo> getVideoInputDevices() {
        List<MediaDeviceInfo> devices = new ArrayList<>();
        try {
            List<VideoDevice> videoDevices = MediaDevices.getVideoCaptureDevices();
            int index = 0;
            for (VideoDevice device : videoDevices) {
                devices.add(new MediaDeviceInfo(
                        String.valueOf(index++),
                        device.getName(),
                        MediaDeviceInfo.Kind.VIDEO_INPUT
                ));
            }
        } catch (Exception e) {
            // Device enumeration may fail on some platforms
        }
        return devices;
    }

    /**
     * Create a local audio track using the default audio device.
     */
    public LocalAudioTrack createAudioTrack() {
        return createAudioTrack(null, "audio");
    }

    /**
     * Create a local audio track using a specific device.
     *
     * @param deviceId Device ID (index as string), or null for default device
     * @param name     Track name
     */
    public LocalAudioTrack createAudioTrack(String deviceId, String name) {
        try {
            if (audioDeviceModule == null) {
                audioDeviceModule = new AudioDeviceModule();
            }

            // Set capture device if specified
            if (deviceId != null) {
                int deviceIndex = Integer.parseInt(deviceId);
                List<AudioDevice> devices = MediaDevices.getAudioCaptureDevices();
                if (deviceIndex >= 0 && deviceIndex < devices.size()) {
                    audioDeviceModule.setRecordingDevice(devices.get(deviceIndex));
                }
            }

            AudioOptions audioOptions = new AudioOptions();
            AudioTrackSource audioSource = factory.createAudioSource(audioOptions);
            AudioTrack nativeTrack = factory.createAudioTrack("audio-" + UUID.randomUUID(), audioSource);

            String trackId = UUID.randomUUID().toString();
            return new LocalAudioTrack(trackId, name, nativeTrack);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Create a local video track using the default video device.
     */
    public LocalVideoTrack createVideoTrack() {
        return createVideoTrack(null, "video", 1280, 720, 30);
    }

    /**
     * Create a local video track with specified parameters.
     *
     * @param deviceId  Device ID (index as string), or null for default device
     * @param name      Track name
     * @param width     Video width
     * @param height    Video height
     * @param frameRate Frame rate in fps
     */
    public LocalVideoTrack createVideoTrack(String deviceId, String name, int width, int height, int frameRate) {
        try {
            List<VideoDevice> devices = MediaDevices.getVideoCaptureDevices();
            if (devices.isEmpty()) {
                return null;
            }

            int deviceIndex = 0;
            if (deviceId != null) {
                deviceIndex = Integer.parseInt(deviceId);
                if (deviceIndex < 0 || deviceIndex >= devices.size()) {
                    deviceIndex = 0;
                }
            }

            VideoDevice selectedDevice = devices.get(deviceIndex);

            // Create capability with desired resolution
            VideoCaptureCapability capability = new VideoCaptureCapability(width, height, frameRate);

            VideoDeviceSource videoSource = new VideoDeviceSource();
            videoSource.setVideoCaptureDevice(selectedDevice);
            videoSource.setVideoCaptureCapability(capability);
            videoSource.start();

            VideoTrack nativeTrack = factory.createVideoTrack("video-" + UUID.randomUUID(), videoSource);

            String trackId = UUID.randomUUID().toString();
            return new LocalVideoTrack(trackId, name, nativeTrack);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Dispose of resources used by this helper.
     */
    public void dispose() {
        if (audioDeviceModule != null) {
            audioDeviceModule.dispose();
            audioDeviceModule = null;
        }
    }
}
