package com.boilerplate.network.utils

import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

object NetworkUtils {
    fun <T : Any> deepEquals(obj1: T, obj2: T): Boolean {

        kotlin.runCatching {

            if (obj1 != obj2) return false
            // Get the properties of the objects using reflection
            val properties1 = obj1::class.declaredMemberProperties
            val properties2 = obj2::class.declaredMemberProperties

            // Check if the number of properties is the same
            if (properties1.size != properties2.size) return false

            // Iterate through the properties and compare their values
            for (property1 in properties1) {
                val property2 = properties2.find { it.name == property1.name } ?: return false
                val value1 = (property1 as? KProperty1<T, *>)?.get(obj1)
                val value2 = (property2 as? KProperty1<T, *>)?.get(obj2)

                if (property1.name == "primaryKey") continue
                if (property1.name == "playlistId") continue

                // If the properties have different values, return false
                if (value1 != value2) return false

                // If the properties are objects, recursively compare them
                if (value1 != null && value1 is Any && property1.returnType.isMarkedNullable.not()) {
                    if (!deepEquals(value1, value2 as Any)) {
                        return false
                    }
                }
            }

            // If all properties have the same values, return true
            return true
        }
        return false
    }
}