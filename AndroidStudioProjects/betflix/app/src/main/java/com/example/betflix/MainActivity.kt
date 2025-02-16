package com.example.betflix


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.betflix.ui.theme.BetflixTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GlobalVars {
    var email: String = ""
}
object AppInfo {
    val appPackageNames = listOf(
        "com.google.android.youtube",  // YouTube
        "com.instagram.android",       // Instagram
        "com.zhiliaoapp.musically",    // TikTok
        "com.snapchat.android"         // Snapchat
    )
}
object Transaction {

}

data class PendingTransaction(
    val sender_email: String,
    val target_email: String,
    val value: Int,
    val app: String,
    val accept: String
)

fun parseJson(jsonString: String): PendingTransaction {
    // Remove the curly braces and any extra spaces
    val cleanedJson = jsonString.trim().removeSurrounding("{", "}")

    // Split by commas to separate each key-value pair
    val keyValuePairs = cleanedJson.split(",")

    // Create a map of key-value pairs by parsing each pair
    val jsonMap = keyValuePairs.map {
        val (key, value) = it.split(":").map { it.trim().removeSurrounding("\"") }
        key to value
    }.toMap()

    // Extract values from the map and convert "value" to an integer
    return PendingTransaction(
        sender_email = jsonMap["sender_email"] ?: "",
        target_email = jsonMap["target_email"] ?: "",
        value = jsonMap["value"]?.toInt() ?: 0, // Convert value to Int
        app = jsonMap["app"] ?: "",
        accept = jsonMap["accept"] ?: ""
    )
}


object pending {
    val pendingTransactions = ArrayList<PendingTransaction>()
}

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BetflixTheme {
                var showLoginPopup by remember { mutableStateOf(true) }
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                val coroutineScope = rememberCoroutineScope()

                if (showLoginPopup) {
                    LoginPopup(
                        onLogin = { enteredUsername, enteredPassword ->
                            username = enteredUsername
                            GlobalVars.email = enteredUsername
                            password = enteredPassword
                            showLoginPopup = false
                            // Send credentials to the server
                            coroutineScope.launch {
                                sendCredentialsToServer(enteredUsername, enteredPassword)
                            }
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {
                            Text(
                                text = "Betflix",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            val lastTransaction = pending.pendingTransactions.lastOrNull()
                            if (lastTransaction != null) {
                                Text(
                                    text = "Transactions:\n {${lastTransaction.value.toString()}, ${lastTransaction.target_email.toString()}, ${lastTransaction.sender_email.toString()} }",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),

                                    )
                            }
                            BetForm(onSendBet = { friendEmail, wagerAmount, selectedApp ->
                                // Handle the bet submission logic
                                println("Bet Sent: Friend: $friendEmail, Wager: $wagerAmount, App: $selectedApp")
                            }, context = this@MainActivity)
                            ScreenTimeDisplay(
                                modifier = Modifier.padding(innerPadding),
                                context = this@MainActivity // Pass the context here
                            )
                        }
                    }
                }
                val youtubeTime = getYouTubeScreenTime(this)
                Text(text = "YouTube Time: $youtubeTime ms")
            }
        }
    }
    private fun sendCredentialsToServer(username: String, password: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // Set connection timeout
            .readTimeout(30, TimeUnit.SECONDS)    // Set read timeout
            .writeTimeout(30, TimeUnit.SECONDS)   // Set write timeout
            .build()

        val jsonBody = """
            {
                "email": "$username",
                "password": "$password"
            }
        """.trimIndent()
        val requestBody: RequestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(), jsonBody
        )

        val request: Request = Request.Builder()
            .url("http://10.194.66.156:8000/login") // Replace with your server's URL
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle network errors
                Log.e("MainActivity", "Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle server response
                if (response.isSuccessful) {
                    Log.d("MainActivity", "Credentials sent successfully")
                } else {
                    Log.e("MainActivity", "Server error: ${response.code}")
                }
            }
        })
    }


}

