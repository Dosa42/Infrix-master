package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.UserRole

class RoleConverters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String {
        return role.code
    }

    @TypeConverter
    fun toUserRole(code: String): UserRole {
        return UserRole.fromString(code)
    }
}
