package com.example.aplikasicapstonelaskarai

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplikasicapstonelaskarai.databinding.FragmentAudioBinding
import java.io.File

class AudioFragment : Fragment() {
    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!
    private lateinit var ttsManager: TtsManager
    private var isPlaying = false
    private var script: String = ""
    private val TAG = "AudioFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val script = arguments?.getString("script")
        val audioData = arguments?.getByteArray("audioData")
        Log.d(TAG, "Script received: $script, AudioData: ${audioData != null}")

        if (audioData == null) {
            binding.textViewStatus.text = "Tidak ada audio untuk diputar"
        } else {
            binding.textViewStatus.text = "Tekan tombol untuk memutar terapi"
            saveAudioFile(audioData) // Simpan audio ke file sementara
        }

        binding.buttonPlayPause.setOnClickListener {
            if (isPlaying) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                binding.buttonPlayPause.setImageResource(R.drawable.play)
                binding.textViewStatus.text = "Audio dihentikan. Tekan untuk memutar lagi."
                isPlaying = false
            } else {
                if (audioData != null) {
                    mediaPlayer = MediaPlayer().apply {
                        context?.let { it1 -> setDataSource(it1, Uri.fromFile(File(context?.cacheDir, "therapy_audio.wav"))) }
                        prepare()
                        start()
                    }
                    binding.buttonPlayPause.setImageResource(R.drawable.stop)
                    binding.textViewStatus.text = "Memutar terapi..."
                    isPlaying = true
                    mediaPlayer?.setOnCompletionListener {
                        binding.buttonPlayPause.setImageResource(R.drawable.play)
                        binding.textViewStatus.text = "Audio selesai. Tekan untuk memutar lagi."
                        isPlaying = false
                    }
                } else {
                    binding.textViewStatus.text = "Tidak ada audio untuk diputar"
                }
            }
        }

        binding.buttonFeedback.setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            binding.buttonPlayPause.setImageResource(R.drawable.play)
            isPlaying = false
            binding.textViewStatus.text = "Audio dihentikan."
            findNavController().navigate(R.id.action_audioFragment_to_feedbackFragment)
        }
    }

    private fun saveAudioFile(audioData: ByteArray) {
        val file = File(context?.cacheDir, "therapy_audio.wav")
        file.writeBytes(audioData)
    }

    private var mediaPlayer: MediaPlayer? = null

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }


}