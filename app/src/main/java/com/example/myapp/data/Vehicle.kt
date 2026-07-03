package com.example.myapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Vehicle(
    var vehicleId: Int = 0,
    var year: String? = null,
    var make: String? = null,
    var model: String? = null,
    var submodel: String? = null,
    var engine: String? = null,
    var notes: String? = null,
    var entryTime: Long? = null
) : Parcelable {
    fun vehicleTitle(): String {
        return "$year $make $model $submodel"
    }
}
