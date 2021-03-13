
package com.androidvip.hebf.fragments;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidvip.hebf.R;
import com.androidvip.hebf.helpers.CPUDatabases;
import com.androidvip.hebf.utils.FileUtils;
import com.androidvip.hebf.utils.K;
import com.androidvip.hebf.utils.RootUtils;
import com.androidvip.hebf.utils.Utils;
import com.google.android.material.navigation.NavigationView;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.collection.ArrayMap;

import static android.content.Context.ACTIVITY_SERVICE;

public class DeviceInfoFragment extends BaseFragment {
    private ImageView imgDevice, imgMemory, imgCPU, imgKernel, imgSDK, imgHardware;
    private TextView txtDevice, txtMemory, txtCPU, txtKernel, memoria, cpu, kernel, device;
    private View infoCard;
    private ActivityManager.MemoryInfo memoryInfo;
    private ActivityManager activityManager;
    private Toast stepsToUnlock;
    private static int cont = 0;
    private long totalMem = 0;

    public DeviceInfoFragment() {

    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_device_info, container, false);
        bindViews(view);

        boolean hasUnlockedAdvancedOptions = getUserPrefs().getStringSet(
                K.PREF.ACHIEVEMENT_SET, new HashSet<>()
        ).contains("advanced");

        imgDevice.setOnClickListener(mostrarMenosListener(txtDevice));
        imgMemory.setOnClickListener(mostrarMaisListener(txtMemory));
        imgCPU.setOnClickListener(mostrarMaisCPU(txtCPU));
        imgKernel.setOnClickListener(mostrarMaisListener(txtKernel));

        new Thread(() -> {
            final String cpuInfo = FileUtils.readMultilineFile("/proc/cpuinfo", "error");
            final String kernelVersion = FileUtils.readMultilineFile("/proc/version", "Linux Kernel");
            String hardware = getHardware();
            if (isActivityAlive()) {
                requireActivity().runOnUiThread(() -> {
                    if (cpuInfo.toLowerCase().contains("error"))
                        txtCPU.setText(FileUtils.readMultilineFile("/proc/cpuinfo", "Detailed CpuManager Info could not be shown"));
                    else
                        txtCPU.setText(cpuInfo);

                    txtKernel.setText(kernelVersion);
                    try {
                        kernel.setText(kernelVersion.split("-")[1].trim().split("\\(")[0].trim() + " " + kernelVersion.split("-")[0].trim().replace("Linux", "").replace("version", ""));
                    } catch (Exception ignored) {}

                    switch (Build.VERSION.SDK_INT) {
                        case 16: case 17: case 18:
                            imgSDK.setImageResource(R.drawable.ic_sdk_16); break;
                        case 19:
                            imgSDK.setImageResource(R.drawable.ic_sdk_19); break;
                        case 21: case 22:
                            imgSDK.setImageResource(R.drawable.ic_sdk_21); break;
                        case 23:
                            imgSDK.setImageResource(R.drawable.ic_sdk_23); break;
                        case 24: case 25:
                            imgSDK.setImageResource(R.drawable.ic_sdk_24); break;
                        case 26: case 27:
                            imgSDK.setImageResource(R.drawable.ic_sdk_26); break;
                        case 28:
                            imgSDK.setImageResource(R.drawable.ic_sdk_28); break;
                        case 29:
                            imgSDK.setImageResource(R.drawable.ic_sdk_29); break;
                        default:
                            imgSDK.setImageResource(android.R.drawable.sym_def_app_icon); break;
                    }

                    device.setText(Build.BRAND + " " + Build.MODEL);

                    for (ArrayMap.Entry<String, String> processorEntry : CPUDatabases.INSTANCE.getData().entrySet()) {
                        String key = processorEntry.getKey();
                        if (hardware.toLowerCase().contains(key.toLowerCase())) {
                            cpu.setText(processorEntry.getValue());
                            break;
                        } else{
                            cpu.setText(hardware.trim());
                        }
                    }
                    if (hardware.contains("Qualcomm") || hardware.startsWith("sdm") || hardware.startsWith("msm")) {
                        imgHardware.setImageResource(R.drawable.ic_hardware_snapdragon);
                    } else {
                        if (hardware.toUpperCase().startsWith("MT")) {
                            imgHardware.setImageResource(R.drawable.ic_hardware_mtk);
                        } else {
                            if (hardware.contains("exynos")) {
                                imgHardware.setImageResource(R.drawable.ic_hardware_exynos);
                            } else {
                                imgHardware.setVisibility(View.GONE);
                            }
                        }
                    }

                    txtDevice.setText("Model: " + Build.MODEL +"\n" +
                            "Device: " + Build.DEVICE + "\n" +
                            "Manufactured by: " + Build.MANUFACTURER + "\n" +
                            "Brand: " + Build.BRAND + "\n" +
                            "Bootloader: " + Build.BOOTLOADER + "\n" +
                            "Device hardware: " + Build.HARDWARE + "\n" +
                            "Android " + Build.VERSION.RELEASE + " SDK " + Build.VERSION.SDK_INT);
                });
            }
        }).start();

