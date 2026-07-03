package com.example.myapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Record(
    var recordId: Int = 0,
    var title: String? = null,
    var description: String? = null,
    var odometer: String? = null,
    var date: String? = null,
    var vehicle: String? = null,
    var entryTime: Long? = null,
    var orderBy: String? = null
) : Parcelable
