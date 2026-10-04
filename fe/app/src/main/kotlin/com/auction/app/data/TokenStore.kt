package com.auction.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "auction_session")

class TokenStore(private val context: Context) {
    private val keyToken = stringPreferencesKey("access_token")
    private val keyUserId = stringPreferencesKey("user_id")
    private val keyFullName = stringPreferencesKey("full_name")
    private val keyIsAdmin = booleanPreferencesKey("is_admin")

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[keyToken] }
    val isAdminFlow: Flow<Boolean> = context.dataStore.data.map { it[keyIsAdmin] ?: false }
    val fullNameFlow: Flow<String?> = context.dataStore.data.map { it[keyFullName] }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[keyUserId] }

    fun userIdBlocking(): String? = runBlocking { userIdFlow.first() }

    fun accessTokenBlocking(): String? = runBlocking { accessTokenFlow.first() }

    suspend fun save(token: String, userId: String, fullName: String, isAdmin: Boolean) {
        context.dataStore.edit {
            it[keyToken] = token
            it[keyUserId] = userId
            it[keyFullName] = fullName
            it[keyIsAdmin] = isAdmin
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
