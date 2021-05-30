package com.androidvip.hebf.utils;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.Keep;
import androidx.collection.ArrayMap;

import com.androidvip.hebf.BuildConfig;

import java.io.File;
import java.util.Locale;

public final class K {
    public static final int USER_TYPE_NORMAL = 1;
    public static final int USER_TYPE_EXPERT = 2;
    public static final int USER_TYPE_CHUCK_NORRIS = 3;

    public static final int NOTIF_DISMISS_ONLY_ID = 0;
    public static final int NOTIF_REAPPLY_ID = 1;
    public static final int NOTIF_GB_ID = 2;
    public static final int NOTIF_GB_LESS_ID = 3;
    public static final int NOTIF_VIP_ID = 4;
    public static final int NOTIF_VIP_LESS_ID = 5;
    public static final int NOTIF_WM_ID = 6;

    public static final int NOTIF_ACTION_DISMISS_ID = 300;
    public static final int NOTIF_ACTION_WM_RESET_ID = 301;
    public static final int NOTIF_ACTION_BOOST_ID = 302;
    public static final int NOTIF_ACTION_BOOST_LESS_ID = 303;
    public static final int NOTIF_ACTION_STOP_GB_ID = 304;
    public static final int NOTIF_ACTION_STOP_GB_LESS_ID = 305;
    public static final int NOTIF_ACTION_STOP_VIP_ID = 306;
    public static final int NOTIF_ACTION_STOP_VIP_LESS_ID = 307;

    static final int VIP_JOB_ID = 10;
    static final int VIP_CHARGER_JOB_ID = 16;
    static final int DOZE_JOB_ID = 11;
    static final int FSTRIM_JOB_ID = 12;
    public static final int GAME_LESS_JOB_ID = 13;
    public static final int VIP_LESS_JOB_ID = 14;
    static final int MEDIASERVER_JOB_ID = 15;
    public static final int SCREEN_OFF_JOB_ID = 16;
    public static final int POWER_CONNECTED_JOB_ID = 17;
    public static final String EXTRA_NOTIF_ID = "hebf_notif_id";
    public static final String EXTRA_NOTIF_ACTION_ID = "hebf_notif_action_id";
    public static final String EXTRA_SHORTCUT_ID = "shortcut_id";
    public static final String EXTRA_GOVERNOR_TUNABLES = "governor_tunables";
    public static final String EXTRA_RESTORE_ACTIVITY = "restore_activity";
    public static final String EXTRA_FILE = "file";
    public static final String EXTRA_APP = "app";
    public static final String SHORTCUT_ID_CPU = "shortcut_cpu";
    public static final String SHORTCUT_ID_CLEANER = "shortcut_cleaner";
    public static final String SHORTCUT_ID_PERFORMANCE = "shortcut_performance";
    public static final String SHORTCUT_ID_BATTERY = "shortcut_battery";

    public static final String NOTIF_CHANNEL_ID_ONGOING = "ongoing";
    public static final String NOTIF_CHANNEL_ID_GENERAL = "general";
    public static final String NOTIF_CHANNEL_ID_PUSH = "push";
    public static final String NOTIF_CHANNEL_ID_OFFERS = "offers";
    public static final String NOTIF_CHANNEL_ID_LOW_PRIORITY = "low_priority";

    private K() {
        // No instances
    }

    public static class HEBF {
        @Deprecated
        public static final File HEBF_FOLDER = new File(Environment.getExternalStorageDirectory(), "HEBF");

        public static File getLogFile(Context ctx) {
            return new File(ctx.getExternalFilesDir(null), "app.log");
        }

        public static File getFstrimLog(Context ctx) {
            return new File(ctx.getExternalFilesDir(null), "fstrim.log");
        }

        public static File getTempDir(Context ctx) {
            return new File(ctx.getExternalFilesDir(null), "temp");
        }
    }

