package com.example.betflix

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import java.util.Calendar
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun getAppUsageStats(context: Context): List<UsageStats> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()
    val endTime = calendar.timeInMillis
    calendar.add(Calendar.WEEK_OF_YEAR, -1) // Go back one week
    val startTime = calendar.timeInMillis

    if (!hasUsageStatsPermission(context)) {
        requestUsageStatsPermission(context)
        return emptyList()
    }

    val usageStatsList = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_WEEKLY,
        startTime,
        endTime
    )

    return usageStatsList
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun requestUsageStatsPermission(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun calculateTotalScreenTime(usageStatsList: List<UsageStats>): Long {
    var totalTime = 0L
    for (usageStats in usageStatsList) {
        totalTime += usageStats.totalTimeInForeground
    }
    return totalTime
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun getAppUsageBreakdown(context: Context, usageStatsList: List<UsageStats>): Map<String, Long> {
    val appUsageMap = mutableMapOf<String, Long>()
    val packageManager = context.packageManager

    for (usageStats in usageStatsList) {
        try {
            val appInfo = packageManager.getApplicationInfo(usageStats.packageName, 0)
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                appUsageMap[usageStats.packageName] = usageStats.totalTimeInForeground
            } else {
                //anything to add??
            }
        } catch (e: PackageManager.NameNotFoundException) {
            //anything to add??
        }
    }
    return appUsageMap
}