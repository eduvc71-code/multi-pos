package com.multipos.app.ui.pos 
 
import android.os.Bundle 
import android.view.LayoutInflater 
import android.view.View 
import android.view.ViewGroup 
import androidx.fragment.app.Fragment 
import com.multipos.app.R 
 
class PosFragment : Fragment() { 
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { 
        return inflater.inflate(R.layout.fragment_pos, container, false) 
    } 
} 