    public static class PREF {
        public static final String USER_HAS_ROOT = "user_has_root";
        public static final String FORCE_STOP_APPS_SET = "force_stop_set";
        public static final String HAS_CRASHED = "crashed";
        public static final String CRASH_MESSAGE = "crash_msg";
        public static final String EXTENDED_LOGGING_ENABLED = "extended_logging_enabled";
        public static final String IS_FIRST_START = "firststart";
        public static final String UNLOCKED_ADVANCED_OPTIONS = "unlocked_advanced_options";
        public static final String INFO_SHOWN = "info_shown";
        public static final String THEME = "theme";
        public static final String ENGLISH_LANGUAGE = "english_language";
        public static final String USER_TYPE = "user_type";
        public static final String MEDIASERVER_SCHDL_INTERVAL_MILLIS = "mediaserver_schedule_interval";
        public static final String MEDIASERVER_JOB_SCHEDULED = "mediaserver_job_scheduled";
        public static final String MEDIASERVER_SCHEDULED = "mediaserver_scheduled";

        public static final String VIP_ENABLED = "vip_enabled";
        public static final String VIP_DEFAULT_SAVER = "default_saver";
        public static final String VIP_SCREEN_OFF = "enable_on_screen_off";
        public static final String VIP_CHANGE_GOV = "change_gov";
        public static final String VIP_GOV = "governor";
        public static final String VIP_FORCE_STOP = "force_stop_enabled";
        public static final String VIP_AUTO_TURN_ON = "auto_turn_on_enabled";
        public static final String VIP_DISABLE_DATA = "disable_data_enabled";
        public static final String VIP_DISABLE_SYNC = "disable_sync_enabled";
        public static final String VIP_DISABLE_SYNC_LESS = "disable_sync_enabled_less";
        public static final String VIP_DISABLE_BLUETOOTH = "disable_bluetooth_enabled";
        public static final String VIP_DISABLE_BLUETOOTH_LESS = "disable_bluetooth_enabled_less";
        public static final String VIP_GRAYSCALE = "grayscale_enabled";
        public static final String VIP_DEVICE_IDLE = "device_idle_enabled";
        public static final String VIP_SMART_PIXELS = "smart_pixels_enabled";
        public static final String VIP_PERCENTAGE = "percentage";
        public static final String VIP_PERCENTAGE_LESS = "percentage_less";
        public static final String VIP_AUTO_TURN_ON_SELECTION = "percentage_selection";
        public static final String VIP_PERCENTAGE_SELECTION_LESS = "percentage_selection_less";
        public static final String VIP_DISABLE_WHEN_CHARGING = "disable_when_connecting";
        public static final String VIP_SHOULD_STILL_ACTIVATE = "should_still_activate_automatically";
        public static final String VIP_CHANGE_BRIGHTNESS = "change_brightness";
        public static final String VIP_BRIGHTNESS_LEVEL_ENABLED = "brightness_level_enable";
        public static final String VIP_BRIGHTNESS_LEVEL_ENABLED_LESS = "brightness_level_enable_less";
        public static final String VIP_BRIGHTNESS_LEVEL_DISABLED = "brightness_level_disable";
        public static final String VIP_BRIGHTNESS_LEVEL_DISABLED_LESS = "brightness_level_disable_less";
        public static final String VIP_IS_SCHEDULED = "is_service_scheduled";
        public static final String VIP_AUTO_TURN_ON_LESS = "auto_turn_on_enabled_less";
        public static final String VIP_DISABLE_WIFI_LESS = "disable_wifi_enabled_less";
        public static final String VIP_ALLOW_STATS_COLLECTION = "allow_stats_collection";

