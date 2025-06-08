package com.example.aplikasicapstonelaskarai

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.aplikasicapstonelaskarai.databinding.FragmentPembukaanBinding

class PembukaanFragment : Fragment() {

    private var _binding: FragmentPembukaanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPembukaanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.hide()

        binding.buttonpembukaan.setOnClickListener {
            // Tampilkan animasi
            binding.lottieloading.visibility = View.VISIBLE

            // Delay selama 1-2 detik sebelum navigasi
            Handler(Looper.getMainLooper()).postDelayed({
                val navOptions = navOptions {
                    popUpTo(R.id.pembukaanFragment) {
                        inclusive = true
                    }
                }
                findNavController().navigate(
                    R.id.action_pembukaanFragment_to_mainFragment,
                    null,
                    navOptions
                )
            }, 1500) // 2000 ms = 2 detik
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
}

