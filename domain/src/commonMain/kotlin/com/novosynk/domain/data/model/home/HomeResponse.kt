package com.novosynk.domain.data.model.home

import com.novosynk.domain.data.model.home.chart.Chart
import com.novosynk.domain.data.model.mood.Mood
import com.novosynk.domain.utils.Resource

data class HomeResponse(
    val homeItem: Resource<ArrayList<HomeItem>>,
    val exploreMood: Resource<Mood>,
    val exploreChart: Resource<Chart>,
)