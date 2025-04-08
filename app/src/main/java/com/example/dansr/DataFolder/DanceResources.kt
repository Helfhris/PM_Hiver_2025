package com.example.dansr.DataFolder

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class InfoCard(
    @StringRes val titleResourceId: Int,
    @StringRes val descriptionResourceId: Int,
    @StringRes val urlResourceId: Int
)