fun getAppScreenTime(context: Context, appPackage: String): Long {
    val usageStatsManager =
        (context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager)
            ?: return 0L

    val currentTime = System.currentTimeMillis()
    val startTime = currentTime - 24 * 60 * 60 * 1000 // Last 24 hours

    val usageStatsList = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY, startTime, currentTime
    )

    val appUsage = usageStatsList.find { it.packageName == appPackage }?.totalTimeInForeground ?: 0L

    return appUsage // Time in milliseconds
}
//@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun getYouTubeScreenTime(context: Context): Long {
    val usageStatsManager =
        (context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager)
            ?: return 0L

    val currentTime = System.currentTimeMillis()
    val startTime = currentTime - 24 * 60 * 60 * 1000 // Last 24 hours

    val usageStatsList = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY, startTime, currentTime
    )

    if (usageStatsList.isNullOrEmpty()) {
        Log.e("YouTubeScreenTime", "No usage stats available.")
        return 0L
    }

    val youtubePackage = "com.google.android.youtube"
    val youtubeUsage = usageStatsList.find { it.packageName == youtubePackage }?.totalTimeInForeground ?: 0L

    return youtubeUsage // Time in milliseconds
}
fun sendBetToServer(target_email: String, wager_amount: String, app: String) {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    val em = GlobalVars.email
    val jsonBody = """
        {
            "sender_email": "$em",
            "target_email": "$target_email",
            "value": $wager_amount,
            "app": "$app"
        }
        """.trimIndent()
    val requestBody: RequestBody = RequestBody.create(
        "application/json".toMediaTypeOrNull(), jsonBody
    )
    val request: Request = Request.Builder()
        .url("http://10.194.66.156:8000/propose-transaction") // Replace with your server's URL
        .post(requestBody)
        .addHeader("Content-Type", "application/json")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("HTTP_ERROR", "Request failed: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("HTTP_SUCCESS", "Response: $responseBody")
            } else {
                Log.e("HTTP_ERROR", "Failed to send: ${response.code}")
            }
        }
    })
}
fun sendScreenTimeToServer(screenTime: Long, app: String) {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    val em = GlobalVars.email
    val jsonBody = """
            {
                "email":"$em",
                "screentime": $screenTime,
                "app":"$app"
                
            }
        """.trimIndent()

    val requestBody: RequestBody = RequestBody.create(
        "application/json".toMediaTypeOrNull(), jsonBody
    )


    val request: Request = Request.Builder()
        .url("http://10.194.66.156:8000/screentime") // Replace with your server's URL
        .post(requestBody)
        .addHeader("Content-Type", "application/json")
        .build()
//    val re
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("MainActivity", "Error sending screen time: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                // Get the JSON response as a string
                val jsonResponse = response.body?.string() ?: "{}" // Default to empty JSON if null

                // Log the raw JSON response
                Log.d("MainActivity", "Screen time sent successfully, response: $jsonResponse")

                // You can now parse the JSON if needed
                try {
                    val jsonObject = JSONObject(jsonResponse) // Requires org.json.JSONObject

                    // For example, extract a value from the JSON response
                    val someValue = jsonObject.optString("key") // Replace "key" with actual key

                    // Log the extracted value
                    Log.d("MainActivity", "Extracted value: $someValue")
                } catch (e: JSONException) {
                    Log.e("MainActivity", "Failed to parse JSON: ${e.message}")
                }
                Log.d("MainActivity", "Screen time sent successfully")
            } else {
                Log.e("MainActivity", "Failed to send screen time: ${response.code}")
            }
        }
    })
}