        public static final String GB_ENABLED = "gb_enabled";
        public static final String GB_CLEAR_CACHES = "clear_cache_enabled";
        public static final String GB_CHANGE_LMK = "change_lmk_params";
        public static final String GB_CHANGE_GOV = "change_gov";
        public static final String GB_GOV = "governor";
        public static final String GB_CUSTOM_LMK_PARAMS = "lmk_param";
        public static final String GB_LMK_PROFILE_SELECTION = "lmk_profile_selection";
        public static final String GB_FORCE_STOP = "force_stop_enabled";
        public static final String GB_IS_SCHEDULED_LESS = "is_game_job_scheduled";
        public static final String GB_DND = "dnd_enabled";
        public static final String GB_CHANGE_BRIGHTNESS = "change_brightness";
        public static final String GB_BRIGHTNESS_LEVEL_ENABLED = "brightness_level_enable";
        public static final String GB_BRIGHTNESS_LEVEL_DISABLED = "brightness_level_disable";
        public static final String DNS_SPINNER_SELECTION = "dns_selection";
        public static final String DNS_ON_BOOT = "dns_on_boot";
        public static final String DNS_1 = "dns1";
        public static final String DNS_2 = "dns2";
        public static final String CPU_ON_BOOT = "cpu_on_boot";
        public static final String CPU_MIN_FREQ = "cpu_min_freq";
        public static final String CPU_MAX_FREQ = "cpu_max_freq";
        public static final String CPU_GOV = "cpu_governor";
        public static final String NET_GOOGLE_DNS = "google_dns";
        public static final String NET_HOSTNAME = "hostname";
        public static final String NET_TCP = "tcp_tweaks";
        public static final String NET_SIGNAL = "onSinal";
        public static final String NET_BUFFERS = "net_buffers";
        public static final String NET_STREAM_TWEAKS = "stream_tweaks";
        public static final String NET_IPV6_STATE = "ipv6_state";
        public static final String ENTROPY_SPINNER_SELECTION = "entropy_profiles";
        public static final String ENTROPY_READ_THRESHOLD = "entropy_read";
        public static final String ENTROPY_WRITE_THRESHOLD = "entropy_write";
        public static final String ENTROPY_ON_BOOT = "entropy_on_boot";
        public static final String ENTROPY_ADD_RANDOM = "entropy_add_random";
        public static final String ENTROPY_MIN_RESEED_SECS = "entropy_min_reseed_secs";
        public static final String KERNEL_OPTIONS_ON_BOOT = "kernelOptions";
        public static final String FSTRIM_SYSTEM = "fstrim_system";
        public static final String FSTRIM_DATA = "fstrim_data";
        public static final String FSTRIM_CACHE = "fstrim_cache";
        public static final String FSTRIM_SCHEDULE_MINUTES = "schedule_fstrim_interval";
        public static final String FSTRIM_SPINNER_SELECTION = "fstrim_spinner_selection";
        public static final String FSTRIM_SCHEDULED = "fstrim_scheduled";
        public static final String FSTRIM_ON_BOOT = "fstrimOnBoot";
        public static final String DOZE_AGGRESSIVE = "aggressive_doze";
        public static final String DOZE_CHARGER = "aggressive_doze_disable_charger";
        public static final String DOZE_SCREEN_OFF = "aggressive_doze_screen_off";
        public static final String DOZE_IS_DOZE_SCHEDULED = "is_doze_service_scheduled";
        public static final String DOZE_IDLING_MODE = "doze_idling_mode";
        public static final String DOZE_INTERVAL_MINUTES = "doze_waiting_interval";
        public static final String DOZE_IS_IN_IDLE = "doze_is_in_idle";
        public static final String BATTERY_PSFMC = "onPSFMC";
        public static final String BATTERY_LOW_RAM_FLAG = "low_ram_device_flag";
        public static final String BATTERY_IMPROVE = "onBtt";
        public static final String PERFORMANCE_PERF_TWEAK = "onPf";
        public static final String PERFORMANCE_MULTITASKING = "onMulti";
        public static final String PERFORMANCE_LS_UI = "onLs";
        public static final String PERFORMANCE_SCROLLING = "onRolar";
        public static final String PERFORMANCE_FPS_UNLOCKER = "fps_unlocker";
        public static final String PERFORMANCE_GPU = "onGPU";
        public static final String PERFORMANCE_RENDERING = "onRender";
        public static final String PERFORMANCE_CALL_RING = "onRing";
        public static final String TOOLS_ZIPALIGN = "zipalign";
        public static final String TOOLS_LOGCAT = "onLog";
        public static final String TOOLS_KERNEL_PANIC = "kernel_panic";
        public static final String LESS_GAME_BOOSTER = "game_booster_less";
        public static final String LESS_AUTO_OPT = "auto_optimizer_less";
        public static final String LESS_AUTO_OPT_PER = "auto_optimizer_less_per";
        public static final String ACHIEVEMENT_SET = "achievement_set";
        public static final String QUICK_PROFILE = "quick_profile";
        public static final String QUICK_PROFILE_LESS = "quick_profile_less";
        public static final String VM_ON_BOOT = "__vm_apply_on_boot";
        public static final String VM_DISK_SIZE_MB = "__vm_disk_size";
        public static final String SHOW_PUSH_NOTIFICATIONS = "show_push_notifications";
    }
}
