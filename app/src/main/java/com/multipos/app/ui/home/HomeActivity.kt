package com.multipos.app.ui.home 
 
import android.os.Bundle 
import androidx.appcompat.app.AppCompatActivity 
import com.multipos.app.R 
import com.multipos.app.databinding.ActivityHomeBinding
import com.multipos.app.ui.inventory.InventoryFragment
import com.multipos.app.ui.pos.PosFragment
import com.multipos.app.ui.history.HistoryFragment
import com.multipos.app.ui.clients.ClientsFragment
 
class HomeActivity : AppCompatActivity() { 
    override fun onCreate(savedInstanceState: Bundle?) { 
        super.onCreate(savedInstanceState) 
        val binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) supportFragmentManager.beginTransaction().replace(R.id.homeContainer, PosFragment()).commit()
        binding.btnSales.setOnClickListener { supportFragmentManager.beginTransaction().replace(R.id.homeContainer, PosFragment()).commit() }
        binding.btnInventory.setOnClickListener { supportFragmentManager.beginTransaction().replace(R.id.homeContainer, InventoryFragment()).commit() }
        binding.btnHistory.setOnClickListener { supportFragmentManager.beginTransaction().replace(R.id.homeContainer, HistoryFragment()).commit() }
        binding.btnClients.setOnClickListener { supportFragmentManager.beginTransaction().replace(R.id.homeContainer, ClientsFragment()).commit() }
    } 
} 
