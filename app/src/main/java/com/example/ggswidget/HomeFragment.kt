package com.example.ggswidget

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
        val settingsBtn = rootView.findViewById<FloatingActionButton>(R.id.fab_settings)
        settingsBtn.setOnClickListener{
            findNavController().navigate(R.id.settingsFragment)
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val fab = view.findViewById<FloatingActionButton>(R.id.fab_settings)
        fab.setOnClickListener{
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }

        loadSettings()
    }

    private fun loadSettings() {
        val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

}