        infoCard.setOnClickListener(view1 -> {
            cont += 1;
            String cont_s = Integer.toString(10-cont);
            String text = String.format(getResources().getString(R.string.advanced_toast_cont), cont_s);
            if (!hasUnlockedAdvancedOptions) {
                if (cont > 2 && cont <= 8) {
                    if (stepsToUnlock == null) {
                        stepsToUnlock = Toast.makeText(getContext(), "", Toast.LENGTH_SHORT);
                        stepsToUnlock.setText(text);
                        stepsToUnlock.show();
                    }
                }
                if (cont == 9){
                    stepsToUnlock.cancel();
                    final Dialog dialog = new Dialog(findContext());
                    dialog.setContentView(R.layout.dialog_charada);
                    dialog.setTitle("Puzzle");
                    dialog.setCancelable(false);
                    Button b01 = dialog.findViewById(R.id.charada_wrong);
                    Button b02 = dialog.findViewById(R.id.charada_wrong_2);
                    Button b03 = dialog.findViewById(R.id.charada_correct);
                    dialog.show();
                    b01.setOnClickListener(v -> {dialog.dismiss();
                        cont = 0;
                        Toast.makeText(getContext(), getString(R.string.advanced_fail), Toast.LENGTH_SHORT).show();
                    });
                    b02.setOnClickListener(v -> {dialog.dismiss();
                        cont = 0;
                        Toast.makeText(getContext(), getString(R.string.advanced_fail), Toast.LENGTH_SHORT).show();
                    });
                    b03.setOnClickListener(v -> {
                        Set<String> achievementsSet = getUserPrefs().getStringSet(K.PREF.ACHIEVEMENT_SET, new HashSet<>());
                        if (!achievementsSet.contains("advanced")) {
                            Utils.addAchievement(getApplicationContext(), "advanced");
                            Toast.makeText(findContext(), getString(R.string.achievement_unlocked, getString(R.string.advanced_toast)), Toast.LENGTH_LONG).show();
                        }
                        getUserPrefs().putBoolean("unlocked_advanced_options", true);
                        dialog.dismiss();

                        NavigationView navigationView = requireActivity().findViewById(R.id.navigationView);
                        navigationView.getMenu().clear();
                        navigationView.inflateMenu(R.menu.drawer_advanced);
                        navigationView.setCheckedItem(R.id.nav_settings);
                    });
                }
            }
        });

        setUpHandler();

        return view;
    }

    private void bindViews(View view) {
        imgDevice = view.findViewById(R.id.device_mais);
        imgMemory = view.findViewById(R.id.memoria_mais);
        imgCPU = view.findViewById(R.id.cpu_mais);
        imgKernel = view.findViewById(R.id.kernel_mais);

        imgSDK = view.findViewById(R.id.img_device_info_sdk);
        imgHardware = view.findViewById(R.id.img_device_info_hardware);

        txtDevice = view.findViewById(R.id.info_device);
        txtMemory = view.findViewById(R.id.info_memory);
        txtCPU = view.findViewById(R.id.info_cpu);
        txtKernel = view.findViewById(R.id.info_kernel);

        memoria = view.findViewById(R.id.memoria_resumo);
        cpu = view.findViewById(R.id.cpu_resumo);
        kernel = view.findViewById(R.id.kernel_resumo);
        device = view.findViewById(R.id.device_resumo);

        infoCard = view.findViewById(R.id.cv_unlock_advanced_options);
    }

    @WorkerThread
    private String getHardware() {
        String cpuInfoHardware = RootUtils.executeSync("cat /proc/cpuinfo | grep Hardware | cut -d: -f2", "").trim();
        return TextUtils.isEmpty(cpuInfoHardware) ? Build.HARDWARE : cpuInfoHardware;
    }

    private View.OnClickListener mostrarMaisListener(final TextView textView) {
        return v -> {
            textView.setVisibility(View.VISIBLE);
            imgSDK.setVisibility(View.VISIBLE);
            ((ImageView) v).setImageResource(R.drawable.ic_up);
            v.setOnClickListener(mostrarMenosListener(textView));
        };
    }

    private View.OnClickListener mostrarMenosListener(final TextView textView) {
        return v -> {
            textView.setVisibility(View.GONE);
            if (textView == txtDevice)
                imgSDK.setVisibility(View.GONE);
            ((ImageView) v).setImageResource(R.drawable.ic_down);
            v.setOnClickListener(mostrarMaisListener(textView));
        };
    }

    private View.OnClickListener mostrarMaisCPU(final TextView textView) {
        return v -> {
            textView.setVisibility(View.VISIBLE);
            imgHardware.setVisibility(View.VISIBLE);
            ((ImageView) v).setImageResource(R.drawable.ic_up);
            v.setOnClickListener(mostrarMenosCPU(textView));
        };
    }

    private View.OnClickListener mostrarMenosCPU(final TextView textView) {
        return v -> {
            textView.setVisibility(View.GONE);
            imgHardware.setVisibility(View.GONE);
            ((ImageView) v).setImageResource(R.drawable.ic_down);
            v.setOnClickListener(mostrarMaisCPU(textView));
        };
    }

    private void setUpHandler() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                getMemInfo();
                handler.postDelayed(this, 1600);
            }
        }, 1600);
    }

    private void getMemInfo() {
        Thread task = new Thread(() -> {
            final String mem = FileUtils.readMultilineFile("/proc/meminfo", "Detailed memory info could not be shown");
            long usedMemory;
            try {
                memoryInfo = new ActivityManager.MemoryInfo();
                activityManager = (ActivityManager) findContext().getSystemService(ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(memoryInfo);
                totalMem = (memoryInfo.totalMem / 1048567);
                usedMemory = (totalMem - (memoryInfo.availMem / 1048567));

            } catch (Exception e) {
                usedMemory = 0;
                totalMem = 0;
            }
            if (getActivity() != null) {
                long finalUsedMemory = usedMemory;
                getActivity().runOnUiThread(() -> {
                    if (!mem.toLowerCase().contains("error"))
                        txtMemory.setText(mem);
                    memoria.setText(finalUsedMemory + "MB / " + totalMem + "MB");
                });
            }
        });
        task.start();
    }
}