package com.rench.kvartstone.ui.heroselection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rench.kvartstone.data.repositories.HeroPowerRepository
import com.rench.kvartstone.domain.HeroPower
import kotlinx.coroutines.launch

class HeroPowerSelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val heroPowerRepository = HeroPowerRepository(application)

    private val _heroPowers = MutableLiveData<List<HeroPower>>()
    val heroPowers: LiveData<List<HeroPower>> = _heroPowers

    private val _selectedHeroPower = MutableLiveData<HeroPower?>()
    val selectedHeroPower: LiveData<HeroPower?> = _selectedHeroPower

    fun loadAvailableHeroPowers() {
        viewModelScope.launch {

            heroPowerRepository.refreshHeroPowers()

            heroPowerRepository.allHeroPowers.collect { powers ->
                _heroPowers.value = powers
            }
        }
    }

    fun selectHeroPower(heroPower: HeroPower) {
        _selectedHeroPower.value = heroPower
    }
}
