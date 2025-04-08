package com.example.dansr.DataFolder

import com.example.dansr.R

class Datasource() {
    fun loadAffirmations(): List<InfoCard> {
        return listOf<InfoCard>(
            InfoCard(R.string.titleResource1, R.string.descriptionResource1, R.string.urlResource1),
            InfoCard(R.string.titleResource2, R.string.descriptionResource2, R.string.urlResource2),
            InfoCard(R.string.titleResource3, R.string.descriptionResource3, R.string.urlResource3),
            InfoCard(R.string.titleResource4, R.string.descriptionResource4, R.string.urlResource4))
    }
}