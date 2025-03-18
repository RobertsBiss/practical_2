package com.example.practical_2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class Fact(
    val id: String,
    val text: String,
    val source: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactsScreen(navController: NavController) {
    var facts by remember { mutableStateOf<List<Fact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch facts when the screen is first composed
    LaunchedEffect(key1 = true) {
        try {
            val factsList = mutableListOf<Fact>()
            // Fetch multiple facts to populate the list
            repeat(10) {
                val fact = fetchRandomFact()
                if (fact != null) {
                    factsList.add(fact)
                }
            }
            facts = factsList
            isLoading = false
        } catch (e: Exception) {
            error = "Failed to load facts: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Random Facts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                error = null
                                try {
                                    val factsList = mutableListOf<Fact>()
                                    repeat(10) {
                                        val fact = fetchRandomFact()
                                        if (fact != null) {
                                            factsList.add(fact)
                                        }
                                    }
                                    facts = factsList
                                } catch (e: Exception) {
                                    error = "Failed to refresh facts: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Facts")
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(
                    text = error ?: "Unknown error occurred",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(facts) { fact ->
                        FactCard(fact)
                    }
                }
            }
        }
    }
}

@Composable
fun FactCard(fact: Fact) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = fact.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Start
        )
        Text(
            text = "Source: ${fact.source}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

suspend fun fetchRandomFact(): Fact? = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://uselessfacts.jsph.pl/api/v2/facts/random?language=en")
        .build()

    try {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val jsonObject = JSONObject(response.body?.string())
                val id = jsonObject.getString("id")
                val text = jsonObject.getString("text")
                val source = jsonObject.optString("source", "Unknown")

                return@withContext Fact(id, text, source)
            } else {
                return@withContext null
            }
        }
    } catch (e: Exception) {
        return@withContext null
    }
}