fun getTransactionsFromServer() {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    val em = GlobalVars.email


    val request: Request = Request.Builder()
        .url("http://10.194.66.156:8000/screentime") // Replace with your server's URL
        .get()
        .addHeader("Content-Type", "application/json")
        .build()
//    val re
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("MainActivity", "Error sending screen time: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                // Get the JSON response as a string
                val jsonResponse = response.body?.string() ?: "{}" // Default to empty JSON if null

                // Log the raw JSON response
                Log.d("MainActivity", "Screen time sent successfully, response: $jsonResponse")

                // You can now parse the JSON if needed
                try {
                    val jsonObject = JSONObject(jsonResponse) // Requires org.json.JSONObject

                    // For example, extract a value from the JSON response
                    val someValue = jsonObject.optString("email") // Replace "key" with actual key

                    // Log the extracted value
                    Log.d("MainActivity", "Extracted value: $someValue")
                } catch (e: JSONException) {
                    Log.e("MainActivity", "Failed to parse JSON: ${e.message}")
                }
                Log.d("MainActivity", "Screen time sent successfully")
            } else {
                Log.e("MainActivity", "Failed to send screen time: ${response.code}")
            }
        }
    })
}
@Composable
fun BetForm(onSendBet: (String, String, String) -> Unit, context: Context) {
    var friendEmail by remember { mutableStateOf("") }
    var wagerAmount by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val youtubePackage = "com.google.android.youtube"
    val instagramPackage = "com.instagram.android"

    val apps = listOf(
         youtubePackage,
        instagramPackage
    )
    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = friendEmail,
            onValueChange = { friendEmail = it },
            label = { Text("Friend's Email") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = wagerAmount,
            onValueChange = { wagerAmount = it },
            label = { Text("Wager Amount") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.clickable { expanded = true }) {
            Text(text = selectedApp.ifEmpty { "Select App" })
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            apps.forEach { app ->
                DropdownMenuItem(
                    text = { Text(app) },
                    onClick = {
                        selectedApp = app
                        expanded = false
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onSendBet(friendEmail, wagerAmount, selectedApp) }) {
            Text("Send Bet")
            LaunchedEffect(friendEmail, wagerAmount, selectedApp) {
                sendBetToServer(friendEmail, wagerAmount, selectedApp)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPopup(onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* Do nothing on dismiss */ },
        title = { Text("Login") },
        text = {
            Column {
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") }
                )
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onLogin(username, password) }) {
                Text("Login")
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@Composable
fun ScreenTimeDisplay(modifier: Modifier = Modifier, context: Context) {
    var totalScreenTime by remember { mutableStateOf(0L) }
    var isPermissionGranted by remember { mutableStateOf(false) }
    var appUsageBreakdown by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(key1 = true) {
        val usageStats = getAppUsageStats(context) // Use the passed context
        if (usageStats.isNotEmpty()) {
            isPermissionGranted = true
            totalScreenTime = calculateTotalScreenTime(usageStats)
            appUsageBreakdown = getAppUsageBreakdown(context, usageStats)

            // Send screen time to server every minute
            coroutineScope.launch {
                while (true) {
                    sendScreenTimeToServer(totalScreenTime, "com.google.android.youtube")
                    sendScreenTimeToServer(totalScreenTime, "com.instagram.android")
                    sendScreenTimeToServer(totalScreenTime, "com.google.android.youtube")
                    delay(10_000)  // 1 minute delay
                }
            }
        } else {
            isPermissionGranted = false
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        if (isPermissionGranted) {
            val hours = TimeUnit.MILLISECONDS.toHours(totalScreenTime)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalScreenTime) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(totalScreenTime) % 60

            Text(
                text = "Weekly Screen Time:",
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$hours hours, $minutes minutes, $seconds seconds",
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.padding(8.dp))

            Text(
                text = "App Usage Breakdown:",
                color = MaterialTheme.colorScheme.onBackground
            )
            LazyColumn {
                items(appUsageBreakdown.toList()) { (packageName, timeInForeground) ->
                    val appHours = TimeUnit.MILLISECONDS.toHours(timeInForeground)
                    val appMinutes = TimeUnit.MILLISECONDS.toMinutes(timeInForeground) % 60
                    val appSeconds = TimeUnit.MILLISECONDS.toSeconds(timeInForeground) % 60
                    Text(
                        text = "$packageName: $appHours hours, $appMinutes minutes, $appSeconds seconds",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        } else {
            Text(text = "Usage access permission not granted.", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}









