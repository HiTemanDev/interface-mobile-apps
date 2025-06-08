package com.example.aplikasicapstonelaskarai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplikasicapstonelaskarai.databinding.FragmentFeedbackBinding

class FeedbackFragment : Fragment() {
    private var _binding: FragmentFeedbackBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSubmit.setOnClickListener {
            val selectedId = binding.radioGroupFeeling.checkedRadioButtonId
            val feedbackText = binding.editTextFeedback.text.toString().trim()

            if (selectedId == -1) {
                binding.textViewStatus.text = getString(R.string.feedback_error_select_option)
                return@setOnClickListener
            }

            val selectedFeeling = binding.radioGroupFeeling.findViewById<android.widget.RadioButton>(selectedId).text.toString()

            // Tampilkan feedback di UI (opsional, bisa dihapus jika tidak diperlukan)
            binding.textViewStatus.text = "Feedback: $selectedFeeling\nKomentar: $feedbackText"

            // Tampilkan pop-up "Feedback berhasil dikirim"
            showSuccessDialog()
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sukses")
            .setMessage("Feedback berhasil dikirim!")
            .setPositiveButton("OK") { _, _ ->
                // Navigasi ke MainFragment dan hapus semua backstack
                navigateToMainFragment()
            }
            .setCancelable(false) // Pop-up tidak bisa ditutup dengan tombol back
            .show()
    }

    private fun navigateToMainFragment() {
        // Hapus semua backstack hingga MainFragment
        findNavController().popBackStack(R.id.mainFragment, false)

        // Pastikan navigasi ke MainFragment
        if (findNavController().currentDestination?.id != R.id.mainFragment) {
            findNavController().navigate(R.id.mainFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}