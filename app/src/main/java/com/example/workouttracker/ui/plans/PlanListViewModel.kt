package com.example.workouttracker.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApplication
import com.example.workouttracker.data.PlanDao
import com.example.workouttracker.data.TrainingPlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanListViewModel(private val planDao: PlanDao) : ViewModel() {

    /** Null while loading, so screens don't flash an empty state. */
    val plans: StateFlow<List<TrainingPlan>?> = planDao.observePlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun create(name: String, onCreated: (Long) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { onCreated(planDao.insertPlan(TrainingPlan(name = trimmed))) }
    }

    fun toggleDefault(plan: TrainingPlan) {
        viewModelScope.launch {
            if (plan.isDefault) planDao.clearDefault() else planDao.setDefault(plan.id)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                PlanListViewModel(app.database.planDao())
            }
        }
    }
}

/** The plan the Training tab opens directly: the default plan, or the only plan. */
fun List<TrainingPlan>.autoStartPlan(): TrainingPlan? = firstOrNull { it.isDefault } ?: singleOrNull()
